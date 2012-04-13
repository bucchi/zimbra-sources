#!/usr/bin/perl
# 
# ***** BEGIN LICENSE BLOCK *****
# Zimbra Collaboration Suite Server
# Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
# 
# The contents of this file are subject to the Zimbra Public License
# Version 1.3 ("License"); you may not use this file except in
# compliance with the License.  You may obtain a copy of the License at
# http://www.zimbra.com/license.
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
# ***** END LICENSE BLOCK *****
# 

use strict;

use Getopt::Std;
use LWP::Simple;
use MIME::Lite;
use Net::SMTP;
use File::Basename;

$SIG{QUIT} = sub { my $sig = shift; &quit("KILLED", "Received signal $sig"); };
$SIG{INT} = sub { my $sig = shift; &quit("KILLED", "Received signal $sig"); };
$SIG{KILL} = sub { my $sig = shift; &quit("KILLED", "Received signal $sig"); };

my $build_id=getpwnam("build");
if ( $> != $build_id ) {
  warn "*** RUN AS build USER!\n\n";
  exit (1);
}

my %GlobalOpts = ();
my %config = ();

$config{buildUser} = "build";
$config{buildHome} = "/home/$config{buildUser}";
$config{sshKey} = "$config{buildHome}/dfood.key";
$config{buildDir} = "$config{buildHome}/builds";
$config{confDir} = "$config{buildHome}/conf";
$config{scriptDir} = "$config{buildHome}/scripts";
$config{htmlDir} = "$config{buildHome}/httpd/htdocs";
$config{keepBuilds} = 3;

$ENV{JAVA_HOME}="/usr/local/java";
$ENV{PATH}="/usr/local/mysql/bin:/usr/local/ant/bin:$ENV{JAVA_HOME}/bin:$ENV{PATH}:/Developer/Applications/Utilities/PackageMaker.app/Contents/Resources/:/usr/local/p4/bin";

usage() unless (getopts('b:t:a:RTr:c:wH:v', \%GlobalOpts));

checkOpts();
$config{verbose} = ($GlobalOpts{v} ? 1 : 0);
$config{arch} = $GlobalOpts{a};
$config{type} = $GlobalOpts{t};
$config{branch} = $GlobalOpts{b};
$config{mainHost} = $GlobalOpts{H};
$config{dateStamp} = getDateStamp($GlobalOpts{r});
$config{release} = "$config{dateStamp}_$config{type}";
$config{buildRoot} = "$config{buildDir}/$config{arch}/$config{branch}/$config{release}";

if (-d $config{buildRoot}) {
  if (-f "$config{buildRoot}/FAILED") {
    `rm -rf $config{buildRoot}`;
  } else {
    addToReport("Build already exists!");
    exit 0;
  }
}

$config{logDir}     = "$config{buildRoot}/logs";
$config{buildLog}   = "$config{logDir}/build.log";
$config{p4Log}      = "$config{logDir}/p4.log";
$config{installLog} = "$config{logDir}/install.log";
$config{testLog}    = "$config{logDir}/test.log";
$config{changeLog}  = "$config{logDir}/change.log";
$config{bugLog}     = "$config{logDir}/bug.log";

$config{p4User}     = "build";
$config{p4Pass}     = "build1pass";
$config{p4Port}     = "depot:1666";
$config{P4} = "p4 -u $config{p4User} -P $config{p4Pass} -p $config{p4Port}";

$config{lastBuild}  = getLastBuild();
$config{lastRelease} = getLastRelease();

createLock();

addToReport("*** Creating log dir $config{logDir}\n");
my $rc = 0xffff & system("mkdir -p $config{logDir}");
quit("SETUP", "Failed to create $config{logDir}") if ($rc);

addToReport("*** Building $config{type} on $config{arch} at $config{dateStamp}\n");
addToReport("*** Last build: $config{lastBuild}\n");
addToReport("*** Last release: $config{lastRelease}\n");

