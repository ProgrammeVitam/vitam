#!/bin/bash

WORKING_FOLDER=$(dirname $0)

pushd ${WORKING_FOLDER}/vitam-user-vitam-1.0

debuild -us -uc

popd
