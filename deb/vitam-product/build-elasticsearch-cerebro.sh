#!/bin/bash
set -e
CEREBRO_VERSION="0.8.4"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-cerebro/vitam/app/cerebro/

curl -k -L https://github.com/lmenezes/cerebro/releases/download/v${CEREBRO_VERSION}/cerebro-${CEREBRO_VERSION}.zip -o cerebro-${CEREBRO_VERSION}.zip
unzip cerebro-${CEREBRO_VERSION}.zip
mv cerebro-${CEREBRO_VERSION}/* .
rm -rf cerebro-${CEREBRO_VERSION}
rm -f cerebro-${CEREBRO_VERSION}.zip

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-cerebro ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-elasticsearch-cerebro/vitam/app/cerebro/

for item in $(ls); do
    rm -rf ${item}
done

popd
