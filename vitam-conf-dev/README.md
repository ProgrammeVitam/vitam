# Install VITAM
1. Clone with git vitam at `https://gitlab.dev.programmevitam.fr/vitam/vitam.git`.
2. Clone with git vitam-itest at `https://gitlab.dev.programmevitam.fr/vitam/vitam-itests.git`.
3. Run the `vitam-conf-dev/scripts/maven-setup-chapelle-edition.sh` file to configure maven settings.
4. Add in your `/etc/hosts` some values with `sudo echo '127.0.0.1       metadata.service.consul
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
127.0.0.1		security-internal.service.consul' > /etc/hosts`
5. Run the `dev-deployment/run_cots.sh` file in order to create container with necessary components for vitam (like databases).
    1. Run command `vitam-build-repo` to create/download all components.
    2. Run command `vitam-deploy-cots` to deploy necessary component for vitam.
6. Launch all vitam servers with conf in `vitam-conf-dev/conf/`.
    1. Launch for example IhmDemoApplication with parameter `vitam-conf-dev/conf/ihm-demo/ihm-demo.conf`.
7. Run the `init_data_vitam.sh` file to init vitam with ontology/contexts/certificate.
8. Create a file in `/vitam/data/storage` with `echo 'offer-fs-1.service.consul' > fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl`.
9. Run the cucumber `_init.feature` in order to initialize with necessary data like contracts.
10. Launch mongo-express docker container with `docker run -d -p 10081:8081 --name="mongo-express" -e ME_CONFIG_MONGODB_ADMINUSERNAME="vitamdb-admin" -e ME_CONFIG_MONGODB_ADMINPASSWORD="azerty" -e ME_CONFIG_MONGODB_SERVER="172.17.0.2" --link vitam-rpm-cots-dev:mongo docker.programmevitam.fr/mongo-express`

# Configuration Mac specific
Redirection of ElastiSearch traffic to docker (loopback) with:
* `sudo ifconfig lo0 alias 172.17.0.2`

Add in elasticsearch conf to connect everywhere with:
* `vim /vitam/conf/elasticsearch-data/elasticsearch.yml`
* `> transport.host: 0.0.0.0`
