#!/bin/bash
. $(dirname $0)/functions.sh

echo "Lancement de la procédure de création d'une CA"
echo "=============================================="
if [ ! -d ${REPERTOIRE_CA} ]; then
	echo "Répertoire ${REPERTOIRE_CA} absent ; création..."
	mkdir -p ${REPERTOIRE_CA};
fi
if [ ! -d ${TEMP_CERTS} ]; then
	echo "Création du répertoire de travail temporaire newcerts sous ${TEMP_CERTS}..."
	mkdir -p ${TEMP_CERTS}
fi

for  i in server client
do
	mkdir -p ${REPERTOIRE_CA}/${i}
	echo "	Création de CA root pour ${i}..."
	generate_ca_root carootkeypassword ${i} ${i} # FIXME : parameters for passwords
	if [ $?==0 ]; then
		echo "	CA root pour ${i} créée sous ${REPERTOIRE_CA}/${i} !"
	else
		echo "problème de génération de CA root pour ${i} !"
		exit 1;
	fi
	mkdir -p ${REPERTOIRE_CA}/${i}_intermediate
	echo "	Création de la CA intermediate pour ${i}..."
	generate_ca_interm caintermediatekeypassword carootkeypassword ${i}_intermediate ${i} # FIXME : parameters for passwords
	if [ $?==0 ]; then
		echo "	CA intemédiaire ${i} créée sous ${REPERTOIRE_CA}/${i}_intermediate !"
	else
		echo "problème de génération de CA intermediate !"
		exit 1;
	fi
	echo "----------------------------------------------------------------------"
done
echo "=========================================================================="
echo "Fin du shell"