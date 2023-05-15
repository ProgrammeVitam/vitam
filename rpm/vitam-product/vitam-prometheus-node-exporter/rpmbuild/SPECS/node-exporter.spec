Name:    vitam-node-exporter
Version: 1.5.0
Release: 1%{?dist}
Summary: Prometheus exporter for hardware and OS metrics.
License: ASL 2.0
URL:     https://github.com/prometheus/node_exporter

Source0: https://github.com/prometheus/node_exporter/releases/download/v%{version}/node_exporter-%{version}.linux-amd64.tar.gz

%global datafolder /vitam/app/node_exporter
%global appfolder /vitam/app/node_exporter
%global conffolder /vitam/conf/node_exporter
%global binfolder /vitam/bin/node_exporter
%global logfolder /vitam/log/node_exporter

Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

Prometheus exporter for hardware and OS metrics exposed by *NIX kernels, written in Go with pluggable metric collectors.
This package contains binary to export node metrics to prometheus.

%prep
%setup -n node_exporter-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install node_exporter %{buildroot}%{binfolder}/node_exporter

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
%attr(750, vitam, vitam)      %{binfolder}/node_exporter

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE
%attr(644, vitam, vitam)      %{appfolder}/NOTICE

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
