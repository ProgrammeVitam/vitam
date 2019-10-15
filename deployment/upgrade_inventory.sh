#!/bin/bash

. "$(dirname $0)/pki/scripts/lib/functions.sh"

# Vérification des paramètres
if [ "${1}" == "" ]; then
    pki_logger "ERROR" "This script needs to know on which environment you want to apply to !"
    exit 1
fi
ENVIRONNEMENT_FILE="${1}"

if [ -f ${ENVIRONNEMENT_FILE} ]
then
    pki_logger "Upgrading zone..."
    {
        sed -E -i 's/zone-([a-z]+)/zone_\1/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory zones groups successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded for zones !"
        exit 1
    }

    pki_logger "Upgrading ansible group names with 4 members..."
    {
        sed -E -i 's/^\[([a-zA-Z]+)-([a-z]+)-([a-z]+)-([a-z]+)\]$/[\1_\2_\3_\4]/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory group names with 4 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible group names with 3 members..." 
    {
        sed -E -i 's/^\[([a-zA-Z]+)-([a-z]+)-([a-z]+)(-[a-z]+)?\]$/[\1_\2_\3]/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory group names with 3 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible group names with 2 members..."
    {
        sed -E -i 's/^\[([a-zA-Z]+)-([a-z]+)\]$/[\1_\2]/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory group names with 2 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible super group names..."
    {
        sed -E -i 's/^\[([a-zA-Z]+)-([a-z]+)-([a-z]+)(-[a-z]+)?\]$/[\1_\2_\3]/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory super group names successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible host group names with 4 members..."
    {
        sed -E -i 's/hosts-([a-z]+)-([a-z]+)-([a-z]+)-([a-z]+)/hosts_\1_\2_\3_\4/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory hosts group names with 4 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible host group names with 3 members..."
    {
        sed -E -i 's/hosts-([a-z]+)-([a-z]+)-([a-z]+)/hosts_\1_\2_\3/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory hosts group names with 3 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible host group names with 2 members..."
    {
        sed -E -i 's/hosts-([a-z]+)-([a-z]+)/hosts_\1_\2/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory hosts group names with 2 members successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading ansible host group names with 1 member..."
    {
        sed -E -i 's/hosts-([a-z]+)/hosts_\1/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory hosts group names with 1 member successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }

    pki_logger "Upgrading log-servers..."
    {
        sed -i 's/log-servers/log_servers/g' ${ENVIRONNEMENT_FILE}
        pki_logger "Success" "Ansible inventory log-servers successfully upgraded !"
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} could not be upgraded !"
        exit 1
    }
    pki_logger "Validation..."
    { 
        OCCURENCE=$(egrep "hosts-|zone-" ${ENVIRONNEMENT_FILE} |wc -l)
        if [ ${OCCURENCE} -eq 0 ]
        then
            pki_logger "Success" "File ${ENVIRONMENT_FILE} successfully validated !"
        else
            pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} is not correctly upgraded !"
            exit 1;
        fi
    } || {
        pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} badly upgraded !"
        exit 1
    }
    pki_logger "Success" "END OF SCRIPT : Ansible inventory fully successfully upgraded !"
else
    pki_logger "ERROR" "File ${ENVIRONNEMENT_FILE} does not exist !"
fi