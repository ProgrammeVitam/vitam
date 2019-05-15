#!/bin/bash

ICU_VERSION="6.7.2"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-analysis-icu/usr/share/elasticsearch/plugins/analysis-icu

curl -k -L https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-${ICU_VERSION}.zip -o analysis-icu-${ICU_VERSION}.zip
unzip analysis-icu-${ICU_VERSION}.zip
mv elasticsearch/* .
rmdir elasticsearch
#mv elasticsearch-kopf-${KOPF_VERSION}/* .
#rm -rf elasticsearch-kopf-${KOPF_VERSION}
rm -f analysis-icu-${ICU_VERSION}.zip

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-analysis-icu ${WORKING_FOLDER}/target

popd

pushd ${WORKING_FOLDER}/vitam-elasticsearch-analysis-icu/usr/share/elasticsearch/plugins/analysis-icu

for item in $(ls); do
    rm -rf ${item}
done

popd
