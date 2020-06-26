#!/bin/bash
set -e
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
#
# contact.vitam@culture.gouv.fr
#
# This software is a computer program whose purpose is to implement a digital archiving back-office system managing
# high volumetry securely and efficiently.
#
# This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
# software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
# circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
#
# As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
# users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
# successive licensors have only limited liability.
#
# In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
# developing or reproducing the software by the user in light of its specific status of free software, that may mean
# that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
# experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
# software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
# to be ensured and, more generally, to use and operate it in the same conditions as regards security.
#
# The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
# accept its terms.
#*******************************************************************************
WORKING_FOLDER=$(dirname $0)

if [ ! -d ${WORKING_FOLDER}/target ]; then
	mkdir -p ${WORKING_FOLDER}/target
fi

for item in $(ls -d ${WORKING_FOLDER}/*/ | grep -v "target" | grep -v "sources" |grep -v _v2 | awk -F "/" '{print $(NF-1)}'); do
	# Need to give the target folder relatively to the base folder...
	echo ${item}
	${WORKING_FOLDER}/build-generic.sh ${item} target
done

echo "vitam-consul"
${WORKING_FOLDER}/build-consul.sh
echo "vitam-gatling"
${WORKING_FOLDER}/build-gatling.sh
echo "vitam-elasticsearch-head"
${WORKING_FOLDER}/build-elasticsearch-head.sh
echo "vitam-elasticsearch-cerebro"
${WORKING_FOLDER}/build-elasticsearch-cerebro.sh
echo "vitam-siegfried"
${WORKING_FOLDER}/build-siegfried.sh
echo "Elasticsearch analysis-icu"
${WORKING_FOLDER}/build-elasticsearch-icu.sh
echo "Prometheus stack"
${WORKING_FOLDER}/build-prometheus-stack.sh
