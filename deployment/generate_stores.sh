#!/usr/bin/env bash
set -e

######################################################################
############################# Includes  ##############################
######################################################################

. $(dirname $0)/pki/scripts/lib/functions.sh

######################################################################
############################# Functions ##############################
######################################################################

# Pour incorporer un certificat dans un store
function addCrtInJks {
    local STORE="${1}"
    local MDP_STORE="${2}"
    local CERTIFICAT="${3}"
    local ALIAS="${4}"

    keytool -import -keystore ${STORE} \
        -file ${CERTIFICAT} \
        -storepass ${MDP_STORE} \
        -keypass ${MDP_STORE} \
        -noprompt \
        -alias ${ALIAS}
}

# Pour incorporer une CA dans un store
function addCaInJks {
    local STORE="${1}"
    local MDP_STORE="${2}"
    local CERTIFICAT="${3}"
    local ALIAS="${4}"

    keytool -import -trustcacerts -keystore ${STORE} \
        -file ${CERTIFICAT} \
        -storepass ${MDP_STORE} \
        -keypass ${MDP_STORE} \
        -noprompt \
        -alias ${ALIAS}
}

# Génération d'un p12 et d'un pem depuis un certificat
function crtKeyToP12 {
    local BASEFILE="${1}"
    local MDP_KEY="${2}"
    local KEYPAIR_NAME="${3}"
    local MDP_P12="${4}"
    local TARGET_FILE="${5}"

    openssl pkcs12 -export \
        -inkey "${BASEFILE}/${KEYPAIR_NAME}.key" \
        -in "${BASEFILE}/${KEYPAIR_NAME}.crt" \
        -name "${KEYPAIR_NAME}" \
        -passin pass:"${MDP_KEY}" \
        -out "${BASEFILE}/${KEYPAIR_NAME}.p12" \
        -passout pass:"${MDP_P12}"

    if [ "${BASEFILE}/${KEYPAIR_NAME}.p12" != "${TARGET_FILE}" ]; then
        mkdir -p $(dirname ${TARGET_FILE})
        mv "${BASEFILE}/${KEYPAIR_NAME}.p12" "${TARGET_FILE}"
    fi
}

# Pour incorporer un certificat p12 dans un keystore jks
function addP12InJks {
    local JKS_KEYSTORE="${1}"
    local JKS_KEYSTORE_PASSWORD="${2}"
    local P12_KEYSTORE="${3}"
    local P12_STORE_PASSWORD="${4}"

    mkdir -p "$(dirname ${JKS_KEYSTORE})"

    keytool -importkeystore \
        -srckeystore ${P12_KEYSTORE} -srcstorepass ${P12_STORE_PASSWORD} -srcstoretype PKCS12 \
        -destkeystore ${JKS_KEYSTORE} -storepass ${JKS_KEYSTORE_PASSWORD} \
        -keypass ${JKS_KEYSTORE_PASSWORD} -deststorepass ${JKS_KEYSTORE_PASSWORD} \
        -destkeypass ${JKS_KEYSTORE_PASSWORD} -deststoretype JKS
}

# Renvoie la clé du keystore pour un composant donné
function getKeystorePassphrase {
    local YAML_PATH="${1}"
    local RETURN_CODE=0

    if [ ! -f "${VAULT_KEYSTORES}" ]; then
        pki_logger "ERROR" "Failed to find file ${VAULT_KEYSTORES}"
        return 1
    fi

    # Decrypt vault file
    ansible-vault decrypt ${VAULT_KEYSTORES} ${ANSIBLE_VAULT_PASSWD}
    if [ ${?} != 0 ]; then
        pki_logger "ERROR" "Failed to decrypt ${VAULT_KEYSTORES}"
        pki_logger "ERROR" "Please check if the vault password is correct in vault_pass.txt file"
        return 1
    fi

    # Try/catch/finally stuff with bash (to make sure the vault stay encrypted)
    {
        # Try
        # Generate bash vars with the yml file:
        #       $certKey_blah
        #       $certKey_blahblah
        #       $certKey_........
        eval $(parse_yaml ${VAULT_KEYSTORES} "storeKey_") && \
        # Get the value of the variable we are interested in
        # And store it into another var: $CERT_KEY
        eval $(echo "STORE_KEY=\$storeKey_$(echo ${YAML_PATH} |sed 's/[\.-]/_/g')") && \
        # Print the $CERT_KEY var
        echo "${STORE_KEY}"
    } || {
        # Catch
        RETURN_CODE=1
        pki_logger "ERROR" "Error while reading keystore passphrase for ${YAML_PATH} in keystores vault: ${VAULT_KEYSTORES}"
    } && {
        # Finally
        if [ "${STORE_KEY}" == "" ]; then
            pki_logger "ERROR" "Error while retrieving the store key: ${YAML_PATH}"
            RETURN_CODE=1
        fi
        ansible-vault encrypt ${VAULT_KEYSTORES} ${ANSIBLE_VAULT_PASSWD}
        return ${RETURN_CODE}
    }
}

