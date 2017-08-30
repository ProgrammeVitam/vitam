Name:          vitam-mongoc
Version:       3.4.7
Release:       1%{?dist}
Summary:       Service files for Vitam mongoc cluster configuration nodes
Group:         Applications/Databases
License:       Cecill v2.1
BuildArch:     noarch
URL:           http://www.mongodb.org
Source0:       vitam-mongoc.service
%global        vitam_service_name vitam-mongoc

BuildRequires: systemd-units
Requires:      systemd
Requires:      mongodb-org >= 3.4
Requires:      vitam-user-vitamdb

%description
Service files for Vitam mongoc cluster configuration nodes

%prep

%install
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE0} %{buildroot}/%{_unitdir}/vitam-mongoc.service

%pre

%post
%systemd_post vitam-mongoc.service

%preun
%systemd_preun  vitam-mongoc.service

%postun
%systemd_postun  vitam-mongoc.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%{_unitdir}/vitam-mongoc.service

%doc


%changelog
* Tue Oct 11 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
