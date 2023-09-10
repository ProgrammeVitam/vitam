Name:    vitam-prometheus
Version: 2.42.0
Release: 1%{?dist}
Summary: The Prometheus monitoring system and time series database.
License: ASL 2.0
URL:     https://prometheus.io

Source0: https://github.com/prometheus/prometheus/releases/download/v%{version}/prometheus-%{version}.linux-amd64.tar.gz

%global appfolder /vitam/app/prometheus
%global conffolder /vitam/conf/prometheus
%global binfolder /vitam/bin/prometheus

Requires:      vitam-user-vitam
Conflicts:     prometheus

%description
Prometheus is a systems and service monitoring system. It collects metrics from
configured targets at given intervals, evaluates rule expressions, displays the
results, and can trigger alerts if some condition is observed to be true.

%prep
%setup -n prometheus-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install prometheus %{buildroot}%{binfolder}/prometheus
install promtool %{buildroot}%{binfolder}/promtool

mkdir -p %{buildroot}%{appfolder}
install LICENSE %{buildroot}%{appfolder}/LICENSE
install NOTICE %{buildroot}%{appfolder}/NOTICE

cp -vrp console_libraries %{buildroot}%{appfolder}
cp -vrp consoles %{buildroot}%{appfolder}

mkdir -p %{buildroot}%{conffolder}
install prometheus.yml %{buildroot}%{conffolder}/prometheus.yml

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %attr(750, vitam, vitam) %{binfolder}
%attr(750, vitam, vitam)      %{binfolder}/prometheus
%attr(750, vitam, vitam)      %{binfolder}/promtool

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}

%dir %attr(750, vitam, vitam) %{conffolder}
%config(noreplace)            %{conffolder}/prometheus.yml

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
