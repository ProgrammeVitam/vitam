Name:          vitam-elasticsearch-kopf
Version:       2.0
Release:       1%{?dist}
Summary:       kopf is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.
Group:         Applications/File
License:       MIT Licence
BuildArch:     x86_64
URL:           https://github.com/lmenezes/elasticsearch-kopf
Source0:       https://github.com/lmenezes/elasticsearch-kopf/archive/%{version}.tar.gz

Requires:      elasticsearch < 5

%global vitam_service_name kopf

# https://bugzilla.redhat.com/show_bug.cgi?id=995136#c12
# cf https://fedoraproject.org/wiki/PackagingDrafts/Go#Debuginfo
# %global _dwz_low_mem_die_limit 0

%description
kopf is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.

It offers an easy way of performing common tasks on an elasticsearch cluster. Not every single API is covered by this plugin, but it does offer a REST client which allows you to explore the full potential of the ElasticSearch API.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/usr/share/elasticsearch/plugins/%{vitam_service_name}
# On pousse les fichiers 
cp -rp elasticsearch-kopf-2.0/* %{buildroot}/usr/share/elasticsearch/plugins/%{vitam_service_name}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /usr/share/elasticsearch/plugins/%{vitam_service_name}
%attr(755, elasticsearch, elasticsearch) /usr/share/elasticsearch/plugins/%{vitam_service_name}


%doc


%changelog
* Thu Oct 13 2016 Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
- Initial version