#!/bin/bash

SIEGFRIED_VERSION="1.7.12"
WORKING_FOLDER=$(dirname $0)
GO_PROGRAM="go"
#GO_PROGRAM="/usr/bin/go"

# Inspired by RPM build script as dh-make-golang seems to have vendors dependencies issues since go 1.6
# https://github.com/Debian/dh-make-golang/issues/46
# TODO: When dh-make-golang issue will be fixed, use it:
# apt-get install git-buildpackage
# apt-get install dh-make-golang
# git-pbuilder create # we may need to edit the repo url in /etc/pbuilderrc for ubuntu distros
# dh-make-golang github.com/richardlehane/siegfried
# cd siegfried
# git add debian && git commit -a -m 'Packaging'
# gbp buildpackage --git-pbuilder
# what next ???

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-siegfried

# Get & extract the archive
curl -k -L https://github.com/richardlehane/siegfried/archive/v${SIEGFRIED_VERSION}.tar.gz -o v${SIEGFRIED_VERSION}.tar.gz
tar xzf v${SIEGFRIED_VERSION}.tar.gz

# Create a GOPATH env var
mkdir -p siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane
ln -s $(pwd)/siegfried-${SIEGFRIED_VERSION} siegfried-${SIEGFRIED_VERSION}/_build/src/github.com/richardlehane/siegfried
export GOPATH=$(pwd)/siegfried-${SIEGFRIED_VERSION}/_build

# Build sf & roy, then move the binary to the vitam directories
${GO_PROGRAM} build -o sf github.com/richardlehane/siegfried/cmd/sf
${GO_PROGRAM} build -o roy github.com/richardlehane/siegfried/cmd/roy
mv sf vitam/bin/siegfried
mv roy vitam/bin/siegfried

# Copy the roy data files
mv siegfried-${SIEGFRIED_VERSION}/cmd/roy/data/* vitam/app/siegfried/

# Delete the sources
rm -rf siegfried-${SIEGFRIED_VERSION}
rm -f v${SIEGFRIED_VERSION}.tar.gz

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-siegfried ${WORKING_FOLDER}/target
rm -rf vitam-siegfried/vitam/app/siegfried/*
rm -rf vitam-siegfried/vitam/bin/siegfried/*

popd
