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
docker-compose exec swift "swift/bin/register-swift-endpoint.sh" http://127.0.0.1:8080
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml up logs -f --tail=all"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml logs -f --tail=all
echo "Stopping docker-compose..."
echo "docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down"
docker-compose --file ${DOCKER_COMPOSE_DIRNAME}/docker-compose.yml down