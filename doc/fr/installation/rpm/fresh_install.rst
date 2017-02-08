Procédure de première installation
##################################


.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments-rpm``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam-rpm``

Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le sous-répertoire |repertoire_deploiement| . Ils consistent en 2 parties :

 * le playbook ansible, présent dans le sous-répertoire |repertoire_playbook ansible|, qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le sous-répertoire |repertoire_inventory|


Configuration du déploiement
============================

.. _inventaire:

Informations "plate-forme"
--------------------------

Pour configurer le déploiement, il est nécessaire de créer dans le répertoire |repertoire_inventory| un nouveau fichier d'inventaire à nommer ``hosts.<environnement>`` ( où <environnement> sera utilisé par la suite ) comportant les informations suivantes :

.. literalinclude:: ../../../../deployment/environments-rpm/hosts.example
   :language: ini
   :linenos:

Pour chaque type de "host" (lignes 2 à 176), indiquer le(s) serveur(s) défini(s) pour chaque fonction. Une colocalisation de composants est possible.

.. note:: pour les "hosts-worker", il est possible d'ajouter, à la suite de chaque "host", 2 paramètres optionnels : capacity et workerFamily. Se référer au :term:`DEX` pour plus de précisions.

Ensuite, dans la section ``hosts:vars`` (lignes 179 à 240), renseigner les valeurs comme décrit :

.. csv-table:: Définition des variables
   :header: "Clé", "Description","Valeur d'exemple"
   :widths: 10, 10,10

   "ansible_ssh_user","Utilisateurs ansible sur les machines sur lesquelles VITAM sera déployé",""
   "ansible_become","Propriété interne à ansible pour passer root",""
   "local_user","En cas de déploiement en local",""
   "environnement","Suffixe",""
   "vitam_reverse_domain","Cas de la gestion d'un reverse proxy",""
   "consul_domain","nom de domaine consul",""
   "vitam_ihm_demo_external_dns","Déprécié ; ne pas utiliser",""
   "rpm_version","Version à installer",""
   "days_to_delete","Période de grâce des données sous Elastricsearch avant destruction (valeur en jours)",""
   "days_to_close","Période de grâce des données sous Elastricsearch avant fermeture des index (valeur en jours)",""
   "days_to_delete_topbeat","Période de grâce des données sous Elastricsearch  - index Topbeat - avant destruction (valeur en jours)",""
   "days_to_delete_local","Période de grâce des log VITAM - logback (valeur en jours)",""
   "dns_server","Serveur DNS que Consul peut appeler s'il n'arrive pas à faire de résolution","172.16.1.21"
   "log_level","Niveau de log de logback","WARN"
   "web_dir_soapui_tests","URL pour récupérer data.json et les tests pour SoapUI","http://vitam-prod-ldap-1.internet.agri:8083/webdav"
   "reverse_proxy_port","port du reverse proxy pour configuration du vhost","8080"
   "days_to_close_metrics","Période de grâce avant fermeture des index des métriques JVM","7"
   "days_to_delete_metrics","Période de grâce avant destruction des index fermés des métriques JVM","30"
   "installation_clamav","Choix d'installation de ClamAV (true/false)","true"
   "http_proxy_environnement","Cas particulier de la récupération des jeux de tests ; URL de squid",""
   "mongoclientPort","Port par lequel mongoclient est acessible","27016"
   "mongoclientDbName","Nom de la Base de donnée stockant la configuration mongoclient","mongoclient"
   "vitam_tenant_ids","Liste des tenants de plateforme","[0,1,2] ; [0] par défaut"


A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées sous |repertoire_inventory| ``/group_vars/all/all``, comme suit :

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/all
   :language: yaml
   :linenos:


Le fichier ``vault.yml`` est également présent sous |repertoire_inventory| ``/group_vars/all/all`` et contient les secrets ; ce fichier est encrypté par ``ansible-vault`` et doit être paramétré avant le lancement de l'orchestration de déploiement.

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/vault.txt
   :language: ini
   :linenos:

.. note:: Si le mot de passe du fichier ``vault.yml`` est changé, ne pas oublier de le répercuter dans le fichier ``vault_pass.txt`` (et le sécuriser à l'issue de l'installation).


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

.. warning:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.


Paramétrage de mongoclient (administration mongoclient)
======================================================

Le package rpm vitam-mongoclient nécessite une bases de données mongoDB (mongoclient) pour stocker sa configuration.
Cette base de données est créée dans :term:`VITAM` durant la première installation.
La configuration est également générée en fonction des paramètres de l'inventaire.

Mongoclient permet de se connecter aux différentes bases de données mongoDB utilisées par VITAM.


