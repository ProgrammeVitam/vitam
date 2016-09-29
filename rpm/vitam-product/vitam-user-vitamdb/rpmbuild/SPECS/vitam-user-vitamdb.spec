Name: vitam-user-vitamdb
Version: 0.7.0
Release: 4%{?dist}
Summary: Package used to create the vitamdb user and group	
BuildArch: noarch
License: Cecill v2.1

Requires(pre):  shadow-utils


%description
Package to create the vitamdb user and group


%prep


%build


%install


%files

%pre
getent group  vitam >/dev/null || groupadd -g 2000 vitam
getent passwd vitamdb >/dev/null || useradd -u 2001 -g 2000 -s /bin/bash -c "Vitam database user" vitamdb

getent group  vitamdb-admin >/dev/null || groupadd -g 3001 vitamdb-admin

echo '%vitamdb-admin    ALL=NOPASSWD: /bin/systemctl stop elasticsearch,/bin/systemctl start elasticsearch,/bin/systemctl restart elasticsearch' >> /etc/sudoers

echo '%vitamdb-admin    ALL=NOPASSWD: /bin/systemctl stop mongos,/bin/systemctl start mongos,/bin/systemctl restart mongos' >> /etc/sudoers

echo '%vitamdb-admin    ALL=NOPASSWD: /bin/systemctl stop mongoc,/bin/systemctl start mongoc,/bin/systemctl restart mongoc' >> /etc/sudoers

echo '%vitamdb-admin    ALL=NOPASSWD: /bin/systemctl stop mongod,/bin/systemctl start mongod,/bin/systemctl restart mongod' >> /etc/sudoers

%changelog

