#!/bin/bash

SOAPUI_VERSION="5.2.1"
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir ${WORKING_FOLDER}/target
fi

pushd ${WORKING_FOLDER}/vitam-soapui/vitam/app/soapui

echo "Récupération SoapUI-${SOAPUI_VERSION}-linux-bin.tar.gz"
curl -k -L http://cdn01.downloads.smartbear.com/soapui/${SOAPUI_VERSION}/SoapUI-${SOAPUI_VERSION}-linux-bin.tar.gz -o SoapUI-${SOAPUI_VERSION}-linux-bin.tar.gz
echo "Extraction..."
tar xzf SoapUI-${SOAPUI_VERSION}-linux-bin.tar.gz
mv SoapUI-${SOAPUI_VERSION}/* .
echo "Purge des répertoires temporaires"
rm -rf SoapUI-${SOAPUI_VERSION}
rm -f SoapUI-${SOAPUI_VERSION}-linux-bin.tar.gz
# curl -k -L http://repository.codehaus.org/org/codehaus/groovy/modules/http-builder/http-builder/0.6/http-builder-0.6.jar -o ./bin/ext/http-builder-0.6.jar
echo "Récupération http-builder-0.6.jar"
curl -k -L http://central.maven.org/maven2/org/codehaus/groovy/modules/http-builder/http-builder/0.6/http-builder-0.6.jar -o ./bin/ext/http-builder-0.6.jar
echo "Récupération xml-resolver-1.1.jar"
curl -k -L http://central.maven.org/maven2/xml-resolver/xml-resolver/1.1/xml-resolver-1.1.jar -o ./bin/ext/xml-resolver-1.1.jar

popd
pushd ${WORKING_FOLDER}

dpkg-deb --build vitam-soapui ${WORKING_FOLDER}/target

popd
pushd ${WORKING_FOLDER}/vitam-soapui/vitam/app/soapui

for item in $(ls); do
    rm -rf ${item}
done

popd
