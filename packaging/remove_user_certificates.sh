#!/usr/bin/env bash

echo "remove_user_certificates.sh"
pwd
ls -l ../deployment/environments/group_vars/all/vitam_security.yml
sed -i 's/admin_personal_certs\s*:\s*\[[^]]*\]/admin_personal_certs: \[\]/' ../deployment/environments/group_vars/all/vitam_security.yml
