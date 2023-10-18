#!/bin/bash

# execute databases scripts
docker exec --user vitamdb vitam-rpm-cots-dev mongo localhost:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer3/enable-sharding.js
docker exec --user vitamdb vitam-rpm-cots-dev mongo localhost:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer3/users-offer.js
docker exec --user vitamdb vitam-rpm-cots-dev mongo localhost:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer3/init-offer-database.js
docker exec --user vitamdb vitam-rpm-cots-dev mongo localhost:27017/admin -u vitamdb-admin -p azerty /code/vitam-conf-dev/scripts/offer3/init-tape-offer-database.js
