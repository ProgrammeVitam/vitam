# Install VITAM

1. Clone repositories:
	1. Clone with git vitam at `https://gitlab.dev.programmevitam.fr/vitam/vitam.git`.
	2. git-lfs should be installed and enabled on vitam-itests (https://git-lfs.github.com/)
	3. Clone with git vitam-itest at `https://gitlab.dev.programmevitam.fr/vitam/vitam-itests.git`.

3. Initialize project settings (npm/maven/certificates):
	1. Install Java JDK and Maven `sudo apt install openjdk-11-jre-headless`, `sudo apt install openjdk-11-jdk-headless`, `sudo apt install maven`
	2. JAVA_HOME and M2_HOME must be set in bashrc or bashprofile (These values can be get with `mvn -version` command)
	3. Run the `vitam-conf-dev/scripts/maven-setup-chapelle-edition.sh`

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
127.0.0.1       worker.service.consul
127.0.0.1       security-internal.service.consul
127.0.0.1       batch-report.service.consul
127.0.0.1       offer-fs-1.service.consul' > /etc/hosts`

Should be usefull: Add 'export VITAMDEV_GIT_REPO=/path/to/git/vitam/repo' in .bashrc before launch run_cots

5. If not done yet, install docker:
	1. sudo apt install docker.io
	2. Add docker permissions to current user: 'sudo usermod -a -G docker $USER', then relog (reboot if needed)

6. Run the `dev-deployment/run_cots.sh` file in order to create container with necessary components for vitam (like databases).
	1. If DNS error occures while launching run_cots.sh, it could be usefull to change default docker dns creating a '/etc/docker/daemon.json' containing { "dns": ["10.100.211.222"] } where '10.100.211.222' is the programmevitam culture dns.
	2. After update DNS conf, restart docker with `sudo systemctl restart docker`
	3. If `/etc/resolv.conf` contains `options ends0`, it must be disabled.
	3. In cots: Run command `vitam-build-repo` to create/download all components.
	4. In cots: Run command `vitam-deploy-cots` to deploy necessary component for vitam.

7. While vitam-build-repo / vitam-deploy-cots is building, configure VITAM module launch from IDE (Example conf are explain for IntelliJ IDE)
	1. Create '/vitam'
		a. Create `/vitam/data/storage` folder and init Driver config with `echo 'offer-fs-1.service.consul' > fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl`
		b. Change the user permission for `/vitam` with `chown <userName>:<userGroup> -R /vitam`
	2. Configure vitam serveur application (Example config for access-external)
		a. Main class: fr.gouv.vitam.access.external.rest.AccessExternalMain
		b. VM Options: -Xms256m -Xmx256m -Dlogback.configurationFile=path/to/vitam/vitam-conf-dev/conf/access-external/logback.xml
		c. Program arguments: path/to/vitam/vitam-conf-dev/conf/access-external/access-external.conf
		d. Working directory: path/to/vitam/vitam-conf-dev/conf/access-external
		e. module classpath: access-external-rest
		f. Log (Add new log entry): /vitam/log/access-external/access-external.*.log
	3. Configure multi-run in order to launch all VITAM modules in the correct dependencies order with one click
		a. Download/Install Multirun plugin for IntelliJ
		b. Add new Multirun configuration
		c. Add configuration to run in the following order: Workspace, MetaData, Logbook, InternalSecurity, Storage, DefaultOffer, ProcessManagement, BatchReport, Worker, AdminManagement, IngestInternal, IngestExternal, AccessInternal, AccessExternal, IhmDemo

8. One vitam-build-repo AND vitam-deploy-cots are done without error, launch your configured multirun task in order to launch all vitam modules. 
	1. If Some servers are not correctly launched, check in your docker cots that all vitam services are successfully launched with `systemctl -a | grep vitam`

9. Run the `init_data_vitam.sh` file to init vitam with SecurityProfiles/Ontology/Contexts/Certificate.

10. Run the cucumber `_init.feature` in order to initialize with necessary data like contracts.

11. Launch mongo-express docker container:
	1. Login to programmevitam docker repo with `docker login https://docker.programmevitam.fr` and your vitam LDAP credentials
	2. Run mongo express with `docker run -d -p 10081:8081 --name="mongo-express" -e ME_CONFIG_MONGODB_ADMINUSERNAME="vitamdb-admin" -e ME_CONFIG_MONGODB_ADMINPASSWORD="azerty" -e ME_CONFIG_MONGODB_SERVER="172.17.0.2" --link vitam-rpm-cots-dev:mongo docker.programmevitam.fr/mongo-express`

# Configuration Mac specific
Redirection of ElastiSearch traffic to docker (loopback) with:
* `sudo ifconfig lo0 alias 172.17.0.2`

Add in elasticsearch conf to connect everywhere with:
* `vim /vitam/conf/elasticsearch-data/elasticsearch.yml`
* `> transport.host: 0.0.0.0`
