#!/bin/bash

MONGOCLIENT_VERSION="1.4.0"
METEOR_VERSION="1.4.2"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-mongoclient

HOME=$(pwd)

# Get & extract the archive
curl -k -L https://github.com/rsercano/mongoclient/archive/${MONGOCLIENT_VERSION}.zip -o ${MONGOCLIENT_VERSION}.zip
unzip ${MONGOCLIENT_VERSION}.zip
rm -f ${MONGOCLIENT_VERSION}.zip

cd mongoclient-${MONGOCLIENT_VERSION}
meteor npm install --production
mkdir ${HOME}/buildir
meteor build ${HOME}/buildir --architecture os.linux.x86_64
cd ${HOME}/buildir
tar -xzf mongoclient-${MONGOCLIENT_VERSION}.tar.gz
rm mongoclient-${MONGOCLIENT_VERSION}.tar.gz
cd ${HOME}/buildir/bundle/programs/server/


# curl -sL https://deb.nodesource.com/setup_0.12 | sudo bash -
# sudo apt-get install -y nodejs

npm install
# Note : following line should not be necessary in theory, but somehow this dependency is missing in the runtime...
npm install tunnel-ssh

cd ${HOME}
cp -r ${HOME}/buildir/bundle/* ${HOME}/vitam/bin/mongoclient/
cp ${HOME}/.meteor/packages/meteor-tool/${METEOR_VERSION}/mt-os.linux.x86_64/dev_bundle/bin/node ${HOME}/vitam/bin/mongoclient

# TODO: clean
rm -rf ${HOME}/mongoclient-${MONGOCLIENT_VERSION}
rm -f  ${HOME}/.meteorsession
rm -rf ${HOME}/.meteor
rm -rf ${HOME}/.npm
rm -rf ${HOME}/buildir

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-mongoclient ${WORKING_FOLDER}/target

popd

pushd ${WORKING_FOLDER}/vitam-mongoclient

rm -rf vitam/bin/mongoclient/*

popd
