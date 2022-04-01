#!/usr/bin/env bash
set -e

REPERTOIRE_ROOT="$( cd "$( readlink -f $(dirname ${BASH_SOURCE[0]}) )/../../.." ; pwd )"
REPERTOIRE_CERTIFICAT="${REPERTOIRE_ROOT}/environments/certs"
REPERTOIRE_CA="${REPERTOIRE_ROOT}/pki/ca"
REPERTOIRE_CONFIG="${REPERTOIRE_ROOT}/pki/config"
TEMP_CERTS="${REPERTOIRE_ROOT}/pki/tempcerts"
PARAM_KEY_CHIFFREMENT="rsa:4096"
VAULT_KEYSTORES="${REPERTOIRE_ROOT}/environments/group_vars/all/vault-keystores.yml"

if [ -f "${REPERTOIRE_ROOT}/vault_pass.txt" ]; then
    ANSIBLE_VAULT_PASSWD="--vault-password-file ${REPERTOIRE_ROOT}/vault_pass.txt"
else
    ANSIBLE_VAULT_PASSWD="--ask-vault-pass"
fi
if [ -f "${REPERTOIRE_ROOT}/vault_pki.pass" ]; then
    ANSIBLE_VAULT_PKI_PASSWD="--vault-password-file ${REPERTOIRE_ROOT}/vault_pki.pass"
else
    ANSIBLE_VAULT_PKI_PASSWD="--ask-vault-pass"
fi

# Check if gawk is present
hash gawk

function read_ansible_var {
    local ANSIBLE_VAR="${1}"
    local ANSIBLE_HOST="${2}"

    ANSIBLE_CONFIG="${REPERTOIRE_ROOT}/pki/scripts/lib/ansible.cfg" \
    ansible ${ANSIBLE_HOST} -i ${ENVIRONNEMENT_FILE} ${ANSIBLE_VAULT_PASSWD} -m debug -a "var=${ANSIBLE_VAR}" \
    | grep "${ANSIBLE_VAR}" | gawk -F ":" '{gsub("\\s","",$2); print $2}'
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

function initVault {
    local TYPE="${1}"

    VAULT_FILE="${REPERTOIRE_CERTIFICAT}/vault-${TYPE}.yml"

    if [ -f "${VAULT_FILE}" ]; then
        pki_logger "Réinitialisation du fichier ${VAULT_FILE}"
        ansible-vault decrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
        echo '---' > ${VAULT_FILE}
        ansible-vault encrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
    else
        pki_logger "Création du fichier ${VAULT_FILE}"
        mkdir -p "${VAULT_FILE%/*}"
        touch ${VAULT_FILE}
        ansible-vault encrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
    fi

    if [ -f "${VAULT_FILE}.example" ]; then
        rm -f "${VAULT_FILE}.example"
    fi
}

function getComponentPassphrase {
    local TYPE="${1}"
    local KEY_FILE="${2}"
    local RETURN_CODE=0

    VAULT_FILE="${REPERTOIRE_CERTIFICAT}/vault-${TYPE}.yml"

    if [ ! -f "${VAULT_FILE}" ]; then
        return 1
    fi

    # Decrypt vault file
    ansible-vault decrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
    # Try/catch/finally stuff with bash (to make sure the vault stay encrypted)
    {
        # Try
        # Generate bash vars with the yml file:
        #       $certKey_blah
        #       $certKey_blahblah
        #       $certKey_........
        eval $(parse_yaml ${VAULT_FILE} "certKey_") && \
        # Get the value of the variable we are interested in
        # And store it into another var: $CERT_KEY
        eval $(echo "CERT_KEY=\$certKey_$(normalize_key ${KEY_FILE})") && \
        # Print the $CERT_KEY var
        echo "${CERT_KEY}"
    } || {
        # Catch
        RETURN_CODE=1
        pki_logger "ERROR" "Error while reading certificate passphrase for ${KEY_FILE} in certificates vault: ${VAULT_FILE}"
    } && {
        # Finally
        if [ "${CERT_KEY}" == "" ]; then
            pki_logger "ERROR" "Error while retrieving the key: ${KEY_FILE}"
            RETURN_CODE=1
        fi
        ansible-vault encrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
        return ${RETURN_CODE}
    }
}

# KWA TODO: explain & comonize the sed usage ;
# KWA TODO: change replacement string in sed : /_/ ==> /__/
# TODO: produce an example cert vault
function setComponentPassphrase {
    local TYPE="${1}"
    local KEY_FILE="${2}"
    local KEY="${3}"
    local RETURN_CODE=0

    VAULT_FILE="${REPERTOIRE_CERTIFICAT}/vault-${TYPE}.yml"

    # if [ ! -f ${REPERTOIRE_CERTIFICAT}/${KEY_FILE} ]; then
    #     pki_logger "ERROR" "The certificate key file does exists: ${REPERTOIRE_CERTIFICAT}/${KEY_FILE}"
    #     return 1
    # fi

    # Manage initial state (non-existing vault)
    if [ -f "${VAULT_FILE}" ]; then
        ansible-vault decrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
    else
        if [ -f "${VAULT_FILE}.example" ]; then
            rm -f "${VAULT_FILE}.example"
        fi
    fi

    # Try/catch/finally stuff with bash (to make sure the vault stay encrypted)
    {
        # Try
        # Add key to example vault
        normalize_key "${KEY_FILE}: changeme" >> "${VAULT_FILE}.example" && \
        # Add key to vault
        normalize_key "${KEY_FILE}: ${KEY}" >> "${VAULT_FILE}"
    } || {
        # Catch
        RETURN_CODE=1
        pki_logger "ERROR" "Error while writing to vault file: ${VAULT_FILE}"
    } && {
        # Finally
        ansible-vault encrypt ${VAULT_FILE} ${ANSIBLE_VAULT_PKI_PASSWD}
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
    gawk -F$fs '{
        indent = length($1)/2;
        vname[indent] = $2;
        for (i in vname) {if (i > indent) {delete vname[i]}}
        if (length($3) > 0) {
            vn=""; for (i=0; i<indent; i++) {vn=(vn)(vname[i])("_")}
            printf("%s%s%s=\"%s\"\n", "'$prefix'",vn, $2, $3);
        }
    }'
}
