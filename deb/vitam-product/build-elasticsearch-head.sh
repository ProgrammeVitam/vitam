#!/bin/bash
HEAD_VERSION="1.x"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-elasticsearch-head/usr/share/elasticsearch/plugins/head

curl -k -L https://github.com/mobz/elasticsearch-head/archive/${HEAD_VERSION}.tar.gz -o ${HEAD_VERSION}.tar.gz
tar xzf ${HEAD_VERSION}.tar.gz
mv elasticsearch-head-${HEAD_VERSION}/* .
rm -f ${HEAD_VERSION}.tar.gz

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-elasticsearch-head ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-elasticsearch-head/usr/share/elasticsearch/plugins/head

for item in $(ls); do
    rm -rf ${item}
done

popd
