Name:    vitam-mongodb-exporter
Version: 0.47.2
Release: 1%{?dist}
Summary: mongodb exporter for prometheus.
License: Apache License 2.0
URL:     https://github.com/percona/mongodb_exporter

Source0: https://github.com/percona/mongodb_exporter/releases/download/v%{version}/mongodb_exporter-%{version}.linux-amd64.tar.gz

Requires:      vitam-user-vitam

%global appfolder /vitam/app/mongodb_exporter
%global binfolder /vitam/bin/mongodb_exporter

%description
mongodb exporter
This package contains binary to export node metrics to prometheus.

%prep
%setup -n mongodb_exporter-%{version}.linux-amd64

%install
mkdir -p %{buildroot}%{binfolder}
install mongodb_exporter %{buildroot}%{binfolder}/mongodb_exporter

mkdir -p %{buildroot}%{appfolder}
install LICENSE %{buildroot}%{appfolder}/LICENSE

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %attr(750, vitam, vitam) %{binfolder}
%attr(750, vitam, vitam)      %{binfolder}/mongodb_exporter

%dir %attr(750, vitam, vitam) %{appfolder}
%attr(644, vitam, vitam)      %{appfolder}/LICENSE

%doc

%changelog
* Mon Mar 04 2024 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
