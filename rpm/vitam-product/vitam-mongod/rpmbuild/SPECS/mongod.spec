Name:          vitam-mongod
Version:       3.4.13
Release:       1%{?dist}
Summary:       Service files for Vitam mongod cluster configuration nodes
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     noarch
URL:           http://www.mongodb.org
Source0:       vitam-mongod.service
%global        vitam_service_name vitam-mongod

BuildRequires: systemd-units
Requires:      systemd
Requires:      mongodb-org >= 3.4
Requires:      vitam-user-vitamdb

%description
Service files for Vitam mongod cluster configuration nodes

%prep

%install
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE0} %{buildroot}/%{_unitdir}/vitam-mongod.service

%pre

%post
%systemd_post vitam-mongod.service

%preun
%systemd_preun  vitam-mongod.service

%postun
%systemd_postun  vitam-mongod.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%{_unitdir}/vitam-mongod.service

%doc

%changelog
* Fri Oct 6 2017 Bumped to 3.4.9 mongo version French Prime minister Office/SGMAP/DINSIC/Vitam Programm <contact.vitam@culture.gouv.fr>
* Tue Oct 11 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
