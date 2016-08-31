Name:          vitam-consul
Version:       0.6.4
Release:       2%{?dist}
Summary:       Consul is a distributed, highly-available, and multi-datacenter aware tool for service discovery, configuration, and orchestration. This package includes vitam-specific folders configuration.
Group:         System Environment/Daemons
License:       Cecill v2.1
BuildArch:     x86_64
URL:           http://www.consul.io
Source0:       https://releases.hashicorp.com/consul/%{version}/consul_%{version}_linux_amd64.zip
Source1:       consul.env
Source2:       vitam-consul.service

BuildRequires: systemd-units
Requires:      systemd
Requires:      vitam-user-vitam

%description
Consul has multiple components, but as a whole, it is a tool for discovering and configuring services in your infrastructure. It provides several key features:
- Service Discovery: Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.
- Health Checking: Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.
- Key/Value Store: Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.
- Multi Datacenter: Consul supports multiple datacenters out of the box. This means users of Consul do not have to worry about building additional layers of abstraction to grow to multiple regions.

%prep
%setup -q -c

%install
mkdir -p %{buildroot}/vitam/bin/%{name}
cp consul %{buildroot}/vitam/bin/%{name}

mkdir -p %{buildroot}/vitam/conf/%{name}/sysconfig
cp %{SOURCE1} %{buildroot}/vitam/conf/%{name}/sysconfig/consul

mkdir -p %{buildroot}/vitam/data/%{name}

mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE2} %{buildroot}/%{_unitdir}/

%pre

%post
%systemd_post %{name}.service

%preun
%systemd_preun %{name}.service

%postun
%systemd_postun %{name}.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir %attr(750, vitam, vitam) /vitam/conf/%{name}
%dir %attr(750, vitam, vitam) /vitam/conf/%{name}/sysconfig
%config(noreplace) /vitam/conf/%{name}/sysconfig/consul
%attr(640, vitam, vitam) /vitam/conf/%{name}/sysconfig/consul
%dir %attr(750, vitam, vitam) /vitam/data/%{name}
%{_unitdir}/%{name}.service
%dir %attr(750, vitam, vitam) /vitam/bin/%{name}
%attr(755, vitam, vitam) /vitam/bin/%{name}/consul

%doc


%changelog
* Fri Aug 19 2016 Kristopher Waltzer <kristopher.waltzer.ext@agriculture.gouv.fr>
- Initial version
