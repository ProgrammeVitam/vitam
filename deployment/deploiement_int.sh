#!/bin/sh
ENVT=int
SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )
ansible-playbook ${SCRIPTPATH}/ansible-vitam/vitam.yml -i ${SCRIPTPATH}/environments/hosts.${ENVT}.deploy
