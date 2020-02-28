#!/usr/bin/env bash
set -e
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
#
# contact.vitam@culture.gouv.fr
#
# This software is a computer program whose purpose is to implement a digital archiving back-office system managing
# high volumetry securely and efficiently.
#
# This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
# software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
# circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
#
# As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
# users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
# successive licensors have only limited liability.
#
# In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
# developing or reproducing the software by the user in light of its specific status of free software, that may mean
# that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
# experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
# software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
# to be ensured and, more generally, to use and operate it in the same conditions as regards security.
#
# The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
# accept its terms.
#*******************************************************************************
WORKING_DIR=$(dirname $0)

SOURCES_FILE=${WORKING_DIR}/sources # Contains all the urls where download rpm
TARGET_DIR=${WORKING_DIR}/target      # Targer dir where copying rpm dowloaded
mkdir -p ${TARGET_DIR}

if [ -f "${SOURCES_FILE}" ]
then
	cat ${SOURCES_FILE} |  
	while read SRC_URL   
	do
		echo "SRC_URL : ${SRC_URL}"
		if [[ $(echo "${SRC_URL}" | grep -E -o '^[^#]') ]] # skip is the line is commented
		then
			FILE=$(echo "${SRC_URL}" | grep -E -o '[^/]+$') # get the name of the rpm file
			if [ -f "${TARGET_DIR}/${FILE}" ]
			then
			 	echo "${FILE} already exists in ${TARGET_DIR} ! Skipping..." 
			else # if [ -f "${TARGET_DIR}/${FILE}" ]
			 	echo "Downloading ${SRC_URL} into ${TARGET_DIR}..."
			 	curl -k ${SRC_URL} -o ${TARGET_DIR}/${FILE}.tmp	  
			 	mv ${TARGET_DIR}/${FILE}.tmp ${TARGET_DIR}/${FILE}
			 	echo "Download done."
			fi 
		else #if [echo "${SRC_URL}" | grep -E -o '^[^#]']
			echo "${SRC_URL} is commented  ! Skipping..."
		fi
	done
else # if [ -f "${SOURCES_FILE}" ]
	echo "${SOURCES_FILE} doesn't exists  ! Exiting..."
fi 
