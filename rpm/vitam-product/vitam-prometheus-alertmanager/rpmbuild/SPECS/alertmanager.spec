%define debug_package %{nil}

Name:    vitam-alertmanager
Version: 0.20.0
Release: 2%{?dist}
Summary: Prometheus Alertmanager.
License: ASL 2.0
URL:     https://github.com/prometheus/alertmanager

Source0: https://github.com/prometheus/alertmanager/releases/download/v%{version}/alertmanager-%{version}.linux-amd64.tar.gz
Source1: %{name}.service
Source2: alertmanager.env

%global vitam_service_name %{name}.service
%global alertmanager_appfolder /vitam/app/alertmanager
%global alertmanager_conffolder /vitam/conf/alertmanager
%global alertmanager_binfolder /vitam/bin/alertmanager
%global alertmanager_datafolder /vitam/data/alertmanager

%{?systemd_requires}
Requires(pre): shadow-utils
Requires:      vitam-user-vitam

%description

The Alertmanager handles alerts sent by client applications such as the
Prometheus server. It takes care of deduplicating, grouping, and routing them to
the correct receiver integration such as email, PagerDuty, or OpsGenie. It also
takes care of silencing and inhibition of alerts.

%prep
%setup -q -n alertmanager-%{version}.linux-amd64

%build
/bin/true

%install
mkdir -p %{buildroot}%{alertmanager_binfolder}
mkdir -p %{buildroot}%{alertmanager_appfolder}
mkdir -p %{buildroot}%{alertmanager_conffolder}
mkdir -p %{buildroot}%{alertmanager_conffolder}/sysconfig
mkdir -p %{buildroot}%{alertmanager_datafolder}

install -D -m 755 alertmanager %{buildroot}%{alertmanager_binfolder}/alertmanager
install -D -m 755 amtool %{buildroot}%{alertmanager_binfolder}/amtool

install -D -m 755 LICENSE %{buildroot}%{alertmanager_appfolder}/LICENSE
install -D -m 755 NOTICE %{buildroot}%{alertmanager_appfolder}/NOTICE

install -D -m 644 alertmanager.yml %{buildroot}%{alertmanager_conffolder}/alertmanager.yml
install -D -m 644 %{SOURCE1} %{buildroot}%{_unitdir}/%{vitam_service_name}
install -D -m 644 %{SOURCE2} %{buildroot}%{alertmanager_conffolder}/sysconfig/alertmanager


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

%{alertmanager_binfolder}/alertmanager
%{alertmanager_binfolder}/amtool
%{alertmanager_appfolder}/LICENSE
%{alertmanager_appfolder}/NOTICE
%{_unitdir}/%{vitam_service_name}

%dir %attr(755, vitam, vitam)       %{alertmanager_datafolder}
%dir %attr(750, vitam, vitam)       %{alertmanager_binfolder}
%dir %attr(750, vitam, vitam)       %{alertmanager_appfolder}
%dir %attr(750, vitam, vitam)       %{alertmanager_conffolder}
    %dir %attr(750, vitam, vitam)   %{alertmanager_conffolder}/sysconfig

%config(noreplace)                  %{alertmanager_conffolder}/alertmanager.yml

%config(noreplace)                  %{alertmanager_conffolder}/sysconfig/alertmanager
%attr(640, vitam, vitam)            %{alertmanager_conffolder}/sysconfig/alertmanager

%attr(755, vitam, vitam)            %{alertmanager_binfolder}/alertmanager
%attr(755, vitam, vitam)            %{alertmanager_binfolder}/amtool

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version