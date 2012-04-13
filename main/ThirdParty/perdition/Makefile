PERDITION_ROOT := $(shell pwd)
P4_ROOT ?= $(shell cd $(PERDITION_ROOT)/../..; pwd)

BUILD_PLATFORM ?= $(shell sh $(P4_ROOT)/ZimbraBuild/rpmconf/Build/get_plat_tag.sh)

PERDITION_RELEASE ?= 1.17
PERDITION_PATCHLEVEL ?= 1
PERDITION_VERSION ?= $(PERDITION_RELEASE).$(PERDITION_PATCHLEVEL)
VANESSA_LOGGER_VERSION ?= 0.0.7
VANESSA_ADT_VERSION ?= 0.0.7
VANESSA_SOCKET_VERSION ?= 0.0.7

ZIMBRA_HOME ?= /opt/zimbra

PERDITION_INSTALL := $(ZIMBRA_HOME)/perdition-$(PERDITION_VERSION)

PERDITION_TGZ_TARGET := \
	$(PERDITION_ROOT)/builds/$(BUILD_PLATFORM)/perdition-$(PERDITION_VERSION).tgz

MYSQL_INCLUDE_DIR := /opt/zimbra/mysql/include

ifeq ($(BUILD_PLATFORM), MACOSX)
	MYSQL_INCLUDE_DIR := /opt/zimbra/mysql/include/mysql
endif
ifeq ($(BUILD_PLATFORM), MACOSXx86)
	MYSQL_INCLUDE_DIR := /opt/zimbra/mysql/include/mysql
endif
ifeq ($(BUILD_PLATFORM), RHEL4_64)
	LIB_DIR=-L/usr/lib64
endif

all: build tar

build: logger adt socket perdition

perdition:
	(tar xzf perdition-$(PERDITION_RELEASE).tar.gz; \
	mv perdition-$(PERDITION_RELEASE) perdition-$(PERDITION_VERSION); \
	cd perdition-$(PERDITION_VERSION); \
	patch -g0 -p1 < ../perdition-ldap-escapes.patch; \
	cd $(PERDITION_ROOT))
	mv -f perdition-$(PERDITION_VERSION)/perdition/db/ldap/perditiondb_ldap.c \
		perdition-$(PERDITION_VERSION)/perdition/db/ldap/perditiondb_ldap.c.fcs
	sed -e 's|.*static int pldap_version.*|static int pldap_version = 3;|' \
		perdition-$(PERDITION_VERSION)/perdition/db/ldap/perditiondb_ldap.c.fcs > \
		perdition-$(PERDITION_VERSION)/perdition/db/ldap/perditiondb_ldap.c
	(cd perdition-$(PERDITION_VERSION); \
		CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include -I$(ZIMBRA_HOME)/sleepycat/include" \
		LDFLAGS="$(LIB_DIR) -L$(ZIMBRA_HOME)/openldap/lib -L/opt/zimbra/lib -L$(PERDITION_INSTALL)/lib -L$(ZIMBRA_HOME)/sleepycat/lib -lpthread" \
		./configure --prefix=$(PERDITION_INSTALL) \
		--with-mysql-includes=$(MYSQL_INCLUDE_DIR) \
		--with-mysql-libraries=/opt/zimbra/mysql/lib \
		--with-ldap-includes=/opt/zimbra/openldap/include \
		--disable-odbc \
		--disable-pg \
		--disable-gdbm \
		--disable-ldap-doc \
		--with-ldap-libraries=/opt/zimbra/openldap/lib; \
		make CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include -I/usr/kerberos/include -I/opt/zimbra/include -I$(ZIMBRA_HOME)/sleepycat/include" \
                LDFLAGS="$(LIB_DIR) -L$(PERDITION_INSTALL)/lib -L/opt/zimbra/lib -L$(ZIMBRA_HOME)/sleepycat/lib -lpthread"; \
		 make install)
	ln -s $(PERDITION_INSTALL) $(ZIMBRA_HOME)/perdition

logger:
	tar xzf vanessa_logger-$(VANESSA_LOGGER_VERSION).tar.gz
	(cd vanessa_logger-$(VANESSA_LOGGER_VERSION); \
		LDFLAGS="$(LIB_DIR)" \
		./autogen.sh --prefix=$(PERDITION_INSTALL); LDFLAGS="$(LIB_DIR)" make LDFLAGS="$(LIB_DIR)" ; \
		make install)

adt:
	tar xzf vanessa_adt-$(VANESSA_ADT_VERSION).tar.gz
	(cd vanessa_adt-$(VANESSA_ADT_VERSION); \
		CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include" \
		LDFLAGS="$(LIB_DIR) -L$(PERDITION_INSTALL)/lib" \
		./configure --prefix=$(PERDITION_INSTALL); \
		make CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include" \
                LDFLAGS="$(LIB_DIR) -L$(PERDITION_INSTALL)/lib"; \
		make install)

socket:
	tar xzf vanessa_socket-$(VANESSA_SOCKET_VERSION).tar.gz
	(cd vanessa_socket-$(VANESSA_SOCKET_VERSION); \
		CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include" \
		LDFLAGS="$(LIB_DIR) -L$(PERDITION_INSTALL)/lib" \
		./configure --prefix=$(PERDITION_INSTALL); \
		make CFLAGS="-fPIC -I$(PERDITION_INSTALL)/include" \
                LDFLAGS="$(LIB_DIR) -L$(PERDITION_INSTALL)/lib"; \
		make install)

tar:
	mkdir -p $(PERDITION_ROOT)/builds/$(BUILD_PLATFORM)
	(cd $(ZIMBRA_HOME); tar czf $(PERDITION_TGZ_TARGET) \
	perdition-$(PERDITION_VERSION))
	chmod -R a+w $(PERDITION_ROOT)/builds/$(BUILD_PLATFORM)

p4edit: $(PERDITION_TGZ_TARGET)
	p4 add $(PERDITION_TGZ_TARGET)
	p4 edit $(PERDITION_TGZ_TARGET)
	

clean:
	rm -rf vanessa_logger-$(VANESSA_LOGGER_VERSION)
	rm -rf vanessa_adt-$(VANESSA_ADT_VERSION)
	rm -rf vanessa_socket-$(VANESSA_SOCKET_VERSION)
	rm -rf perdition-$(PERDITION_VERSION)

allclean: clean
	rm -rf $(PERDITION_INSTALL)
	rm -rf $(ZIMBRA_HOME)/perdition
