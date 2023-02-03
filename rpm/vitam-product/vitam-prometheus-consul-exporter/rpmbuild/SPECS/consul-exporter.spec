%define debug_package %{nil}

Name:    vitam-consul-exporter
Version: 0.9.0
Release: 1%{?dist}
Summary: Consul exporter for prometheus.
License: ASL 2.0
URL:     https://github.com/prometheus/consul_exporter

Source0: https://github.com/prometheus/consul_exporter/releases/download/v%{version}/consul_exporter-%{version}.linux-amd64.tar.gz
Source1: %{name}.service

%global vitam_service_name %{name}.service
%global consul_exporter_datafolder /vitam/data/consul_exporter
%global consul_exporter_appfolder /vitam/app/consul_exporter
%global consul_exporter_conffolder /vitam/conf/consul_exporter
%global consul_exporter_binfolder /vitam/bin/consul_exporter
%global consul_exporter_logfolder /vitam/log/consul_exporter

%{?systemd_requires}
Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

Consul exporter
This package contains binary to export node metrics to prometheus.


%prep
%setup -q -n consul_exporter-%{version}.linux-amd64

%build
/bin/true

%install
mkdir -p %{buildroot}%{consul_exporter_binfolder}
mkdir -p %{buildroot}%{consul_exporter_appfolder}
mkdir -p %{buildroot}%{consul_exporter_datafolder}
mkdir -p %{buildroot}%{consul_exporter_conffolder}
mkdir -p %{buildroot}%{consul_exporter_conffolder}/sysconfig

install -D -m 755 consul_exporter %{buildroot}%{consul_exporter_binfolder}/consul_exporter
install -D -m 755 LICENSE %{buildroot}%{consul_exporter_appfolder}/LICENSE
install -D -m 755 NOTICE %{buildroot}%{consul_exporter_appfolder}/NOTICE

install -D -m 644 %{SOURCE1} %{buildroot}%{_unitdir}/%{vitam_service_name}

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

%{consul_exporter_binfolder}/consul_exporter
%{consul_exporter_appfolder}/LICENSE
%{consul_exporter_appfolder}/NOTICE
%{_unitdir}/%{vitam_service_name}

%dir %attr(750, vitam, vitam) %{consul_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{consul_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{consul_exporter_appfolder}
%dir %attr(750, vitam, vitam) %{consul_exporter_datafolder}
%dir %attr(750, vitam, vitam) %{consul_exporter_conffolder}
%dir %attr(750, vitam, vitam) %{consul_exporter_conffolder}/sysconfig

%attr(755, vitam, vitam) %{consul_exporter_binfolder}/consul_exporter

%doc

%changelog
* Fri Jan 11 2022 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version