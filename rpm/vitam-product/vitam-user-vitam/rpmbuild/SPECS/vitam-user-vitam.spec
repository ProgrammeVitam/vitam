
Name: vitam-user-vitam
Version: 0.8.0
Release: 4%{?dist}
Summary: Package used to create the vitam user and group
BuildArch: noarch
Source0: vitam.sudoers
License: Cecill v2.1

Requires(pre): shadow-utils
Requires:      sudo


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
getent group  vitam >/dev/null || groupadd vitam 
getent passwd vitam >/dev/null || useradd -g vitam -s /bin/bash -c "Vitam application user" vitam
getent group  vitam-admin >/dev/null || groupadd vitam-admin


%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
