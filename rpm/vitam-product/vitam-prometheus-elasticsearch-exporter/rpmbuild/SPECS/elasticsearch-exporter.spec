%define debug_package %{nil}

Name:    vitam-elasticsearch-exporter
Version: 1.5.0
Release: 1%{?dist}
Summary: Elasticsearch Exporter for Prometheus
License: AL 2.0
URL:     https://github.com/prometheus-community/elasticsearch_exporter

Source0: https://github.com/prometheus-community/elasticsearch_exporter/releases/download/v%{version}/elasticsearch_exporter-%{version}.linux-amd64.tar.gz
Source1: %{name}.service

%global vitam_service_name %{name}.service
%global elasticsearch_exporter_datafolder /vitam/data/elasticsearch_exporter
%global elasticsearch_exporter_appfolder /vitam/app/elasticsearch_exporter
%global elasticsearch_exporter_conffolder /vitam/conf/elasticsearch_exporter
%global elasticsearch_exporter_binfolder /vitam/bin/elasticsearch_exporter
%global elasticsearch_exporter_logfolder /vitam/log/elasticsearch_exporter

%{?systemd_requires}
Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

Prometheus exporter for various metrics about ElasticSearch, written in Go.


%prep
%setup -q -n elasticsearch_exporter-%{version}.linux-amd64

%build
/bin/true

%install
mkdir -p %{buildroot}%{elasticsearch_exporter_binfolder}
mkdir -p %{buildroot}%{elasticsearch_exporter_appfolder}
mkdir -p %{buildroot}%{elasticsearch_exporter_datafolder}
mkdir -p %{buildroot}%{elasticsearch_exporter_conffolder}
mkdir -p %{buildroot}%{elasticsearch_exporter_conffolder}/sysconfig

install -D -m 755 elasticsearch_exporter %{buildroot}%{elasticsearch_exporter_binfolder}/elasticsearch_exporter
install -D -m 755 LICENSE %{buildroot}%{elasticsearch_exporter_appfolder}/LICENSE

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

%{elasticsearch_exporter_binfolder}/elasticsearch_exporter
%{elasticsearch_exporter_appfolder}/LICENSE
%{_unitdir}/%{vitam_service_name}

%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_binfolder}
%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_appfolder}
%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_datafolder}
%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_conffolder}
%dir %attr(750, vitam, vitam) %{elasticsearch_exporter_conffolder}/sysconfig

%attr(755, vitam, vitam) %{elasticsearch_exporter_binfolder}/elasticsearch_exporter

%doc

%changelog
* Fri Jan 11 2022 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
