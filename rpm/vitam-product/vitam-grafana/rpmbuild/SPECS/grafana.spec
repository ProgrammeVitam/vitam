%define debug_package %{nil}
%define version 7.0.3

Name:          vitam-grafana
Version:       %{version}
Release:       1%{?dist}
Summary:       Grafana
License:       Apache 2.0
URL:           https://grafana.com


BuildRequires: systemd-units
Requires:      systemd
Requires:      grafana = %{version}

%description

%prep

%install

%pre

%post

%preun

%postun

%files

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version