#!/bin/bash
set -e
SIEGFRIED_VERSION="1.9.6"
WORKING_FOLDER=$(dirname $0)
SIEGFRIED_URL_BUILD="https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/siegfried_1-9-6_linux64.zip"
SIEGFRIED_URL_DATA="https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/data_1-9-6.zip"

if [ ! -d ${WORKING_FOLDER}/target ]; then
  mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-siegfried

mkdir -p vitam/bin/siegfried
mkdir -p vitam/app/siegfried

curl -k -L ${SIEGFRIED_URL_BUILD} -o siegfried_${SIEGFRIED_VERSION}.zip

echo "unzip siegfried_${SIEGFRIED_VERSION}.zip"
unzip -q siegfried_${SIEGFRIED_VERSION}.zip

mv -v sf roy vitam/bin/siegfried

curl -k -L ${SIEGFRIED_URL_DATA} -o data_${SIEGFRIED_VERSION}.zip

echo "unzip siegfried_${SIEGFRIED_VERSION}.zip"
unzip -q data_${SIEGFRIED_VERSION}.zip

# Copy the roy data files
mv -v siegfried/* vitam/app/siegfried/

# Delete the sources
rm -f siegfried_${SIEGFRIED_VERSION}.zip
rm -f data_${SIEGFRIED_VERSION}.zip
rm -rf siegfried
popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-siegfried ${WORKING_FOLDER}/target

# Cleaning files after pkg build
rm -rf vitam-siegfried/vitam/app/siegfried
rm -rf vitam-siegfried/vitam/bin/siegfried

popd

