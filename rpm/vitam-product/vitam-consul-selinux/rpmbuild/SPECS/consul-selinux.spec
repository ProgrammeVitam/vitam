%global selinuxtype	targeted
%global moduletype	contrib
%global modulename	vitam_consul

Name: vitam-consul-selinux
Version: 1.0
Release: 1%{?dist}
Summary: SELinux security policy module vitam-consul
License: CeCILL 2.1
URL:     https://github.com/ProgrammeVitam/vitam
Source0: %{modulename}.fc
Source1: %{modulename}.te
Source2: Makefile
BuildArch: noarch
BuildRequires: selinux-policy
BuildRequires: selinux-policy-devel
Requires: vitam-consul
Requires: policycoreutils-python

%description
SELinux security policy module vitam-consul

%prep
rm -rf vitam_consul*
cp %{SOURCE0} %{SOURCE1} %{SOURCE2} .

%build
make

%install
install -d %{buildroot}%{_datadir}/selinux/packages
install -m 0644 %{modulename}.pp.bz2 %{buildroot}%{_datadir}/selinux/packages
bzip2 -d %{buildroot}%{_datadir}/selinux/packages/%{modulename}.pp.bz2

%post
# Install the module
semodule -i %{_datadir}/selinux/packages/vitam_consul.pp
# If it's an update, remove managed ports before adding them again
if [ $1 -gt 1 ]; then
    semanage port -D -t vitam_consul_port_t
fi
# Add ports consul is using
semanage port -a -t vitam_consul_port_t -p tcp 9900
semanage port -a -t vitam_consul_port_t -p tcp 29900
semanage port -a -t vitam_consul_port_t -p tcp 8500
semanage port -a -t vitam_consul_port_t -p tcp 8302
semanage port -a -t vitam_consul_port_t -p tcp 8301
semanage port -a -t vitam_consul_port_t -p tcp 8300
semanage port -a -t vitam_consul_port_t -p udp 8301
semanage port -a -t vitam_consul_port_t -p udp 8302
# Relabel
restorecon -R /vitam/bin/consul
restorecon -R /vitam/conf/consul
restorecon -R /vitam/data/consul
restorecon -R /vitam/tmp/consul
restorecon /usr/lib/systemd/system/vitam-consul.service

%postun
# If it's a real uninstall (not an update), remove everything
if [ $1 -eq 0 ]; then
    semanage port -D -t vitam_consul_port_t
    # Seems to fail at uninstall 
    # libsemanage.semanage_direct_remove_key: Unable to remove module vitam_consul at priority 400. (No such file or directory). 
    semodule -r vitam_consul  
    # 
    restorecon -R /vitam/bin/consul
    restorecon -R /vitam/conf/consul
    restorecon -R /vitam/data/consul
    restorecon -R /vitam/tmp/consul
    restorecon /usr/lib/systemd/system/vitam-consul.service
fi

%files
%attr(0644,root,root) %{_datadir}/selinux/packages/%{modulename}.pp

%doc


%changelog
* Fri Oct 18 2019 French Prime minister Office/SGMAP/DINSIC/Vitam Program <contact.vitam@culture.gouv.fr>
- Initial version
