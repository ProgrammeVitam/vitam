%define version 6.8.5

Name:          vitam-kibana
Version:       %{version}
Release:       1%{?dist}
Summary:       A Distributed RESTful Search Engine (with vitam systemd units)
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     x86_64
URL:           https://www.elastic.co/products/elasticsearch

BuildRequires: systemd-units
Requires:      systemd
Requires:      kibana = %{version}

%description
Kibana lets you visualize your Elasticsearch data and navigate the Elastic Stack.

%prep

%install

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files



%changelog
* Fri Feb 22 2019 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
