Name:    vitam-alertmanager
Version: 0.25.0
Release: 1%{?dist}
Summary: Prometheus Alertmanager.
License: Apache License 2.0
URL:     https://github.com/prometheus/alertmanager

Source0: https://github.com/prometheus/alertmanager/releases/download/v%{version}/alertmanager-%{version}.linux-amd64.tar.gz

%global appfolder /vitam/app/alertmanager
%global binfolder /vitam/bin/alertmanager

Requires:      vitam-user-vitam

%description
The Alertmanager handles alerts sent by client applications such as the
Prometheus server. It takes care of deduplicating, grouping, and routing them to
the correct receiver integration such as email, PagerDuty, or OpsGenie. It also
takes care of silencing and inhibition of alerts.

%prep
%setup -n alertmanager-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install alertmanager %{buildroot}%{binfolder}/alertmanager
install amtool %{buildroot}%{binfolder}/amtool

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
%attr(750, vitam, vitam)      %{binfolder}/alertmanager
%attr(750, vitam, vitam)      %{binfolder}/amtool

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE
%attr(644, vitam, vitam)      %{appfolder}/NOTICE

%doc

%changelog
* Fri Jun 12 2020 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
