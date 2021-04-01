Notes et procédures spécifiques R16
###################################

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
il faut impérativement, lors d'une montée de version, vérifier tous les types de règles de gestion existants sur Mongo et ES et les modifier en cas d'incohérence.


Vérification de la bonne migration des données
==============================================

Audit coherence
---------------

Il est recommandé de procéder à un audit de cohérence aléatoire suite à une procédure de montée de version VITAM ou de migration de données.
Pour ce faire, se référer au dossier d'exploitation (DEX) de la solution VITAM, section ``Audit de cohérence``.
