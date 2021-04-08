Notes et procédures spécifiques R15
###################################

.. |repertoire_inventory| replace:: ``environments``


Étapes préalables à la montée de version
========================================

Déplacement des paramètres relatif à la gestion des tenants
-----------------------------------------------------------

Dans le cadre de la nouvelle fonctionnalité permettant de regrouper les tenants, les paramètres suivants ont étés déplacés du fichier d'inventaire au fichier ``group_vars/all/tenants_vars.yml``
  - vitam_tenant_ids
  - vitam_tenant_admin

Il faut supprimer les valeurs précédentes de votre fichier d'inventaire et les reporter dans le fichier tenants_vars.yml en respectant la syntaxe yml adéquate.

.. seealso:: Se référer à la documentation d'installation pour plus d'informations concernant le fichier |repertoire_inventory| ``/group_vars/all/tenants_vars.yml``


Vérification de la bonne migration des données
==============================================

Audit coherence
---------------

Il est recommandé de procéder à un audit de cohérence aléatoire suite à une procédure de montée de version VITAM ou de migration de données.
Pour ce faire, se référer au dossier d'exploitation (DEX) de la solution VITAM, section ``Audit de cohérence``.
