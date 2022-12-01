Notes et procédures spécifiques V6RC
##################################

.. caution:: Pour une montée de version depuis la version R16 de Vitam, veuillez appliquer les procédures spécifiques de la V5RC et de la V5 en complément des procédures suivantes. Pour une montée de version depuis la version V5RC de Vitam, veuillez appliquer les procédures spécifiques de la V5 en complément des procédures suivantes. Pour une montée de version depuis la V5, vous pouvez appliquer la procédure suivante directement.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V6RC

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

..

Montée de version vers mongo 4.4
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V6RC

.. caution:: Sans cette opération, la montée de version d'une version existante vers une v6rc sera bloquée au démarrage des instances mongod par une incompatibilité.

Exécutez le playbook suivant:

.. code-block:: bash

     ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_44.yml --ask-vault-pass

Ce playbook effectue la montée de version de mongodb d'une version 4.2 vers une version 4.4 selon la procédure indiquée dans la documentation Mongodb
https://www.mongodb.com/docs/v4.4/release-notes/4.4-upgrade-replica-set/ . Cette procédure n'a pas été testée avec une version mongodb inférieure à 4.2.

Montée de version vers mongo 5.0
--------------------------------

.. caution:: Cette montée de version doit être effectuée AVANT la montée de version V6RC de vitam et après la montée de version en mongodb 4.4 ci dessus.

Exécutez le playbook suivant:

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_50.yml --ask-vault-pass

Ce playbook change le "Read and write Concern" des replicaset par reconfiguration, il désinstalle et réinstalle les binaires et . Il change également le paramètre
"SetFeatureCompatibility" à 5.0.

Une fois ces montées de version de Mongodb réalisées la montée de version Vitam classique peut être réalisée.

Nettoyage des fichiers timers, services et conf suite à la migration vers le scheduler
--------------------------------------------------------------------------------------

.. caution:: Cette étape doit être effectuée AVANT la montée de version V6RC de vitam et après l'arrêt des Timers.

Executez le playbook suivant :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_files_for_scheduler_migration.yml --ask-vault-pass

Ce playbook supprime les fichiers .service, .sh, .timers et .conf suite au passage vers le scheduler Quartz sur les hosts concernés.

Réinitialisation de la reconstruction des registres de fond des sites secondaires
---------------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de :

  - migration majeure depuis une version R16.6- (4.0.6 ou inférieure)
  - migration majeure depuis une version v5.rc.3- (v5.rc.3 ou inférieure)
  - migration majeure depuis une version v5.0.

Cette procédure permet la réinitialisation de la reconstruction des registre de fonds sur les sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que les timers de Vitam aient bien été préalablement arrêtés (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_accession_register_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de :

  - migration majeure depuis une version R16.6- (4.0.6 ou inférieure)
  - migration majeure depuis une version v5.rc.3- (v5.rc.3 ou inférieure)
  - migration majeure depuis une version v5.0.

Cette procédure permet le contrôle et la purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V6RC

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

Arrêt des jobs Vitam et des accès externes à Vitam
--------------------------------------------------

.. caution:: Cette opération doit être effectuée IMMÉDIATEMENT APRÈS la montée de version vers la V6RC

Les jobs Vitam et les services externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..

Migration des groupes d'objets
------------------------------

.. caution:: Cette migration doit être effectuée APRÈS la montée de version V6RC mais avant la réouverture du service aux utilisateurs.

Cette migration de données consiste à ajouter les champs ``_acd`` (date de création approximative) et ``_aud`` (date de modification approximative) dans la collection ObjectGroup.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

- Migration des unités archivistiques sur mongo-data (le playbook va stopper les externals avant de procéder à la migration) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v6rc.yml --ask-vault-pass

Après le passage du script de migration, il faut procéder à la réindexation de toutes les groupes d'objets :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags objectgroup --ask-vault-pass

  ..

Recalcul du graph des métadonnées des sites secondaires
-------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de :

  - migration majeure depuis une version R16.6- (4.0.6 ou inférieure)
  - migration majeure depuis une version v5.rc.3- (v5.rc.3 ou inférieure)
  - migration majeure depuis une version v5.0.

Cette procédure permet le recalcul du graphe des métadonnées sur les sites secondaires

La procédure est à réaliser sur tous les **sites secondaires** de Vitam APRÈS l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_metadata_graph_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Redémarrage des Jobs Vitam et des accès externes à Vitam
--------------------------------------------------------

La montée de version est maintenant terminée, vous pouvez réactiver les services externals ainsi que les jobs Vitam sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduler.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduling.yml --ask-vault-pass

  ..