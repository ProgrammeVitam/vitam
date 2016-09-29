Name: vitam-user-vitam
Version: 0.7.0
Release: 4%{?dist}
Summary: Package used to create the vitam user and group	
BuildArch: noarch
License: Cecill v2.1

Requires(pre):  shadow-utils


%description
Package to create the vitam user and group


%prep


%build


%install


%files

%pre
getent group  vitam >/dev/null || groupadd -g 2000 vitam 
getent passwd vitam >/dev/null || useradd -u 2000 -g 2000 -s /bin/bash -c "Vitam application user" vitam

getent group  vitam-admin >/dev/null || groupadd -g 3000 vitam-admin 
echo '%vitam-admin    ALL=NOPASSWD: /bin/systemctl stop vitam-*,/bin/systemctl start  vitam-*,/bin/systemctl restart  vitam-*' >> /etc/sudoers
echo '%vitam-admin    ALL=NOPASSWD: /bin/systemctl stop consul,/bin/systemctl start consul,/bin/systemctl restart consul' >> /etc/sudoers
echo '%vitam-admin    ALL=NOPASSWD: /bin/systemctl stop logstash,/bin/systemctl start logstash,/bin/systemctl restart logstash' >> /etc/sudoers
echo '%vitam-admin    ALL=NOPASSWD: /bin/systemctl stop kibana,/bin/systemctl start kibana,/bin/systemctl restart kibana' >> /etc/sudoers
echo '%vitam-admin    ALL=NOPASSWD: /bin/systemctl stop curator,/bin/systemctl start curator,/bin/systemctl restart curator' >> /etc/sudoers
%changelog

