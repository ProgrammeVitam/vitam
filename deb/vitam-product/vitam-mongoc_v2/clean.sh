#!/bin/bash

WORKING_FOLDER=$(dirname $0)

pushd ${WORKING_FOLDER}

rm -f vitam-mongoc_1.0.1*

popd

pushd ${WORKING_FOLDER}/vitam-mongoc-1.0/debian

for item in $(ls vitam-mongoc.* |grep -v service); do
    rm -f ${item}
done
rm -f debhelper-build-stamp
rm -rf vitam-mongoc

popd
