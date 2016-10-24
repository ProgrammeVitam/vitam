
Name: vitam-user-vitamdb
Version: 0.7.0
Release: 4%{?dist}
Summary: Package used to create the vitamdb user and group
BuildArch: noarch
Source0: vitamdb.sudoers
License: Cecill v2.1

Requires(pre): shadow-utils
Requires:      sudo


%description
Package to create the vitamdb user and group


%prep


%build


%install
mkdir -p %{buildroot}/etc/sudoers.d/
cp %{SOURCE0} %{buildroot}/etc/sudoers.d/vitamdb


%files
%defattr(-,root,root,-)

%config /etc/sudoers.d/vitamdb

%pre
getent group  vitam >/dev/null || groupadd -g 2000 vitam
getent passwd vitamdb >/dev/null || useradd -u 2001 -g 2000 -s /bin/bash -c "Vitam database user" vitamdb
getent group  vitamdb-admin >/dev/null || groupadd -g 3001 vitamdb-admin


%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
