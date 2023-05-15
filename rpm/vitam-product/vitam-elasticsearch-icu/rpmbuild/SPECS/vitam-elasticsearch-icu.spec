Name:          vitam-elasticsearch-analysis-icu
Version:       7.17.8
Release:       1%{?dist}
Summary:       The ICU Analysis plugin for ElasticSearch integrates Lucene ICU module into elasticsearch, adding ICU relates analysis components.
Group:         Applications/File
License:       MIT Licence
BuildArch:     noarch
URL:           https://github.com/elastic/elasticsearch/tree/main/plugins/analysis-icu

Source0:       https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-%{version}.zip

Requires:      elasticsearch = %{version}

%global icu_plugin_folder analysis-icu
%global destfolder /usr/share/elasticsearch/plugins/%{icu_plugin_folder}

%description
The ICU Analysis plugin for ElasticSearch integrates Lucene ICU module
into elasticsearch, adding ICU relates analysis components.

%prep
%setup -c

%install
mkdir -p %{buildroot}%{destfolder}
cp -vrp * %{buildroot}%{destfolder}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir %{destfolder}
%attr(755, elasticsearch, elasticsearch) %{destfolder}

%doc

%changelog
* Thu Oct 13 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
