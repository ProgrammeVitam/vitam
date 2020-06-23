%define debug_package %{nil}

Name:    vitam-node-exporter
Version: 1.0.0
Release: 1%{?dist}
Summary: Prometheus exporter for hardware and OS metrics.
License: ASL 2.0
URL:     https://github.com/prometheus/node_exporter

Source0: https://github.com/prometheus/node_exporter/releases/download/v%{version}/node_exporter-%{version}.linux-amd64.tar.gz
Source1: %{name}.service
Source2: node_exporter.env

%global vitam_service_name %{name}.service
%global node_exporter_datafolder /vitam/app/node_exporter
%global node_exporter_appfolder /vitam/app/node_exporter
%global node_exporter_conffolder /vitam/conf/node_exporter
%global node_exporter_binfolder /vitam/bin/node_exporter
%global node_exporter_logfolder /vitam/log/node_exporter

%{?systemd_requires}
Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

Prometheus exporter for hardware and OS metrics exposed by *NIX kernels, written in Go with pluggable metric collectors.
This package contains binary to export node metrics to prometheus.


%prep
%setup -q -n node_exporter-%{version}.linux-amd64

%build
/bin/true

%install
mkdir -p %{buildroot}%{node_exporter_binfolder}
mkdir -p %{buildroot}%{node_exporter_appfolder}
mkdir -p %{buildroot}%{node_exporter_datafolder}
mkdir -p %{buildroot}%{node_exporter_datafolder}/textfile_collector
mkdir -p %{buildroot}%{node_exporter_conffolder}
mkdir -p %{buildroot}%{node_exporter_conffolder}/sysconfig

install -D -m 755 node_exporter %{buildroot}%{node_exporter_binfolder}/node_exporter
install -D -m 755 LICENSE %{buildroot}%{node_exporter_appfolder}/LICENSE
install -D -m 755 NOTICE %{buildroot}%{node_exporter_appfolder}/NOTICE

install -D -m 644 %{SOURCE1} %{buildroot}%{_unitdir}/%{vitam_service_name}
install -D -m 644 %{SOURCE2} %{buildroot}%{node_exporter_conffolder}/sysconfig/node_exporter


%pre

%post
%systemd_post %{vitam_service_name}

%preun
%systemd_preun %{vitam_service_name}

%postun
%systemd_postun %{vitam_service_name}

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)

%{node_exporter_binfolder}/node_exporter
%{node_exporter_appfolder}/LICENSE
%{node_exporter_appfolder}/NOTICE
%{_unitdir}/%{vitam_service_name}

%dir %attr(750, vitam, vitam) %{node_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{node_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{node_exporter_appfolder}
%dir %attr(750, vitam, vitam) %{node_exporter_datafolder}
%dir %attr(750, vitam, vitam) %{node_exporter_datafolder}/textfile_collector
%dir %attr(750, vitam, vitam) %{node_exporter_conffolder}
%dir %attr(750, vitam, vitam) %{node_exporter_conffolder}/sysconfig

%config(noreplace)            %{node_exporter_conffolder}/sysconfig/node_exporter
%attr(640, vitam, vitam)      %{node_exporter_conffolder}/sysconfig/node_exporter

%attr(755, vitam, vitam) %{node_exporter_binfolder}/node_exporter

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version