Name:          vitam-elasticsearch-cerebro
Version:       0.9.4
Release:       1%{?dist}
Summary:       Cerebro is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.
Group:         Applications/File
License:       MIT Licence
BuildArch:     noarch
URL:           https://github.com/lmenezes/cerebro

Source0:       https://github.com/lmenezes/cerebro/releases/download/v%{version}/cerebro-%{version}.zip

Requires:      systemd
Requires:      java-11-openjdk-headless
Requires:      vitam-user-vitam

%global appfolder /vitam/app/cerebro

%description
Cerebro is an open source(MIT License) elasticsearch web admin tool built using Scala, Play Framework, AngularJS and Bootstrap.

%prep
%setup -n cerebro-%{version}

%install
mkdir -p %{buildroot}%{appfolder}
cp -vrp * %{buildroot}%{appfolder}

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%dir %{appfolder}
%attr(750, vitam, vitam) %{appfolder}

%doc

%changelog
* Thu May 29 2017 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
