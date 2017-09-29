#!/bin/bash
set -e

WORKING_FOLDER=$(dirname $0)
GATLING_VERSION="2.3.0"

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-gatling/vitam/bin/gatling/
echo "Repertoire courant: $(pwd)"
echo "Récupérer gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip"
if [ ! -f gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip ]; then
	curl -k https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/${GATLING_VERSION}/gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip -o gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip
fi
echo "Décompacter gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip"
unzip -o gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip
mv gatling-charts-highcharts-bundle-${GATLING_VERSION}/* .
if [ $? != 0 ]; then echo "erreur unzip: $?"; fi
rm -f gatling-charts-highcharts-bundle-${GATLING_VERSION}-bundle.zip
rmdir gatling-charts-highcharts-bundle-${GATLING_VERSION}
#if [ ! -f consul ]; then echo "Erreur: binaire consul non presente !"; fi

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-gatling ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-gatling/vitam/bin/gatling

for item in $(ls |grep -v run.sh); do
    rm -rf ${item}
done

popd
