Name:          vitam-siegfried
Version:       1.7.12
Release:       1%{?dist}
Summary:       Siegfried is a signature-based file format identification tool
Group:         Applications/File
License:       Apache License - Version 2.0
BuildArch:     x86_64
URL:           http://www.itforarchivists.com/siegfried
Source0:       https://github.com/richardlehane/siegfried/archive/v%{version}.tar.gz
Source1:       siegfried.env
Source2:       vitam-siegfried.service

BuildRequires: systemd-units
BuildRequires: golang >= 1.6
Requires:      systemd
Requires:      vitam-user-vitam

%global vitam_service_name      siegfried
%global debug_package           %{nil}

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
%setup -q -n siegfried-%{version}

%build
# *** ERROR: No build ID note found in /.../BUILDROOT/etcd-2.0.0-1.rc1.fc22.x86_64/usr/bin/etcd
# cf https://fedoraproject.org/wiki/PackagingDrafts/Go#Debuginfo
# TODO: we disabled debug_package build, so we should be able to build siegfried normally -> remove gobuild function
function gobuild { go build -a -ldflags "-B 0x$(head -c20 /dev/urandom|od -An -tx1|tr -d ' \n')" -v "$@"; }

mkdir -p ./_build/src/github.com/richardlehane
ln -s $(pwd) ./_build/src/github.com/richardlehane/siegfried
export GOPATH=$(pwd)/_build
gobuild -o sf github.com/richardlehane/siegfried/cmd/sf
gobuild -o roy github.com/richardlehane/siegfried/cmd/roy

# fix for strange behavior change ; give write access to delete...
chmod -R 700 ./_build
rm -rf ./_build
# Le rep de build contient des fichiers rpm posant probleme a createrepo
find . -type f -name '*.rpm' -exec rm -f {} \;

%install
# On pousse les binaire
mkdir -p %{buildroot}/vitam/bin/%{vitam_service_name}/
cp sf %{buildroot}/vitam/bin/%{vitam_service_name}/
cp roy %{buildroot}/vitam/bin/%{vitam_service_name}/
# On copie le rep data
mkdir -p %{buildroot}/vitam/app/%{vitam_service_name}/
cp -r ./cmd/roy/data/* %{buildroot}/vitam/app/%{vitam_service_name}/
# conf
mkdir -p %{buildroot}/vitam/conf/%{vitam_service_name}/sysconfig
cp %{SOURCE1} %{buildroot}/vitam/conf/%{vitam_service_name}/sysconfig/%{vitam_service_name}
# unit dir
mkdir -p %{buildroot}/%{_unitdir}
cp %{SOURCE2} %{buildroot}/%{_unitdir}/

%pre

%post
%systemd_post %{name}.service

%preun
%systemd_preun %{name}.service

%postun
%systemd_postun %{name}.service

%clean
rm -rf %{buildroot}
rm -f %{SOURCE0}

%files
%defattr(-,root,root,-)
%dir %attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}
%dir %attr(750, vitam, vitam) /vitam/conf/%{vitam_service_name}/sysconfig
%config(noreplace)            /vitam/conf/%{vitam_service_name}/sysconfig/%{vitam_service_name}
%attr(640, vitam, vitam)      /vitam/conf/%{vitam_service_name}/sysconfig/%{vitam_service_name}
%attr(750, vitam, vitam)      /vitam/app/%{vitam_service_name}

%{_unitdir}/%{name}.service

%dir %attr(750, vitam, vitam) /vitam/bin/%{vitam_service_name}
%attr(755, vitam, vitam)      /vitam/bin/%{vitam_service_name}/sf
%attr(755, vitam, vitam)      /vitam/bin/%{vitam_service_name}/roy

%doc


%changelog
* Fri Aug 19 2016 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
