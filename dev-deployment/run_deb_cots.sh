#!/usr/bin/env bash

if [ ${EUID} -eq 0 ]
then
	echo "Please dont't run this script as root !"
	exit 1
fi
WORKING_FOLDER=$(dirname $0)
export VITAM_TARGET=deb-cots
${WORKING_FOLDER}/run.sh
