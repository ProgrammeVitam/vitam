Name:          vitam-mongo
Version:       3.2.10
Release:       1%{?dist}
Summary:       MongoDB open source document-oriented database system (metapackage with Vitam extras)
Group:         Applications/Databases
License:       AGPL 3.0
BuildArch:     x86_64
URL:           http://www.mongodb.org
Source0:       vitam-mongoc.service
Source1:       vitam-mongod.service
Source2:       vitam-mongos.service
%global        vitam_service_name vitam-mongo

BuildRequires: systemd-units
Requires:      systemd
Requires:      mongodb-org
Requires:      vitam-user-vitamdb

%description
MongoDB is built for scalability, performance and high availability, scaling from single server deployments to large, complex multi-site architectures. By leveraging in-memory computing, MongoDB provides high performance for both reads and writes. MongoDB’s native replication and automated failover enable enterprise-grade reliability and operational flexibility.

MongoDB is an open-source database used by companies of all sizes, across all industries and for a wide variety of applications. It is an agile database that allows schemas to change quickly as applications evolve, while still providing the functionality developers expect from traditional databases, such as secondary indexes, a full query language and strict consistency.

MongoDB has a rich client ecosystem including hadoop integration, officially supported drivers for 10 programming languages and environments, as well as 40 drivers supported by the user community.

MongoDB features:
* JSON Data Model with Dynamic Schemas
* Auto-Sharding for Horizontal Scalability
* Built-In Replication for High Availability
* Rich Secondary Indexes, including geospatial
* TTL indexes
* Text Search
* Aggregation Framework & Native MapReduce

This metapackage will install the mongo shell, import/export tools, other client utilities, server software, default configuration, init.d scripts, and Vitam extras for mongodb (systemd unit, vitamdb user, ...).

%prep

%install
mkdir -p %{buildroot}/usr/lib/systemd/system
cp %{SOURCE0} %{buildroot}/usr/lib/systemd/system/vitam-mongoc.service
cp %{SOURCE1} %{buildroot}/usr/lib/systemd/system/vitam-mongod.service
cp %{SOURCE2} %{buildroot}/usr/lib/systemd/system/vitam-mongos.service

%pre

%post
%systemd_post vitam-mongoc.service
%systemd_post vitam-mongod.service
%systemd_post vitam-mongos.service

%preun
%systemd_preun  vitam-mongoc.service
%systemd_preun  vitam-mongod.service
%systemd_preun  vitam-mongos.service

%postun
%systemd_postun  vitam-mongoc.service
%systemd_postun  vitam-mongod.service
%systemd_postun  vitam-mongos.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
/usr/lib/systemd/system/vitam-mongoc.service
/usr/lib/systemd/system/vitam-mongod.service
/usr/lib/systemd/system/vitam-mongos.service

%doc


%changelog
* Tue Oct 11 2016 Nicolas Ménétrier <nicolas.menetrier.ext@culture.gouv.fr>
- Initial version
