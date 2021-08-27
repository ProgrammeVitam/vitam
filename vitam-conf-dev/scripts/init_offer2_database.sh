#!/bin/bash

# execute databases scripts
docker exec --user vitamdb vitam-rpm-cots-dev mongo 172.17.0.2:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer2/enable-sharding.js
docker exec --user vitamdb vitam-rpm-cots-dev mongo 172.17.0.2:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer2/users-offer.js
docker exec --user vitamdb vitam-rpm-cots-dev mongo 172.17.0.2:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer2/init-offer-database.js
