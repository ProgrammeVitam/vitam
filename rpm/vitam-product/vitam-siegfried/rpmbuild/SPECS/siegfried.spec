Name:          vitam-siegfried
Version:       1.9.6
Release:       1%{?dist}
Summary:       Siegfried is a signature-based file format identification tool
Group:         Applications/File
License:       Apache License - Version 2.0
BuildArch:     x86_64
URL:           http://www.itforarchivists.com/siegfried

Source0:       https://github.com/richardlehane/siegfried/releases/download/v%{version}/siegfried_1-9-6_linux64.zip
Source1:       https://github.com/richardlehane/siegfried/releases/download/v%{version}/data_1-9-6.zip

Requires:      systemd
Requires:      vitam-user-vitam

%global vitam_service_name      siegfried

# https://bugzilla.redhat.com/show_bug.cgi?id=995136#c12
# cf https://fedoraproject.org/wiki/PackagingDrafts/Go#Debuginfo
%global _dwz_low_mem_die_limit 0

%description
Siegfried is a signature-based file format identification tool
It implements:
- the National Archives UK's PRONOM file format signatures
- freedesktop.org's MIME-info file format signatures
- the Library of Congress's FDD file format signatures (BETA).

%prep
%setup -c
%setup -T -D -a 1

%install
mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}/
install sf %{buildroot}/vitam/bin/%{vitam_service_name}/
install roy %{buildroot}/vitam/bin/%{vitam_service_name}/

mkdir -p %{buildroot}/vitam/app/%{vitam_service_name}/
cp -vr siegfried/* %{buildroot}/vitam/app/%{vitam_service_name}/

%pre

%post

%preun

%postun

%clean
rm -rf %{buildroot}

%files
%attr(750, vitam, vitam)      /vitam/app/%{vitam_service_name}

%dir %attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(750, vitam, vitam)      /vitam/bin/%{vitam_service_name}/sf
%attr(750, vitam, vitam)      /vitam/bin/%{vitam_service_name}/roy

%doc

%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
