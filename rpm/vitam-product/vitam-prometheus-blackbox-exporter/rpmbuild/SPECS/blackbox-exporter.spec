Name:    vitam-blackbox-exporter
Version: 0.28.0
Release: 1%{?dist}
Summary: blackbox exporter for prometheus.
License: Apache License 2.0
URL:     https://github.com/prometheus/blackbox_exporter

Source0: https://github.com/prometheus/blackbox_exporter/releases/download/v%{version}/blackbox_exporter-%{version}.linux-amd64.tar.gz

Requires:      vitam-user-vitam

%global appfolder /vitam/app/blackbox_exporter
%global binfolder /vitam/bin/blackbox_exporter

%description
blackbox exporter
This package contains binary to export node metrics to prometheus.

%prep
%setup -n blackbox_exporter-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install blackbox_exporter %{buildroot}%{binfolder}/blackbox_exporter

mkdir -p %{buildroot}%{appfolder}
install LICENSE %{buildroot}%{appfolder}/LICENSE
install NOTICE %{buildroot}%{appfolder}/NOTICE

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %attr(750, vitam, vitam) %{binfolder}
%attr(750, vitam, vitam)      %{binfolder}/blackbox_exporter

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE
%attr(644, vitam, vitam)      %{appfolder}/NOTICE

%doc

%changelog
* Fri Feb 16 2024 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