Première utilisation de mongoclient
===================================

Par défault, mongoclient est accessible par l'url: http://hostname:27016/mongoclient suivant les hôtes configurés dans le groupes hosts-mongoclients de l'inventaire Vitam.

.. warning:: les versions de mongoclient inférieures à la version 1.5.0 présentent un message d'erreur "route not found" à l'apparition de l'interface. les fonctionnalités de l'application sont indisponibles dans cet état. Ce problème est aisemment contournable en cliquant sur le bouton "Go to Dashboard" pour revenir à un état normal de l'application.

Lors de la première utilisation de mongoclient, il convient de configurer les connexions aux bases de données à superviser. (Cette procédure devrait disparaître à l'issue de la phase Beta)

Procédure pour configurer la connexion aux bases vitam::
 #) Cliquer sur le bouton "Connect" situé en haut de la page (l'emplacement dépend de la taille de la fenêtre)
 #) Dans la fenêtre "Connections", cliquer sur le bouton "Create New". => la fenêtre Add connection apparait contenant 4 sections : Connection, Authentication, URL, SHH
 #) Dans la section "Connection", saisir un nom à donner à la connexion dans "name", le nom ou l'ip du server mongos à cibler dans "hostname", changer éventuellement le "port", définir la base de donnée sur laquelle le client doit se connecter
 #) Dans la section "Authentication", saisir les paramètres d'autentification du compte à utiliser pour se connecter à la base configurée en section "connection"
 #) Dans la section URL, en fonction du la configuration des services, choisir cette méthode de connexion en lieu et place des autres méthodes.
 #) Dans la section "SHH", si le service mongoDB n'est accessible qu'au travers d'une connexion SSH, renseigner les paramètres de cette connexion pour accéder au serveur.
 #) Sauvegarder les paramètres avec le boutton "save changes"
 #) La nouvelle connexion doit apparaître avec un résumé de ses paramètres dans la fenêtre "Connections"
 #) CLiquer sur la ligne de la connexion puis cliquer sur le boutton "Connect Now" pour utiliser se connecter.


Si les identifiants utilisés disposent de droit suffisants, Mongoclient vas afficher les métriques du service mongoDB.

Mongoclient ne permet de gérer qu'une seule base à la fois, il est toutefois possible de changer de base de donnée rapidement en ouvrant le menu "More" => "Switch Database" qui affichera la liste des bases de données accessibles (suivant les identifiants renseignés).


Paramétrage de l'antivirus (ingest-externe)
-------------------------------------------

L'antivirus utilisé par ingest-externe est modifiable ; pour cela :

* Créer un autre shell (dont l'extension doit être ``.sh.j2``) sous ``ansible-vitam-rpm/roles/vitam/templates/ingest-external`` ; prendre comme modèle le fichier ``scan-clamav.sh.j2``. Ce fichier est un template Jinja2, et peut donc contenir des variables qui seront interprétées lors de l'installation.
* Modifier le fichier ``ansible-vitam-rpm/roles/vitam/templates/ingest-external/ingest-external.conf.j2`` en pointant sur le nouveau fichier.


Ce script shell doit respecter le contrat suivant :

* Argument : chemin absolu du fichier à analyser
* Sémantique des codes de retour

    - 0 : Analyse OK - pas de virus
    - 1 : Analyse OK - virus trouvé et corrigé
    - 2 : Analyse OK - virus trouvé mais non corrigé
    - 3 : Analyse NOK

* Contenu à écrire dans stdout / stderr

    - stdout : Nom des virus trouvés, un par ligne ; Si échec (code 3) : raison de l’échec
    - stderr : Log « brut » de l’antivirus


Paramétrage des certificats (\*-externe)
-----------------------------------------

Se reporter à l'étape "PKI" du déploiement, décrite plus bas.


Déploiement
===========

Fichier de mot de passe
-----------------------

Si le fichier ``deployment/vault_pass.txt`` est renseigné avec le mot de passe du fichier ``environnements-rpm/group_vars/all/vault.yml``, le mot de passe ne sera pas demandé. Si le fichier est absent, le mot de passe du "vault" sera demandé.

PKI
---

1. paramétrer le fichier ``environnements-rpm/group_vars/all/vault.yml`` et le fichier d'inventaire de la plate-forme sous ``environnements-rpm`` (se baser sur le fichier hosts.example)

2. Lancer le script

.. code-block:: bash

   pki-generate-ca.sh

En cas d'absence de PKI, il permet de générer  une PKI, ainsi que des certificats pour les échanges https entre composants. Se reporter au chapitre PKI si le client préfère utiliser sa propre PKI.

3. Lancer le script

.. code-block:: bash

   generate_certs.sh <environnement>

Basé sur le contenu du fichier ``vault.yml``, ce script  génère des certificats  nécessaires au bon fonctionnement de VITAM.

3. Lancer le script

.. code-block:: bash

   generate_stores.sh <environnement>

Basé sur le contenu du fichier ``vault.yml``, ce script  génère des stores nécessaires au bon fonctionnement de VITAM.

4. Lancer le script

.. code-block:: bash

   copie_fichiers_vitam.sh <environnement>

pour recopier dans les bons répertoires d'ansiblerie les certificats et stores précédemment créés.

Mise en place des repositories VITAM (optionnel)
-------------------------------------------------
Si gestion par VITAM des repositories CentOS spécifiques à VITAM :

Editer le fichier ``environments-rpm/group_vars/all/example_repo.yml`` (sert de modèle)

.. literalinclude:: ../../../../deployment/environments-rpm/group_vars/all/example_repo.yml
   :language: yaml
   :linenos:

Ce fichier permet de définir une liste de repositories. Décommenter et adapter à votre cas.

Pour mettre en place ces repositories sur les machines cibles, lancer la commande :

``ansible-playbook ansible-vitam-rpm-extra/bootstrap.yml -i environments-rpm/<fichier d'inventaire>  --ask-vault-pass``

ou

``ansible-playbook ansible-vitam-rpm-extra/bootstrap.yml -i environments-rpm/<fichier d'inventaire> --vault-password-file vault_pass.txt``


Déploiement
-------------

Une fois l'étape de PKI effectuée avec succès, le déploiement est à réaliser avec la commande suivante :

.. code-block:: bash

   ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/<ficher d'inventaire> --vault-password-file vault_pass.txt

Extra
------

Deux playbook d'extra sont fournis pour usage "tel quel".

1. ihm-recette

Ce playbook permet d'installer également le composant :term:`VITAM` ihm-recette.

.. code-block:: bash

   ansible-playbook ansible-vitam-rpm-extra/ihm-recette.yml -i environments-rpm/<ficher d'inventaire> --vault-password-file vault_pass.txt


2. extra complet

Ce playbook permet d'installer :
  - topbeat
  - packetbeat
  - un serveur Apache pour naviguer sur le ``/vitam``  des différentes machines hébergeant :term:`VITAM`
  - mongo-express (en docker  ; une connexion internet est alors nécessaire)
  - le composant :term:`VITAM` library, hébergeant les documentations du projet
  - le composant :term:`VITAM` ihm-recette (nécessite un accès à un répertoire "partagé" pour récupérer les jeux de tests)
  - un reverse proxy, afin de simplifier les appels aux composants


.. code-block:: bash

   ansible-playbook ansible-vitam-rpm-extra/extra.yml -i environments-rpm/<ficher d'inventaire> --vault-password-file vault_pass.txt

Import automatique d'objets dans Kibana
=========================================

Il peut être utile de vouloir automatiquement importer dans l'outil de visualisation Kibana des dashboards préalablement crées. Cela ce fait simplement avec le système d'import automatique mis en place. Il suffit de suivre les différentes étapes :

1. Ouvrir l'outil Kibana dans son navigateur.
2. Créer ses dashboards puis sauvegarder.
3. Aller dans l'onglets **Settings** puis **Objects**.
4. Sélectionner les composants à exporter puis cliquer sur le bouton **Export**. (ou bien cliquer sur **Export Everything** pour tout exporter).
5. Copier le/les fichier(s) *.json* téléchargés à l'emplacement ``deployment\ansible-vitam-rpm\roles\log-server\files\kibana-objects``.
6. Les composants sont prêts à être importés automatique lors du prochain déploiement.

Pour éviter d'avoir à recréer les "index-pattern" définis dans l'onglet **Settings** de Kibana, ceux-ci aussi sont pris en charge par le système de déploiement automatique. En revanche ils ne sont pas exportables, il est donc nécessaire de créer à la main le fichier *.json* correspondant. Pour ce faire :

1. Faire une requête GET sur l'url suivante ``http://<ip-elasticsearch-log>/.kibana/index-pattern/_search``.
2. Récupérer le contenu au format JSON et extraire le contenu de la clé **hits.hits** (qui doit être un tableau).
3. Copier ce tableau dans un fichier.
4. Copier le fichier créé à l'étape 3 dans l'emplacement ``deployment\ansible-vitam-rpm\roles\log-server\files\kibana-objects``.
5. Les index-pattern sont prêts à être importés.
