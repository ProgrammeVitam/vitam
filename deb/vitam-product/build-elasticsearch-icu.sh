#!/bin/bash
set -e

ICU_VERSION=8.14.3
ICU_FILE=analysis-icu-${ICU_VERSION}.zip
INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
    mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-analysis-icu/usr/share/elasticsearch/plugins/analysis-icu

echo "Downloading ${ICU_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${ICU_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${ICU_FILE} -o ${ICU_FILE}
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/${ICU_FILE} -o ${ICU_FILE}
fi

unzip ${ICU_FILE}
rm -f ${ICU_FILE}

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-analysis-icu ${WORKING_FOLDER}/target

popd

pushd ${WORKING_FOLDER}/vitam-elasticsearch-analysis-icu/usr/share/elasticsearch/plugins/analysis-icu

for item in $(ls); do
    rm -rf ${item}
done

popd
