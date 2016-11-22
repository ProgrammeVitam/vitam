Name:          vitam-mongoclient
Version:       1.4.0
Release:       1%{?dist}
Summary:       Mongoclient is a completely free and open-source mongodb management tool. It’s written in MeteorJS. Additionally it’s fully responsive and have a nice look and feel. Available on most platforms including Mac, Linux, Windows with portable distributions, as an advantage of responsive design and MeteorJS, it’s easier to use Mongoclient on most mobile platforms.
Group:         Applications/Databases
License:       MIT
BuildArch:     x86_64
URL:           http://www.mongoclient.com/
Source0:       https://github.com/rsercano/mongoclient/archive/%{version}.zip
Source1:       vitam-mongoclient.service
Source2:       vitam-mongoclient.conf
%global        vitam_service_name mongoclient
%global        debug_package %{nil}

BuildRequires: systemd-units
Requires:      systemd
Requires:      nodejs
Requires:      vitam-user-vitam

%description
Cross-platform easy to use mongodb management tool

%prep
%setup -q -n mongoclient-%{version}

%build
meteor npm install
meteor build %{_builddir} --architecture os.linux.%{buildarch}
cd %{_builddir}
tar -xzf mongoclient-%{version}.tar.gz
rm mongoclient-%{version}.tar.gz
cd %{_builddir}/bundle/programs/server/
npm install
# Note : following line should not be necessary in theory, but somehow this dependency is missing in the runtime...
npm install tunnel-ssh

%install
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE1} %{buildroot}/%{_unitdir}/

mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}/
mkdir -p %{buildroot}/vitam/conf/%{vitam_service_name}/
cp %{SOURCE2} %{buildroot}/vitam/conf/%{vitam_service_name}/
cp -r %{_builddir}/bundle/* %{buildroot}/vitam/bin/%{vitam_service_name}/
%pre

%post
%systemd_post vitam-mongoclient.service

%preun
%systemd_preun  vitam-mongoclient.service

%postun
%systemd_postun  vitam-mongoclient.service

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%attr(700, root, root) %{_unitdir}/vitam-mongoclient.service
%attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}

%doc


%changelog
* Tue Nov 14 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
