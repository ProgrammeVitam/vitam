#!/bin/bash

KOPF_VERSION="2.0"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-kopf/usr/share/elasticsearch/plugins/kopf

curl -k -L https://github.com/lmenezes/elasticsearch-kopf/archive/${KOPF_VERSION}.tar.gz -o ${KOPF_VERSION}.tar.gz
tar xzf ${KOPF_VERSION}.tar.gz
mv elasticsearch-kopf-${KOPF_VERSION}/* .
rm -rf elasticsearch-kopf-${KOPF_VERSION}
rm -f ${KOPF_VERSION}.tar.gz

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-kopf ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-elasticsearch-kopf/usr/share/elasticsearch/plugins/kopf

for item in $(ls); do
    rm -rf ${item}
done

popd
