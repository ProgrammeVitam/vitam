#!/bin/bash
set -e

HEAD_VERSION=1.x
HEAD_FILE=elasticsearch-head-${HEAD_VERSION}.tar.gz
INTERNAL_REPO=${SERVICE_REPOSITORY_URL}/vitam-product-binaries

WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
    mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-head/usr/share/elasticsearch/plugins/head

echo "Downloading ${HEAD_FILE}..."
if curl --head --silent --fail "${INTERNAL_REPO}/${HEAD_FILE}" > /dev/null; then
    echo "File exists in internal cache repository."
    curl -k -L ${INTERNAL_REPO}/${HEAD_FILE} -o ${HEAD_FILE}
else
    echo "File does not exist in internal cache repository."
    curl -k -L https://github.com/mobz/elasticsearch-head/archive/${HEAD_VERSION}.tar.gz -o ${HEAD_FILE}
fi

tar xzf ${HEAD_FILE}
mv elasticsearch-head-${HEAD_VERSION}/* .
rm -f ${HEAD_VERSION}

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-head ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-elasticsearch-head/usr/share/elasticsearch/plugins/head

for item in $(ls); do
    rm -rf ${item}
done

popd
