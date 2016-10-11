Name:          vitam-elasticsearch-log
Version:       2.4.0
Release:       1%{?dist}
Summary:       A Distributed RESTful Search Engine (with vitam systemd units)
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     x86_64
URL:           https://www.elastic.co/products/elasticsearch
Source0:       vitam-elasticsearch-log.service
%global        vitam_service_name vitam-elasticsearch-log

BuildRequires: systemd-units
Requires:      systemd
Requires:      java-1.8.0
Requires:      elasticsearch
Requires:      vitam-user-vitamdb

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
* Tue Oct 12 2016 Nicolas Ménétrier <nicolas.menetrier.ext@culture.gouv.fr>
- Initial version
