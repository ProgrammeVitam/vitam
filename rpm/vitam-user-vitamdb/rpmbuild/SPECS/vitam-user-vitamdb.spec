Name: vitam-user-vitamdb
Version: 0.7.0
Release: 1%{?dist}
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
getent group  vitamdb >/dev/null || groupadd -g 2001 vitamdb
getent passwd vitamdb >/dev/null || useradd -u 2001 -g 2001 -s /sbin/nologin -c "Vitam database user" vitamdb

%changelog

