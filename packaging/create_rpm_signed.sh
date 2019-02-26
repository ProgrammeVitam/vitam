#!/bin/bash

. ./functions.sh

# due to expect...
export LANG=C

# if [ -d ${HOME}/.gnupg ]
# then
#    echo "     Delete old .gnupg..."
#    rm -rf ${HOME}/.gnupg
# fi
# echo "     Adding .gnupg key..."
# cp -r ${CURDIR}/.gnupg ${HOME}/
echo "     Checking .gnupg rights..."
if [ ! -d ${HOME}/.gnupg ]
then
    echo "No .gnupg directory in ${HOME} ! Aborting..."
    exit 1
fi
chmod  700 ${HOME}/.gnupg
chmod 600 ${HOME}/.gnupg/*

if [ -z ${GPG_PASSPHRASE} ]
then
    echo "Environment variable GPG_PASSPHRASE is not set !"
    read -s -p "Please enter GPG passphrase if needed or leave blank :" GPG_PASSPHRASE
    export GPG_PASSPHRASE
fi

# echo "Import GPG keypair if ${HOME}/.gnupg/private.key exists "
# if [ -f ${HOME}/.gnupg/private.key ]
# then
#     gpg --import ${HOME}/.gnupg/private.key
# fi
echo ""
echo "Signature des paquets rpm..."
if [ -d ${CURDIR}/rpm_signed ]
then
    for i in `ls -d ${CURDIR}/rpm_signed/vitam-*/*`
    do
        echo "Paquet : $i";
        # pushd ${i}
        echo "     Signature des paquets dans ${i}"
        ${CURDIR}/expect_gpg.sh ${i}
        # popd
    done
fi
# popd

echo "Suppression du .rpmmacros"
rm -f ${HOME}/.rpmmacros

echo "Purge de la variable GPG_PASSPHRASE..."
unset GPG_PASSPHRASE
