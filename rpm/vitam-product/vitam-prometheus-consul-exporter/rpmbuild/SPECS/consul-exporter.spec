Name:    vitam-consul-exporter
Version: 0.9.0
Release: 1%{?dist}
Summary: Consul exporter for prometheus.
License: Apache License 2.0
URL:     https://github.com/prometheus/consul_exporter

Source0: https://github.com/prometheus/consul_exporter/releases/download/v%{version}/consul_exporter-%{version}.linux-amd64.tar.gz

Requires:      vitam-user-vitam

%global appfolder /vitam/app/consul_exporter
%global binfolder /vitam/bin/consul_exporter

%description
Consul exporter
This package contains binary to export node metrics to prometheus.

%prep
%setup -n consul_exporter-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install consul_exporter %{buildroot}%{binfolder}/consul_exporter

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
%attr(750, vitam, vitam)      %{binfolder}/consul_exporter

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE
%attr(644, vitam, vitam)      %{appfolder}/NOTICE

%doc

%changelog
* Fri Jan 11 2022 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
