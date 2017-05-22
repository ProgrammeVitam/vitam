Name:          vitam-resolvconf
Version:       1.0.0
Release:       1%{?dist}
Summary:       Service files to ensure consul dns is set in /etc/resolv.conf
Group:         System Environment/Daemons
License:       CeCILL 2.1
BuildArch:     noarch
URL:           https://github.com/ProgrammeVitam/vitam
Source0:       vitam_dns_localhost_enforce.service
Source1:       vitam_dns_localhost_enforce.path
Source2:       vitam_dns_localhost_enforce.sh

%global        vitam_service_name      vitam_dns_localhost_enforce

BuildRequires: systemd-units
Requires:      systemd

%description
Service files to ensure consul dns is set in /etc/resolv.conf

%prep

%install
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE0} %{buildroot}/%{_unitdir}/%{vitam_service_name}.service
cp %{SOURCE1} %{buildroot}/%{_unitdir}/%{vitam_service_name}.path
mkdir -p %{buildroot}/vitam/script/system
cp %{SOURCE2} %{buildroot}/vitam/script/system/%{vitam_service_name}.sh

%pre

%post
%systemd_post %{vitam_service_name}.service
%systemd_post %{vitam_service_name}.path

%preun
%systemd_preun %{vitam_service_name}.service
%systemd_preun %{vitam_service_name}.path

%postun
%systemd_postun %{vitam_service_name}.service
%systemd_postun %{vitam_service_name}.path

%clean
rm -rf %{buildroot}

%files
%defattr(-,root,root,-)
%{_unitdir}/%{vitam_service_name}.service
%{_unitdir}/%{vitam_service_name}.path
%dir %attr(750, root, vitam-admin) /vitam/script/system
/vitam/script/system/%{vitam_service_name}.sh

%doc


%changelog
* Mon May 22 2017 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
