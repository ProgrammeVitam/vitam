#!/usr/bin/env bash

# To use this script, first compress the deployment directory:
# tar cfz deployment.tar.gz vitam.git/deployment
# Then call the script, you have to be in the directory that contains the deployment archive
# vitam.git/packaging/strip_package_for_prod.sh

set -e

TARGET_DIR=target

mkdir -p ${TARGET_DIR}

SRC_TAR=$(find . -regextype posix-extended -maxdepth 1 -regex '.*\.(tgz|tar\.gz)')

if [ $(echo ${SRC_TAR} | wc -l) != 1 ]; then
    echo "Multiple source packages found : ${SRC_TAR}. Only one should be there (ending with .tar.gz or .tgz)"
    exit 1
fi

echo "Uncompressing ${SRC_TAR} into ${TARGET_DIR}..."
tar xvzf ${SRC_TAR} --directory ${TARGET_DIR}

pushd target

echo "Cleaning up..."

# Clean up deployment
rm -f  "deployment/demo_backup_vitam.sh"
rm -f  "deployment/environments/antivirus/scan-dev.sh"
rm -rf "deployment/ansible-vitam-extra"
rm -f  "deployment/vault_pass.txt"
rm -f  "deployment/environments/hosts.local"
rm -f  "deployment/environments/hosts.fulllocal"
rm -f  "deployment/environments/hosts.cots"
rm -f  "deployment/environments/group_vars/all/vault-extra.example"
rm -f  "deployment/environments/group_vars/all/vault-keystores.yml"
rm -f  "deployment/environments/group_vars/all/vault-vitam.yml"
rm -rf "vitam-conf-dev/"

# Clean up PKI (but keep the lib/functions.sh)
rm -rf "deployment/pki/ca"
rm -rf "deployment/pki/config"
rm -f  deployment/pki/scripts/*.sh

rm -rf "deployment/environments/certs/vault-certs.yml"
find "deployment/environments/certs" -name '*.crt' -execdir rm -f {} \;
find "deployment/environments/keystores" -name '*.p12' -execdir rm -f {} \;
find "deployment/environments/keystores" -name '*.jks' -execdir rm -f {} \;


# Remove dev packages
rm -f deb/vitam-product/ihm-recette-*.deb
rm -f rpm/vitam-product/ihm-recette-*.rpm
rm -f rpm/vitam-product/vitam-gatling-*.rpm
rm -f deb/vitam-product/vitam-gatling_*.deb

rm -f rpm/vitam-external/metricbeat-*.rpm
rm -f deb/vitam-external/metricbeat-*.deb
rm -f rpm/vitam-external/packetbeat-*.rpm
rm -f deb/vitam-external/packetbeat-*.deb

popd

TARGET_TAR=${SRC_TAR}.prod.tgz

echo "Building result archive..."
tar cvzf ${TARGET_TAR} ${TARGET_DIR}/*
shasum -a 256 ${TARGET_TAR} > ${TARGET_TAR}.sha256

echo "DONE ! Result file : ${TARGET_TAR}"
