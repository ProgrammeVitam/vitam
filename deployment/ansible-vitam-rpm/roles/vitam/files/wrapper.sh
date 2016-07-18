#!/bin/sh
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
