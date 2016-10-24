Name:          vitam-mongod
Version:       3.2.10
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
Requires:      mongodb-org
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
* Tue Oct 11 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
