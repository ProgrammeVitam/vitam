Notes et procédures spécifiques R16
###################################

Supprimer les indexes de configuration kibana
---------------------------------------------

.. caution:: Cette opération doit être effectuée avant la montée de version vers la R16 sur l'ensemble des sites.

.. caution:: Sans cette opération, l'installation kibana est bloquée et arrête l'installation de Vitam

Lors de la montée de version ELK, les indices de configuration kibana : .kibana et .kibana_task_manager persistent avec une version et des informations incorrectes (celles de la version d'avant). Il est nécessaire des les effacer; autrement la montée de version est bloquée.

- Arrêt de Kibana (ne doit pas être relancé avant la fin de la procédure de montée de version).

.. code-block:: bash

  ansible hosts_kibana_log,hosts_kibana_data --vault-password-file vault_pass.txt -v -a "systemctl stop kibana" -i environments/<inventaire>

..

- Supprimer les indexes `.kibana*` via cerebro dans les clusters `elasticsearch-log` & `elasticsearch-data`.

Attention, ils faut cocher la case `.special` dans l'interface pour les voir.

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


Vérification de la bonne migration des données
==============================================

Audit coherence
---------------

Il est recommandé de procéder à un audit de cohérence aléatoire suite à une procédure de montée de version VITAM ou de migration de données.
Pour ce faire, se référer au dossier d'exploitation (DEX) de la solution VITAM, section ``Audit de cohérence``.
