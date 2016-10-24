#!/bin/bash
if [ "$1" == "" ]; then
    echo "This script needs to know on which environment you want to apply to !"
    exit 1;
fi
ENVIRONNEMENT=${1}

. $(dirname $0)/functions.sh

echo "Sourcer les informations nécessaires dans vault.yml"
eval $(ansible-vault view $(dirname $0)/environments-rpm/group_vars/all/vault.yml | sed -e 's/: /=/')

echo "Generation du certificat client de ihm-demo"
generateclientcertificate ihm-demo ihmdemoclientkeypassword caintermediatekeypassword
echo "	Conversion en p12..."
crtkey2p12 ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ihm-demo ihmdemoclientkeypassword ihm-demo ${p12_ihm_demo_password}
echo "	Fin de conversion sous ${REPERTOIRE_CERTIFICAT}/client/ihm-demo/ !"

echo "Fin de génération du certificat client de ihm-demo"
echo "--------------------------------------------------"


for j in ingest access; do
	echo "Generation du certificat server de ${j}-external"
	for i in $(ansible -i environments-rpm/hosts.${ENVIRONNEMENT} --list-hosts hosts-${j}-external --ask-vault-pass| sed "1 d"); do
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