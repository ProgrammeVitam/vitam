=====
Configuration de l'environnement de développement
=====

Voici comment préparez votre environnement de deéveloppement afin de pouvoir coder, démarrer les micros services, débugger...


1. Prérequis
L'installation du poste de travail a été faite (installation de GIT, Maven, Docker, IntelliJ...)
Assurez-vous que le plugin lfs pour GIT a été installé. Dans le cas contraire voici la ligne de commande à lancer :

Ubuntu :
``$ git lfs install``

CentOS :
``$ sudo yum install git-lfs``


2. Récupèrer le code source
Placez-vous dans le dossier ou vous voulez mettre le code source Vitam sur lequel vous allez travailler :
``$ git clone <gitlab vitam/vitam>``
``$ git clone <gitlab vitam/vitam-conf-dev>``
``$ git clone <gitlab vitam/vitam-itests>``
*Remarque : toutes les lignes de commande ``cd`` des points suivants supposent que vous êtes dans votre dossier de travail*

3. Démarrer Docker :
Déplacer vous dans le dossier suivant et exécuter la commande ``run_cots.sh``
``$ cd vitam/dev-deployment``
``$ ./run_cots.sh``

4. Dans Docker :
``[xxxxx@xxxxxxxxxxxx code]$ vitam-build-repo``
``[xxxxx@xxxxxxxxxxxx code]$ vitam-deploy-cots``

5. Ajoutez les lignes suivantes dans le fichier ``/etc/hosts``
``127.0.0.1       metadata.service.consul``
``127.0.0.1       logbook.service.consul``
``127.0.0.1       storage.service.consul``
``127.0.0.1       workspace.service.consul``
``127.0.0.1       functional-administration.service.consul``
``127.0.0.1       processing.service.consul``
``127.0.0.1       ingest-external.service.consul``
``127.0.0.1       ingest-internal.service.consul``
``127.0.0.1       access-internal.service.consul``
``127.0.0.1       access-external.service.consul``
``127.0.0.1       workspace.service.consul``
``127.0.0.1       external.service.consul``
``127.0.0.1       ihm-recette.service.consul``
``127.0.0.1       offer.service.consul``
``127.0.0.1    ihm-demo.service.consul``
``127.0.0.1       metadata.service.consul``
``127.0.0.1       logbook.service.consul``
``127.0.0.1       storage.service.consul``
``127.0.0.1       workspace.service.consul``
``127.0.0.1       functional-administration.service.consul``
``127.0.0.1       processing.service.consul``
``127.0.0.1       ingest-external.service.consul``
``127.0.0.1       ingest-internal.service.consul``
``127.0.0.1       access-internal.service.consul``
``127.0.0.1       access-external.service.consul``
``127.0.0.1       workspace.service.consul``
``127.0.0.1       external.service.consul``
``127.0.0.1       ihm-recette.service.consul``
``127.0.0.1       offer.service.consul``
``127.0.0.1    offer-fs-1.service.consul``
``127.0.0.1    ihm-demo.service.consul``
``127.0.0.1    security-internal.service.consul``
``192.30.253.113    github.com``




7. Lancez IntelliJ et installer le plugin "Multirun".

8. Inporter le project Vitam dans IntelliJ
En utilisant le menu Import Project puis selecttionez ``vitam/sources/pom.xml``

9. Initialisez la configuration
Télécharger le fichier ``runConfiguration.zip`` et extraire le fichier dans le dossier ~/vitam/sources/.idea (automatiquement créé par IntelliJ)
Redémarrez IntelliJ.

*(XX. Ajouter le XML snippet: ``vitam/logback/vitam-logback.xml`` par exemple dans votre dossier ``HOME``)*

10. Dans IntelliJ, configurez les chemins suivants pour chaque module du projet :
- Dans le menu déroulant des configurations de debug/run d'IntelliJ > Edit Configurations...
- Dans la boite de dialogue Run/Debug Configuration dépliez l'item "Application" et selectionner je premier projet.
- Modifiez les champs :
	- VM options (vérifie le chemin de l'option ``-Dlogback.configurationFile=`` qui doit pointer vers le fichier vitam-logback.xml précédent)
	- Program arguments
	- Working directory

11. Dossier de travail:
Exexuter le commade suivante :
``$ sudo chmod -R ugo+w /vitam``
Dans ``/vitam/data/storage`` creez le fichier ``offer-fs-1.service.consul`` contenant la ligne suivante ``fr.gouv.vitam.storage.offers.workspace.driver.DriverImpl`` 

12. initialisation de la base de données :
$ cd vitam/vitam-conf-dev/scripts
$ ./init_data_vitam.sh

Puis dans IntelliJ : lancer "launch cucumber_init"

13. Démarrer les services dans IntelliJ
Dans le menu déroulant des configurations de debug/run d'IntelliJ selectionnez vitamIhm
Lancez les services en cliquant sur bouton debug

14. Démarrage de l'IHM
$ cd vitam/sources/ihm-demo/ihm-demo-front/
$ npm run start

$ cd vitam/sources/ihm-recette/ihm-recette-web-front/
$ npm run start

15. Utiliser Vitam
- Transfert SIP et plan de classement http://localhost:4201

- Recette : Tests des requêtes DSL http://localhost:4202

*Remarque :*
- login : aadmin
- password : aadmin1234

