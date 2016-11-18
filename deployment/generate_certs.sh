#!/bin/bash
if [ "$1" == "" ]; then
    echo "This script needs to know on which environment you want to apply to !"
    exit 1;
fi
ENVIRONNEMENT=${1}

. $(dirname $0)/functions.sh

check_password_file

echo "Sourcer les informations nécessaires dans vault.yml"
eval $(ansible-vault view $(dirname $0)/environments-rpm/group_vars/all/vault.yml ${ANSIBLE_VAULT_PASSWD}| sed -e 's/: /=/')

echo "Generation du certificat client de ihm-demo"
generateclientcertificate ihm-demo ihmdemoclientkeypassword caintermediatekeypassword
echo "	Conversion en p12..."
crtkey2p12 ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ihm-demo ihmdemoclientkeypassword ihm-demo ${p12_ihm_demo_password}
echo "	Fin de conversion sous ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ !"

echo "Fin de génération du certificat client de ihm-demo"
echo "--------------------------------------------------"





echo "Generation du certificat client de ihm-recette"
generateclientcertificate ihm-recette ihmrecetteclientkeypassword caintermediatekeypassword
echo "	Conversion en p12..."
crtkey2p12 ${REPERTOIRE_CERTIFICAT}/client/ihm-recette/ihm-recette ihmrecetteclientkeypassword ihm-recette ${p12_ihm_recette_password}
echo "	Fin de conversion sous ${REPERTOIRE_CERTIFICAT}/client/ihm-recette/ !"

echo "Fin de génération du certificat client de ihm-recette"
echo "--------------------------------------------------"



for j in ingest access; do
	echo "Generation du certificat server de ${j}-external"
	for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-${j}-external ${ANSIBLE_VAULT_PASSWD}| sed "1 d"); do
		echo "	Génération pour ${i}..."
		generatehostcertificate ${j}-external ${j}externalserverkeypassword caintermediatekeypassword ${i} server ${j}-external.service.consul
		echo "	Conversion en p12..."
		crtkey2p12 ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/${COMPOSANT} ingestexternalserverkeypassword ${i} ${p12_ihm_demo_password}
		echo "	Fin de conversion sous ${REPERTOIRE_CERTIFICAT}/server/hosts/${i}/ !"
	done
	echo "Fin de génération du certificat server de ${j}-external"
	echo "---------------------------------------------------------"
done

echo "============================================================================================="
echo "Fin de script."