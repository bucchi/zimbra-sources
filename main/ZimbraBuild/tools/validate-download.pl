#!/usr/bin/perl
# 
# ***** BEGIN LICENSE BLOCK *****
# Zimbra Collaboration Suite Server
# Copyright (C) 2009, 2010 Zimbra, Inc.
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
use LWP::Simple;
use Digest::MD5;
my $url="http://files2.zimbra.com/downloads";
my @list = <>;
my $failed=0;
$|=1;

foreach my $dl (@list) {
  my ($build,$temp,$platform,$release);
  chomp($dl);
  print "Checking $dl...";
  $temp=$dl;
  $temp =~ s/^zcs-(NETWORK-){0,1}//;
  ($release,$build) =
            $temp =~ m/(\d\.\d\.\d+_\S+)_(\S+)\.\S+.\.\S+\..*/;
  next if ($release eq "");
  mkdir($release) if (! -d $release);
  my $myurl = "${url}/${release}/${dl}";
  my $mymd5 = "${url}/${release}/${dl}.md5";
  my $dest = "${release}/${dl}";
  my $md5sum;
  my $code = getstore($myurl,"$dest")
    if (! -f "$dest");

  if ($code eq "200" or -f $dest) {
    $code = getstore($mymd5,"${dest}.md5");
    if ($code eq "200") {
      open(MD5, "${dest}.md5") || warn "$!\n";
      while (<MD5>) {
        chomp;
        ($md5sum,undef) = split(/\s+/, $_,2);
        last if $md5sum ne "";
      }
      if ($md5sum eq "") {
        print "md5sum is null.\n";
        $failed++;
        next;
      }
    }
  } else {
    print "Download *******FAILED********* code: $code.\n";
    $failed++;
    next;
  }
  my $digest = md5sum($dest);
  if ($digest eq $md5sum) {
    print "Download verified.\n";
    unlink("${dest}");
    unlink("${dest}.md5");
  } else {
    print "md5 mismatch.\n\t \"$digest\" ne \"$md5sum\"\n";
    $failed++;
    next;
  } 

}
exit $failed;

sub omd5sum {
  my ($file) = @_;
  my $digest;
  eval {
    open(MD5, "md5sum $file|") or die "Can't exec md5sum: $!\n";
    while (<MD5>) {
      chomp;
      ($digest,undef) = split(/\s+/, $_,2);
      last if ($digest ne "");
    }
  };
  print "Found $digest\n";
  return (($@) ? undef : $digest);
}

sub md5sum {
  my ($file) = @_;
  my $digest;
  eval {
    open(DL, $file) or die "Can't open $file: $!\n";
    binmode(DL);
    my $ctx = Digest::MD5->new;
    $ctx->addfile(*DL);
    $digest = $ctx->hexdigest;
    close(DL);
  };
  if ($@) {
    print $@;
    return undef;
  }
  return $digest;
}
