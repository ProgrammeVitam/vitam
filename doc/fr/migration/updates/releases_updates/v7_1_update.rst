Notes et procédures spécifiques V7.1
####################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: V5 -> V6RC -> V6 -> V7.0.

Adaptation des sources de déploiement ansible
=============================================

Modification de la personnalisation des rôles des clusters Elasticsearch
------------------------------------------------------------------------

Elasticsearch ayant fait évoluer la définition des paramètres des rôles pour chacun des noeuds, si vous avez personnalisés les rôles associés à vos serveurs pour chacun de vos clusters Elasticsearch (log ou data), les paramètres de configuration au niveau de l'ansiblerie doivent être adaptés.

Les paramètres suivants permettant la personnalisation des rôles ont étés retirés:

.. code-block:: ini

    is_master=true is_data=true is_ingest=false

..

Vous devrez les remplacer par la nouvelle convention sous forme de tableau pour chacun des hosts personnalisés:

.. code-block:: ini

    elasticsearch_roles=[master, data]

..

Cette nouvelle convention offre plus de souplesse dans la définition de l'ensemble des rôles possible pour un cluster Elasticsearch (cf. `Documentation officielle - Node Roles <https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-node.html#node-roles>`_).
Attention une mauvaise configuration de ces paramètres avancés pourrait mener à une incohérence de la configuration rendant vos clusters non fonctionnels.

Modifications du rôle curator
-----------------------------

Le rôle curator permet la rotation des indexes du cluster elasticsearch-log. Il a pour but de permettre de fermer et supprimer au bout d'un certain temps les anciens indexes afin de limiter l'empreinte mémoire associée.

Dans un contexte d'environnement de production, les logs applicatif vitam son stockés par défaut pour une durée de 365j et les accesslogs pour une durée de 180j.

Les paramètres ``curator.log.*`` ont évolués en ``curator.indices.*``.

Ancienne configuration par défaut:

.. code-block:: yaml

curator:
  log:
    metrics:
      close: 7
      delete: 30
    logstash:
      close: 10
      delete: 30
    metricbeat:
      close: 5
      delete: 10
    packetbeat:
      close: 5
      delete: 10

..

Nouvelle configuration par défaut:

.. code-block:: yaml

curator:
  ## Pour personnaliser les dates d'exécution des actions close/delete
  # actions:
  #   close:
  #     calendar: '*-*-* 00:10:00'
  #   delete:
  #     calendar: '*-*-* 00:20:00'
  indices:
    vitam:
      close: 30
      delete: 365
    access:
      close: 30
      delete: 180
    system:
      close: 7
      delete: 30
    metricbeat:
      close: 5
      delete: 10
    packetbeat:
      close: 5
      delete: 10
    ## Exemple d'index personnel avec préfixe personnalisé
    ## Sans le paramètre prefix, les actions seront exécutées sur les indexes nommés logstash-mycustom*
    ## Avec le paramètre prefix défini, les actions seront exécutées sur les indexes nommés myprefix*
    # mycustom:
    #   prefix: myprefix
    #   close: 15
    #   delete: 30

..

Le listing des éléments ``curator.indices.<index_name>`` permet de créer les fichiers de configuration adaptés à la mise en place des rotations de chacun des indexes.

Chacun des nouveaux indices sera préfixé selon la convention de nommage ``logstash-<index_name>``.

Si vous aviez personnalisés le prefix des indexes à gérer par curator à l'aide de la variable ``curator.log.prefix`` (par défaut 'logstash-*'). Vous devez maintenant la modifier à l'aide du paramètre ``curator.indices.<index_name>.prefix``.

Ajout de nouveaux exporters
---------------------------

Ces nouveaux exporters permettent de collecter des métriques afin d'afficher ces éléments via Grafana dans les dashboards associés.

Blackbox Exporter permet de collecter des métriques sur le statut des URLs listées dans ``prometheus.blackbox_exporter.targets``.

MongoDB Exporter permet de collecter des métriques sur les différents clusters mongo-vitam.

Il est possible de les désactiver via ``prometheus.blackbox_exporter.enabled: false`` ou ``prometheus.mongodb_exporter.enabled: false``

Ouvrir les ports suivants de Prometheus -> Exporters pour permettre la collecte des métriques:

* Blackbox Exporter: ``prometheus.blackbox_exporter.port: 9115``
* MongoC Exporter: ``prometheus.mongodb_exporter.port_mongoc: 9216``
* MongoD Exporter: ``prometheus.mongodb_exporter.port_mongod: 9217``

Modification de la durée de rétention des logs par défaut
---------------------------------------------------------

Par défaut on conserve maintenant 365j de logs (accesslogs & applicatif) dans une limite de 5GB (par composant). De plus, nous avons réduit la quantité de logs gc de ``32*64m=2048m`` à ``8*32m=256m``.

Il est toujours possible de personnaliser ce paramétrage par défaut via les variables suivantes:

* Pour les gc:
  * ``vitam_defaults.jvm_opts.gc`` ou par composant en utilisant la variable ``vitam.<composant>.jvm_opts.gc``.

* Pour les accesslogs:
  * ``vitam_defaults.access_retention_days: 365`` ou par composant en utilisant la variable ``vitam.<composant>.access_retention_days: 365``.
  * ``vitam_defaults.access_total_size_cap: 5GB`` ou par composant en utilisant la variable ``vitam.<composant>.access_total_size_cap: 5GB``.

* Pour les logs applicatifs:
  * ``vitam_defaults.logback_total_size_cap.file.history_days: 365`` ou par composant en utilisant la variable ``vitam.<composant>.logback_total_size_cap.file.history_days: 365``.
  * ``vitam_defaults.logback_total_size_cap.file.totalsize: 5GB`` ou par composant en utilisant la variable ``vitam.<composant>.logback_total_size_cap.file.totalsize: 5GB``.

