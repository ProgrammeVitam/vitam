#!/bin/bash
set -e

CONSUL_VERSION="1.12.9"
CONSUL_FILE=consul_${CONSUL_VERSION}_linux_amd64.zip
INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)
if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-consul/vitam/bin/consul/

echo "Downloading ${CONSUL_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${CONSUL_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${CONSUL_FILE} -o ${CONSUL_FILE}
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://releases.hashicorp.com/consul/${CONSUL_VERSION}/${CONSUL_FILE} -o ${CONSUL_FILE}
fi

unzip -o ${CONSUL_FILE}
if [ $? != 0 ]; then echo "erreur unzip: $?"; fi
rm -f ${CONSUL_FILE}
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
