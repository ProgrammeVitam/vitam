#!/bin/bash
DOCKER_COMPOSE_DIRNAME="$(dirname ${BASH_SOURCE[0]})"

echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up -d mongo elasticsearch"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up -d mongo elasticsearch

echo "Waiting for ES container to start"
sleep 5
until $(curl --output /dev/null --silent --head --fail http://localhost:9200/); do
    echo 'Waiting for ES container to start...'
    sleep 1
    ((c++)) && ((c==60)) && break
done

echo "Updating ES conf"
curl -XPUT -H "Content-Type: application/json" http://localhost:9200/_cluster/settings -d '{ "transient": { "cluster.routing.allocation.disk.threshold_enabled": false } }'
echo ""

echo "Tail docker logs..."
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up logs -f --tail=all"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml logs -f --tail=all

echo "Stopping docker-compose..."
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down
