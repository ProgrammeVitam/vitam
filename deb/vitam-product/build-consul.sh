#!/bin/bash
WORKING_FOLDER=$(dirname $0)
CONSUL_VERSION="1.5.1"

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-consul/vitam/bin/consul/
echo "Repertoire courant: $(pwd)"
echo "Récupérer consul_${CONSUL_VERSION}_linux_amd64.zip"
if [ ! -f consul_${CONSUL_VERSION}_linux_amd64.zip ]; then
	curl -k --max-time 120 https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_linux_amd64.zip -o consul_${CONSUL_VERSION}_linux_amd64.zip
	if [ ${?} != 0 ]; then
		echo "Erreur sur le telechargement du fichier zip de consul: https://releases.hashicorp.com/consul/${CONSUL_VERSION}/consul_${CONSUL_VERSION}_linux_amd64.zip"
		exit 1
	fi
fi
echo "Décompacter consul_${CONSUL_VERSION}_linux_amd64.zip"
unzip -o consul_${CONSUL_VERSION}_linux_amd64.zip
if [ $? != 0 ]; then echo "erreur unzip: $?"; fi
rm -f consul_${CONSUL_VERSION}_linux_amd64.zip
if [ ! -f consul ]; then echo "Erreur: binaire consul non presente !"; fi

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-consul ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-consul/vitam/bin/consul

for item in $(ls); do
    rm -rf ${item}
done

popd
