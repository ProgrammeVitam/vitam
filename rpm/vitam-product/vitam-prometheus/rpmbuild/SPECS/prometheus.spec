%define debug_package %{nil}

Name:	 vitam-prometheus
Version: 2.19.0
Release: 1%{?dist}
Summary: The Prometheus monitoring system and time series database.
License: ASL 2.0
URL:     https://prometheus.io
Conflicts: prometheus

Source0: https://github.com/prometheus/prometheus/releases/download/v%{version}/prometheus-%{version}.linux-amd64.tar.gz
Source1: %{name}.service
Source2: prometheus.env

%global vitam_service_name %{name}.service
%global prometheus_appfolder /vitam/app/prometheus
%global prometheus_conffolder /vitam/conf/prometheus
%global prometheus_binfolder /vitam/bin/prometheus
%global prometheus_datafolder /vitam/data/prometheus


%{?systemd_requires}
Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

Prometheus is a systems and service monitoring system. It collects metrics from
configured targets at given intervals, evaluates rule expressions, displays the
results, and can trigger alerts if some condition is observed to be true.

%prep
%setup -q -n prometheus-%{version}.linux-amd64

%build
/bin/true

%install
mkdir -p %{buildroot}%{prometheus_binfolder}
mkdir -p %{buildroot}%{prometheus_appfolder}
mkdir -p %{buildroot}%{prometheus_conffolder}
mkdir -p %{buildroot}%{prometheus_conffolder}/sysconfig
mkdir -p %{buildroot}%{prometheus_datafolder}

install -D -m 755 prometheus %{buildroot}%{prometheus_binfolder}/prometheus
install -D -m 755 promtool %{buildroot}%{prometheus_binfolder}/promtool
install -D -m 755 tsdb %{buildroot}%{prometheus_binfolder}/tsdb

for dir in console_libraries consoles; do
  for file in ${dir}/*; do
    install -D -m 644 ${file} %{buildroot}%{prometheus_appfolder}/${file}
  done
done

install -D -m 644 prometheus.yml %{buildroot}%{prometheus_conffolder}/prometheus.yml
install -D -m 644 %{SOURCE1} %{buildroot}%{_unitdir}/%{vitam_service_name}
install -D -m 644 %{SOURCE2} %{buildroot}%{prometheus_conffolder}/sysconfig/prometheus

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
 %{prometheus_binfolder}/prometheus
 %{prometheus_binfolder}/promtool
 %{prometheus_binfolder}/tsdb

%dir %attr(755, vitam, vitam) %{prometheus_datafolder}
%dir %attr(750, vitam, vitam) %{prometheus_binfolder}
%dir %attr(750, vitam, vitam) %{prometheus_appfolder}
%dir %attr(750, vitam, vitam) %{prometheus_conffolder}
%dir %attr(750, vitam, vitam) %{prometheus_conffolder}/sysconfig


%config(noreplace)            %{prometheus_conffolder}/prometheus.yml

%config(noreplace)            %{prometheus_conffolder}/sysconfig/prometheus
%attr(640, vitam, vitam)      %{prometheus_conffolder}/sysconfig/prometheus

%{prometheus_appfolder}
%{_unitdir}/%{vitam_service_name}

%attr(755, vitam, vitam) %{prometheus_binfolder}/prometheus
%attr(755, vitam, vitam) %{prometheus_binfolder}/promtool
%attr(755, vitam, vitam) %{prometheus_binfolder}/tsdb

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version