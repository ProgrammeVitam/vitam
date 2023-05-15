Name:    vitam-elasticsearch-exporter
Version: 1.5.0
Release: 1%{?dist}
Summary: Elasticsearch Exporter for Prometheus
License: Apache License 2.0
URL:     https://github.com/prometheus-community/elasticsearch_exporter

Source0: https://github.com/prometheus-community/elasticsearch_exporter/releases/download/v%{version}/elasticsearch_exporter-%{version}.linux-amd64.tar.gz

Requires:      vitam-user-vitam

%global appfolder /vitam/app/elasticsearch_exporter
%global binfolder /vitam/bin/elasticsearch_exporter

%description

Prometheus exporter for various metrics about ElasticSearch, written in Go.

%prep
%setup -n elasticsearch_exporter-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install elasticsearch_exporter %{buildroot}%{binfolder}/elasticsearch_exporter

mkdir -p %{buildroot}%{appfolder}
install LICENSE %{buildroot}%{appfolder}/LICENSE

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %attr(750, vitam, vitam) %{binfolder}
%attr(750, vitam, vitam)      %{binfolder}/elasticsearch_exporter

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE

%doc

%changelog
* Fri Jan 11 2022 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
