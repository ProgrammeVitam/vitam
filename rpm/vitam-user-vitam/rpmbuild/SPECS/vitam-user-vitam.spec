Name: vitam-user-vitam
Version: 0.7.0
Release: 1%{?dist}
Summary: Package used to create the vitam user and group	
BuildArch: noarch
License: Cecill v2.1

Requires(pre):  shadow-utils


%description
Package to create the vitam user and group


%prep


%build


%install


%files

%pre
getent group  vitam >/dev/null || groupadd -g 2000 vitam 
getent passwd vitam >/dev/null || useradd -u 2000 -g 2000 -s /sbin/nologin -c "Vitam application user" vitam

%changelog

