Name:          vitam-elasticsearch-head
Version:       1.x
Release:       1%{?dist}
Summary:       A web front end for an Elasticsearch cluster
Group:         Applications/File
License:       Apache License, Version 2.0
BuildArch:     noarch
URL:           https://github.com/mobz/elasticsearch-head
Source0:       https://github.com/mobz/elasticsearch-head/archive/%{version}.tar.gz


Requires:      elasticsearch < 5

%global head_plugin_folder head

%description
Head plug-in for Elasticsearch is a web front-end for an Elasticsearch cluster.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/usr/share/elasticsearch/plugins/%{head_plugin_folder}
# On pousse les fichiers 
cp -rp elasticsearch-head-1.x/* %{buildroot}/usr/share/elasticsearch/plugins/%{head_plugin_folder}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /usr/share/elasticsearch/plugins/%{head_plugin_folder}
%attr(755, elasticsearch, elasticsearch) /usr/share/elasticsearch/plugins/%{head_plugin_folder}


%doc


%changelog
* Thu Oct 14 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version