#!/bin/bash
if [ -z ${CURDIR} ]
then
    export CURDIR=$(pwd)
fi
echo "Current dir is ${CURDIR}"

export TARGET_DIR=target

set -e