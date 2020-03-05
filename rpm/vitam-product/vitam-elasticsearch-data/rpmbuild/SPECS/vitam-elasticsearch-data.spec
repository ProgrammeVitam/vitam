%define version 7.6.0

Name:          vitam-elasticsearch-data
Version:       %{version}
Release:       2%{?dist}
Summary:       A Distributed RESTful Search Engine (with vitam systemd units)
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     x86_64
URL:           https://www.elastic.co/products/elasticsearch
Source0:       vitam-elasticsearch-data.service
%global        vitam_service_name vitam-elasticsearch-data

BuildRequires: systemd-units
Requires:      systemd
Requires:      java-11-openjdk-headless
Requires:      elasticsearch = %{version}
Requires:	   vitam-elasticsearch-analysis-icu = %{version}
Requires:      vitam-user-vitamdb

%description
Elasticsearch is a distributed RESTful search engine built for the cloud.

%prep

%install
mkdir -p %{buildroot}/usr/lib/systemd/system
cp %{SOURCE0} %{buildroot}/usr/lib/systemd/system/vitam-elasticsearch-data.service

%pre

%post
%systemd_post vitam-elasticsearch-data.service

%preun
%systemd_preun  vitam-elasticsearch-data.service

%postun
%systemd_postun  vitam-elasticsearch-data.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
/usr/lib/systemd/system/vitam-elasticsearch-data.service

%doc


%changelog
* Tue Oct 12 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
