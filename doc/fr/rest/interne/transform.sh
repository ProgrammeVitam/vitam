#!/bin/sh
WORKDIR=`dirname $0`
OUTPUT_DIR="${WORKDIR}/target/html"
if [ -d ${OUTPUT_DIR} ]
then
	echo "Purge du répertoire cible"
	rm -rf ${OUTPUT_DIR}
fi

mkdir -p ${OUTPUT_DIR}

for i in Internal_Logbook Internal_Metadata Internal_Processing Internal_Storage Internal_Worker Internal_Workspace Internal_Ingest Module_Acces Module_Ingest Module_Logbook
do
	echo ${i}
	raml2html -i ${i}.raml -t template/template.nunjucks -o ${OUTPUT_DIR}/${i}.html
done