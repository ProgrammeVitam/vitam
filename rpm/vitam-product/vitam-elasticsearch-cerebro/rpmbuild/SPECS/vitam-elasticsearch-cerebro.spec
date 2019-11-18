Name:          vitam-elasticsearch-cerebro
Version:       0.8.5
Release:       1%{?dist}
Summary:       Cerebro is a simple web administration tool for elasticsearch written in JavaScript + AngularJS + jQuery + Twitter bootstrap.
Group:         Applications/File
License:       MIT Licence
BuildArch:     noarch
URL:           https://github.com/lmenezes/cerebro
Source0:       https://github.com/lmenezes/cerebro/releases/download/v%{version}/cerebro-%{version}.zip
Source1:       vitam-elasticsearch-cerebro.service
Source2:       application.conf

BuildRequires: systemd-units
Requires:      systemd
Requires:      java-1.8.0
Requires:      vitam-user-vitam

%global cerebro_folder vitam/app/cerebro
%global cerebro_conffolder vitam/conf/cerebro
%global cerebro_datafolder vitam/data/cerebro
%global cerebro_tmpfolder vitam/tmp/cerebro

%description
Cerebro is an open source(MIT License) elasticsearch web admin tool built using Scala, Play Framework, AngularJS and Bootstrap.

%prep
%setup -q -c

%build

%install
# On crée l'arborescence cible
mkdir -p %{buildroot}/%{cerebro_folder}
mkdir -p %{buildroot}/%{cerebro_conffolder}
mkdir -p %{buildroot}/%{cerebro_datafolder}
mkdir -p %{buildroot}/%{cerebro_tmpfolder}

mkdir -p %{buildroot}/usr/lib/systemd/system
# On pousse les fichiers
cp -rp cerebro-%{version}/* %{buildroot}/%{cerebro_folder}
cp %{SOURCE1} %{buildroot}/usr/lib/systemd/system/vitam-elasticsearch-cerebro.service
cp %{SOURCE2} %{buildroot}/%{cerebro_conffolder}/application.conf


%pre

%post
%systemd_post %{name}.service

%preun
%systemd_preun  %{name}.service

%postun
%systemd_postun  %{name}.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%dir /%{cerebro_folder}
%attr(750, vitam, vitam) /%{cerebro_folder}
%dir /%{cerebro_conffolder}
%attr(750, vitam, vitam) /%{cerebro_conffolder}
%config(noreplace)  /%{cerebro_conffolder}/application.conf
%dir /%{cerebro_datafolder}
%attr(750, vitam, vitam) /%{cerebro_datafolder}
%dir /%{cerebro_tmpfolder}
%attr(750, vitam, vitam) /%{cerebro_tmpfolder}
%defattr(-,root,root,-)
/usr/lib/systemd/system/vitam-elasticsearch-cerebro.service



%doc


%changelog
* Thu May 29 2017 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
