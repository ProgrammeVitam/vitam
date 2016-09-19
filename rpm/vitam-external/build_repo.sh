#!/usr/bin/env bash

SOURCES_FILE=./sources # Contains all the urls where download rpm
TARGET_DIR=target      # Targer dir where copying rpm dowloaded
mkdir -p ${TARGET_DIR}

if [ -f "${SOURCES_FILE}" ]
then
	cat ${SOURCES_FILE} |  
	while read SRC_URL   
	do
		echo "SRC_URL : ${SRC_URL}"
		if [ $(echo "${SRC_URL}" | grep -E -o '^[^#]') ] # skip is the line is commented
		then
			FILE=$(echo "${SRC_URL}" | grep -E -o '[^/]+$') # get the name of the rpm file
			if [ -f "${TARGET_DIR}/${FILE}" ]
			then
			 	echo "${FILE} already exists in ${TARGET_DIR} ! Skipping..." 
			else # if [ -f "${TARGET_DIR}/${FILE}" ]
			 	echo "Downloading ${SRC_URL} into ${TARGET_DIR}..."
			 	curl ${SRC_URL} -o ./${TARGET_DIR}/${FILE}.tmp	  
			 	mv ./${TARGET_DIR}/${FILE}.tmp ./${TARGET_DIR}/${FILE}
			 	echo "Download done."
			fi 
		else #if [echo "${SRC_URL}" | grep -E -o '^[^#]']
			echo "${SRC_URL} is commented  ! Skipping..."
		fi
	done
else # if [ -f "${SOURCES_FILE}" ]
	echo "${SOURCES_FILE} doesn't exists  ! Exiting..."
fi 