Notes et procédures spécifiques R16
###################################

Supprimer les indexes de configuration kibana
----------------------------------------------

.. caution:: Cette opération doit être effectuée avant la montée de version vers la R16.5 ou supérieur (4.0.5+).

.. caution:: Sans cette opération, l'installation kibana est bloquée et arrête l'installation de Vitam

Lors de la montée de version ELK, les indices de configuration kibana : .kibana et .kibana_task_manager persistent avec une version et des informations incorrectes (celles de la version d'avant). Il est nécessaire des les effacer; autrement la montée de version est bloquée.

Executez le playbook suivant:

.. code-block:: bash

     ansible-playbook -i environments/<inventaire> ansible-vitam-migration/remove_old_kibana_indexes.yml.yml --ask-vault-pass

Ce playbook clone les indices de configuration (.kibana et .kibana_task_manager) et efface les originaux. Les clones d'indice sont conservés.

La montée de version va recréer ces indices avec les nouvelles configurations relatives au nouvel ELK.

Étapes préalables à la montée de version
========================================

Déplacement des paramètres relatif à la gestion des tenants
-----------------------------------------------------------

Dans le cadre de la fonctionnalité introduite en R15 permettant de regrouper les tenants, les paramètres suivants ont étés déplacés du fichier d'inventaire au fichier ``group_vars/all/tenants_vars.yml``

  - ``vitam_tenant_ids``
  - ``vitam_tenant_admin``

Si la montée de version s'effectue à partir d'une R13, il faut supprimer les valeurs précédentes de votre fichier d'inventaire et les reporter dans le fichier ``tenants_vars.yml`` en respectant la syntaxe YML adéquate.

.. seealso:: Se référer à la documentation d'installation pour plus d'informations concernant le fichier ``environments/group_vars/all/tenants_vars.yml``

Gestion des régles de gestion
-----------------------------

Dans le cadre d'une correction de bug permettant la validation stricte des types de règles lors de l'import du référentiel des règles de gestion,
il faut impérativement, lors d'une montée de version, vérifier tous les types de règles de gestion existants sur Mongo et ES et les modifier manuellement en cas d'incohérence.
il faut que les types de règles de gestion respecte la casse (les majuscules et les minuscules)
Exemple :
    ``APPRAISALRULE`` devrait être ``AppraisalRule``

Contrôle et nettoyage de journaux du storage engine des sites secondaires
-------------------------------------------------------------------------

Lors d'une montée de version majeure vers une version R16.7+ (4.0.7 ou supérieure), un contrôle / purge des journaux d'accès et des journaux d'écriture du storage engine des sites secondaires est nécessaire.

La procédure est à réaliser sur tous les **sites secondaires** de Vitam :

- S'assurer que Vitam soit bien préalablement arrêté (via le playbook ``ansible-vitam-exploitation/stop_vitam.yml``)
- Exécuter le playbook :

  .. code-block:: bash

     ansible-playbook ansible-vitam-migration/migration_purge_storage_logs_secondary_sites.yml -i environments/hosts.{env} --ask-vault-pass

Vérification de la bonne migration des données
==============================================

Audit coherence
---------------

Il est recommandé de procéder à un audit de cohérence aléatoire suite à une procédure de montée de version VITAM ou de migration de données.
Pour ce faire, se référer au dossier d'exploitation (DEX) de la solution VITAM, section ``Audit de cohérence``.
