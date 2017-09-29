#!/bin/sh
if [ $# -ne 2 ]
then
	echo "Usage"
	echo "$0 <nom de la simulation> <classe de simulation>"
	exit 1
fi
NOM_SIMULATION=${1}
CLASSE_SIMULATION=${2}

SIMULATION_ROOT_DIR=/vitam/data/gatling/${NOM_SIMULATION}

DEFAULT_GATLING_HOME=`pwd`

if [ -d ${SIMULATION_ROOT_DIR} ]
then
	export JAVA_OPTS="-DconfigSimulation=${SIMULATION_ROOT_DIR}/application.properties" 
	cd ${DEFAULT_GATLING_HOME}/bin
	./gatling.sh -sf ${SIMULATION_ROOT_DIR}/simulations -s simulations.${CLASSE_SIMULATION} -bdf ${SIMULATION_ROOT_DIR}/bodies -df ${SIMULATION_ROOT_DIR}/data -rf ${SIMULATION_ROOT_DIR}/results/ -m
else
	echo "${SIMULATION_ROOT_DIR} inexistant !"
	exit 1
fi
