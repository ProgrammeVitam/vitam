#!/usr/bin/env bash
set -e

######################################################################
############################# Includes  ##############################
######################################################################

. $(dirname $0)/lib/functions.sh


######################################################################
############################# Functions ##############################
######################################################################

# Génération de la CA root
function generate_ca_root {
    local MDP_CAROOT_KEY="${1}"
    local REPERTOIRE_SORTIE="${2}"
    local CONFIG_DIR="${3}"

    # Correctly set certificate CN (env var is read inside the openssl configuration file)
    export OPENSSL_CN=ca_root_${REPERTOIRE_SORTIE}
    # Correctly set certificate DIRECTORY (env var is read inside the openssl configuration file)
    export OPENSSL_CA_DIR=${REPERTOIRE_SORTIE}

    if [ ! -d ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE} ]; then
        pki_logger "Création du sous-répertoire ${REPERTOIRE_SORTIE}"
        mkdir -p ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE};
    fi

    pki_logger "Create CA request..."
    openssl req \
        -config ${REPERTOIRE_CONFIG}/ca-config \
        -new \
        -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-root.req \
        -keyout ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-root.key \
        -passout pass:${MDP_CAROOT_KEY} \
        -batch

    pki_logger "Create CA certificate..."
    openssl ca \
        -config ${REPERTOIRE_CONFIG}/ca-config \
        -selfsign \
        -extensions extension_ca_root \
        -in ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-root.req \
        -passin pass:${MDP_CAROOT_KEY} \
        -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-root.crt \
        -batch
}

# Génration de la CA intermédiaire
function generate_ca_interm {
    local MDP_CAINTERMEDIATE_KEY="${1}"
    local MDP_CAROOT_KEY="${2}"
    local REPERTOIRE_SORTIE="${3}"
    local TYPE_CA="${4}"

    # Correctly set certificate CN (env var is read inside the openssl configuration file)
    export OPENSSL_CN=ca_intermediate_${REPERTOIRE_SORTIE}
    # Correctly set certificate DIRECTORY (env var is read inside the openssl configuration file)
    export OPENSSL_CA_DIR=${REPERTOIRE_SORTIE}

    if [ ! -d ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE} ]; then
        pki_logger "Création du sous-répertoire ${REPERTOIRE_SORTIE}"
        mkdir -p ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE};
    fi

    pki_logger "Generate intermediate request..."
    openssl req \
    -config ${REPERTOIRE_CONFIG}/ca-config \
    -new \
    -newkey ${PARAM_KEY_CHIFFREMENT} \
    -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-intermediate.req \
    -keyout ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-intermediate.key \
    -passout pass:${MDP_CAINTERMEDIATE_KEY} \
    -batch

    pki_logger "Sign..."
    openssl ca \
    -config ${REPERTOIRE_CONFIG}/ca-config \
    -extensions extension_ca_intermediate \
    -in ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-intermediate.req \
    -passin pass:${MDP_CAROOT_KEY} \
    -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca-intermediate.crt \
    -batch
}


######################################################################
#############################    Main    #############################
######################################################################

pki_logger "Lancement de la procédure de création des CA"
pki_logger "=============================================="
if [ ! -d ${REPERTOIRE_CA} ]; then
    pki_logger "Répertoire ${REPERTOIRE_CA} absent ; création..."
    mkdir -p ${REPERTOIRE_CA};
fi
if [ ! -d ${TEMP_CERTS} ]; then
    pki_logger "Création du répertoire de travail temporaire tempcerts sous ${TEMP_CERTS}..."
    mkdir -p ${TEMP_CERTS}
fi

# Création des répertoires pour les différentes CA
# Création des CA root dans pki/ca
# Création des CA intermédiaires pki/ca
for ITEM in server client-external client-storage timestamping
do
    mkdir -p ${REPERTOIRE_CA}/${ITEM}
    pki_logger "Création de CA root pour ${ITEM}..."
    generate_ca_root carootkeypassword ${ITEM} ${ITEM} # FIXME : parameters for passwords

    pki_logger "Création de la CA intermediate pour ${ITEM}..."
    generate_ca_interm caintermediatekeypassword carootkeypassword ${ITEM} ${ITEM} # FIXME : parameters for passwords

    purge_directory "${REPERTOIRE_CONFIG}/${ITEM}"
    purge_directory "${REPERTOIRE_CA}/${ITEM}"

    pki_logger "----------------------------------------------"
done
pki_logger "=============================================="
pki_logger "Fin de la procédure de création des CA"
