%define version 7.5.2
%define epoch 1

Name:          vitam-logstash
Version:       %{version}
Release:       1%{?dist}
Summary:       An extensible logging pipeline
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     noarch
URL:           https://www.elastic.co/products/elasticsearch

BuildRequires: systemd-units
Requires:      systemd
Requires:      logstash = %{epoch}:%{version}

%description
An extensible logging pipeline.

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
* Fri Jun 28 2019 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
