#!/bin/bash
if [ "$1" == "" ]; then
    echo "This script needs to know on which environment you want to apply to !"
    exit 1;
fi
ENVIRONNEMENT=${1}

. $(dirname $0)/functions.sh

check_password_file

echo "Recopie des stores dans VITAM"
for j in access ingest; do

	echo "	Recopie pour ${j}-external..."
	mkdir -p ansible-vitam-rpm/roles/vitam/files/${j}-external
	for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-${j}-external ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
		# FIXME : be more restrictive on jks files
		cp -f ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/${j}-external/
	done
	echo "	Fichiers recopiés"
	echo "------------------------"
done


echo "	Recopie pour ihm-demo..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/ihm-demo
cp -f ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/*p12 ansible-vitam-rpm/roles/vitam/files/ihm-demo/keystore_ihm-demo.p12
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-ihm-demo ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp -f ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/ihm-demo/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "	Recopie pour ihm-recette..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/ihm-recette
# recopie du p12
cp -f ${REPERTOIRE_CERTIFICAT}/client/ihm-recette/*p12 ansible-vitam-rpm/roles/vitam/files/ihm-recette/keystore_ihm-recette.p12
# recopie du pem
cp -f ${REPERTOIRE_CERTIFICAT}/client/ihm-recette/*pem ansible-vitam-rpm/roles/vitam/files/ihm-recette/keystore_ihm-recette.pem
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-ihm-recette ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp -f ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/ihm-recette/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "	Recopie pour logbook..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/logbook
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-logbook ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp -f ${REPERTOIRE_CERTIFICAT}/timestamping/hosts/${i}/*p12 ansible-vitam-rpm/roles/vitam/files/logbook/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "	Recopie pour reverse..."
cp -f ${REPERTOIRE_CERTIFICAT}/client/reverse/*p12 ${REPERTOIRE_ROOT}/ansible-vitam-rpm-extra/roles/reverse/files/keystore_client.p12
cat ${REPERTOIRE_CA}/client/ca.crt              >  ${REPERTOIRE_ROOT}/ansible-vitam-rpm-extra/roles/reverse/files/ca_client.crt
cat ${REPERTOIRE_CA}/client_intermediate/ca.crt >> ${REPERTOIRE_ROOT}/ansible-vitam-rpm-extra/roles/reverse/files/ca_client.crt
echo "	Fichiers recopiés"
echo "------------------------"

echo "	Recopie pour storage-engine..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/storage
cp -f ${REPERTOIRE_CERTIFICAT}/client/storage/storage.p12 ansible-vitam-rpm/roles/vitam/files/storage/keystore_storage.p12
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-storage-engine ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp -f ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/storage/
done


echo "	Recopie pour offer-default..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/storage-offer-default
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-storage-offer-default ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp -f ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/storage-offer-default/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "	Fichiers recopiés"
echo "------------------------"



echo "============================================================================================="
echo "Fin de procédure ; vous pouvez déployer l'ansiblerie."
