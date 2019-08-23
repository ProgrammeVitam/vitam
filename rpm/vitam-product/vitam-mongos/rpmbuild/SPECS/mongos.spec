Name:          vitam-mongos
Version:       4.2
Release:       1%{?dist}
Summary:       Service files for Vitam mongos cluster configuration nodes
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     noarch
URL:           http://www.mongodb.org
Source0:       vitam-mongos.service
%global        vitam_service_name vitam-mongos

BuildRequires: systemd-units
Requires:      systemd
Requires:      mongodb-org >= 4.2.0
Requires:      vitam-user-vitamdb

%description
Service files for Vitam mongos cluster configuration nodes

%prep

%install
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE0} %{buildroot}/%{_unitdir}/vitam-mongos.service

%pre

%post
%systemd_post vitam-mongos.service

%preun
%systemd_preun  vitam-mongos.service

%postun
%systemd_postun  vitam-mongos.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%{_unitdir}/vitam-mongos.service

%doc


%changelog
* Fri Jun 7 2019 Bumped to 4.0.10 ongo version French Prime minister Office/SGMAP/DINSIC/Vitam Programm <contact.vitam@culture.gouv.fr>
* Mon Nov 5 2018 Bumped to 4.0.3 mongo version French Prime minister Office/SGMAP/DINSIC/Vitam Programm <contact.vitam@culture.gouv.fr>
* Mon Aug 27 2018 Bumped to 4.0.1 mongo version French Prime minister Office/SGMAP/DINSIC/Vitam Programm <contact.vitam@culture.gouv.fr>
* Fri Oct 6 2017 Bumped to 3.4.9 mongo version French Prime minister Office/SGMAP/DINSIC/Vitam Programm <contact.vitam@culture.gouv.fr>
* Tue Oct 11 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
