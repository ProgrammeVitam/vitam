
Name: vitam-user-vitam
Version: 0.7.0
Release: 4%{?dist}
Summary: Package used to create the vitam user and group	
BuildArch: noarch
Source0: vitam.sudoers
License: Cecill v2.1

Requires(pre):  shadow-utils


%description
Package to create the vitam user and group


%prep


%build


%install
mkdir -p %{buildroot}/etc/sudoers.d/
cp %{SOURCE0} %{buildroot}/etc/sudoers.d/vitam


%files
%defattr(-,root,root,-)

%config /etc/sudoers.d/vitam

%pre
getent group  vitam >/dev/null || groupadd -g 2000 vitam 
getent passwd vitam >/dev/null || useradd -u 2000 -g 2000 -s /bin/bash -c "Vitam application user" vitam
getent group  vitam-admin >/dev/null || groupadd -g 3000 vitam-admin


%changelog

