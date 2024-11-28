#!/bin/bash
set -e

CEREBRO_VERSION=0.9.4
CEREBRO_FILE=cerebro-${CEREBRO_VERSION}.zip
INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
    mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-cerebro/vitam/app/cerebro/

echo "Downloading ${CEREBRO_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${CEREBRO_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${CEREBRO_FILE} -o ${CEREBRO_FILE}
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://github.com/lmenezes/cerebro/releases/download/v${CEREBRO_VERSION}/${CEREBRO_FILE} -o ${CEREBRO_FILE}
fi

unzip ${CEREBRO_FILE}
mv cerebro-${CEREBRO_VERSION}/* .
rm -rf cerebro-${CEREBRO_VERSION}
rm -f ${CEREBRO_FILE}

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-cerebro ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-elasticsearch-cerebro/vitam/app/cerebro/

for item in $(ls); do
    rm -rf ${item}
done

popd
