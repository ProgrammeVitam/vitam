#!/usr/bin/env bash

echo "remove_griffins.sh"
pwd
ls -l ../deployment/environments/group_vars/all/main/main.yml
sed -i 's/vitam_griffins\s*:\s*\[[^]]*\]/vitam_griffins: \[\]/' ../deployment/environments/group_vars/all/main/main.yml
