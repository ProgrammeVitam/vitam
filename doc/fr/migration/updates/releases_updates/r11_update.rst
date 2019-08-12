Notes et procédures spécifiques R11
###################################

.. caution:: Rappel : la montée de version vers la *release* R11 s'effectue depuis la *release* R9 et doit être réalisée en s'appuyant sur les dernières versions *bugfixes* publiées. 

Prérequis à la montée de version
================================

Sans objet. 

Montée de version
=================

La montée de version vers la *release* R11 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`. 

Etapes de migration 
===================

Migration des données de certificats
------------------------------------

La version R11 apporte une modification quant à la déclaration des certificats. En effet, un bug empêchait l'intégration dans la solution :term:`VITAM` de certificats possédant un serial number long. 

La commande suivante est à exécuter depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/R11_upgrade_serial_number.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/R11_upgrade_serial_number.yml --ask-vault-pass``

Migration des contrats d'entrée
-------------------------------

La montée de version vers la *release* R11 requiert une migration de données (contrats d'entrée) suite à une modification sur les droits relatifs aux rattachements. Cette migration s'effectue à l'aide du playbook :


``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r9_r11_ingestcontracts.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r9_r11_ingestcontracts.yml --ask-vault-pass``

Le template ``upgrade_contracts.js`` contient : 

.. literalinclude::  ../../../../../deployment/ansible-vitam-exploitation/roles/upgrade_R10_contracts/templates/upgrade_contracts.js.j2
   :language: javascript

Réindexation ES Data
--------------------

La montée de version vers la *release* R11 requiert une réindexation totale d'ElasticSearch. Cette réindexation s'effectue à l'aide du playbook :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml  --ask-vault-pass --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml``

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de vérifier que la procédure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être réalisée manuellement.

Vérification de la bonne migration des données
----------------------------------------------

A l'issue de la migration, il est fortement conseillé de lancer un "Audit de cohérence" sur les différents tenants. Pour rappel du :term:`DEX`, pour lancer un audit de cohérence, il faut lancer le *playbook* comme suit :

   ansible-playbook -i <inventaire> ansible-playbok-exploitation/audit_coherence.yml --ask-vault-pass -e "access_contract=<contrat multitenant>"

Ou, si un fichier vault-password-file existe ::

    ansible-playbook -i <inventaire> ansible-playbok-exploitation/audit_coherence.yml --vault-password-file vault_pass.txt -e "access_contract=<contrat multitenant>"

.. note:: L'audit est lancé sur tous les *tenants* ; cependant, il est nécessaire de donner le contrat d'accès adapté. Se rapprocher du métier pour cet *id* de contrat. Pour limiter la liste des *tenants*, il faut rajouter un *extra var* à la ligne de commande ansible. Exemple ::

   -e vitam_tenant_ids=[0,1]

   pour limiter aux `tenants` 0 et 1.
