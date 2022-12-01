Notes et procédures spécifiques V5RC
####################################

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V5RC

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

..

Supprimer les indexes de configuration kibana
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée majeure depuis une version R16.4- (R16.4 ou inférieur)

.. caution:: Sans cette opération, l'installation kibana est bloquée et arrête l'installation de Vitam

Lors de la montée de version ELK, les indices de configuration kibana : .kibana et .kibana_task_manager persistent avec une version et des informations incorrectes (celles de la version d'avant). Il est nécessaire des les effacer; autrement la montée de version est bloquée.

Exécutez le playbook suivant:

.. code-block:: bash

     ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_kibana_indexes.yml --ask-vault-pass

Ce playbook clone les indices de configuration (.kibana et .kibana_task_manager) et efface les originaux. Les clones d'indice sont conservés.

La montée de version va recréer ces indices avec les nouvelles configurations relatives au nouvel ELK.

Réinitialisation de la reconstruction des registres de fond des sites secondaires
---------------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure). Elle permet la réinitialisation de la reconstruction des registre de fonds sur les sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que les timers de Vitam aient bien été préalablement arrêtés (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_accession_register_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure). Elle permet le contrôle et la purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V5RC

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée IMMÉDIATEMENT APRÈS la montée de version vers la V5RC

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

..

Migration des unités archivistiques
-----------------------------------

.. caution:: Cette migration doit être effectuée APRÈS la montée de version V5RC mais avant la réouverture du service aux utilisateurs.

Cette migration de données consiste à :

- Supprimer le champ ``_us_sp``.
- Rendre inactive l'indexation des champs dynamiques créés au niveau des règles de gestion héritées au niveau de la propriété ``endDates``.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

Lancez la migration via la commande suivante :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v5rc.yml --ask-vault-pass

Après le passage du script de migration, il faut procéder à la réindexation de toutes les unités archivistiques :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags unit --ask-vault-pass

  ..

Migrations offres Swift V2 & V3 en cas de présence d'objets très volumineux (4Go+)
----------------------------------------------------------------------------------

.. caution:: Cette procédure doit être lancée une seule fois, et pour chaque offre Swift V2/V3, APRÈS upgrade Vitam.

Si vous disposez d'une instance R16.3 ou inférieur (4.0.3-), et que vous utilisez des offres Swift V2/V3 (providers openstack-swift-v2 et/ou openstack-swift-v3), il est nécessaire de procéder à une migration des données :

.. code-block:: bash

    $ ansible-playbook ansible-vitam-migration/migration_swift_v2_and_v3.yml -i environments/hosts.{env} --ask-vault-pass

    # Confirm playbook execution
    # Enter swift offer id (ex offer-swift-1)
    # Select migration mode
    # > Enter '0' for analysis only mode : This mode will only log anomalies (in offer technical logs), no update will be proceeded
    # > Enter '1' to fix inconsistencies : This mode will update swift objects to fix inconsistencies. However, this does not prune objects (delete partially written or eliminated objects segments to free space).
    # > Enter '2' to fix inconsistencies and purge all deleted objects segments to free storage space.
    # Reconfirm playbook execution

Il est recommandé de lancer la procédure en mode 0 (analyse seule) et de vérifier les erreurs de cohérence dans les logs.

Seul les offres Swift V2/V3 avec des objets volumineux (>= 4Go) nécessitent une migration. Un exemple d'incohérence journalisés dans les logs (/vitam/log/offers) est donnée ici : ::

    INCONSISTENCY FOUND : Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq has old segment names [aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/2, aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/1]. Run migration script with fix inconsistencies mode to prune container.
    INCONSISTENCY FOUND : Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq has missing metadata. Run migration script with fix inconsistencies mode enabled to set object metadata.

Si la détection des anomalies est terminée en succès, et que des anomalies sont trouvées, il est recommandé de lancer le mode 1 (correction des anomalies). Les migrations de données sont également journalisées dans les logs (/vitam/log/offers) : ::

    Renaming segment env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/2 to env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/00000002
    Renaming segment env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/1 to env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq/00000001
    Object env_2_object/aeaaaaaaaagbcaacaamboal2tk643jqaaaaq migrated successfully. Digest: 8959ea1290aa064a3c64d332f31e049bd4f9d4e95bebe0b46d38613bb079761d52c865dce64c88fd7e02313d340f9a2f8c0c6b5dbf8909a3cbda071d26ce21d4

Si des problèmes de cohérence de type "Orphan large object segments" persistent ::

    INCONSISTENCY FOUND : Orphan large object segments [...] without parent object manifest: env_2_object/aeaaaaaaaagbcaacaamboal2tk7dzmiaaaaq. Eliminated object? Incomplete write? Run migration script with delete mode to prune container.

Dans ce cas, il est recommandé de vérifier préalablement que les objets concernés n'existent pas sur les autres offres (mêmes container & objectName).
Si les objets n'existent pas dans les autres offres, il s'agit alors de reliquats d'objets non complètement éliminés. Le lancement du mode 2 (correction des anomalies + purge des objets) est à réaliser.
Dans le cas contraire (cas où l'objet existe dans les autres offres), il faudra envisager la "Procédure de resynchronisation ciblée d’une offre" décrite dans la Documentation d’EXploitation (DEX) de Vitam pour synchroniser l'offre Swift pour les éléments concernés.

Recalcul du graph des métadonnées des sites secondaires
-------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure depuis une version R16.6- (4.0.6 ou inférieure). Elle permet le recalcul du graphe des métadonnées sur les sites secondaires

La procédure est à réaliser sur tous les **sites secondaires** de Vitam APRÈS l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_metadata_graph_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Redémarrage des timers et des accès externes à Vitam
----------------------------------------------------

La montée de version est maintenant terminée, vous pouvez réactiver les services externals ainsi que les timers sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --ask-vault-pass

  ..
