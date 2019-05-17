#!/usr/bin/env bash
set -e

REPERTOIRE_ROOT="$( cd "$( readlink -f $(dirname ${BASH_SOURCE[0]}) )/../../.." ; pwd )"
REPERTOIRE_CERTIFICAT="${REPERTOIRE_ROOT}/environments/certs"
REPERTOIRE_CA="${REPERTOIRE_ROOT}/pki/ca"
REPERTOIRE_CONFIG="${REPERTOIRE_ROOT}/pki/config"
TEMP_CERTS="${REPERTOIRE_ROOT}/pki/tempcerts"
PARAM_KEY_CHIFFREMENT="rsa:4096"
VAULT_CERTS="${REPERTOIRE_CERTIFICAT}/vault-certs.yml"
VAULT_KEYSTORES="${REPERTOIRE_ROOT}/environments/group_vars/all/vault-keystores.yml"

if [ -f "${REPERTOIRE_ROOT}/vault_pass.txt" ]; then
    ANSIBLE_VAULT_PASSWD="--vault-password-file ${REPERTOIRE_ROOT}/vault_pass.txt"
else
    ANSIBLE_VAULT_PASSWD="--ask-vault-pass"
fi

function read_ansible_var {
    local ANSIBLE_VAR="${1}"
    local ANSIBLE_HOST="${2}"
    local ANSIBLE_CONFIG="${REPERTOIRE_ROOT}/pki/scripts/lib/ansible.cfg"

    ansible ${ANSIBLE_HOST} -i ${ENVIRONNEMENT_FILE} ${ANSIBLE_VAULT_PASSWD} -m debug -a "var=${ANSIBLE_VAR}" \
    | grep "${ANSIBLE_VAR}" | awk -F ":" '{gsub("\\s","",$2); print $2}'
}

# Delete useless files
function purge_directory {
    local DIR_TO_PURGE="${1}"

    if [ ! -d "${DIR_TO_PURGE}" ]; then
        pki_logger "ERROR" "Directory ${DIR_TO_PURGE} does not exists"
        return 1
    fi

    find "${DIR_TO_PURGE}" -type f -name "*.attr" -exec rm -f {} \;
    find "${DIR_TO_PURGE}" -type f -name "*.old"  -exec rm -f {} \;
    find "${DIR_TO_PURGE}" -type f -name "*.req"  -exec rm -f {} \;
}

function generatePassphrase {
    cat /dev/urandom | tr -dc 'a-zA-Z0-9' | head -c 48
}

function normalize_key {
    local KEY="${1}"

    echo "${KEY}" | sed 's/[\\/\.-]/_/g'
}

function getComponentCertPassphrase {
    local KEY_FILE="${1}"
    local RETURN_CODE=0

    if [ ! -f "${VAULT_CERTS}" ]; then
        return 1
    fi

    # Decrypt vault file
    ansible-vault decrypt ${VAULT_CERTS} ${ANSIBLE_VAULT_PASSWD}
    # Try/catch/finally stuff with bash (to make sure the vault stay encrypted)
    {
        # Try
        # Generate bash vars with the yml file:
        #       $certKey_blah
        #       $certKey_blahblah
        #       $certKey_........
        eval $(parse_yaml ${VAULT_CERTS} "certKey_") && \
        # Get the value of the variable we are interested in
        # And store it into another var: $CERT_KEY
        eval $(echo "CERT_KEY=\$certKey_$(normalize_key ${KEY_FILE})") && \
        # Print the $CERT_KEY var
        echo "${CERT_KEY}"
    } || {
        # Catch
        RETURN_CODE=1
        pki_logger "ERROR" "Error while reading certificate passphrase for ${KEY_FILE} in certificates vault: ${VAULT_CERTS}"
    } && {
        # Finally
        if [ "${CERT_KEY}" == "" ]; then
            pki_logger "ERROR" "Error while retrieving the key: ${KEY_FILE}"
            RETURN_CODE=1
        fi
        ansible-vault encrypt ${VAULT_CERTS} ${ANSIBLE_VAULT_PASSWD}
        return ${RETURN_CODE}
    }
}

# KWA TODO: explain & comonize the sed usage ;
# KWA TODO: change replacement string in sed : /_/ ==> /__/
# TODO: produce an example cert vault
function setComponentCertPassphrase {
    local KEY_FILE="${1}"
    local KEY="${2}"
    local RETURN_CODE=0

    # if [ ! -f ${REPERTOIRE_CERTIFICAT}/${KEY_FILE} ]; then
    #     pki_logger "ERROR" "The certificate key file does exists: ${REPERTOIRE_CERTIFICAT}/${KEY_FILE}"
    #     return 1
    # fi

    # Manage initial state (non-existing vault)
    if [ -f "${VAULT_CERTS}" ]; then
        ansible-vault decrypt ${VAULT_CERTS} ${ANSIBLE_VAULT_PASSWD}
    else
        if [ -f "${VAULT_CERTS}.example" ]; then
            rm -f "${VAULT_CERTS}.example"
        fi
    fi

    # Try/catch/finally stuff with bash (to make sure the vault stay encrypted)
    {
        # Try
        # Add key to example vault
        normalize_key "${KEY_FILE}: changeme" >> "${VAULT_CERTS}.example" && \
        # Add key to vault
        normalize_key "${KEY_FILE}: ${KEY}" >> "${VAULT_CERTS}"
    } || {
        # Catch
        RETURN_CODE=1
        pki_logger "ERROR" "Error while writing to vault file: ${VAULT_CERTS}"
    } && {
        # Finally
        ansible-vault encrypt ${VAULT_CERTS} ${ANSIBLE_VAULT_PASSWD}
        return ${RETURN_CODE}
    }
}

function pki_logger {
    if (( ${#} >= 2 )); then
        local ERR_LEVEL="${1}"
        local MESSAGE="${2}"
    else
        local ERR_LEVEL="INFO"
        local MESSAGE="${1}"
    fi
    echo "[${ERR_LEVEL}] [$(basename ${0}): ${FUNCNAME[ 1 ]}] ${MESSAGE}" 1>&2
}

# https://gist.github.com/pkuczynski/8665367
function parse_yaml {
    local prefix=$2
    local s='[[:space:]]*' w='[a-zA-Z0-9_]*' fs=$(echo @|tr @ '\034')
    sed -ne "s|^\($s\)\($w\)$s:$s\"\(.*\)\"$s\$|\1$fs\2$fs\3|p" \
        -e "s|^\($s\)\($w\)$s:$s\(.*\)$s\$|\1$fs\2$fs\3|p"  $1 |
    awk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }'
}
