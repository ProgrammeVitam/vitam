#!/bin/bash
DOCKER_COMPOSE_DIRNAME="$(dirname ${BASH_SOURCE[0]})"

echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up mongo elasticsearch"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up mongo elasticsearch

echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down mongo elasticsearch"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down mongo elasticsearch