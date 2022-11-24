#!/bin/bash
SIEGFRIED_VERSION="1.9.1"
SIEGFRIED_URL="https://github.com/richardlehane/siegfried/archive/v${SIEGFRIED_VERSION}.tar.gz"
WORKING_FOLDER=$(dirname $0)

function gobuild { go build -a -ldflags "-B 0x$(head -c20 /dev/urandom|od -An -tx1|tr -d ' \n')" -v "$@"; }

if [ ! -d ${WORKING_FOLDER}/target ]; then
  mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-siegfried

# Get & extract the archive
curl -k -L ${SIEGFRIED_URL} -o v${SIEGFRIED_VERSION}.tar.gz
if [ $? != 0 ]; then
  echo "ERROR downloading siegfried: ${SIEGFRIED_URL}"
  exit 1
fi
echo "untar v${SIEGFRIED_VERSION}.tar.gz"
tar xzf v${SIEGFRIED_VERSION}.tar.gz
if [ $? != 0 ]; then echo "ERROR untar: $?"; exit 1; fi

# Create a GOPATH env var
mkdir -p siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane
ln -s $(pwd)/siegfried-${SIEGFRIED_VERSION} siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane/siegfried
export GOPATH=$(pwd)/siegfried-${SIEGFRIED_VERSION}/_build

go version
# fix for strange behavior in dependencies required
go mod init vendor
# Build sf & roy, then move the binary to the vitam directories
go get -d github.com/richardlehane/siegfried/cmd/sf@v${SIEGFRIED_VERSION}
gobuild -o sf github.com/richardlehane/siegfried/cmd/sf
if [ $? != 0 ]; then echo "ERROR build sf"; exit 1; fi
go get -d github.com/richardlehane/siegfried/cmd/sf@v${SIEGFRIED_VERSION}
gobuild -o roy github.com/richardlehane/siegfried/cmd/roy
if [ $? != 0 ]; then echo "ERROR build roy"; exit 1; fi
mv -v sf roy vitam/bin/siegfried

# Copy the roy data files
mv -v siegfried-${SIEGFRIED_VERSION}/cmd/roy/data/* vitam/app/siegfried/

# Delete the sources
# fix for strange behavior change ; give write access to delete...
chmod -R 700 siegfried-${SIEGFRIED_VERSION}/
rm -rf siegfried-${SIEGFRIED_VERSION}/
rm -f v${SIEGFRIED_VERSION}.tar.gz

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-siegfried ${WORKING_FOLDER}/target
if [ $? != 0 ]; then echo "ERROR dpkg-deb --build vitam-siegfried"; exit 1; fi

# Cleaning files after pkg build
rm -rf vitam-siegfried/vitam/app/siegfried/*
rm -rf vitam-siegfried/vitam/bin/siegfried/*

popd
