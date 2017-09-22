Procédure de première installation
##################################


.. |repertoire_deploiement| replace:: ``deployment``
.. |repertoire_inventory| replace:: ``environments``
.. |repertoire_playbook ansible| replace:: ``ansible-vitam``

Les fichiers de déploiement sont disponibles dans la version VITAM livrée dans le sous-répertoire |repertoire_deploiement| . Ils consistent en 2 parties :

 * le playbook ansible, présent dans le sous-répertoire |repertoire_playbook ansible|, qui est indépendant de l'environnement à déployer
 * les fichiers d'inventaire (1 par environnement à déployer) ; des fichiers d'exemple sont disponibles dans le sous-répertoire |repertoire_inventory|


Configuration du déploiement
============================

.. _inventaire:

Informations "plate-forme"
--------------------------

Pour configurer le déploiement, il est nécessaire de créer dans le répertoire |repertoire_inventory| un nouveau fichier d'inventaire à nommer ``hosts.<environnement>`` ( où <environnement> sera utilisé par la suite ) comportant les informations suivantes :

.. literalinclude:: ../../../../deployment/environments/hosts.example
   :language: ini
   :linenos:

Pour chaque type de "host", indiquer le(s) serveur(s) défini(s) pour chaque fonction. Une colocalisation de composants est possible.

.. caution:: en cas de colocalisation, bien prendre en compte la taille JVM de chaque composant (VITAM : -Xmx512m par défaut) pour éviter de swapper.

.. note:: pour les "hosts-worker", il est possible d'ajouter, à la suite de chaque "host", 2 paramètres optionnels : capacity et workerFamily. Se référer au :term:`DEX` pour plus de précisions.

Ensuite, dans la section ``hosts:vars``, renseigner les valeurs comme décrit :

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
   "package_version","Version à installer","Par défaut, indiquer *"
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
   "vitam_tenant_ids","Liste des tenants de plateforme","[0,1,2] ; [0] par défaut"
   "vitam_tests_gitrepo_protocol","Protocole d'attaque du git lfs des TNR",""
   "vitam_tests_gitrepo_baseurl","domaine du git lfs des TNR",""
   "vitam_tests_gitrepo_url","Création de l'URL à partir des lignes précédentes",""
   "vitam_tests_branch","Branche à récupérer sur le git lfs","master"


A titre informatif, le positionnement des variables ainsi que des dérivations des déclarations de variables sont effectuées sous |repertoire_inventory| ``/group_vars/all/all``, comme suit :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/all
   :language: yaml
   :linenos:


Le fichier ``vault-vitam.yml`` est également présent sous |repertoire_inventory| ``/group_vars/all/all`` et contient les secrets ; ce fichier est encrypté par ``ansible-vault`` et doit être paramétré avant le lancement de l'orchestration de déploiement.

.. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-vitam.txt
   :language: ini
   :linenos:

.. note:: Si le mot de passe du fichier ``vault-vitam.yml`` est changé, ne pas oublier de le répercuter dans le fichier ``vault_pass.txt`` (et le sécuriser à l'issue de l'installation).


Le fichier ``vault-extra.yml`` peut être également présent sous |repertoire_inventory| ``/group_vars/all/all`` et contient des secrets supplémentaires ; ce fichier est encrypté par ``ansible-vault`` et doit être paramétré avant le lancement de l'orchestration de déploiement, si le composant ihm-recette est déployé avec récupération des TNR.

.. literalinclude:: ../../../../deployment/environments/group_vars/all/example_vault-extra.yml
   :language: ini
   :linenos:

.. note:: Pour ce fichier, l'encrypter avec le même mot de passe que ``vault-vitam.yml``.


Le déploiement s'effectue depuis la machine "ansible" et va distribuer la solution VITAM selon l'inventaire correctement renseigné.

.. warning:: le playbook ``vitam.yml`` comprend des étapes avec la mention ``no_log`` afin de ne pas afficher en clair des étapes comme les mots de passe des certificats. En cas d'erreur, il est possible de retirer la ligne dans le fichier pour une analyse plus fine d'un éventuel problème sur une de ces étapes.

