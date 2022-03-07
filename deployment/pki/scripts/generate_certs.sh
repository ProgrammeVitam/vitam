#!/usr/bin/env bash
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
    local SERVICE_HOSTNAME="${6}"
    local SERVICE_DC_HOSTNAME="${7}"

    # Correctly set Subject Alternate Name (env var is read inside the openssl configuration file)
    export OPENSSL_SAN="DNS:${SERVICE_HOSTNAME},DNS:${HOSTNAME},DNS:${SERVICE_DC_HOSTNAME}"
    # Correctly set certificate CN (env var is read inside the openssl configuration file)
    export OPENSSL_CN="${SERVICE_DC_HOSTNAME}"
    # Correctly set certificate DIRECTORY (env var is read inside the openssl configuration file)
    export OPENSSL_CRT_DIR=${TYPE_CERTIFICAT}

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour ${COMPOSANT} hébergé sur ${HOSTNAME}..."
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${CERT_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req" \
        -nodes \
        -config "${REPERTOIRE_CONFIG}/crt-config" \
        -batch

    pki_logger "Generation du certificat signé avec CA ${TYPE_CERTIFICAT}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/crt-config" \
        -passin pass:"${INTERMEDIATE_CA_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req" \
        -extensions extension_${TYPE_CERTIFICAT} -batch

    purge_directory "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}"
    purge_directory "${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}"
}

