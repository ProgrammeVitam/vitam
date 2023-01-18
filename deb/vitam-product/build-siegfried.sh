#!/bin/bash
SIEGFRIED_VERSION="1.9.6"
WORKING_FOLDER=$(dirname $0)
SIEGFRIED_URL_BUILD="https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/siegfried_1-9-6_linux64.zip"
SIEGFRIED_URL_DATA="https://github.com/richardlehane/siegfried/releases/download/v${SIEGFRIED_VERSION}/data_1-9-6.zip"

if [ ! -d ${WORKING_FOLDER}/target ]; then
  mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-siegfried

# Create a GOPATH env var
mkdir -p siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane
ln -s $(pwd)/siegfried-${SIEGFRIED_VERSION} siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane/siegfried
export GOPATH=$(pwd)/siegfried-${SIEGFRIED_VERSION}/_build


curl -k -L ${SIEGFRIED_URL_BUILD} -o siegfried_${SIEGFRIED_VERSION}.zip
if [ $? != 0 ]; then
  echo "ERROR downloading siegfried: ${SIEGFRIED_URL_BUILD}"
  exit 1
fi

echo "unzip siegfried_${SIEGFRIED_VERSION}.zip"
unzip -q siegfried_${SIEGFRIED_VERSION}.zip
if [ $? != 0 ]; then echo "ERROR unzip: $?"; exit 1; fi
mv -v sf roy vitam/bin/siegfried


curl -k -L ${SIEGFRIED_URL_DATA} -o data_${SIEGFRIED_VERSION}.zip
if [ $? != 0 ]; then
  echo "ERROR downloading data: ${SIEGFRIED_URL_DATA}"
  exit 1
fi

echo "unzip siegfried_${SIEGFRIED_VERSION}.zip"
unzip -q data_${SIEGFRIED_VERSION}.zip
if [ $? != 0 ]; then echo "ERROR unzip: $?"; exit 1; fi
# Copy the roy data files
mv -v siegfried/* vitam/app/siegfried/

# Delete the sources
# fix for strange behavior change ; give write access to delete...
chmod -R 700 siegfried-${SIEGFRIED_VERSION}/
rm -rf siegfried-${SIEGFRIED_VERSION}/
rm -f siegfried_${SIEGFRIED_VERSION}.zip
rm -f data_${SIEGFRIED_VERSION}.zip
rm -rf siegfried
popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-siegfried ${WORKING_FOLDER}/target
if [ $? != 0 ]; then echo "ERROR dpkg-deb --build vitam-siegfried"; exit 1; fi

# Cleaning files after pkg build
rm -rf vitam-siegfried/vitam/app/siegfried/*
rm -rf vitam-siegfried/vitam/bin/siegfried/*

popd

