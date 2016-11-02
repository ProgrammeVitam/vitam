#!/bin/bash
if [ "$1" == "" ]; then
    echo "This script needs to know on which environment you want to apply to !"
    exit 1;
fi
ENVIRONNEMENT=${1}

. $(dirname $0)/functions.sh

echo "Sourcer les informations nécessaires dans vault.yml"
eval $(ansible-vault view $(dirname $0)/environments-rpm/group_vars/all/vault.yml | sed -e 's/: /=/')

for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-ihm-demo --ask-vault-pass| sed "1 d"); do
	echo "Génération du keystore de ihm-demo"
	echo "	Génération pour ${i}..."
	echo "Génération du truststore de ${j}-external..."
	#generationstore ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ingest-external.jks truststore_ingest-external ${TrustStorePassword_ingest_external}
	echo "	Import des CA server dans truststore de ${j}-external..."
	echo "		... import CA server root..."
	mkdir -p ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/
	addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ihm-demo.jks $(eval "echo \$TrustStore_ihm_demo_password") ${REPERTOIRE_CA}/server/ca.crt ca_server_root_crt
	echo "		... import CA server intermediate..."
	addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ihm-demo.jks $(eval "echo \$TrustStore_ihm_demo_password") ${REPERTOIRE_CA}/server_intermediate/ca.crt ca_server_interm_root_crt
	echo "		... import CA client root..."
	addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ihm-demo.jks $(eval "echo \$TrustStore_ihm_demo_password") ${REPERTOIRE_CA}/client/ca.crt ca_client_root_crt
	echo "		... import CA client intermediate..."
	addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ihm-demo.jks $(eval "echo \$TrustStore_ihm_demo_password") ${REPERTOIRE_CA}/client_intermediate/ca.crt ca_client_interm_root_crt

	echo "Fin de génération du trustore de ${j}-external"
	echo "------------------------------------------------"
done

for j in ingest access; do
	for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-${j}-external --ask-vault-pass| sed "1 d"); do
		echo "Génération du keystore de access-external"
		echo "	Génération pour ${i}..."
		#generationstore ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/keystore_ingest-external.jks keystore_ingest-external ${KeyStorePassword_ingest_external}
		
		# Importer les clés de ingest-external
		echo "	Import du p12 de ${j}-external dans le keystore"
		#addp12injks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/keystore_ingest-external.jks ${KeyStorePassword_ingest_external} ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/ingest-external.p12 ${p12_ihm_demo_password} ingest-external
		# MDP=$(eval "echo \$KeyStorePassword_${j}_external")
		# echo $MDP
		# echo addp12injks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/keystore_${j}-external.jks  $MDP ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/${j}-external.p12 ${p12_ihm_demo_password} ${j}-external
		addp12injks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/keystore_${j}-external.jks  $(eval "echo \$KeyStorePassword_${j}_external") ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/${j}-external.p12 ${p12_ihm_demo_password} ${j}-external
		echo "Fin de génération du keystore ${j}-external"
		echo "---------------------------------------------"

		echo "Génération du truststore de ${j}-external..."
		#generationstore ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ingest-external.jks truststore_ingest-external ${TrustStorePassword_ingest_external}
		echo "	Import des CA server dans truststore de ${j}-external..."
		echo "		... import CA server root..."
		addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_${j}-external.jks $(eval "echo \$TrustStorePassword_${j}_external") ${REPERTOIRE_CA}/server/ca.crt ca_server_root_crt
		echo "		... import CA server intermediate..."
		addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_${j}-external.jks $(eval "echo \$TrustStorePassword_${j}_external") ${REPERTOIRE_CA}/server_intermediate/ca.crt ca_server_interm_root_crt
		echo "		... import CA client root..."
		addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_${j}-external.jks $(eval "echo \$TrustStorePassword_${j}_external") ${REPERTOIRE_CA}/client/ca.crt ca_client_root_crt
		echo "		... import CA client intermediate..."
		addcainjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_${j}-external.jks $(eval "echo \$TrustStorePassword_${j}_external") ${REPERTOIRE_CA}/client_intermediate/ca.crt ca_client_interm_root_crt

		echo "Fin de génération du trustore de ${j}-external"
		echo "------------------------------------------------"

		echo "Génération du grantedstore de ${j}-external..."
		# Gros doute sur comme ça se fait...
		#generationstore ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/grantstore_ingest-external.jks grantedstore_ingest-external ${grantedKeyStorePassphrase_ingest_external}

	#	generatetruststore ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ihm-demo.crt ihmdemotototiti ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/grantstore_ingest-external.jks grantedstore_ingest-external ${grantedKeyStorePassphrase_ingest_external}
	#	generatetruststore ${REPERTOIRE_CA}/server_intermediate/ca.crt azerty ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ingest-external.jks truststore_ingest-external ${TrustStorePassword_ingest_external}
		echo "	Import certificat IHM-demo du grantedstore de ${j}-external..."	
		# FIXMEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE !
		addcrtinjks ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/grantstore_${j}-external.jks $(eval "echo \$grantedKeyStorePassphrase_${j}_external") ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ihm-demo.crt ihm-demo
		echo "------------------------------------------------"
	#	importcastore ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/truststore_ingest-external.jks ${TrustStorePassword_ingest_external} ${REPERTOIRE_CA}/server/ca.crt azerty ca_server_root_crt
	done
done

echo "============================================================================================="
echo "Fin de script."