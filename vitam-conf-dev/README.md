vitam-conf-dev
##############

For dev usage

/etc/hosts :
127.0.0.1       metadata.service.consul
127.0.0.1       logbook.service.consul
127.0.0.1       storage.service.consul
127.0.0.1       workspace.service.consul
127.0.0.1       functional-administration.service.consul
127.0.0.1       processing.service.consul
127.0.0.1       ingest-external.service.consul
127.0.0.1       ingest-internal.service.consul
127.0.0.1       access-internal.service.consul
127.0.0.1       access-external.service.consul
127.0.0.1       external.service.consul
127.0.0.1       ihm-recette.service.consul
127.0.0.1       offer.service.consul
127.0.0.1       ihm-demo.service.consul
127.0.0.1		worker.service.consul
127.0.0.1		security-internal.service.consul


Deploy debug
############

* Init configuration 
* launch dev-deployment/run_cots.sh
** first time :
*** build rpm/vitam-external
*** build rpm/vitam-products
*** createrepo .
** vitam-deploy-cots
* launch local servers
* optional delete /vitam/data/offer manually or execute .init_data_vitam.sh this script delete all directories in /vitam/data/offer/*_backup
* cd scripts/
* ./init_data_vitam.sh
* launch vitam-itests/_init.feature (for populating database and elastic)


