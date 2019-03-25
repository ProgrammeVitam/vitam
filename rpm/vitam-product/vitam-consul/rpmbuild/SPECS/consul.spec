Name:          vitam-consul
Version:       1.4.4
Release:       1%{?dist}
Summary:       Consul is a distributed, highly-available, and multi-datacenter aware tool for service discovery, configuration, and orchestration. This package includes vitam-specific folders configuration.
Group:         System Environment/Daemons
License:       Mozilla Public License, version 2.0
BuildArch:     x86_64
URL:           http://www.consul.io
Source0:       https://releases.hashicorp.com/consul/%{version}/consul_%{version}_linux_amd64.zip
Source1:       consul.env
Source2:       vitam-consul.service
%global vitam_service_name consul

BuildRequires: systemd-units
Requires:      systemd
Requires:      libcap >= 2.22
Requires:      vitam-user-vitam
Conflicts:     bind,

%description
Consul has multiple components, but as a whole, it is a tool for discovering and configuring services in your infrastructure. It provides several key features:
- Service Discovery: Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.
- Health Checking: Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.
- Key/Value Store: Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.
- Multi Datacenter: Consul supports multiple datacenters out of the box. This means users of Consul do not have to worry about building additional layers of abstraction to grow to multiple regions.

%prep
%setup -q -c

%install
mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}
cp consul %{buildroot}/vitam/bin/%{vitam_service_name}/

mkdir -p %{buildroot}/vitam/conf/%{vitam_service_name}/sysconfig
cp %{SOURCE1} %{buildroot}/vitam/conf/%{vitam_service_name}/sysconfig/consul

mkdir -p %{buildroot}/vitam/data/%{vitam_service_name}

mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE2} %{buildroot}/%{_unitdir}

%pre

%post
%systemd_post %{name}.service
setcap CAP_NET_BIND_SERVICE=+eip /vitam/bin/%{vitam_service_name}/consul

%preun
%systemd_preun  %{name}.service

%postun
%systemd_postun  %{name}.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir %attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}
%dir %attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}/sysconfig
%config(noreplace) /vitam/conf/%{vitam_service_name}/sysconfig/consul
%attr(640, vitam, vitam) /vitam/conf/%{vitam_service_name}/sysconfig/consul
%dir %attr(750, vitam, vitam) /vitam/data/%{vitam_service_name}
%{_unitdir}/%{name}.service
%dir %attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(755, vitam, vitam) /vitam/bin/%{vitam_service_name}/consul

%doc


%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
