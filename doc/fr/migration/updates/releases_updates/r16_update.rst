Notes et procédures spécifiques R16
###################################

Vérification préalable avant la migration
=========================================

Gestion des règles de gestion
-----------------------------

Dans le cadre d'une correctif concernant la validation stricte des types de règles lors de l'import du référentiel des règles de gestion, il faut impérativement, AVANT la montée de version, vérifier tous les types de règles de gestion existants sur Mongo et ES et les modifier manuellement en cas d'incohérence.

Pour ce faire, il faut s'assurer que tous les types de règles de gestion (``RuleType``) respectent la casse (les majuscules et les minuscules).

Ci-après la liste des valeurs valides autorisées :

  - ``AppraisalRule``
  - ``AccessRule``
  - ``StorageRule``
  - ``DisseminationRule``
  - ``ClassificationRule``
  - ``ReuseRule``
  - ``HoldRule``

Exemple :
    ``APPRAISALRULE`` devrait être ``AppraisalRule``


Adaptation des sources de déploiement ansible
=============================================

Déplacement des paramètres relatif à la gestion des tenants
-----------------------------------------------------------

Dans le cadre de la fonctionnalité introduite en R15 permettant de regrouper les tenants, les paramètres suivants ont étés déplacés du fichier d'inventaire au fichier ``group_vars/all/advanced/tenants_vars.yml``

  - ``vitam_tenant_ids``
  - ``vitam_tenant_admin``

Si la montée de version s'effectue à partir d'une R13, il faut supprimer les valeurs précédentes de votre fichier d'inventaire et les reporter dans le fichier ``tenants_vars.yml`` en respectant la syntaxe YML adéquate.

.. seealso:: Se référer à la documentation d'installation pour plus d'informations concernant le fichier ``environments/group_vars/all/advanced/tenants_vars.yml``

Procédures à exécuter AVANT la montée de version
================================================

Supprimer les indexes de configuration kibana
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la R16.5 ou supérieur (4.0.5+).

.. caution:: Sans cette opération, l'installation kibana est bloquée et arrête l'installation de Vitam

Lors de la montée de version ELK, les indices de configuration kibana : .kibana et .kibana_task_manager persistent avec une version et des informations incorrectes (celles de la version d'avant). Il est nécessaire des les effacer; autrement la montée de version est bloquée.

Exécutez le playbook suivant:

.. code-block:: bash

     ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_kibana_indexes.yml --ask-vault-pass

Ce playbook clone les indices de configuration (.kibana et .kibana_task_manager) et efface les originaux. Les clones d'indice sont conservés.

La montée de version va recréer ces indices avec les nouvelles configurations relatives au nouvel ELK.

Réinitialisation de la reconstruction des registres de fond des sites secondaires
---------------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure vers une version R16.7+ (4.0.7 ou supérieure). Elle permet la réinitialisation de la reconstruction des registre de fonds sur les sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que les timers de Vitam aient bien été préalablement arrêtés (via le playbook ``ansible-vitam-exploitation/stop_vitam_timers.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_accession_register_reconstruction.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure vers une version R16.7+ (4.0.7 ou supérieure). Elle permet le contrôle et la purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam AVANT l'installation de la nouvelle version :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

  ..

Procédures à exécuter APRÈS la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

Recalcul du graph des métadonnées des sites secondaires
-------------------------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure vers une version R16.7+ (4.0.7 ou supérieure). Elle permet le recalcul du graphe des métadonnées sur les sites secondaires

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

Vérification de la bonne migration des données
==============================================

Audit coherence
---------------

Il est recommandé de procéder à un audit de cohérence aléatoire suite à une procédure de montée de version VITAM ou de migration de données.
Pour ce faire, se référer au dossier d'exploitation (DEX) de la solution VITAM, section ``Audit de cohérence``.