# Generate a trustore
function generateTrustStore {
    local TRUSTORE_TYPE=${1}
    local CLIENT_TYPE=${2}

    if [ "${TRUSTORE_TYPE}" != "server" ] && [ ${TRUSTORE_TYPE} != "client" ]; then
        pki_logger "ERROR" "Invalid trustore type: ${TRUSTORE_TYPE}"
        return 1
    fi

    # Set truststore path and delete the store if already exists
    if [ "${TRUSTORE_TYPE}" == "client" ]; then
        JKS_TRUST_STORE=${REPERTOIRE_KEYSTORES}/client-${CLIENT_TYPE}/truststore_${CLIENT_TYPE}.jks
        TRUST_STORE_PASSWORD=$(getKeystorePassphrase "truststores_client_${CLIENT_TYPE}")
    elif [ "${TRUSTORE_TYPE}" == "server" ]; then
        JKS_TRUST_STORE=${REPERTOIRE_KEYSTORES}/server/truststore_server.jks
        TRUST_STORE_PASSWORD=$(getKeystorePassphrase "truststores_server")
    else
        pki_logger "ERROR" "Invalid trustore type: ${TRUSTORE_TYPE}"
        return 1
    fi

    if [ -f "${JKS_TRUST_STORE}" ]; then
        rm -f "${JKS_TRUST_STORE}"
    fi

    # Add the public client ca certificates to the truststore
    pki_logger "Ajout des certificats client dans le truststore"
    if [ "${TRUSTORE_TYPE}" == "client" ]; then

        for CRT_FILE in $(ls ${REPERTOIRE_CERTIFICAT}/client-${CLIENT_TYPE}/ca/*.crt); do
            pki_logger "Ajout de ${CRT_FILE} dans le truststore ${CLIENT_TYPE}"
            ALIAS="client-${CLIENT_TYPE}-$(basename ${CRT_FILE})"
            addCrtInJks ${JKS_TRUST_STORE} \
                        ${TRUST_STORE_PASSWORD} \
                        ${CRT_FILE} \
                        ${ALIAS}
        done

    fi

    # Add the server certificates to the truststore
    pki_logger "Ajout des certificats serveur dans le truststore"
    for CRT_FILE in $(ls ${REPERTOIRE_CERTIFICAT}/server/ca/*.crt); do
        pki_logger "Ajout de ${CRT_FILE} dans le truststore ${CLIENT_TYPE}"
        ALIAS="server-$(basename ${CRT_FILE})"
        addCrtInJks ${JKS_TRUST_STORE} \
                    ${TRUST_STORE_PASSWORD} \
                    ${CRT_FILE} \
                    ${ALIAS}
    done
}

function generateHostKeystore {
    local COMPONENT="${1}"
    local JKS_KEYSTORE="${2}"
    local P12_KEYSTORE="${3}"
    local CRT_KEY_PASSWORD="${4}"
    local JKS_PASSWORD="${5}"
    local TMP_P12_PASSWORD="${6}"

    if [ -f ${JKS_KEYSTORE} ]; then
        rm -f ${JKS_KEYSTORE}
    fi

    pki_logger "Génération du p12"
    crtKeyToP12 $(dirname ${P12_KEYSTORE}) \
                ${CRT_KEY_PASSWORD} \
                ${COMPONENT} \
                ${TMP_P12_PASSWORD} \
                ${P12_KEYSTORE}

    pki_logger "Génération du jks"
    addP12InJks ${JKS_KEYSTORE} \
                ${JKS_PASSWORD} \
                ${P12_KEYSTORE} \
                ${TMP_P12_PASSWORD}

    pki_logger "Suppression du p12"
    if [ -f ${P12_KEYSTORE} ]; then
        rm -f ${P12_KEYSTORE}
    fi
}

######################################################################
#############################    Main    #############################
######################################################################

cd $(dirname $0)

TMP_P12_PASSWORD="$(generatePassphrase)"
REPERTOIRE_KEYSTORES="${REPERTOIRE_ROOT}/environments/keystores"

if [ ! -d ${REPERTOIRE_KEYSTORES}/server ]; then
    mkdir -p ${REPERTOIRE_KEYSTORES}/server
fi

# Remove old keystores & servers directories
find ${REPERTOIRE_KEYSTORES} -type f -name *.jks -exec rm -f {} \;
find ${REPERTOIRE_KEYSTORES} -type f -name *.p12 -exec rm -f {} \;
find ${REPERTOIRE_KEYSTORES}/server -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} \;

# Generate the server keystores
for SERVER in $(ls ${REPERTOIRE_CERTIFICAT}/server/hosts/); do

    mkdir -p ${REPERTOIRE_KEYSTORES}/server/${SERVER}

    # awk : used to strip extension
    for COMPONENT in $( ls ${REPERTOIRE_CERTIFICAT}/server/hosts/${SERVER}/ 2>/dev/null | awk -F "." '{for (i=1;i<NF;i++) print $i}' | sort | uniq ); do

        pki_logger "-------------------------------------------"
        pki_logger "Creation du keystore de ${COMPONENT} pour le serveur ${SERVER}"
        JKS_KEYSTORE=${REPERTOIRE_KEYSTORES}/server/${SERVER}/keystore_${COMPONENT}.jks
        P12_KEYSTORE=${REPERTOIRE_CERTIFICAT}/server/hosts/${SERVER}/${COMPONENT}.p12
        CRT_KEY_PASSWORD=$(getComponentPassphrase certs "server_${COMPONENT}_key")
        JKS_PASSWORD=$(getKeystorePassphrase "keystores_server_${COMPONENT}")

        generateHostKeystore    ${COMPONENT} \
                                ${JKS_KEYSTORE} \
                                ${P12_KEYSTORE} \
                                ${CRT_KEY_PASSWORD} \
                                ${JKS_PASSWORD} \
                                ${TMP_P12_PASSWORD}

    done

done


# Generate the timestamp keystores
# awk : used to strip extension
for USAGE in $( ls ${REPERTOIRE_CERTIFICAT}/timestamping/vitam/ 2>/dev/null | awk -F "." '{for (i=1;i<NF;i++) print $i}' | sort | uniq ); do

    pki_logger "-------------------------------------------"
    pki_logger "Creation du keystore timestamp de ${USAGE}"
    P12_KEYSTORE=${REPERTOIRE_KEYSTORES}/timestamping/keystore_${USAGE}.p12
    TMP_P12_KEYSTORE=${REPERTOIRE_CERTIFICAT}/timestamping/vitam/${USAGE}.p12
    CRT_KEY_PASSWORD=$(getComponentPassphrase certs "timestamping_${USAGE}_key")
    P12_PASSWORD=$(getKeystorePassphrase "keystores_timestamping_${USAGE}")

    crtKeyToP12 $(dirname ${TMP_P12_KEYSTORE}) \
                ${CRT_KEY_PASSWORD} \
                ${USAGE} \
                ${P12_PASSWORD} \
                ${P12_KEYSTORE}
    # KWA TODO: generate two keystores : private (with crt + key) + public (with only the crt)
done


# Keystores generation foreach client type (storage, external)
for CLIENT_TYPE in external storage; do

    # Set grantedstore path and delete the store if already exists
    JKS_GRANTED_STORE=${REPERTOIRE_KEYSTORES}/client-${CLIENT_TYPE}/grantedstore_${CLIENT_TYPE}.jks
    GRANTED_STORE_PASSWORD=$(getKeystorePassphrase "grantedstores_client_${CLIENT_TYPE}")

    # Delete the old granted store if already exists
    if [ -f ${JKS_GRANTED_STORE} ]; then
        rm -f ${JKS_GRANTED_STORE}
    fi

    # client-${CLIENT_TYPE} keystores generation
    for COMPONENT in $( ls ${REPERTOIRE_CERTIFICAT}/client-${CLIENT_TYPE}/clients 2>/dev/null | grep -v "^external$" ); do

        # Generate the p12 keystore
        pki_logger "-------------------------------------------"
        pki_logger "Creation du keystore client de ${COMPONENT}"
        CERT_DIRECTORY=${REPERTOIRE_CERTIFICAT}/client-${CLIENT_TYPE}/clients/${COMPONENT}
        CRT_KEY_PASSWORD=$(getComponentPassphrase certs "client_client-${CLIENT_TYPE}_${COMPONENT}_key")
        P12_KEYSTORE=${REPERTOIRE_KEYSTORES}/client-${CLIENT_TYPE}/keystore_${COMPONENT}.p12
        P12_PASSWORD=$(getKeystorePassphrase "keystores_client_${CLIENT_TYPE}_${COMPONENT}")

        if [ -f ${P12_KEYSTORE} ]; then
            rm -f ${P12_KEYSTORE}
        fi

        pki_logger "Génération du p12"
        crtKeyToP12 ${CERT_DIRECTORY} \
                    ${CRT_KEY_PASSWORD} \
                    ${COMPONENT} \
                    ${P12_PASSWORD} \
                    ${P12_KEYSTORE}

        # Add the public certificates to the grantedstore
        for CRT_FILE in $(ls ${REPERTOIRE_CERTIFICAT}/client-${CLIENT_TYPE}/clients/${COMPONENT}/*.crt); do
            pki_logger "Ajout du certificat ${CRT_FILE} dans le grantedstore ${CLIENT_TYPE} de ${COMPONENT}"
            ALIAS="client-$(basename ${CRT_FILE})"
            addCrtInJks ${JKS_GRANTED_STORE} \
                        ${GRANTED_STORE_PASSWORD} \
                        ${CRT_FILE} \
                        ${ALIAS}
        done

    done

    # Add the external certificates to the granted store
    pki_logger "-------------------------------------------"
    pki_logger "Ajout des certificat public du répertoire external dans le grantedstore ${CLIENT_TYPE}"
    if [ "${CLIENT_TYPE}" == "external" ]; then
        for CRT_FILE in $(ls ${REPERTOIRE_CERTIFICAT}/client-${CLIENT_TYPE}/clients/external/*.crt 2>/dev/null); do
            addCrtInJks ${JKS_GRANTED_STORE} \
                        ${GRANTED_STORE_PASSWORD} \
                        ${CRT_FILE} \
                        $(basename ${CRT_FILE})
        done
    fi

    # Generate the CLIENT_TYPE truststore
    pki_logger "-------------------------------------------"
    pki_logger "Génération du truststore client-${CLIENT_TYPE}"
    generateTrustStore "client" ${CLIENT_TYPE}

done

# Generate the server trustore
pki_logger "-------------------------------------------"
pki_logger "Génération du truststore server"
generateTrustStore "server" "server"

##################################################################
############### VITAM USERS ######################################
##################################################################

pki_logger "-------------------------------------------"
pki_logger "Génération du grantedstore vitam-users"

# Generate grantedstore for vitam-users
# TODO: Rajouter passphrase du grantedstore dans le vault
CLIENT_TYPE="external"
REPERTOIRE_PLUS="vitam-users"
JKS_GRANTED_STORE=${REPERTOIRE_KEYSTORES}/client-${CLIENT_TYPE}/grantedstore_${CLIENT_TYPE}.jks
GRANTED_STORE_PASSWORD=$(getKeystorePassphrase "grantedstores_client_${CLIENT_TYPE}")
if [ -d ${REPERTOIRE_CERTIFICAT}/client-${REPERTOIRE_PLUS} ]; then
    for CRT_FILE in $( ls ${REPERTOIRE_CERTIFICAT}/client-${REPERTOIRE_PLUS}/clients 2>/dev/null ); do
        CRT_FILE="${REPERTOIRE_CERTIFICAT}/client-${REPERTOIRE_PLUS}/clients/${CRT_FILE}"
        pki_logger "Ajout de ${CRT_FILE} dans le grantedstore ${CLIENT_TYPE}"
        addCrtInJks ${JKS_GRANTED_STORE} \
                    ${GRANTED_STORE_PASSWORD} \
                    ${CRT_FILE} \
                    $(basename ${CRT_FILE})
    done
else
    pki_logger "No client-${REPERTOIRE_PLUS} directory is present. Skipping..."
fi
# Generate the vitam-users trustore
pki_logger "-------------------------------------------"
pki_logger "Génération des certif vitam-users dans client-${CLIENT_TYPE}"
JKS_TRUST_STORE=${REPERTOIRE_KEYSTORES}/client-${CLIENT_TYPE}/truststore_${CLIENT_TYPE}.jks
TRUST_STORE_PASSWORD=$(getKeystorePassphrase "truststores_client_${CLIENT_TYPE}")
if [ -d ${REPERTOIRE_CERTIFICAT}/client-${REPERTOIRE_PLUS}/ca ]; then
    for CRT_FILE in $(ls ${REPERTOIRE_CERTIFICAT}/client-${REPERTOIRE_PLUS}/ca/*.crt); do
        pki_logger "Ajout de ${CRT_FILE} dans le truststore ${REPERTOIRE_PLUS}"
        ALIAS="$(basename ${CRT_FILE})"
        addCrtInJks ${JKS_TRUST_STORE} \
                    ${TRUST_STORE_PASSWORD} \
                    ${CRT_FILE} \
                    ${ALIAS}
    done
else
    pki_logger "No client-${REPERTOIRE_PLUS}/ca directory is present. Skipping..."
fi

pki_logger "-------------------------------------------"
pki_logger "Fin de la génération des stores"
