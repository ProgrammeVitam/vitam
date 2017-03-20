#!/bin/bash
set -e


######################################################################
############################# Includes  ##############################
######################################################################

. "$(dirname $0)/lib/functions.sh"


######################################################################
############################# Functions ##############################
######################################################################

# Génération d'un certificat serveur
function generateHostCertificate {
    local COMPOSANT="${1}"
    local CERT_KEY="${2}"
    local INTERMEDIATE_CA_KEY="${3}"
    local HOSTNAME="${4}"
    local TYPE_CERTIFICAT="${5}"
    local CN_VALEUR="${6}"

    local RSA_TYPE=$(echo ${TYPE_CERTIFICAT}| tr '[:lower:]' '[:upper:]')

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour ${COMPOSANT} hébergé sur ${HOSTNAME}..."
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${CERT_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req" \
        -nodes \
        -config "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config" \
        -subj "/CN=${CN_VALEUR}/O=Vitam./C=FR/ST=idf/L=paris" \
        -batch

    pki_logger "Generation du certificat signé avec CA ${TYPE_CERTIFICAT}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config" \
        -passin pass:"${INTERMEDIATE_CA_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req" \
        -name CA_intermediate -extensions ${RSA_TYPE}_RSA_SSL -batch

    purge_directory "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}"
    purge_directory "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}"
}

# Génération d'un certificat de timestamping
function generateTimestampCertificate {
    local COMPOSANT="${1}"
    local CERT_KEY="${2}"
    local INTERMEDIATE_CA_KEY="${3}"
    local HOSTNAME="${4}"
    local CN_VALEUR="${HOSTNAME}"
    local TYPE_CERTIFICAT="timestamping"

    local RSA_TYPE=$(echo ${TYPE_CERTIFICAT}| tr '[:lower:]' '[:upper:]')

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour ${COMPOSANT}"
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${CERT_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${COMPOSANT}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${COMPOSANT}.req" \
        -nodes \
        -config "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config" \
        -subj "/CN=${CN_VALEUR}/O=Vitam./C=FR/ST=idf/L=paris" \
        -batch

    pki_logger "Generation du certificat signé avec CA ${TYPE_CERTIFICAT}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config" \
        -passin pass:"${INTERMEDIATE_CA_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${COMPOSANT}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${COMPOSANT}.req" \
        -name CA_intermediate -extensions ${RSA_TYPE}_RSA_SSL -batch

    purge_directory "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam"
    purge_directory "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}"
}

# Génération d'un certificat client
function generateClientCertificate {
    local CLIENT_NAME="${1}"
    local MDP_KEY="${2}"
    local MDP_CAINTERMEDIATE_KEY="${3}"
    local CLIENT_TYPE="${4}"
    local TYPE_CERTIFICAT="client"

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour ${CLIENT_NAME}"
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${MDP_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.req" \
        -config "${REPERTOIRE_CONFIG}/${CLIENT_TYPE}/ca-config" \
        -subj "/CN=${CLIENT_NAME}/O=Vitam./C=FR/ST=idf/L=paris" \
        -batch

    pki_logger "Generation du certificat signé avec ${CLIENT_TYPE}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/${CLIENT_TYPE}/ca-config" \
        -passin pass:"${MDP_CAINTERMEDIATE_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.req" \
        -name CA_intermediate -extensions CLIENT_RSA_SSL -batch

    purge_directory "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}"
    purge_directory "${REPERTOIRE_CONFIG}/${CLIENT_TYPE}"
}

# Génération des certificats serveur et stockage de la passphrase pour tous les hosts d'un host group donné
function generateHostCertAndStorePassphrase {
    local COMPONENT="${1}"
    local HOSTS_GROUP="${2}"

    # sed "1 d" : remove the first line
    for SERVER in $(ansible -i ${REPERTOIRE_ROOT}/environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts ${HOSTS_GROUP} ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
        # Generate the key
        local CERT_KEY=$(generatePassphrase)
        # Create the certificate
        generateHostCertificate ${COMPONENT} \
                                ${CERT_KEY} \
                                caintermediatekeypassword \
                                ${SERVER} \
                                "server" \
                                "${COMPONENT}.service.consul"
        # Store the key to the vault
        setComponentCertPassphrase  "server_${COMPONENT}_key" \
                                    "${CERT_KEY}"
    done
}

# Génération d'un certificat timestamp (utilise la fonction de génération de certificats serveur)
function generateTimestampCertAndStorePassphrase {
    local COMPONENT="${1}"

    # Generate the key
    local CERT_KEY=$(generatePassphrase)
    # Create the certificate
    generateTimestampCertificate    ${COMPONENT} \
                                    ${CERT_KEY} \
                                    caintermediatekeypassword \
                                    "${COMPONENT}.service.consul"
    # Store the key to the vault
    setComponentCertPassphrase  "timestamping_${COMPONENT}_key" \
                                "${CERT_KEY}"
}

# Génération du certificat client et stockage de la passphrase
function generateClientCertAndStorePassphrase {
    local COMPONENT="${1}"
    local CLIENT_TYPE="${2}"

    # Generate the key
    local CERT_KEY=$(generatePassphrase)
    # Create the certificate
    generateClientCertificate \
            ${COMPONENT} \
            ${CERT_KEY} \
            caintermediatekeypassword \
            ${CLIENT_TYPE}
    # Store the key to the vault
    setComponentCertPassphrase  "client_${CLIENT_TYPE}_${COMPONENT}_key" \
                                "${CERT_KEY}"
}

# Recopie de la CA de pki/CA vers environments-rpm/cert/cert-type/CA
function copyCAFromPki {
    local CERT_TYPE="${1}"

    pki_logger "Copie de la CA (root + intermediate) de ${CERT_TYPE}"
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${CERT_TYPE}/ca"
    for CA in $(ls ${REPERTOIRE_CA}/${CERT_TYPE}/*.crt); do
        cp -f "${CA}" "${REPERTOIRE_CERTIFICAT}/${CERT_TYPE}/ca/$(basename ${CA})"
    done
}


######################################################################
#############################    Main    #############################
######################################################################

# Vérification des paramètres
if [ "${1}" == "" ]; then
    echo "This script needs to know on which environment you want to apply to !"
    exit 1;
fi
ENVIRONNEMENT="${1}"

# Delete the old vault (if exists)
pki_logger "Suppression de l'ancien vault"
if [ -f ${VAULT_CERTS} ]; then
    rm -f ${VAULT_CERTS}
fi

# Copy CA
pki_logger "Recopie des clés publiques des CA"
copyCAFromPki client-external
copyCAFromPki client-storage
copyCAFromPki server
copyCAFromPki timestamping

# Generate hosts certificates
pki_logger "Génération des certificats serveurs"
# Method                                    # Component name         # Host group name
generateHostCertAndStorePassphrase          ingest-external          hosts-ingest-external
generateHostCertAndStorePassphrase          access-external          hosts-access-external
generateHostCertAndStorePassphrase          offer                    hosts-storage-offer-default

# Generate timestamp certificates
pki_logger "Génération des certificats timestamping"
# Method                                    # Component name         # Host group name
generateTimestampCertAndStorePassphrase     logbook                  hosts-logbook

# Generate clients certificates
pki_logger "Génération des certificats clients"
# Method                                    # Component name         # Client type
generateClientCertAndStorePassphrase        ihm-demo                 client-external
generateClientCertAndStorePassphrase        ihm-recette              client-external
generateClientCertAndStorePassphrase        reverse                  client-external
generateClientCertAndStorePassphrase        storage                  client-storage

pki_logger "Fin de script"