$config{p4Client} = createClient();

deleteOldBuilds();

linkBuilds($config{arch},$config{branch});

updateMainBuildServer() if $GlobalOpts{H};

checkOut();

if (!$config{mainhost}) {
  getChanges();
  getBugs();
}

$config{buildNumber} = updateBuildNum() if ($GlobalOpts{r} eq "");

$config{revision} = getRevision();

build();

addSourceTag() if ($GlobalOpts{r} eq "");

createDocs();

reInstall() if ($GlobalOpts{R} || $GlobalOpts{T});

linkBuilds($config{arch},$config{branch});

runTests() if ($GlobalOpts{T});

deleteClient($config{p4Client});

deleteLock();

updateMainBuildServer() if $GlobalOpts{H};

sendMail("COMPLETE", "");

addToReport("*** BUILD COMPLETE\n\n");

#======================================================================
# Subroutines
#======================================================================

sub updateMainBuildServer {
  my $failed = shift;
  if ($config{arch} !~ /MACOSX/) {
    addToReport("\tUpdate of main build server skipped\n\n");
    return;
  }

  my $rsync;
  if ($failed) {
    $rsync = "/usr/bin/rsync --rsh=\"ssh -l $config{buildUser} -i $config{sshKey}\" -az --delete $config{buildRoot}/FAILED $config{mainHost}:$config{buildDir}/$config{arch}/$config{branch}/$config{release}/FAILED";
  } else {
    $rsync = "/usr/bin/rsync --rsh=\"ssh -l $config{buildUser} -i $config{sshKey}\" -az --delete $config{buildRoot} $config{mainHost}:$config{buildDir}/$config{arch}/$config{branch}";
  }

  addToReport("RSYNC: $rsync\n");

  my $rc = 0xffff & system ("$rsync");
  quit ("RSYNC","$!") if ($rc);

  my $ssh = "/usr/bin/ssh -i $config{sshKey} $config{buildUser}\@$config{mainHost}";
  my $cmd = "$config{scriptDir}/build.pl -w";
  my $rc = 0xffff & system ("$ssh \"$cmd\"");
  quit("RSYNC","Remote command FAILED: $ssh $cmd $!") if ($rc);

}

sub createLock {
  if ( -f "$config{buildDir}/$config{arch}/$config{branch}/.lock" ) {
    addToReport("Build in progress!\n\n");
    exit (1);
  }
  `mkdir -p $config{buildRoot}`;
  `touch "$config{buildDir}/$config{arch}/$config{branch}/.lock"`;
  `touch "$config{buildRoot}/.inprogress"`;
}

sub deleteLock {
  unlink "$config{buildDir}/$config{arch}/$config{branch}/.lock";
  unlink "$config{buildRoot}/.inprogress";
}

sub deleteBuild {
  my ($build) = @_;
  my ($label);
  addToReport("*** Deleting build $build\n");
  if (-f "$build/.label") {
    chomp($label = `cat $build/.p4label`);
    #deleteSourceTag($label);
  }
  `rm -rf $build`;
}

sub getRevision {
  my ($major,$minor,$micro,$build);
  chomp($major=`cat $config{buildRoot}/ZimbraBuild/RE/MAJOR`);
  chomp($minor=`cat $config{buildRoot}/ZimbraBuild/RE/MINOR`);
  chomp($micro=`cat $config{buildRoot}/ZimbraBuild/RE/MICRO`);
  chomp($build=`cat $config{buildRoot}/ZimbraBuild/RE/BUILD`);
  return "${major}_${minor}_${micro}_${build}";
}

