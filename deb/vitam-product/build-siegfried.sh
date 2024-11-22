#!/bin/bash
set -e

SIEGFRIED_VERSION=1.9.6
SIEGFRIED_FILE=siegfried_1-9-6_linux64.zip
SIEGFRIED_DATA_FILE=data_1-9-6.zip
INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
    mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-siegfried

mkdir -p vitam/bin/siegfried
mkdir -p vitam/app/siegfried

echo "Downloading ${SIEGFRIED_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${SIEGFRIED_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${SIEGFRIED_FILE} -o siegfried_${SIEGFRIED_VERSION}.zip
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/${SIEGFRIED_FILE} -o siegfried_${SIEGFRIED_VERSION}.zip
fi

echo "unzip siegfried_${SIEGFRIED_VERSION}.zip"
unzip -q siegfried_${SIEGFRIED_VERSION}.zip

mv -v sf roy vitam/bin/siegfried

echo "Downloading ${SIEGFRIED_DATA_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${SIEGFRIED_DATA_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${SIEGFRIED_DATA_FILE} -o data_${SIEGFRIED_VERSION}.zip
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/${SIEGFRIED_DATA_FILE} -o data_${SIEGFRIED_VERSION}.zip
fi

echo "unzip data_${SIEGFRIED_VERSION}.zip"
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
