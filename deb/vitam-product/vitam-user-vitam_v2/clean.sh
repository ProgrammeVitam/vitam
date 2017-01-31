#!/bin/bash

WORKING_FOLDER=$(dirname $0)

pushd ${WORKING_FOLDER}

for item in $(find . -maxdepth 1 -type f |grep vitam-user-vitam); do
    rm -f ${item}
done

popd

pushd ${WORKING_FOLDER}/vitam-user-vitam-1.0/debian

for item in $(ls vitam-user-vitam.* |grep -v service); do
    rm -f ${item}
done
rm -f debhelper-build-stamp
rm -rf vitam-user-vitam

popd