sub updateBuildNum {
  my ($rc, $curBuild, $cmd);

  chomp($curBuild=`cat $config{buildRoot}/ZimbraBuild/RE/BUILD`);
  $curBuild++;

  $cmd = "$config{P4} -c $config{p4Client} edit $config{buildRoot}/ZimbraBuild/RE/BUILD";
  $rc = 0xffff & system("$cmd >> $config{p4Log} 2>&1");
  if ($rc) {
    includeLinesFromLog("12", "$config{p4Log}");
    quit ("P4","updateBuildNum (edit): $!");
  }

  open(F, ">$config{buildRoot}/ZimbraBuild/RE/BUILD");
  print F "${curBuild}";
  close(F);

  $cmd = "$config{P4} -c $config{p4Client} change -o | ".
    "sed -e 's/<enter description here>/bug: 6038 ".
    "Auto update ZCS BUILD to $curBuild/' | ".
    "$config{P4} -c $config{p4Client} submit -i";

  $rc = 0xffff & system("$cmd >> $config{p4Log} 2>&1");
  if ($rc) {
    includeLinesFromLog("12", "$config{p4Log}");
    quit("P4","updateBuildNum (submit): $!");
  }

  return($curBuild);
}

sub checkOut {
  addToReport("*** Syncing P4 to $config{buildRoot} at $config{dateStamp}\n");
  my $r = dateStampToRevDate($config{dateStamp});
  my $cmd = "cd $config{buildRoot}; $config{P4} -c $config{p4Client} sync -f ...\@$r >> $config{p4Log} 2>&1";
  addToReport("$cmd\n");

  my $rc = 0xffff & system($cmd);
  if ($rc) {
    includeLinesFromLog("12", "$config{p4Log}");
    quit("P4","checkOut: $!");
  }

}

sub createClient {

  my $template = "$config{confDir}/templates/BUILD_template_$config{type}";
  my $client = "BUILD_$config{branch}_$config{dateStamp}_$config{type}_$config{arch}";

  addToReport("*** Creating client $client\n");
  my $cmd = "cat $template | ".
    "sed -e \"s|\@\@TAG\@\@|$config{branch}|g\" |".
    "sed -e \"s|\@\@BUILD_ROOT\@\@|$config{buildRoot}|g\" |".
    "sed -e \"s|\@\@RELEASE\@\@|$config{release}|g\" |".
    "sed -e \"s|\@\@ARCH\@\@|$config{arch}|g\" |".
    "$config{P4} -c $client client -i >> $config{p4Log} 2>&1";
  
  my $rc = 0xffff & system($cmd);
  if ($rc) {
    includeLinesFromLog("12", "$config{p4Log}");
    quit("P4","createClient: $!");
  }

  return $client;
}

sub quit {
  my ($stage,$err) = (@_);
  $err = ($err ? $err : "unknown");
  addToReport("FAILED IN $stage: $err\n\n");
  revertChanges($config{p4Client});
  deleteLock();
  deleteClient($config{p4Client});
  `echo "FAILED IN $stage: $err\n\n" > $config{buildRoot}/FAILED`;
  updateMainBuildServer() if $GlobalOpts{H};
  sendMail($stage, $err);
  exit (1);
}

sub deleteClient {
  my $c = shift;
  addToReport("*** DELETING CLIENT $c\n");
  my $cmd = "$config{P4} -c $c client -d $c >> $config{p4Log} 2>&1";
  my $rc = 0xffff & system($cmd);
  return $rc;
}

sub revertChanges {
  my $c = shift;
  addToReport("*** REVERTING unsubmitted changes in client $c\n");
  my $cmd = "$config{P4} -c $c revert $config{buildRoot}/... >> $config{p4Log} 2>&1";
  my $rc = 0xffff & system($cmd);
  return $rc;
}

sub usage {
  print STDERR "$0 -b <branch> -t <type> -a <arch> -T [-r release] [-c change]\n";
  print STDERR "\t-b <branch>\n";
  print STDERR "\t-t <FOSS|NETWORK>\n";
  print STDERR "\t-a <arch>\n";
  print STDERR "\t-r <release>\n";
  print STDERR "\t-c <change>\n";
  print STDERR "\t-T run tests\n";

  exit (1);
}

