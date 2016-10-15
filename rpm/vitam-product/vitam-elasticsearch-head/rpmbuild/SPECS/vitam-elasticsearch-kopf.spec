Name:          vitam-elasticsearch-head
Version:       1.x
Release:       1%{?dist}
Summary:       A web front end for an Elasticsearch cluster
Group:         Applications/File
License:       Apache License, Version 2.0
BuildArch:     x86_64
URL:           https://github.com/mobz/elasticsearch-head
Source0:       https://github.com/mobz/elasticsearch-head/archive/%{version}.tar.gz


Requires:      elasticsearch < 5

%global vitam_service_name head

# https://bugzilla.redhat.com/show_bug.cgi?id=995136#c12
# cf https://fedoraproject.org/wiki/PackagingDrafts/Go#Debuginfo
# %global _dwz_low_mem_die_limit 0

%description
Head plug-in for Elasticsearch is a web front-end for an Elasticsearch cluster.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/usr/share/elasticsearch/plugins/%{vitam_service_name}
# On pousse les fichiers 
cp -rp elasticsearch-head-1.x/* %{buildroot}/usr/share/elasticsearch/plugins/%{vitam_service_name}

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
* Thu Oct 14 2016 Kristopher Waltzer <kristopher.waltzer.ext@culture.gouv.fr>
- Initial version