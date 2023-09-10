Name:          vitam-consul
Version:       1.12.9
Release:       1%{?dist}
Summary:       Consul is a distributed, highly-available, and multi-datacenter aware tool for service discovery, configuration, and orchestration. This package includes vitam-specific folders configuration.
Group:         System Environment/Daemons
License:       Mozilla Public License, version 2.0
BuildArch:     x86_64
URL:           https://www.consul.io

Source:        https://releases.hashicorp.com/consul/%{version}/consul_%{version}_linux_amd64.zip

Requires:      systemd
Requires:      libcap >= 2.22
Requires:      vitam-user-vitam
Conflicts:     bind

%global vitam_service_name consul

%description
Consul has multiple components, but as a whole, it is a tool for discovering and configuring services in your infrastructure. It provides several key features:
- Service Discovery: Clients of Consul can provide a service, such as api or mysql, and other clients can use Consul to discover providers of a given service. Using either DNS or HTTP, applications can easily find the services they depend upon.
- Health Checking: Consul clients can provide any number of health checks, either associated with a given service ("is the webserver returning 200 OK"), or with the local node ("is memory utilization below 90%"). This information can be used by an operator to monitor cluster health, and it is used by the service discovery components to route traffic away from unhealthy hosts.
- Key/Value Store: Applications can make use of Consul's hierarchical key/value store for any number of purposes, including dynamic configuration, feature flagging, coordination, leader election, and more. The simple HTTP API makes it easy to use.
- Multi Datacenter: Consul supports multiple datacenters out of the box. This means users of Consul do not have to worry about building additional layers of abstraction to grow to multiple regions.

%prep
%setup -c

%install
mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}
install consul %{buildroot}/vitam/bin/%{vitam_service_name}/

%pre

%post
setcap CAP_NET_BIND_SERVICE=+eip /vitam/bin/%{vitam_service_name}/consul

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}/consul

%doc

%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