sub getLastBuild {
  my $buildFile = "$config{buildHome}/.dfoodinstalls";
  if (open LAST, $buildFile) {
    my @builds = <LAST>;
    close LAST;
    return dateStampToRevDate($builds[$#builds]);
  } else {
  }
}

sub getLastRelease {
  return;
}

sub dateStampToRevDate {
  my $d = shift;
  return sprintf "%04d/%02d/%02d:%02d:%02d:%02d", 
    substr($d,0,4),substr($d,4,2),substr($d,6,2),
    substr($d,8,2),substr($d,10,2),substr($d,12,2);
}

sub getDateStamp {
  my $r = shift;
  (defined ($r)) && return $r;
  my @d = localtime();
  return sprintf ("%4d%02d%02d%02d%02d%02d",$d[5]+1900,$d[4]+1,$d[3],$d[2],$d[1],$d[0]);
}

sub checkOpts {
  if (defined($GlobalOpts{w})) {
    # Link all builds
    opendir BUILD, $config{buildDir};
    my @arches = grep !/^\.\.?/, readdir BUILD;
    closedir BUILD;
    foreach my $arch (@arches) {
      opendir ARCH, "$config{buildDir}/${arch}";
      my @branches = grep !/^\.\.?/, readdir ARCH;
      closedir ARCH;
      foreach my $branch (@branches) {
        linkBuilds($arch, $branch);
      }
    }
    exit (0);
  }

  unless (defined ($GlobalOpts{b}) && defined ($GlobalOpts{t}) &&
    defined ($GlobalOpts{a}) ) { usage(); }
}

sub build {

  addToReport("*** BUILDING IN $config{buildRoot}\n");
  my $cmd;
  my $targets = "all";
  if (!$GlobalOpts{H}) {
    if ($GlobalOpts{t} eq "FOSS") {
      $targets = "ajaxtar sourcetar $targets";
    } else {
      $targets = "sourcetar $targets";
    }
  }
  if ($GlobalOpts{t} eq "FOSS") {
    $cmd = "cd $config{buildRoot}/ZimbraBuild;".
      "make -f Makefile $targets";
  } else {
    if ($GlobalOpts{b} eq "ARMSTRONG" || 
      $GlobalOpts{b} eq "ARMSTRONG_COMCAST") {
      $cmd = "cd $config{buildRoot}/ZimbraBuild;".
        "make -f Makefile.NETWORK $targets";
    } else {
      $cmd = "cd $config{buildRoot}/ZimbraBuild;".
        "make -f ../ZimbraNetwork/ZimbraBuild/Makefile $targets";
    }
  }

  addToReport("$cmd\n");
  my $rc = 0xffff & system ("$cmd >> $config{buildLog} 2>&1");
  if ($rc) {
    includeLinesFromLog("30", "$config{buildLog}");
    quit("BUILD","$!");
  }
  &informQA();
}

sub getChanges {
  addToReport("*** Getting changes\n");

  open CHANGE, ">$config{changeLog}";
  my $r = dateStampToRevDate($config{release});
  print CHANGE "Changes in $GlobalOpts{b} from $config{lastBuild} to ${r} ".
    "(Full text below)\n";
  print CHANGE "---------------------------------\n\n";

  my $cmd = "$config{P4} -c $config{p4Client} changes  -t ".
    "$config{buildRoot}/...\@$config{lastBuild},$r";

  print CHANGE "$config{P4} -c $config{p4Client} changes  -t $config{buildRoot}/...\@$config{lastBuild},$r\n";

  system ("$cmd > /tmp/ch.txt");

  print CHANGE "---------------------------------\n\n";
  
  open CH, "/tmp/ch.txt";
  my @c = <CH>;
  close CH;

  print CHANGE "@c";

  open CHANGE, ">>$config{changeLog}";
  print CHANGE "\n---------------------------------\n\n";

  close CHANGE;

  foreach (@c) {
    my $n = (split)[1];
    #print "Describing change $n\n";
    my $cmd = "$config{P4} -c $config{p4Client} describe -s $n";
    #print "$cmd\n";

    system ("$cmd >> $config{changeLog}");
    `echo "---------------------------------" >> $config{changeLog}`;
  }

  unlink "/tmp/ch.txt";

}

sub getBugs {
  addToReport("*** Getting fixed bugs\n");
  open BUG, ">>$config{bugLog}";
  print BUG "Bugs fixed in $GlobalOpts{b} from $config{lastBuild} to $config{release}\n";
  print BUG "---------------------------------\n\n";
  close BUG;
  addToReport("$config{scriptDir}/get_fixed_bugs.sh $config{lastBuild} >> $config{bugLog}\n");
  `$config{scriptDir}/get_fixed_bugs.sh $config{lastBuild} >> $config{bugLog} &`;
}

sub createDocs {

  addToReport("*** CREATING DOCS\n");
}

sub deleteOldBuilds {
  my $dir = "$config{buildDir}/$GlobalOpts{a}/$GlobalOpts{b}";

  opendir DIR, $dir;
  my @builds = grep !/^\.\.?/, readdir DIR;
  closedir DIR;

  @builds = reverse sort @builds;

  for (my $i = 0; $i <= $#builds; ) {
    if (-f "$dir/$builds[$i]/FAILED") {
      deleteBuild ("$dir/$builds[$i]");
      splice (@builds, $i, 1);
    } elsif (-f "$dir/$builds[$i]/RELEASED" || 
      -f "$dir/$builds[$i]/ARCHIVED" || 
      -f "$dir/$builds[$i]/.inprogress") {
      splice (@builds, $i, 1);
    } else {
      $i++;
    }
  }

  if ($#builds >= $config{keepBuilds}) {
    for (my $i = $config{keepBuilds}; $i <= $#builds; $i++) {
      deleteBuild ("$dir/$builds[$i]");
    }
  }

}


sub linkBuilds {
  my ($arch, $branch) = (@_);

  my $dir = "$config{buildDir}/${arch}/${branch}";

  opendir DIR, $dir;
  my @builds = grep !/^\.\.?/, readdir DIR;
  closedir DIR;

  my $linkDir = "$config{htmlDir}/links/$arch/$branch";
  `rm -rf ${linkDir}`;
  `mkdir -p ${linkDir}`;

  @builds = sort @builds;

  for (my $i = 0; $i <= $#builds; $i++) {
    addToReport("Linking $dir/$builds[$i] to ${linkDir}/$builds[$i]\n") 
      if $config{verbose};
    symlink("$dir/$builds[$i]", "${linkDir}/$builds[$i]");
    if (-f "$dir/$builds[$i]/ZimbraQA/results/QTPFlag.txt") {
      unlink "${linkDir}/installed";
      symlink("${dir}/$builds[$i]", "${linkDir}/installed");
    }
  }
  symlink("${dir}/$builds[$#builds]", "${linkDir}/latest");
}

sub runTests {
  my $pw=`cat /tmp/ldap.pw`;
  `chmod a+x $config{buildRoot}/ZimbraQA/src/bin/runtests.sh`;
  `touch $config{testLog}`;
  my $rc = 0xffff & system("$config{buildRoot}/ZimbraQA/src/bin/runtests.sh $config{buildRoot} $pw >> $config{testLog} 2>&1");
  quit ("TEST","$!") if ($rc);
}

sub reInstall {
  `rm -f /home/build/TestMailRaw`;
  `ln -s $config{buildRoot}/ZimbraServer/data/TestMailRaw /home/build/TestMailRaw`;
  `chmod -R a+r $config{buildRoot}/ZimbraServer/data`;
  addToReport("*** REINSTALLING FROM $config{buildRoot}\n");
  my $rc = 0xffff & system("sudo $config{scriptDir}/reinstall.sh $config{buildRoot}/ZimbraBuild/i386 >> $config{installLog} 2>&1");
  quit ("INSTALL","$!") if ($rc);
}

sub informQA {
  my $url = "http://qa00/builds/addBuild?";
  $url .= "arch=$GlobalOpts{a}";
  $url .= "&branch=$GlobalOpts{b}";
  $url .= "&name=$config{dateStamp}_$GlobalOpts{t}";
  unless (scalar head($url)) {
    warn "Failed to inform QA via $url\n";
  }
}

sub sendMail {
  my $stage = shift;
  my $err = shift;
  my $state = ($err ? "failed" : "passed");

  my $subj = "Build $state on $stage $config{arch} $config{type} $config{dateStamp}";
  my $dest = "release-engineering\@zimbra.com";
  my $from = "build\@zimbra.com";
  my $smtp = "dogfood.zimbra.com";
  my $mesg = "Build $state in stage $stage\n";
  $mesg .= "$config{dateStamp} $GlobalOpts{t} $GlobalOpts{b} on $GlobalOpts{a}\n\n";
  if (exists $config{label}) {
    $mesg .= "Source labeled with $config{label}\n\n";
  } else {
    $mesg .= "Source not labeled.\n\n";
  }
  $mesg .= "@{$config{message}}\n";

  eval {
    my $msg = MIME::Lite->new(
      From => $from,
      To   => $dest,
      Subject => $subj,
      Type    => 'multipart/mixed',
    ) or warn "ERROR: Can't open: $!\n";

    $msg->attach(
      Type => 'text',
      Data => $mesg,
    ) or warn "Error adding the text message: $!\n";

    addAttachment($msg, $config{p4Log}) 
      if ($state eq "failed" && $stage eq "P4");
    addAttachment($msg, $config{buildLog}) 
      if ($state eq "failed");
    addAttachment($msg, $config{changeLog});
    addAttachment($msg, $config{bugLog});
    addAttachment($msg, $config{installLog});
    addAttachment($msg, $config{testLog});
    MIME::Lite->send('smtp', $smtp, Timeout=>120);
    $msg->send;
  };

  if ($@) {
    addToReport("Failed to email report: $@\n");
  } else {
    addToReport("Email report sent to $dest\n");
  }

}

sub addAttachment($$) {
  my ($ref, $file) = @_;

  return unless (-f "$file");

  my $filename = basename($file);
  $filename =~ s/\.log$/\.txt/;
  $ref->attach(
    Type => 'text/plain',
    Path => $file,
    Filename => $filename,
    Disposition => 'attachment',
  ) or warn "Error adding $file: $!\n";

}

sub deleteSourceTag($) {
  my ($tag) = @_;
  addToReport("*** Deleting label $tag\n");
  my $cmd = "$config{P4} label -f -d $tag >> $config{p4Log} 2>&1";
  $rc = 0xffff & system($cmd);
  addToReport("p4 label -d: $!") if ($rc);
  return($rc);
}

sub addSourceTag {
  my ($cmd,$rc,$label);
  addToReport("*** Tagging BUILD to $config{buildRoot} at $config{release}\n");

  $config{label} = "BUILD_$config{revision}_$config{release}";
  open(L, ">$config{buildRoot}/.p4label") or quit("P4", "label: $!\n");
  print L "$config{label}\n";
  close(L) or quit("P4", "label: $!\n");
  $cmd = "$config{P4} -c $config{p4Client} tag -l $config{label} $config{buildRoot}/... >> $config{p4Log} 2>&1";
  $rc = 0xffff & system($cmd);
  quit ("P4","label: $!") if ($rc);
}

sub addToReport($) {
  my ($line) = @_;
  print $line if $config{verbose};
  push(@{$config{message}}, $line);
}

sub includeLinesFromLog($$) {
  my ($num,$log) = @_;
  open(L, "$log");
  my @log = <L>;
  close(L);
  my $s = ($#log > $num ? $#log-$num : 0);
  foreach my $i ($s..$#log) {
    addToReport($log[$i]);
  }
}
  

__END__