# Génération d'un certificat de timestamping ; le nom du certificat est dérivé de son usage
function generateTimestampCertificate {
    local USAGE="${1}"
    local CERT_KEY="${2}"
    local INTERMEDIATE_CA_KEY="${3}"
    local CN_VALEUR="${USAGE}"
    local TYPE_CERTIFICAT="timestamping"

    # Correctly set certificate CN (env var is read inside the openssl configuration file)
    export OPENSSL_CN="${CN_VALEUR}"
    # Correctly set certificate DIRECTORY (env var is read inside the openssl configuration file)
    export OPENSSL_CRT_DIR=${TYPE_CERTIFICAT}

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour usage ${USAGE}"
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${CERT_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${USAGE}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${USAGE}.req" \
        -nodes \
        -config "${REPERTOIRE_CONFIG}/crt-config" \
        -batch

    pki_logger "Generation du certificat signé avec CA ${TYPE_CERTIFICAT}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/crt-config" \
        -passin pass:"${INTERMEDIATE_CA_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${USAGE}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/vitam/${USAGE}.req" \
        -extensions extension_${TYPE_CERTIFICAT} -batch

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

    # Correctly set certificate CN (env var is read inside the openssl configuration file)
    export OPENSSL_CN="${CLIENT_NAME}"
    # Correctly set certificate DIRECTORY (env var is read inside the openssl configuration file)
    export OPENSSL_CRT_DIR=${CLIENT_TYPE}

    pki_logger "Création du certificat ${TYPE_CERTIFICAT} pour ${CLIENT_NAME}"
    mkdir -p "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}"
    pki_logger "Generation de la clé..."
    openssl req -newkey "${PARAM_KEY_CHIFFREMENT}" \
        -passout pass:"${MDP_KEY}" \
        -keyout "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.key" \
        -out "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.req" \
        -config "${REPERTOIRE_CONFIG}/crt-config" \
        -batch

    pki_logger "Generation du certificat signé avec ${CLIENT_TYPE}..."
    openssl ca -config "${REPERTOIRE_CONFIG}/crt-config" \
        -passin pass:"${MDP_CAINTERMEDIATE_KEY}" \
        -out "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.crt" \
        -in "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}/${CLIENT_NAME}.req" \
        -extensions extension_${TYPE_CERTIFICAT} -batch

    purge_directory "${REPERTOIRE_CERTIFICAT}/${CLIENT_TYPE}/clients/${CLIENT_NAME}"
    purge_directory "${REPERTOIRE_CONFIG}/${CLIENT_TYPE}"
}

# Génération des certificats serveur et stockage de la passphrase pour tous les hosts d'un host group donné
function generateHostCertAndStorePassphrase {
    local COMPONENT="${1}"
    local HOSTS_GROUP="${2}"

    # Récupération du password de la CA_INTERMEDIATE dans le vault-ca
    CA_INTERMEDIATE_PASSWORD=$(getComponentPassphrase ca "ca_intermediate_server")

    # sed "1 d" : remove the first line
    for SERVER in $(ansible -i ${ENVIRONNEMENT_FILE} --list-hosts ${HOSTS_GROUP} ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
        if [ "${COMPONENT}" == "offer" ]; then
            SERVICE_NAME=$(read_ansible_var "offer_conf" ${SERVER})
        else
            SERVICE_NAME=${COMPONENT}
        fi
        echo "${SERVICE_NAME}.service.${VITAM_SITE_NAME}.${CONSUL_DOMAIN}"
        # Generate the key
        local CERT_KEY=$(generatePassphrase)
        # Create the certificate
        generateHostCertificate ${COMPONENT} \
                                ${CERT_KEY} \
                                ${CA_INTERMEDIATE_PASSWORD} \
                                ${SERVER} \
                                "server" \
                                "${SERVICE_NAME}.service.${CONSUL_DOMAIN}" \
                                "${SERVICE_NAME}.service.${VITAM_SITE_NAME}.${CONSUL_DOMAIN}"
        # Store the key to the vault
        setComponentPassphrase certs "server_${COMPONENT}_key" \
                                     "${CERT_KEY}"
    done
}

# Génération d'un certificat timestamp (utilise la fonction de génération de certificats serveur)
function generateTimestampCertAndStorePassphrase {
    local USAGE="${1}"

    # Récupération du password de la CA_INTERMEDIATE dans le vault-ca
    CA_INTERMEDIATE_PASSWORD=$(getComponentPassphrase ca "ca_intermediate_timestamping")

    # Generate the key
    local CERT_KEY=$(generatePassphrase)
    # Create the certificate
    generateTimestampCertificate ${USAGE} \
                                 ${CERT_KEY} \
                                 ${CA_INTERMEDIATE_PASSWORD}
    # Store the key to the vault
    setComponentPassphrase certs "timestamping_${USAGE}_key" \
                                 "${CERT_KEY}"
}

# Génération du certificat client et stockage de la passphrase
function generateClientCertAndStorePassphrase {
    local COMPONENT="${1}"
    local CLIENT_TYPE="${2}"

    # Récupération du password de la CA_INTERMEDIATE dans le vault-ca
    CA_INTERMEDIATE_PASSWORD=$(getComponentPassphrase ca "ca_intermediate_${CLIENT_TYPE}")

    # Generate the key
    local CERT_KEY=$(generatePassphrase)
    # Create the certificate
    generateClientCertificate ${COMPONENT} \
                              ${CERT_KEY} \
                              ${CA_INTERMEDIATE_PASSWORD} \
                              ${CLIENT_TYPE}
    # Store the key to the vault
    setComponentPassphrase certs "client_${CLIENT_TYPE}_${COMPONENT}_key" \
                                 "${CERT_KEY}"
}

# Recopie de la CA de pki/CA vers environments/cert/cert-type/CA
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

cd $(dirname $0)/../..

# Vérification des paramètres
if [ "${1}" == "" ]; then
    pki_logger "ERROR" "This script needs to know on which environment you want to apply to !"
    exit 1
fi
ENVIRONNEMENT="${1}"

ENVIRONNEMENT_FILE="${1}"

if [ ! -f "${ENVIRONNEMENT_FILE}" ]; then
    pki_logger "ERROR" "Cannot find environment file: ${ENVIRONNEMENT_FILE}"
    exit 1
fi

# Get consul_domain
CONSUL_DOMAIN=$(read_ansible_var "consul_domain" "hosts_storage_offer_default[0]")
# Get vitam_site_name
VITAM_SITE_NAME=$(read_ansible_var "vitam_site_name" "hosts_storage_offer_default[0]")

# Cleaning or creating vault file for certs
initVault certs

# Copy CA
pki_logger "Recopie des clés publiques des CA"
copyCAFromPki client-external
copyCAFromPki client-storage
copyCAFromPki server
copyCAFromPki timestamping

# Generate hosts certificates
pki_logger "Génération des certificats serveurs"
# Method                                    # Component name         # Host group name
generateHostCertAndStorePassphrase          ingest-external          hosts_ingest_external
generateHostCertAndStorePassphrase          access-external          hosts_access_external
generateHostCertAndStorePassphrase          offer                    hosts_storage_offer_default
generateHostCertAndStorePassphrase          ihm-recette              hosts_ihm_recette
generateHostCertAndStorePassphrase          ihm-demo                 hosts_ihm_demo
generateHostCertAndStorePassphrase          collect                  hosts_collect

# Generate timestamp certificates
pki_logger "Génération des certificats timestamping"
# Method                                    # Usage
generateTimestampCertAndStorePassphrase     secure-logbook
generateTimestampCertAndStorePassphrase     secure-storage

# Generate clients certificates
pki_logger "Génération des certificats clients"
# Method                                    # Component name         # Client type
generateClientCertAndStorePassphrase        ihm-demo                 client-external
generateClientCertAndStorePassphrase        gatling                  client-external
generateClientCertAndStorePassphrase        vitam-admin-int          client-external
generateClientCertAndStorePassphrase        ihm-recette              client-external
generateClientCertAndStorePassphrase        reverse                  client-external
generateClientCertAndStorePassphrase        collect                  client-external

# Generate storage certificates
pki_logger "Génération des certificats storage"
# Method                                    # Component name         # Client type
generateClientCertAndStorePassphrase        storage                  client-storage

pki_logger "Fin de script"
