%define        version 7.8.1
Name:          vitam-elasticsearch-log
Version:       %{version}
Release:       2%{?dist}
Summary:       A Distributed RESTful Search Engine (with vitam systemd units)
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     x86_64
URL:           https://www.elastic.co/products/elasticsearch
Source0:       vitam-elasticsearch-log.service
%global        vitam_service_name vitam-elasticsearch-log

BuildRequires: systemd-units
Requires:      systemd
Requires:      java-11-openjdk-headless
Requires:      elasticsearch = %{version}
Requires:      vitam-user-vitamdb
Conflicts:     elasticsearch < 5
Packager:	   Programme VITAM

%description
Elasticsearch is a distributed RESTful search engine built for the cloud.

%prep

%install
mkdir -p %{buildroot}/usr/lib/systemd/system
cp %{SOURCE0} %{buildroot}/usr/lib/systemd/system/vitam-elasticsearch-log.service

%pre

%post
%systemd_post vitam-elasticsearch-log.service

%preun
%systemd_preun  vitam-elasticsearch-log.service

%postun
%systemd_postun  vitam-elasticsearch-log.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
/usr/lib/systemd/system/vitam-elasticsearch-log.service

%doc


%changelog
* Tue Oct 12 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
