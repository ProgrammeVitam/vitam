#!/usr/bin/env bash
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

export VITAMDEV_GIT_REPO="$( cd "$( readlink -f $(dirname ${BASH_SOURCE[0]}) )/.." ; pwd )"

if [ ${EUID} -eq 0 ]
then
	echo "Please dont't run this script as root !"
	exit 1
fi

# Either rpm or deb
if [ -z "${VITAM_TARGET}" ] ; then
	VITAM_TARGET=rpm
fi
echo "Using vitam target : ${VITAM_TARGET}"

VITAMDEV_USER=${LOGNAME}
VITAMDEV_IMAGE=vitam/dev-${VITAM_TARGET}-base
VITAMDEV_CONTAINER=vitam-${VITAM_TARGET}-dev

echo "#### VITAM development environment ####"

if [ -z "$(docker ps -a | grep vitam-${VITAM_TARGET}-dev)" ]; then
	echo "Docker container not found locally ; launching it..."

	if [ -z "${VITAMDEV_GIT_REPO}" ] ; then
	  echo "Please enter the location of your vitam git repository"
	  read VITAMDEV_GIT_REPO
	  export VITAMDEV_GIT_REPO
	  echo "Note : to persist this value, insert this environment variable : 'export VITAMDEV_GIT_REPO=${VITAMDEV_GIT_REPO}'"
	fi

	VITAMDEV_HOME=${HOME}
	VITAMDEV_USER_UID=$(id -u ${VITAMDEV_USER})
	VITAMDEV_USER_GID=$(id -g ${VITAMDEV_USER})

	if ! [ -d "${VITAMDEV_GIT_REPO}/.git" ] ; then
		echo "Given vitam git repository is not a git repository !"
		exit 2
	fi

	echo "Building development container..."
	docker build -t ${VITAMDEV_IMAGE} --rm --pull \
		--build-arg http_proxy=${HTTP_PROXY} \
		--build-arg https_proxy=${HTTPS_PROXY} \
		-f dev-base/Dockerfile-${VITAM_TARGET} \
		dev-base
	echo "Launching docker container as daemon (launching systemd init process...)"
	docker run -d --privileged -v "${VITAMDEV_GIT_REPO}:/code:cached" -v  /sys/fs/cgroup:/sys/fs/cgroup:ro -v "${VITAMDEV_HOME}/.m2:/devhome/.m2:cached" -p 80:80 -p 8082:8082 -p 9102:9102 -p 9104:9104 -p 9200:9200 -p 9201:9201 -p 9300:9300 -p 9301:9301 -p 9002:9002 -p 9900:9900 -p 27016:27016 -p 27017:27017 -p 10514:10514 -p 8000-8010:8000-8010 -p 8100-8110:8100-8110 -p 8200-8210:8200-8210 -p 8090:8090 -p 8300-8310:8300-8310 -p 5601:5601 -p 8500:8500 -p 8443:8443 -p 8444:8444 --cap-add=SYS_ADMIN --security-opt seccomp=unconfined --name=${VITAMDEV_CONTAINER} --net=bridge --dns=127.0.0.1 --dns=10.100.211.222 --dns=8.8.8.8 ${VITAMDEV_IMAGE}
	if (( ${?} != 0 )); then
		echo "Container refused to start please correct and retry"
		docker rm ${VITAMDEV_CONTAINER}
		exit 1
	fi
	echo "Registering user ${VITAMDEV_USER} in container..."
	docker exec ${VITAMDEV_CONTAINER} groupadd -g ${VITAMDEV_USER_GID} vitam-dev
	if [ "${VITAM_TARGET}" == "rpm" ]; then
		docker exec ${VITAMDEV_CONTAINER} useradd -u ${VITAMDEV_USER_UID} -g ${VITAMDEV_USER_GID} -G wheel \
			-d /devhome -s /bin/bash -c "Welcome, mister developer !" ${VITAMDEV_USER}
	fi
	if [ "${VITAM_TARGET}" == "deb" ]; then
		docker exec ${VITAMDEV_CONTAINER} useradd -u ${VITAMDEV_USER_UID} -g ${VITAMDEV_USER_GID} -G sudo \
			-d /devhome -s /bin/bash -c "Welcome, mister developer !" ${VITAMDEV_USER}
	fi
	echo "Your container is now configured ; to reuse it, just relaunch this script."

else
	echo "Starting existing container (if stopped) ; please wait..."
	docker start ${VITAMDEV_CONTAINER}
fi

echo "Opening console..."
docker exec -it -u ${VITAMDEV_USER} ${VITAMDEV_CONTAINER} bash
echo "Stopping container..."
docker stop ${VITAMDEV_CONTAINER}
echo "Container stopped !"
echo "Hint : your container is now stopped, but not removed ; it will be used the next time you use this command."
echo "To restart from scratch, run 'docker rm ${VITAMDEV_CONTAINER}' to remove the existing container."
