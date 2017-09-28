Name:          vitam-gatling
Version:       2.3.0
Release:       1%{?dist}
Summary:       Open-Source Load & Performance Testing Tool For Web Applications
Group:         System Environment/Daemons
License:       Mozilla Public License, version 2.0
BuildArch:     x86_64
URL:           http://gatling.io/
Source0:       https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/%{version}/gatling-charts-highcharts-bundle-%{version}-bundle.zip
Source1:       run.sh
%global vitam_service_name gatling


BuildRequires: systemd-units
Requires:      systemd
Requires:      java-1.8.0
Requires:      vitam-user-vitam 

%description
Gatling is a highly capable load testing tool. It is designed for ease of use, maintainability and high performance.
Out of the box, Gatling comes with excellent support of the HTTP protocol that makes it a tool of choice for load testing any HTTP server. As the core engine is actually protocol agnostic, it is perfectly possible to implement support for other protocols. For example, Gatling currently also ships JMS support.
The Quickstart has an overview of the most important concepts, walking you through the setup of a simple scenario for load testing an HTTP server.
Having scenarios that are defined in code and are resource efficient are the two requirements that motivated us to create Gatling. Based on an expressive DSL, the scenarios are self explanatory. They are easy to maintain and can be kept in a version control system.
Gatling’s architecture is asynchronous as long as the underlying protocol, such as HTTP, can be implemented in a non blocking way. This kind of architecture lets us implement virtual users as messages instead of dedicated threads, making them very resource cheap. Thus, running thousands of concurrent virtual users is not an issue.


%prep
%setup -q -c

%install
mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}
cp -r gatling-charts-highcharts-bundle-%{version}/* %{buildroot}/vitam/bin/%{vitam_service_name}/
cp %{SOURCE1} %{buildroot}/vitam/bin/%{vitam_service_name}/

rm -rf %{buildroot}/vitam/bin/%{vitam_service_name}/user-files
rm -rf %{buildroot}/vitam/bin/%{vitam_service_name}/results
mkdir -p %{buildroot}/vitam/conf/%{vitam_service_name}
mkdir -p %{buildroot}/vitam/data/%{vitam_service_name}
mkdir -p %{buildroot}/vitam/log/%{vitam_service_name}
mkdir -p %{buildroot}/vitam/tmp/%{vitam_service_name}

rm -rf gatling-charts-highcharts-bundle-%{version}


%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir %attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(755, vitam, vitam) /vitam/bin/%{vitam_service_name}/*
%attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}/run.sh
%config(noreplace) /vitam/bin/%{vitam_service_name}/conf/gatling.conf
%dir %attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}/
%dir %attr(750, vitam, vitam) /vitam/data/%{vitam_service_name}/
%dir %attr(750, vitam, vitam) /vitam/log/%{vitam_service_name}/
%dir %attr(750, vitam, vitam) /vitam/tmp/%{vitam_service_name}/

%doc


%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
