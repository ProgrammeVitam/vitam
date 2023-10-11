#!/usr/bin/env bash
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

set -e

export VITAMDEV_GIT_REPO="$( cd "$( readlink -f $(dirname ${BASH_SOURCE[0]}) )/.." ; pwd )"
VITAM_CURRENT_BRUNCH="$( git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/\1/' )"

# Set max_map_count (required for elasticsearch)
if [[ $(cat /proc/sys/vm/max_map_count | grep "262144") -eq 0 ]]; then
  sudo sysctl -w vm.max_map_count=262144;
fi

CONTAINER_NAME="dev"
IMAGE_NAME="base"

if [[ $VITAM_CURRENT_BRUNCH == master_* ]]
then
	CONTAINER_NAME=${VITAM_CURRENT_BRUNCH}
	IMAGE_NAME=${VITAM_CURRENT_BRUNCH}
fi

if [ ${EUID} -eq 0 ]
then
	echo "Please don't run this script as root !"
	exit 1
fi

VITAM_TARGET=podman-deb-cots

MAPPING_PORTS="-p 9200:9200 -p 9201:9201 -p 9300:9300 -p 9301:9301 -p 9000:9000 -p 27016:27016 -p 27017:27017 -p 19000:19000  -p 8500:8500"
VOLUME_INGEST="/vitam/data/ingest-external"
VOLUME_WORKER="/vitam/data/worker"
VOLUME_WORKER_TMP="/vitam/tmp/worker"
VOLUME_DATA_TMP="/vitam/data/tmp"
VOLUME_COLLECT_TMP="/vitam/tmp/collect-internal"

echo "Using vitam target : ${VITAM_TARGET}"

VITAMDEV_IMAGE=vitam/dev-${VITAM_TARGET}-${IMAGE_NAME}
VITAMDEV_CONTAINER=vitam-${VITAM_TARGET}-${CONTAINER_NAME}

echo "#### VITAM development environment ####"

if [ -z "$(podman ps -a | grep -w vitam-${VITAM_TARGET}-${CONTAINER_NAME})" ]; then
	echo "Container not found locally ; launching it..."

	VITAMDEV_HOME=${HOME}

	if ! [ -d "${VITAMDEV_GIT_REPO}/.git" ] ; then
		echo "Given vitam git repository is not a git repository !"
		exit 2
	fi

	echo "Building development container..."
	podman build -t ${VITAMDEV_IMAGE} --rm --pull \
		--build-arg http_proxy=${HTTP_PROXY} \
		--build-arg https_proxy=${HTTPS_PROXY} \
		-f dev-base/Dockerfile-${VITAM_TARGET} \
		dev-base
	echo "Launching container as daemon (launching systemd init process...)"
  sudo mkdir -p ${VOLUME_INGEST}
  sudo mkdir -p ${VOLUME_WORKER}
  sudo mkdir -p ${VOLUME_WORKER_TMP}
  sudo mkdir -p ${VOLUME_DATA_TMP}
  sudo mkdir -p ${VOLUME_COLLECT_TMP}

  podman run -d -v "${VITAMDEV_GIT_REPO}:/code" -v ${VOLUME_INGEST}:${VOLUME_INGEST} -v ${VOLUME_WORKER}:${VOLUME_WORKER} -v ${VOLUME_WORKER_TMP}:${VOLUME_WORKER_TMP} -v  ${VOLUME_COLLECT_TMP}:${VOLUME_COLLECT_TMP} -v ${VOLUME_DATA_TMP}:${VOLUME_DATA_TMP} -v /sys/fs/cgroup:/sys/fs/cgroup:ro -v "${VITAMDEV_HOME}/.npmrc:/root/.npmrc"  -v "${VITAMDEV_HOME}/.m2:/root/.m2" ${MAPPING_PORTS} --name=${VITAMDEV_CONTAINER} --net=bridge --dns=127.0.0.1 --dns=10.100.211.222 --dns=8.8.8.8 ${VITAMDEV_IMAGE}

	if (( ${?} != 0 )); then
		echo "Container refused to start please correct and retry"
		podman rm ${VITAMDEV_CONTAINER}
		exit 1
	fi

	echo "Your container is now configured ; to reuse it, just relaunch this script."

else
	echo "Starting existing container (if stopped) ; please wait..."
	podman start ${VITAMDEV_CONTAINER}
fi

echo "Opening console..."
podman exec -it ${VITAMDEV_CONTAINER} bash
echo "Stopping container..."
podman stop ${VITAMDEV_CONTAINER}
echo "Container stopped !"
echo "Hint : your container is now stopped, but not removed ; it will be used the next time you use this command."
echo "To restart from scratch, run 'podman rm ${VITAMDEV_CONTAINER}' to remove the existing container."
