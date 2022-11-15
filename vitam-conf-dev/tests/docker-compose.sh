#!/bin/bash
MINIO_SSL_CERTIF_FOLDER="/tmp/minio_ssl_certs"
DOCKER_COMPOSE_DIRNAME="$(dirname ${BASH_SOURCE[0]})"
VITAMDEV_GIT_REPO="$( cd "$( readlink -f ${DOCKER_COMPOSE_DIRNAME} )/../.." ; pwd )"
echo "Vitam git repo : ${VITAMDEV_GIT_REPO}"
if [ -d "${VITAMDEV_GIT_REPO}/sources/common/common-storage/src/test/resources/s3/tls" ]; then
    echo "Vitam git repo contains S3 certificates folder : ${VITAMDEV_GIT_REPO}/sources/common/common-storage/src/test/resources/s3/tls"
    mkdir -p ${MINIO_SSL_CERTIF_FOLDER}
    cp ${VITAMDEV_GIT_REPO}/sources/common/common-storage/src/test/resources/s3/tls/public.crt ${MINIO_SSL_CERTIF_FOLDER}
    cp ${VITAMDEV_GIT_REPO}/sources/common/common-storage/src/test/resources/s3/tls/private.key ${MINIO_SSL_CERTIF_FOLDER}
    export MINIO_SSL_CERTIF_FOLDER="${MINIO_SSL_CERTIF_FOLDER}"
else
    echo "Vitam git repo does not contains S3 certificates folder : ${VITAMDEV_GIT_REPO}/sources/common/common-storage/src/test/resources/s3/tls"
    exit 1
fi

echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up -d"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up -d

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

echo "Waiting for swift container to start"
sleep 5
until $(curl --output /dev/null --silent --head --fail http://127.0.0.1:35357/v3); do
    echo 'Waiting for swift container to start...'
    sleep 2
    ((c++)) && ((c==15)) && break
done

echo "Configuring swift container"
docker-compose exec swift "swift/bin/register-swift-endpoint.sh" http://127.0.0.1:8080

echo "Tail docker logs..."
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up logs -f --tail=all"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml logs -f --tail=all

echo "Stopping docker-compose..."
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down
