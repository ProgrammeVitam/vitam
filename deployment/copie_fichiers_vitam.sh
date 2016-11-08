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
		cp ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/${j}-external/
	done
	echo "	Fichiers recopiés"
	echo "------------------------"
done

echo "	Recopie pour ihm-demo..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/ihm-demo
cp ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/*p12 ansible-vitam-rpm/roles/vitam/files/ihm-demo/keystore_ihm-demo.p12
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-ihm-demo ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/ihm-demo/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "	Recopie pour ihm-recette..."
mkdir -p ansible-vitam-rpm/roles/vitam/files/ihm-recette
cp ${REPERTOIRE_CERTIFICAT}/client/ihm-recette/*p12 ansible-vitam-rpm/roles/vitam/files/ihm-recette/keystore_ihm-recette.p12
for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-ihm-recette ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
	# FIXME : be more restrictive on jks files
	cp ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/*jks ansible-vitam-rpm/roles/vitam/files/ihm-recette/
done
echo "	Fichiers recopiés"
echo "------------------------"


echo "============================================================================================="
echo "Fin de procédure ; vous pouvez déployer l'ansiblerie."

