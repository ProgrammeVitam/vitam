#!/bin/bash
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
#
# contact.vitam@culture.gouv.fr
#
# This software is a computer program whose purpose is to implement a digital archiving back-office system managing
# high volumetry securely and efficiently.
#
# This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
# software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
# circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
#
# As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
# users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
# successive licensors have only limited liability.
#
# In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
# developing or reproducing the software by the user in light of its specific status of free software, that may mean
# that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
# experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
# software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
# to be ensured and, more generally, to use and operate it in the same conditions as regards security.
#
# The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
# accept its terms.
#*******************************************************************************
function keystore {
	if (( ${#} < 6 )); then
		echo "Usage: ${0} <nom_composant> <certName> <keyIn> <certIn> <passIn> <passOut>"
		exit 0
	fi

	COMPOSANT=${1}
	NAME=${2}
	KEY_IN=${3}
	CERT_IN=${4}
	PASS_IN=${5}
	PASS_OUT=${6}

	P12_FILE=keystore_${COMPOSANT}.p12
	JKS_FILE=keystore_${COMPOSANT}.jks

# Generation PKCS12
openssl pkcs12 -export \
-inkey "${KEY_IN}" \
-in "${CERT_IN}" \
-name "${NAME}" \
-passin pass:"${PASS_IN}" \
-out "${P12_FILE}" \
-passout pass:"${PASS_OUT}"

# Generation JKS
keytool -importkeystore \
-srckeystore "${P12_FILE}" \
-srcstoretype PKCS12 \
-srcstorepass "${PASS_OUT}" \
-srcalias "${NAME}" \
-destkeystore "${JKS_FILE}" \
-deststoretype JKS \
-deststorepass "${PASS_OUT}" \
-destalias "${NAME}"

# Suppression P12
# rm -f ${P12_FILE}
}

function generatecertificate {
	# Generation de la cle
	openssl genrsa -aes128 \
        -out ${COMPOSANT}.key -passout pass:${PASSWD} \
        > /dev/null 2>&1
# echo "Generation du certif"
	openssl req -new -x509 \
        -newkey rsa:2048 -sha256 -key ${COMPOSANT}.key \
        -out ${COMPOSANT}.crt -passin pass:${PASSWD} \
        -subj '/CN=Vitam/O=Vitam./C=FR' \
        > /dev/null 2>&1
}

function importp12inkeystore {
	# Fonction d'import de certif
# param 1 -> certif
# param 2 -> chemin du truststore
    CERT=${1}
    STORE=${2}
    ALIAS=${3}
    keytool -import \
            -file "${CERT}" \
            -alias "${ALIAS}" \
            -keystore ${STORE}
}

function generate_ca {
	openssl req -new -x509 -extensions v3_ca -days 3650 -newkey rsa:4096 -keyout PKI/ca.key -out PKI/ca.crt -config PKI/ca-config -passin pass:${1} -passout pass:${2} -subj '/CN=Vitam/O=Vitam./C=FR'
}







echo "Sourcer les informations nécessaires"
# ansible-vault view environments-rpm/group_vars/all/vault.yml | sed -e 's/: /=/'
echo "Création de la CA"
generate_ca qwerty azerty 

echo "Generation des jks de ihm-demo"
keystore ihm-demo 

echo "Generation des JKS de ingest-external"
keystore ingest-external

echo "Génération du certificat de ihm-demo"

echo "Importer le p12 dans le truststore"

echo "Génerer le grantstore"