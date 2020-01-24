%global selinuxtype	targeted
%global moduletype	contrib
%global modulename	vitam

Name: vitam-selinux
Version: 1.0
Release: 1%{?dist}
Summary: SELinux security policy module for vitam
License: CeCILL 2.1
URL:     https://github.com/ProgrammeVitam/vitam
Source0: %{modulename}.fc
Source1: %{modulename}.te
Source2: Makefile
BuildArch: noarch
BuildRequires: selinux-policy
BuildRequires: selinux-policy-devel
Requires: policycoreutils-python

%description
SELinux security policy module for vitam

%prep
rm -rf vitam*
cp %{SOURCE0} %{SOURCE1} %{SOURCE2} .

%build
make

%install
install -d %{buildroot}%{_datadir}/selinux/packages
install -m 0644 %{modulename}.pp.bz2 %{buildroot}%{_datadir}/selinux/packages
bzip2 -d %{buildroot}%{_datadir}/selinux/packages/%{modulename}.pp.bz2

%post
# Install the module
semodule -i %{_datadir}/selinux/packages/vitam.pp
# If it's an update, remove managed ports before adding them again
if [ $1 -gt 1 ]; then
    semanage port -D -t vitam_worker_port_t
    semanage port -D -t vitam_logbook_port_t
    semanage port -D -t vitam_workspace_port_t
fi
# Relabel
if [ -d /vitam/script/worker ]; then restorecon -R /vitam/script/worker; fi
if [ -d /vitam/conf/worker ]; then restorecon -R /vitam/conf/worker; fi
if [ -d /vitam/lib/worker ]; then restorecon -R /vitam/lib/worker; fi
if [ -d /vitam/log/worker ]; then restorecon -R /vitam/log/worker; fi
if [ -d /vitam/tmp/worker ]; then restorecon -R /vitam/tmp/worker; fi
if [ -d /vitam/run/worker ]; then restorecon -R /vitam/run/worker; fi
if [ -f /usr/lib/systemd/system/vitam-worker.service ]; then
    restorecon /usr/lib/systemd/system/vitam-worker.service
fi

%postun
# If it's a real uninstall (not an update), remove everything
if [ $1 -eq 0 ]; then
    semanage port -D -t vitam_worker_port_t
    semanage port -D -t vitam_logbook_port_t
    semanage port -D -t vitam_workspace_port_t
    semodule -r vitam
    # Is filesystem relabeling really necessary ?
    if [ -d /vitam/script/worker ]; then restorecon -R /vitam/script/worker; fi
    if [ -d /vitam/conf/worker ]; then restorecon -R /vitam/conf/worker; fi
    if [ -d /vitam/lib/worker ]; then restorecon -R /vitam/lib/worker; fi
    if [ -d /vitam/log/worker ]; then restorecon -R /vitam/log/worker; fi
    if [ -d /vitam/tmp/worker ]; then restorecon -R /vitam/tmp/worker; fi
    if [ -d /vitam/run/worker ]; then restorecon -R /vitam/run/worker; fi
    if [ -f /usr/lib/systemd/system/vitam-worker.service ]; then
        restorecon /usr/lib/systemd/system/vitam-worker.service
    fi
fi

%files
%attr(0644,root,root) %{_datadir}/selinux/packages/%{modulename}.pp

%doc


%changelog
* Fri Oct 18 2019 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
