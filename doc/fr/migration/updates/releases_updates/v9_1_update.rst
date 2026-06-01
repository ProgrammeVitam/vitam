Notes et procédures spécifiques V9.1
####################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: V7.1 -> V8.0 -> V8.1 -> V9.0 -> V9.1

Adaptation des sources de déploiement ansible
=============================================

Renommage de variables pour mise en cohérence des noms et groupes
-----------------------------------------------------------------

Afin de faciliter la maintenance et les évolutions autour de l'ansiblerie, certaines variables et groupes d'hôtes ont étés renommés pour correspondre à la convention de nommage établie.

Les variables de configuration suivantes ont étés renommées ainsi:

.. code-block:: yaml

  vitam:
    accessexternal: -> access_external
    accessinternal: -> access_internal
    batchreport: -> batch_report
    ingestexternal: -> ingest_external
    ingestinternal: -> ingest_internal
    elastickibanainterceptor: -> elastic_kibana_interceptor
    storageengine: -> storage
    storageofferdefault: -> offer

..

Les groupes d'hôtes suivants du fichier d'inventaire ont étés renommés ainsi:

.. code-block:: ini

  [hosts_storage_engine] -> [hosts_storage]
  [hosts_storage_offer_default] -> [hosts_offer]

..

Ces modifications sont à reporter dans vos configurations, vous pouvez effectuer cette opération en exécutant les commandes sed suivantes:

.. code-block:: bash

    sed -i 's/accessexternal/access_external/g' environments/group_vars/all/*/*.yml
    sed -i 's/accessinternal/access_internal/g' environments/group_vars/all/*/*.yml
    sed -i 's/batchreport/batch_report/g' environments/group_vars/all/*/*.yml
    sed -i 's/ingestexternal/ingest_external/g' environments/group_vars/all/*/*.yml
    sed -i 's/ingestinternal/ingest_internal/g' environments/group_vars/all/*/*.yml
    sed -i 's/elastickibanainterceptor/elastic_kibana_interceptor/g' environments/group_vars/all/*/*.yml
    sed -i 's/storageengine/storage/g' environments/group_vars/all/*/*.yml
    sed -i 's/storageofferdefault/offer/g' environments/group_vars/all/*/*.yml

    sed -i 's/hosts_storage_engine/hosts_storage/g' environments/<inventaire>
    sed -i 's/hosts_storage_offer_default/hosts_offer/g' environments/<inventaire>

..

Pour information, le playbook ``ansible-vitam/services/vitam/storage-engine.yml`` a été renommé en ``ansible-vitam/services/vitam/storage.yml``.

Configuration du nombre de shards/replicas pour chaque indices elasticsearch-log
--------------------------------------------------------------------------------

Ces modifications impliquent des changements de la configuration Ansible associée aux indices pour plus de cohérence avec les autres variables de configuration.

Ainsi, les clés de configurations suivantes ont étés retirées:

* ``kibana.log.metrics.replica``
* ``kibana.log.metrics.shards``

Elles ont étés remplacées par ces variables spécifiques pour chacun de vos indices:

.. code-block:: yaml

  elasticsearch:
    log:
      indices:
        default_config: # Default configuration if others indices are undefined
          number_of_shards: 1
          number_of_replicas: 1
        vitam: # Configuration for indexes logstash-vitam-*
          number_of_shards: 1
          number_of_replicas: 1
        access: # Configuration for indexes logstash-access-*
          number_of_shards: 1
          number_of_replicas: 1

..

* Si aucune valeur pour les indices ``vitam`` ou ``access`` n'est spécifiée, c'est la configuration ``default_config`` qui sera appliquée.
* Si la configuration ``default_config`` n'est pas spécifiée, les valeurs par défaut sont ``shards: 1`` et ``replicas: 1``.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.1

.. caution:: Cette opération doit être effectuée avec les sources de déploiement de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass

..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Pour le dépôt vitam-external, vous devez renseigner la version adaptée à votre système d'exploitation (par exemple pour la version 9.1.0):

* AlmaLinux 9: https://download.programmevitam.fr/vitam_repository/9.1.0/rpm/vitam-external/9/
* Debian 12: https://download.programmevitam.fr/vitam_repository/9.1.0/deb/vitam-external/12/

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..

