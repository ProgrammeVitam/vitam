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
