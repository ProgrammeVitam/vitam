%define        version 6.7.0
Name:          vitam-elasticsearch-analysis-icu
Version:        %{version}
Release:       1%{?dist}
Summary:       The ICU Analysis plugin for ElasticSearch integrates Lucene ICU module into elasticsearch, adding ICU relates analysis components.
Group:         Applications/File
License:       MIT Licence
BuildArch:     noarch
URL:           https://github.com/elastic/elasticsearch/tree/5.6/plugins/analysis-icu
Source0:       https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-%{version}.zip
Requires:      elasticsearch = %{version}


%global icu_plugin_folder analysis-icu

%description
The ICU Analysis plugin for ElasticSearch integrates Lucene ICU module
into elasticsearch, adding ICU relates analysis components.

%prep
%setup -q -c

%build
true

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/usr/share/elasticsearch/plugins/%{icu_plugin_folder}
# On pousse les fichiers
cp -rp * %{buildroot}/usr/share/elasticsearch/plugins/%{icu_plugin_folder}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /usr/share/elasticsearch/plugins/%{icu_plugin_folder}
%attr(755, elasticsearch, elasticsearch) /usr/share/elasticsearch/plugins/%{icu_plugin_folder}


%doc


%changelog
* Thu Oct 13 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
