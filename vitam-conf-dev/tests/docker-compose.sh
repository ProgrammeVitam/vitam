#!/bin/bash

DOCKER_COMPOSE_DIRNAME="$(dirname ${BASH_SOURCE[0]})"
VITAMDEV_GIT_REPO="$( cd "$( readlink -f ${DOCKER_COMPOSE_DIRNAME} )/../.." ; pwd )"

echo "Vitam git repo : ${VITAMDEV_GIT_REPO}"

echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up
