#!/bin/bash
REPERTOIRE_ROOT=$(dirname $0)
REPERTOIRE_CERTIFICAT=${REPERTOIRE_ROOT}/PKI/certificats
REPERTOIRE_CA=${REPERTOIRE_ROOT}/PKI/CA
REPERTOIRE_CONFIG=${REPERTOIRE_ROOT}/PKI/config
TEMP_CERTS=${REPERTOIRE_ROOT}/PKI/newcerts
PARAM_KEY_CHIFFREMENT="rsa:4096"
ANSIBLE_VAULT_PASSWD="--ask-vault-pass"

function check_password_file {
	if [ -f vault_pass.txt ]; then
		export ANSIBLE_VAULT_PASSWD="--vault-password-file vault_pass.txt"
	fi
}
function generate_ca_root {
	# Arguments
	MDP_CAROOT_KEY=${1}
	REPERTOIRE_SORTIE=${2}
	CONFIG_DIR=${3}

	LDAPLIKE_ROOT="/CN=CA_${REPERTOIRE_SORTIE}/O=Vitam./C=FR/ST=idf/L=paris"

	if [ ! -d ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE} ]; then
		echo "	Création du sous-répertoire ${REPERTOIRE_SORTIE}"
		mkdir -p ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE};
	fi
	# TODO : do like the intermediate CA generation : genkey, then req, then ca (with -selfsign)
	echo "	Create CA request..."
	openssl req -new \
	    -config ${REPERTOIRE_CONFIG}/${CONFIG_DIR}/ca-config \
	    -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.csr \
	    -keyout ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.key \
	    -passout pass:${MDP_CAROOT_KEY} \
	    -subj "${LDAPLIKE_ROOT}" \
	    -batch

	echo "	Create CA certificate..."
	openssl ca -selfsign \
	    -config ${REPERTOIRE_CONFIG}/${CONFIG_DIR}/ca-config \
	    -in ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.csr \
	    -out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.crt \
	    -extensions extension_ca_root \
	    -subj "${LDAPLIKE_ROOT}" \
	    -passin pass:${MDP_CAROOT_KEY} \
	    -batch
}

function generate_ca_interm {
	# Arguments
	MDP_CAINTERMEDIATE_KEY=${1}
	MDP_CAROOT_KEY=${2}
	REPERTOIRE_SORTIE=${3}
	TYPE_CA=${4}

	if [ ! -d ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE} ]; then
		echo "	Création du sous-répertoire ${REPERTOIRE_SORTIE}"
		mkdir -p ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE};
	fi

	LDAPLIKE="/CN=CA_${REPERTOIRE_SORTIE}/O=Vitam./C=FR/ST=idf/L=paris"
	#
	# Generation de la requete de certificat CA intermediaire
	#	
	echo "	Generate intermediate request..."
	# ${PARAM_KEY_CHIFFREMENT}
		# -newkey ${PARAM_KEY_CHIFFREMENT} \

	openssl req -new \
	-newkey ${PARAM_KEY_CHIFFREMENT} \
	-config ${REPERTOIRE_CONFIG}/${TYPE_CA}/ca-config \
	-out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.csr \
	-keyout ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.key \
	-passout pass:${MDP_CAINTERMEDIATE_KEY} \
	-subj "${LDAPLIKE}" \
	-batch 
	#
	# Generation certificat CA intermediaire
	#
	echo "	Sign..."
	openssl ca \
	-config ${REPERTOIRE_CONFIG}/${TYPE_CA}/ca-config \
	-in ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.csr \
	-out ${REPERTOIRE_CA}/${REPERTOIRE_SORTIE}/ca.crt \
	-passin pass:${MDP_CAROOT_KEY} \
	-subj "${LDAPLIKE}" \
	-extensions CA_SSL \
	-batch
}


