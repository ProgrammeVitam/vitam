Name:          vitam-elasticsearch-kopf
Version:       2.0
Release:       1%{?dist}
Summary:       kopf is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.
Group:         Applications/File
License:       MIT Licence
BuildArch:     noarch
URL:           https://github.com/lmenezes/elasticsearch-kopf
Source0:       https://github.com/lmenezes/elasticsearch-kopf/archive/%{version}.tar.gz

Requires:      elasticsearch < 5

%global kopf_plugin_folder kopf

%description
Kopf is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.

It offers an easy way of performing common tasks on an elasticsearch cluster. Not every single API is covered by this plugin, but it does offer a REST client which allows you to explore the full potential of the ElasticSearch API.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/usr/share/elasticsearch/plugins/%{kopf_plugin_folder}
# On pousse les fichiers 
cp -rp elasticsearch-kopf-2.0/* %{buildroot}/usr/share/elasticsearch/plugins/%{kopf_plugin_folder}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /usr/share/elasticsearch/plugins/%{kopf_plugin_folder}
%attr(755, elasticsearch, elasticsearch) /usr/share/elasticsearch/plugins/%{kopf_plugin_folder}


%doc


%changelog
* Thu Oct 13 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
