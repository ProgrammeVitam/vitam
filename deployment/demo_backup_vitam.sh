#!usr/bin/env bash
#*******************************************************************************
# Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
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

# Demo script for Vitam Backup process in an "all in one server" Vitam environment.
# Not designed multi host environment.

[ $UID -ne 0 ] && echo "must be run as root" && exit 1

unset REPLY
read -ers -p "MongoDB admin password: "
mongodbPwd=$REPLY
unset REPLY

set -ex
timestamp=$(date +"%Y-%m-%d")

echo "stopping Vitam..."

systemctl stop vitam-ingest-external
! systemctl is-active vitam-ingest-external

systemctl stop vitam-ingest-internal
! systemctl is-active vitam-ingest-internal

systemctl stop vitam-access-external
! systemctl is-active vitam-access-external

systemctl stop vitam-access-internal
! systemctl is-active vitam-access-internal

## TODO test des Running Workflow

systemctl stop vitam-worker
! systemctl is-active vitam-worker

systemctl stop vitam-processing
! systemctl is-active vitam-processing

systemctl stop vitam-workspace
! systemctl is-active vitam-workspace

systemctl stop vitam-functional-administration
! systemctl is-active vitam-functional-administration

systemctl stop vitam-logbook
! systemctl is-active vitam-logbook

systemctl stop vitam-metadata
! systemctl is-active vitam-metadata

systemctl stop vitam-storage
! systemctl is-active vitam-storage

systemctl stop vitam-offer
! systemctl is-active vitam-offer

systemctl stop vitam-elasticsearch-data
! systemctl is-active vitam-elasticsearch-data

systemctl stop logstash
! systemctl is-active logstash

systemctl stop kibana
! systemctl is-active kibana

systemctl stop vitam-elasticsearch-log
! systemctl is-active stop vitam-elasticsearch-log

mongo admin -u vitamdb-admin -p ${mongodbPwd} --quiet --eval "sh.stopBalancer()"
[ $(mongo admin -u vitamdb-admin -p ${mongodbPwd} --quiet --eval "sh.isBalancerRunning()") = "false" ] || exit 1

mongo  --port 27019 --quiet --eval "db.isMaster()"

systemctl stop vitam-mongod
! systemctl is-active vitam-mongod

mongo admin --port 27018 -u vitamdb-admin -p ${mongodbPwd} --quiet --eval "db.isMaster()"

systemctl stop vitam-mongoc
! systemctl is-active vitam-mongoc

echo "saving /vitam/data"
tar --directory=/vitam -Pz -cf ${timestamp}_demobackup_vitam_data.tar.gz data

echo "saving /vitam/conf"
tar --directory=/vitam -Pz -cf ${timestamp}_demobackup_vitam_conf.tar.gz conf

echo "restarting Vitam..."
systemctl start vitam-mongod
systemctl is-active vitam-mongod

systemctl start vitam-mongoc
systemctl is-active vitam-mongoc

systemctl stop vitam-mongos
systemctl start vitam-mongos

sleep 7
mongo admin -u vitamdb-admin -p ${mongodbPwd} --quiet --eval "sh.setBalancerState(true)"
[ $(mongo admin -u vitamdb-admin -p ${mongodbPwd} --quiet --eval "sh.getBalancerState()") = 'true' ] || exit 1

systemctl start vitam-elasticsearch-log
sleep 3
systemctl is-active vitam-elasticsearch-log

systemctl start kibana
sleep 3
systemctl is-active kibana

systemctl start logstash
sleep 3
systemctl is-active logstash

systemctl start vitam-elasticsearch-data
sleep 3
systemctl is-active vitam-elasticsearch-data

systemctl start vitam-offer
sleep 3
systemctl is-active vitam-offer

systemctl start vitam-storage
sleep 3
systemctl is-active vitam-storage

systemctl start vitam-metadata
sleep 3
systemctl is-active vitam-metadata

systemctl start vitam-logbook
sleep 3
systemctl is-active vitam-logbook

systemctl start vitam-functional-administration
sleep 3
systemctl is-active vitam-functional-administration

systemctl start vitam-workspace
sleep 3
systemctl is-active vitam-workspace

systemctl start vitam-processing
sleep 3
systemctl is-active vitam-processing

systemctl start vitam-worker
sleep 3
systemctl is-active vitam-worker

systemctl start vitam-access-internal
sleep 3
systemctl is-active vitam-access-internal

systemctl start vitam-access-external
sleep 5
systemctl is-active vitam-access-external

systemctl start vitam-ingest-internal
sleep 3
systemctl is-active vitam-ingest-internal

systemctl start vitam-ingest-external
sleep 3
systemctl is-active vitam-ingest-external
sleep 3
echo "Vitam backup demo ended"
