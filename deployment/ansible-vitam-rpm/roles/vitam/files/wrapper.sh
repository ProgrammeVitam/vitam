#!/bin/sh
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
#
# contact.vitam@culture.gouv.fr
#
# This software is a computer program whose purpose is to implement a digital archiving back-office system managing
# high volumetry securely and efficiently.
#
# This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
# software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
# circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
ROOT_FOLDER="$(realpath $(dirname $0)/..)"
ENTRYPOINT_FILE="${ROOT_FOLDER}/bin/entry.sh"
SETENV_FILE="${ROOT_FOLDER}/conf/setenv.sh"

if [ -f ${ENTRYPOINT_FILE} ];
then
   . ${ENTRYPOINT_FILE}
else
   echo "run.sh : Caution : entry point script not found (searched file : ${ENTRYPOINT_FILE})."
fi

if [ -f ${SETENV_FILE} ];
then
   . ${SETENV_FILE}
else
   echo "run.sh : Caution : setenv script not found (searched file : ${SETENV_FILE})."
fi

if [ -z "$JAVA_ENTRYPOINT" ]; then
	echo "No entry point was defined ! Please set the content of the JAVA_ENTRYPOINT variable in your setenv script."
	exit 1
fi

if [ -z "$JAVA_CLASSPATH_USE_WILCARD" ]; then
	JAVA_CLASSPATH_USE_WILCARD=true
fi


IS_JAR=$(echo "$JAVA_ENTRYPOINT" | grep '.*\.jar$' -)
if [ -z "$IS_JAR" ];
then
	# Class name
	JAVA_CLASSPATH_USE_WILCARD=true
else
	# Jar name
	JAVA_ENTRYPOINT="-jar ${ROOT_FOLDER}/lib/${JAVA_ENTRYPOINT}"
	JAVA_CLASSPATH_USE_WILCARD=false
fi

if [ "$JAVA_CLASSPATH_USE_WILCARD" = true ];
then
	# Set default classpath options
	exec java -cp "${ROOT_FOLDER}/lib/*" -Dlogback.configurationFile=$ROOT_FOLDER/conf/logback.xml $JAVA_OPTS $JAVA_ENTRYPOINT $JAVA_ARGS
else
	# No classpath
	exec java -Dlogback.configurationFile=$ROOT_FOLDER/conf/logback.xml $JAVA_OPTS $JAVA_ENTRYPOINT $JAVA_ARGS
fi