Modification de la méthodologie de concentration des logs
---------------------------------------------------------

Un nouveau composant applicatif (Filebeat) permettant de collecter les logs dans le cluster elasticsearch-log a été ajouté.

La méthode de collecte via rsyslog et syslog-ng sera donc dépréciée dans les futures releases.

Vous pouvez continuer à utiliser les précédentes méthodes de concentation de logs via la configuration du paramètre ``syslog.name: filebeat`` (rsyslog, syslog-ng).

Nouveau mode de déploiement en container (beta)
-----------------------------------------------

.. caution:: Attention, à ne pas utiliser en production.

Pour permettre le déploiement en mode conteneur de Vitam, vous devez configurer les valeurs suivantes:

Dans le fichier de configuration des repositories ``environments/group_vars/all/main/repositories.yml``

.. code-block:: yaml

  install_mode: container # Default to legacy

  container_repository:
    registry_url:
    username:
    password:

..

Actuellement l'antivirus n'est pas supporté par le déploiement en mode conteneur, vous devrez configurer la valeur suivante: ``vitam.ingest_external.ignore_antivirus_check: true``.

De plus, le composant library n'est pas supporté en mode container, vous devrez laisser vide le groupe ``[library]`` de votre inventaire.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7.1.

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Pour le dépôt vitam-external, vous devez renseigner la version adaptée à votre système d'exploitation (par exemple pour la version 7.1.0):

* CentOS 7: https://download.programmevitam.fr/vitam_repository/7.1.0/rpm/vitam-external/7/
* AlmaLinux 9: https://download.programmevitam.fr/vitam_repository/7.1.0/rpm/vitam-external/9/
* Debian 11: https://download.programmevitam.fr/vitam_repository/7.1.0/deb/vitam-external/11/
* Debian 12: https://download.programmevitam.fr/vitam_repository/7.1.0/deb/vitam-external/12/

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..


Montée de version vers mongo 6.0
--------------------------------

.. caution:: Cette montée de version doit être effectuée AVANT la montée de version V7.1 de Vitam et après l'arrêt des tâches planifiées et des externals.

.. caution:: Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V7.1.

Exécutez le playbook suivant à partir de l'ansiblerie de la V7.1 **sur tous les sites** :

.. code-block:: bash

    # Mise à jour mongo
    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_60.yml --ask-vault-pass

..

Montée de version vers mongo 7.0
--------------------------------

.. caution:: Cette montée de version doit être effectuée AVANT la montée de version V7.1 de vitam et après la montée de version de MongoDB 6.0 ci-dessus.

Exécutez le playbook suivant à partir de l'ansiblerie de la V7.1 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_70.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7.1

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Suppression des anciens meta-packages de COTS
---------------------------------------------

Exécutez le playbook suivant à partir de l'ansiblerie de la V7.1 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_metapackages.yml --ask-vault-pass

..

Duplication des packages lors de la mise à jour
-----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V7.1

.. caution:: Cette opération doit être effectuée uniquement pour les systèmes d'exploitation à base de RPM (CentOS 7 & AlmaLinux 9) sur un vitam éteint.

Un bug a été introduit dans la construction des packages RPM à partir de la V6RC. Si vous effectuez une montée de version depuis la V6RC (v6.rc.1 ou inférieure), la V6 (v6.2 ou inférieure) ou la V7.0 (v7.0.1 ou inférieure), vous devrez appliquer la commande suivante pour supprimer les précédents packages. Sinon lors de de la mise à jour, le package précédent ne sera pas désinstallé conduisant à la présence de plusieurs versions du même package.

Exemple d'erreur:

.. code-block:: bash

    /var/tmp/rpm-tmp.fGwZMk: ligne 1 : fg: pas de contrôle de tâche
    erreur : %preun(vitam-offer-6.2-1.noarch) scriptlet échoué, état de sortie 1
    Error in PREUN scriptlet in rpm package vitam-offer
    erreur : vitam-offer-6.2-1.noarch: effacer échoué

..

Voici la commande à exécuter pour permettre la désinstallation des packages de la version précédente:

.. code-block:: bash

    ansible vitam --ask-vault-pass -v -a "yum --setopt=tsflags=noscripts remove -y vitam-*.noarch" -i environments/<inventaire>

..

Application de la montée de version
===================================

.. caution:: L'application de la montée de version s'effectue d'abord sur les sites secondaires puis sur le site primaire.

Lancement du master playbook vitam
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam/vitam.yml --ask-vault-pass

..

Lancement du master playbook extra
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/extra.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

Réindexation de l'indice logstash-vitam courant
-----------------------------------------------

Suite à la mise en place de filebeat (mode par défaut), le mapping associé aux indices a évolué. Afin de prendre en compte le nouveau mapping dans l'indice de la journée courante, vous devrez appliquer le playbook de migration suivant sur **tous les sites**.

Tant que vous n'aurez pas appliqué ce playbook, la centralisation des logs sera défectueuse et entrainera de très nombreux logs d'erreurs au niveau des serveurs logstash.

Si vous n'appliquez pas ce playbook, cette erreur se corrigera automatiquement à la création de l'indice du lendemain. Vous n'aurez juste pas accès à la centralisation des logs pour la journée courante dans les outils tel que Kibana.

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/rename_current_logstash_vitam_index.yml --ask-vault-pass

..

Migration des mappings elasticsearch pour les métadonnées
---------------------------------------------------------

Cette migration de données consiste à mettre à jour le modèle d'indexation des métadonnées sur elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..