.. _update_jvm:

Tuning JVM
-----------

.. note:: Cette section est en cours de développement.

Un tuning fin des paramètres JVM de chaque composant VITAM est possible ; pour cela, il faut ajouter/modifier des "hostvars" aux partitions associées.

.. caution:: Limitation technique à ce jour ; il n'est pas possible de définir des variables JVM différentes pour des composants colocalisés sur une même partition.



Paramétrage de l'antivirus (ingest-externe)
-------------------------------------------

L'antivirus utilisé par ingest-externe est modifiable (par défaut, ClamAV) ; pour cela :

* Créer un autre shell (dont l'extension doit être ``.sh.j2``) sous ``ansible-vitam/roles/vitam/templates/ingest-external`` ; prendre comme modèle le fichier ``scan-clamav.sh.j2``. Ce fichier est un template Jinja2, et peut donc contenir des variables qui seront interprétées lors de l'installation.
* Modifier le fichier ``ansible-vitam/roles/vitam/templates/ingest-external/ingest-external.conf.j2`` en pointant sur le nouveau fichier.


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

Se reporter au chapitre dédié à la gestion des certificats: :doc:`../certificats/introduction_certificats`


Déploiement
===========

Fichier de mot de passe
-----------------------

Si le fichier ``deployment/vault_pass.txt`` est renseigné avec le mot de passe du fichier ``environments/group_vars/all/vault-vitam.yml``, le mot de passe ne sera pas demandé. Si le fichier est absent, le mot de passe du "vault" sera demandé.

.. _pkiconfsection:

PKI
---
Se positionner dans le répertoire ``deployment``.


1. paramétrer les fichiers ``environments/group_vars/all/vault-vitam.yml`` et ``environments/group_vars/all/vault-keystores.yml`` (définition des mots de passe des différents stores java - à adapter aux exigences de sécurité de l'exploitant), ainsi que le fichier d'inventaire de la plate-forme sous ``environments`` (se baser sur le fichier hosts.example)

Exemple de fichier ``vault-keystores.yml`` :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/vault-keystores.txt
   :language: ini
   :linenos:

2. En absence d'une PKI, exécuter le script

.. code-block:: bash

   ./pki/scripts/generate_ca.sh

.. note:: En cas d'absence de :term:`PKI`, il permet de générer  une :term:`PKI`, ainsi que des certificats pour les échanges https entre composants. Autrement, passer à l'étape suivante.

3. Génération des certificats, si aucun n'est fourni par le client

.. code-block:: bash

   pki/scripts/generate_certs.sh <environnement>

.. note:: Ce script  génère des certificats  nécessaires au bon fonctionnement de VITAM ainsi qu'un fichier ``(deployment)/environments/certs/vault-certs.yml`` contenant les mots de passe correspondants.

4. Génération des stores Java, s'ile ne sont pas fournis par le client

.. code-block:: bash

   ./generate_stores.sh <environnement>

.. note:: Basé sur le contenu du fichier ``vault.yml``, ce script  génère des stores nécessaires au bon fonctionnement de VITAM et les positionne au bon endroit pour le déploiement.

Mise en place des repositories VITAM (optionnel)
-------------------------------------------------

VITAM fournit un playbook permettant de définir sur les partitions cible la configuration d'appel aux repositories spécifiques à VITAM :


Editer le fichier ``environments/group_vars/all/repositories.yml`` à partir des modèles suivants (décommenter également les lignes) :

Pour une cible de déploiement CentOS :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/example_bootstrap_repo_centos.yml
   :language: yaml
   :linenos:


Pour une cible de déploiement Debian :

.. literalinclude:: ../../../../deployment/environments/group_vars/all/example_bootstrap_repo_debian.yml
   :language: yaml
   :linenos:

Ce fichier permet de définir une liste de repositories. Décommenter et adapter à votre cas.

Pour mettre en place ces repositories sur les machines cibles, lancer la commande :

.. code-block:: bash

  ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/<fichier d'inventaire>  --ask-vault-pass

.. note:: En environnement CentOS, il est recommandé de créer des noms de repository commençant par  "vitam-".

Réseaux
-------

Une fois l'étape de PKI effectuée avec succès, il convient de procéder à la génération des hostvars, qui permettent de définir quelles interfaces réseau utiliser.
Actuellement la solution logicielle Vitam est capable de gérer 2 interfaces réseau:

    - Une d'administration
    - Une de service

Cas 1: Machines avec une seule interface réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles Vitam sera déployé ne disposent que d'une interface réseau, ou si vous ne souhaitez en utiliser qu'une seule, il convient d'utiliser le playbook ``ansible-vitam/generate_hostvars_for_1_network_interface.yml``

Cette définition des host_vars se base sur la directive ansible ``ansible_default_ipv4.address``, qui se base sur l'adresse IP associée à la route réseau définie par défaut.

.. Warning:: Les communication d'administration et de service transiteront donc toutes les deux via l'unique interface réseau disponible.

Cas 2: Machines avec plusieurs interfaces réseau
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Si les machines sur lesquelles Vitam sera déployé disposent de plusieurs interfaces, si celles-ci respectent cette règle:

    - Interface nommée eth0 = ip_service
    - Interface nommée eth1 = ip_admin

Alors il est possible d'utiliser le playbook ``ansible-vitam-extra/generate_hostvars_for_2_network_interfaces.yml``

.. Note:: Pour les autres cas de figure, il sera nécessaire de générer ces hostvars à la main ou de créer un script pour automatiser cela.

Vérification de la génération des hostvars
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A l'issue, vérifier le contenu des fichiers générés sous ``environments/host_vars/`` et les adapter au besoin.

Déploiement
-------------

Une fois l'étape de la génération des hosts a été effectuée avec succès, le déploiement est à réaliser avec la commande suivante :

.. code-block:: bash

   ansible-playbook ansible-vitam/vitam.yml -i environments/<ficher d'inventaire> --ask-vault-pass

Extra
------

Deux playbook d'extra sont fournis pour usage "tel quel".

1. ihm-recette

Ce playbook permet d'installer également le composant :term:`VITAM` ihm-recette.

.. code-block:: bash

   ansible-playbook ansible-vitam-extra/ihm-recette.yml -i environments/<ficher d'inventaire> --ask-vault-pass


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

   ansible-playbook ansible-vitam-extra/extra.yml -i environments/<ficher d'inventaire> --ask-vault-pass

Import automatique d'objets dans Kibana
=========================================

Il peut être utile de vouloir automatiquement importer dans l'outil de visualisation Kibana des dashboards préalablement crées. Cela ce fait simplement avec le système d'import automatique mis en place. Il suffit de suivre les différentes étapes :

1. Ouvrir l'outil Kibana dans son navigateur.
2. Créer ses dashboards puis sauvegarder.
3. Aller dans l'onglets **Settings** puis **Objects**.
4. Sélectionner les composants à exporter puis cliquer sur le bouton **Export**. (ou bien cliquer sur **Export Everything** pour tout exporter).
5. Copier le/les fichier(s) *.json* téléchargés à l'emplacement ``deployment\ansible-vitam\roles\log-server\files\kibana-objects``.
6. Les composants sont prêts à être importés automatique lors du prochain déploiement.

Pour éviter d'avoir à recréer les "index-pattern" définis dans l'onglet **Settings** de Kibana, ceux-ci aussi sont pris en charge par le système de déploiement automatique. En revanche ils ne sont pas exportables, il est donc nécessaire de créer à la main le fichier *.json* correspondant. Pour ce faire :

1. Faire une requête GET sur l'url suivante ``http://<ip-elasticsearch-log>/.kibana/index-pattern/_search``.
2. Récupérer le contenu au format JSON et extraire le contenu de la clé **hits.hits** (qui doit être un tableau).
3. Copier ce tableau dans un fichier.
4. Copier le fichier créé à l'étape 3 dans l'emplacement ``deployment\ansible-vitam\roles\log-server\files\kibana-objects``.
5. Les index-pattern sont prêts à être importés.

NB: Il ne faut pas oublier de selectionner l'index pattern par defaut avant toutes recherches ( se referer à la documention officielle de Kibana pour plus d'informations )
