#!/bin/bash
set -e

PROMETHEUS_VERSION="2.42.0"
PROMETHEUS_NODE_EXPORTER_VERSION="1.5.0"
PROMETHEUS_CONSUL_EXPORTER_VERSION="0.9.0"
PROMETHEUS_ELASTICSEARCH_EXPORTER_VERSION="1.5.0"
PROMETHEUS_ALERTMANAGER_VERSION="0.25.0"

INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)
if [ ! -d ${WORKING_FOLDER}/target ]; then
    mkdir ${WORKING_FOLDER}/target
fi

## Directory where source files will be downloaded
if [ ! -d ${WORKING_FOLDER}/sources ]; then
    mkdir ${WORKING_FOLDER}/sources
fi

#########################
## Prometheus server
#########################
COMPONENT_NAME=prometheus
PACKAGE_NAME=${COMPONENT_NAME}-${PROMETHEUS_VERSION}.linux-amd64.tar.gz
PACKAGE_URL=https://github.com/prometheus/prometheus/releases/download/v${PROMETHEUS_VERSION}/${PACKAGE_NAME}

rm -rf ${WORKING_FOLDER}/vitam-${PACKAGE_NAME}/vitam/app/*
rm -rf ${WORKING_FOLDER}/vitam-${PACKAGE_NAME}/vitam/bin/*
rm -rf ${WORKING_FOLDER}/vitam-${PACKAGE_NAME}/vitam/data/*

mkdir -p ${WORKING_FOLDER}/vitam-${COMPONENT_NAME}/vitam/app/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-${COMPONENT_NAME}/vitam/bin/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-${COMPONENT_NAME}/vitam/conf/${COMPONENT_NAME}

pushd ${WORKING_FOLDER}/sources/
echo "Current directory: $(pwd)"

echo "Downloading ${PACKAGE_NAME}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${PACKAGE_NAME}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
else
    echo "File does not exist in internal cache repository."
    curl -k -L ${PACKAGE_URL}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
fi

echo "Untar ${PACKAGE_NAME}"
tar xvzf ${PACKAGE_NAME} --strip 1 -C ../vitam-prometheus/vitam/bin/${COMPONENT_NAME}/

popd

pushd ${WORKING_FOLDER}/vitam-prometheus/vitam/bin/${COMPONENT_NAME}/
echo "Install files ..."

mv -f LICENSE ../../app/${COMPONENT_NAME}/LICENSE
mv -f NOTICE ../../app/${COMPONENT_NAME}/NOTICE
mv -f -u consoles/ ../../app/${COMPONENT_NAME}/
mv -f -u console_libraries ../../app/${COMPONENT_NAME}/
mv -f ${COMPONENT_NAME}.yml ../../conf/${COMPONENT_NAME}

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-${COMPONENT_NAME} ${WORKING_FOLDER}/target

popd

#########################
## Prometheus node_exporter
#########################
COMPONENT_NAME=node_exporter
PACKAGE_NAME=${COMPONENT_NAME}-${PROMETHEUS_NODE_EXPORTER_VERSION}.linux-amd64.tar.gz
PACKAGE_URL=https://github.com/prometheus/node_exporter/releases/download/v${PROMETHEUS_NODE_EXPORTER_VERSION}/${PACKAGE_NAME}

rm -rf ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/app/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/bin/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/data/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/conf/*

mkdir -p ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/app/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/bin/${COMPONENT_NAME}

pushd ${WORKING_FOLDER}/sources/
echo "Current directory: $(pwd)"

echo "Downloading ${PACKAGE_NAME}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${PACKAGE_NAME}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
else
    echo "File does not exist in internal cache repository."
    curl -k -L ${PACKAGE_URL} -o ${PACKAGE_NAME}
fi

echo "Untar ${PACKAGE_NAME}"
tar xvzf ${PACKAGE_NAME} --strip 1 -C ../vitam-prometheus-node-exporter/vitam/bin/${COMPONENT_NAME}/

popd

pushd ${WORKING_FOLDER}/vitam-prometheus-node-exporter/vitam/bin/${COMPONENT_NAME}/
echo "Install files ..."

mv -f LICENSE ../../app/${COMPONENT_NAME}/LICENSE
mv -f NOTICE ../../app/${COMPONENT_NAME}/NOTICE

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-prometheus-node-exporter ${WORKING_FOLDER}/target

popd

#########################
## Prometheus consul_exporter
#########################
COMPONENT_NAME=consul_exporter
PACKAGE_NAME=${COMPONENT_NAME}-${PROMETHEUS_CONSUL_EXPORTER_VERSION}.linux-amd64.tar.gz
PACKAGE_URL=https://github.com/prometheus/consul_exporter/releases/download/v${PROMETHEUS_CONSUL_EXPORTER_VERSION}/${PACKAGE_NAME}

rm -rf ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/app/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/bin/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/data/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/conf/*

mkdir -p ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/app/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/bin/${COMPONENT_NAME}

pushd ${WORKING_FOLDER}/sources/
echo "Current directory: $(pwd)"

echo "Downloading ${PACKAGE_NAME}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${PACKAGE_NAME}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
else
    echo "File does not exist in internal cache repository."
    curl -k -L ${PACKAGE_URL} -o ${PACKAGE_NAME}
fi

echo "Untar ${PACKAGE_NAME}"
tar xvzf ${PACKAGE_NAME} --strip 1 -C ../vitam-prometheus-consul-exporter/vitam/bin/${COMPONENT_NAME}/

popd

pushd ${WORKING_FOLDER}/vitam-prometheus-consul-exporter/vitam/bin/${COMPONENT_NAME}/
echo "Install files ..."

mv -f LICENSE ../../app/${COMPONENT_NAME}/LICENSE
mv -f NOTICE ../../app/${COMPONENT_NAME}/NOTICE

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-prometheus-consul-exporter ${WORKING_FOLDER}/target

popd

####################################
## Prometheus Elasticsearch Exporter
####################################
COMPONENT_NAME=elasticsearch_exporter
PACKAGE_NAME=${COMPONENT_NAME}-${PROMETHEUS_ELASTICSEARCH_EXPORTER_VERSION}.linux-amd64.tar.gz
PACKAGE_URL=https://github.com/prometheus-community/elasticsearch_exporter/releases/download/v${PROMETHEUS_ELASTICSEARCH_EXPORTER_VERSION}/${PACKAGE_NAME}

rm -rf ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/app/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/bin/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/data/*
rm -rf ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/conf/*

mkdir -p ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/app/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/bin/${COMPONENT_NAME}

pushd ${WORKING_FOLDER}/sources/
echo "Current directory: $(pwd)"

echo "Downloading ${PACKAGE_NAME}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${PACKAGE_NAME}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
else
    echo "File does not exist in internal cache repository."
    curl -k -L ${PACKAGE_URL} -o ${PACKAGE_NAME}
fi

echo "Untar ${PACKAGE_NAME}"
tar xvzf ${PACKAGE_NAME} --strip 1 -C ../vitam-prometheus-elasticsearch-exporter/vitam/bin/${COMPONENT_NAME}/

popd

pushd ${WORKING_FOLDER}/vitam-prometheus-elasticsearch-exporter/vitam/bin/${COMPONENT_NAME}/
echo "Install files ..."

mv -f LICENSE ../../app/${COMPONENT_NAME}/LICENSE

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-prometheus-elasticsearch-exporter ${WORKING_FOLDER}/target

popd

#########################
## Prometheus alertmanager
#########################
COMPONENT_NAME=alertmanager
PACKAGE_NAME=${COMPONENT_NAME}-${PROMETHEUS_ALERTMANAGER_VERSION}.linux-amd64.tar.gz
PACKAGE_URL=https://github.com/prometheus/alertmanager/releases/download/v${PROMETHEUS_ALERTMANAGER_VERSION}/${PACKAGE_NAME}

rm -rf ${WORKING_FOLDER}/vitam-prometheus-${COMPONENT_NAME}/vitam

mkdir -p ${WORKING_FOLDER}/vitam-prometheus-${COMPONENT_NAME}/vitam/app/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-prometheus-${COMPONENT_NAME}/vitam/bin/${COMPONENT_NAME}
mkdir -p ${WORKING_FOLDER}/vitam-prometheus-${COMPONENT_NAME}/vitam/conf/${COMPONENT_NAME}

pushd ${WORKING_FOLDER}/sources/
echo "Current directory: $(pwd)"

echo "Downloading ${PACKAGE_NAME}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${PACKAGE_NAME}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${PACKAGE_NAME} -o ${PACKAGE_NAME}
else
    echo "File does not exist in internal cache repository."
    curl -k -L ${PACKAGE_URL} -o ${PACKAGE_NAME}
fi

echo "Untar ${PACKAGE_NAME}"
tar xvzf ${PACKAGE_NAME} --strip 1 -C ../vitam-prometheus-${COMPONENT_NAME}/vitam/bin/${COMPONENT_NAME}/

popd

pushd ${WORKING_FOLDER}/vitam-prometheus-${COMPONENT_NAME}/vitam/bin/${COMPONENT_NAME}/
echo "Install files ..."

mv -f LICENSE ../../app/${COMPONENT_NAME}/LICENSE
mv -f NOTICE ../../app/${COMPONENT_NAME}/NOTICE
mv -f ${COMPONENT_NAME}.yml ../../conf/${COMPONENT_NAME}

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-prometheus-${COMPONENT_NAME} ${WORKING_FOLDER}/target

popd