function generatehostcertificate {
	# Arguments
	COMPOSANT=${1}
	MDP_KEY=${2}
	MDP_CAINTERMEDIATE_KEY=${3}
	HOSTNAME=${4}
	TYPE_CERTIFICAT=${5}
	CN_VALEUR=${6}

	RSA_TYPE=$(echo ${TYPE_CERTIFICAT}| tr '[:lower:]' '[:upper:]')

	echo "	Création du certificat ${TYPE_CERTIFICAT} pour ${COMPOSANT} hébergé sur ${HOSTNAME}..."
	mkdir -p ${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME};
	echo "	Generation de la clé..."
	openssl req -newkey ${PARAM_KEY_CHIFFREMENT} \
		-passout pass:${MDP_KEY} \
		-keyout ${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.key \
		-out ${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req \
		-nodes \
		-config ${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config \
		-subj "/CN=${CN_VALEUR}/O=Vitam./C=FR/ST=idf/L=paris" \
		-batch

	echo "	Generation du certificat signé avec CA ${TYPE_CERTIFICAT}..."
	openssl ca -config ${REPERTOIRE_CONFIG}/${TYPE_CERTIFICAT}/ca-config \
		-passin pass:${MDP_CAINTERMEDIATE_KEY} \
		-out ${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.crt \
		-in ${REPERTOIRE_CERTIFICAT}/${TYPE_CERTIFICAT}/hosts/${HOSTNAME}/${COMPOSANT}.req \
		-name CA_intermediate -extensions ${RSA_TYPE}_RSA_SSL -batch
}

function generateclientcertificate {
	# Arguments
	CLIENT_NAME=${1}
	MDP_KEY=${2}
	MDP_CAINTERMEDIATE_KEY=${3}

	echo "	Création du certificat ${TYPE_CERTIFICAT} pour ${CLIENT_NAME} hébergé sur ${HOSTNAME}..."
	mkdir -p ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME};
	echo "	Generation de la clé..."
	openssl req -newkey ${PARAM_KEY_CHIFFREMENT} \
		-passout pass:${MDP_KEY} \
		-keyout ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.key \
		-out ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.req \
		-config ${REPERTOIRE_CONFIG}/client/ca-config \
		-subj "/CN=${CLIENT_NAME}/O=Vitam./C=FR/ST=idf/L=paris" \
		-batch

	echo "	Generation du certificat signé avec client..."
	openssl ca -config ${REPERTOIRE_CONFIG}/client/ca-config \
		-passin pass:${MDP_CAINTERMEDIATE_KEY} \
		-out ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.crt \
		-in ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.req \
		-name CA_intermediate -extensions CLIENT_RSA_SSL -batch
	# cat ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.key > ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.pem
	# cat ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.crt >> ${REPERTOIRE_CERTIFICAT}/client/${CLIENT_NAME}/${CLIENT_NAME}.pem
}

function crtkey2p12 {
	# Arguments
	BASEFILE=${1}
	MDP_KEY=${2}
	COMPOSANT=${3}
	MDP_P12=${4} #

	openssl pkcs12 -export \
		-inkey "${BASEFILE}.key" \
		-in "${BASEFILE}.crt" \
		-name "${COMPOSANT}" \
		-passin pass:"${MDP_KEY}" \
		-out "${BASEFILE}.p12" \
		-passout pass:"${MDP_P12}";

	echo "PEM from P12..."
	openssl pkcs12 -nodes -in ${BASEFILE}.p12 -passin pass:"${MDP_P12}" -out ${BASEFILE}.pem
}


function generatetruststore {
	# Arguments
    CERT=${1}
    MDP_CAINTERMEDIATE_KEY=${2}
    STORE=${3}
    ALIAS=${34}
    MDP=${5}
    
    if [ ! -f ${STORE} ]; then
    	echo "	Création du keystore.."
	    keytool -import \
	            -file "${CERT}" \
	            -alias "${ALIAS}" \
	            -keystore ${STORE} \
	            -dname "CN=${ALIAS}/O=Vitam./C=FR" \
	            -storepass ${MDP} \
	            -keypass ${MDP_CAINTERMEDIATE_KEY} \
	            -noprompt
	    # Rajouter CA et CA intermediaire
	    echo "	Import CA intermediate.."
	    keytool -import -keystore ${STORE} -file ${REPERTOIRE_CA}/server_intermediate/ca.crt -alias CA_server_intermediate
	    echo "	Import CA root.."
	    keytool -import -keystore ${STORE} -file ${REPERTOIRE_CA}/server/ca.crt -alias CA_server_intermediate

	else
		echo "Le store ${STORE} existe déjà !"
	fi
}

# function generationstore {
# 	# Arguments
#     STORE=${1}
#     ALIAS=${2}
#     MDP=${3}

#     mkdir -p $(dirname $STORE)
#     if [ ! -f ${STORE} ]; then
#     	echo "	Création du keystore.."
#     	# FIXME: WRONG
# 		keytool -keystore ${STORE} -genkey -alias ${ALIAS}  -storepass ${MDP} -keypass ${MDP} \
# 		-dname "CN=${ALIAS}/O=Vitam./C=FR/ST=idf/L=paris"
# 		echo "	Keystore generation"
# 	else
# 		echo "Le keystore ${STORE} existe déjà !";
# 	fi
# }

function addcrtinjks {
	# Arguments
	STORE=${1}
	MDP_STORE=${2}
	CERTIFICAT=${3}
	ALIAS=${4}

	keytool -import -keystore ${STORE} \
		-file ${CERTIFICAT} \
		-storepass ${MDP_STORE} \
		-keypass ${MDP_STORE} \
		-noprompt \
		-alias ${ALIAS}
}

function addp12injks {
	# Arguments
	STORE=${1}
	MDP_STORE=${2}
	CERTIFICAT=${3}
	MDP_CERTIFICAT=${4}
	ALIAS=${5}

	# KWA TRIED : same mdp for key and store
	keytool -importkeystore \
		-srckeystore ${CERTIFICAT} -srcstorepass ${MDP_CERTIFICAT} -srcstoretype PKCS12 \
		-destkeystore ${1} -storepass ${MDP_STORE} -keypass ${MDP_STORE} -deststorepass ${MDP_STORE} -destkeypass ${MDP_STORE} -deststoretype JKS
}

function addcainjks {
	# Arguments
	STORE=${1}
	MDP_STORE=${2}
	CERTIFICAT=${3}
	ALIAS=${4}

	keytool -import -trustcacerts -keystore ${STORE} \
		-file ${CERTIFICAT} \
		-storepass ${MDP_STORE} \
		-keypass ${MDP_STORE} \
		-noprompt \
		-alias ${ALIAS}
}