Mise à jour intermédiaire des clusters ElasticSearch
----------------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.1

Cette procédure permet de faire le saut intermédiaire entre les versions 7 et 8 des clusters ElasticSearch (data & log).

Le saut final vers la version 9 d'ElasticSearch sera effectué lors de la mise à jour de Vitam via l'exécution du master playbook ``ansible-vitam/vitam.yml``.

Exécutez les playbooks suivants sur **tous les sites** pour mettre à jour vos clusters ElasticSearch vers la version intermédiaire 8.19.8 :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam/services/cots/elasticsearch_log.yml --ask-vault-pass -e "elasticsearch_version=8.19.8"
    ansible-playbook -i environments/<inventaire> ansible-vitam/services/cots/elasticsearch_data.yml --ask-vault-pass -e "elasticsearch_version=8.19.8"

..

Attention, à partir de cette étape, il va falloir s'assurer que l'ensemble des indexes présents sur les clusters ElasticSearch (data & log) sont compatibles avec la version 9 d'ElasticSearch avant de poursuivre la procédure de montée de version. Les indexes incompatibles sont ceux créés avec une version antérieure à la version 8.x d'ElasticSearch.

Pour ElasticSearch-Log
^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/check_incompatible_elasticsearch_v9_indices.yml --ask-vault-pass -t elasticsearch_log

Vous pouvez aussi vérifier la compatibilité via Kibana-Log:

* Accédez à la section ``Management > Stack Management > Stack > Upgrade Assistant``
* Vérifiez que les indexes ne sont pas marqués comme ``Incompatible with the current version of Elasticsearch``

Si des indexes incompatibles sont détectés, soit vous procédez à la migration des données à l'aide de la procédure dédiée, sinon, si les données ne sont pas critiques, vous pouvez les supprimer en ajoutant les paramètres ``-e delete_incompatible_indices=true --tags elasticsearch_log`` au playbook de détection.

.. caution::
    Cette opération supprimera définitivement les indexes concernés. Assurez-vous d'avoir sauvegardé ou réindexé l'ensemble des données nécessaires avant de poursuivre.

Pour ElasticSearch-Data
^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/check_incompatible_elasticsearch_v9_indices.yml --ask-vault-pass -t elasticsearch_data

Si des indexes incompatibles sont détectés, vous devez procéder à la réindexation de ces indexes. Pour cela, exécutez le playbook de réindexation :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass

.. note::
    Si seulement une sous-partie des indexes est incompatible avec la nouvelle version d'ElasticSearch, il ne sera pas nécessaire de réindexer l'ensemble des données. Dans ce cas, référez-vous à la documentation d'exploitation sur la réindexation :ref:`reindexation_es` pour cibler uniquement les indexes concernés.

.. caution::
    Durant la réindexation, les tâches planifiées et les accès externes à Vitam doivent être arrêtés.

    Cette opération peut prendre plusieurs heures en fonction de la quantité de données à réindexer. Si la durée de réindexation est trop longue pour votre production, veuillez vous référer à la procédure de migration de données Vitam vers un nouveau cluster ElasticSearch tampon.

À l'issue de la réindexation, vous pouvez réexécuter le playbook de détection avec les paramètres ``-e delete_incompatible_indices=true --tags elasticsearch_data`` afin de supprimer les indexes incompatibles restants.

.. caution::
    Cette opération supprimera définitivement les indexes concernés. Assurez-vous d'avoir sauvegardé ou réindexé l'ensemble des données nécessaires avant de poursuivre.

Mise à jour de MongoDB 8.0.23
-----------------------------

.. caution:: **Attention**
    Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V9.1.
    Cette opération est à effectuer si vous venez des versions de Vitam suivante: V8.1.2- ou V9.0.0.
    Il est recommandé d'effectuer un backup des bases de données à l'aide de mongodump avant de poursuivre.

Exécutez le playbook suivant à partir de l'ansiblerie de la V9.1 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_80.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V9.1

.. caution:: Cette opération doit être effectuée avec les sources de déploiement de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

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

Migration des mappings elasticsearch pour les métadonnées
---------------------------------------------------------

Cette migration de données consiste à mettre à jour le modèle d'indexation des métadonnées sur elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..
