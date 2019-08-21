Notes et procédures spécifiques R11
###################################

.. caution:: Rappel : la montée de version vers la *release* R11 s’effectue depuis la *release* R9 (LTS V2) ou la *release* R10 (V2, *deprecated*) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 

Prérequis à la montée de version
================================

Gestion du référentiel ontologique 
-----------------------------------

.. caution:: En lien avec la User Story #5928 (livrée avec la *release* R11) et les changements de comportement de l'API d'import des ontologies associés, si un référentiel ontologique personnalisé est utilisé avec la solution logicielle :term:`VITAM`, il faut impérativement, lors d'une montée de version vers la *release* R11 ou supérieure, modifier manuellement le fichier d'ontologie livré par défaut avant toute réinstallation afin d'y réintégrer les modifications. A défaut, l'ontologie sera remplacée en mode forcé (sans contrôle de cohérence). 

Il faut pour celà éditer le fichier situé à l'emplacement ``deployment/ansible-vitam/roles/init_contexts_and_security_profiles/files/VitamOntology.json`` afin d'y réintégrer les éléments du référentiel ontologique personnalisés.  

.. note:: Lors de la montée de version, une sauvegarde du référentiel ontologique courant est réalisée à l'emplacement ``environments/backups/ontology_backup_<date>.json`` 

Arrêt des *timers* systemd
--------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les *timers* systemd ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé de ne lancer la procédure de migration qu'après s'être assuré que plus aucun `workflow` n'est ni en cours, ni en statut **FATAL**. 

.. seealso:: Se référer au chapitre "Suivi des Workflows" du :term:`DEX`, pour plus d'informations sur la façon de vérifier l'état des statuts des *workflows*.

Arrêt des composants *externals*
---------------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les composants *externals* ont été arrêtés, afin de ne pas perturber la migration.

Montée de version
=================

La montée de version vers la *release* R11 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`. 

.. note:: Rappel : avant de procéder à la montée de version, on veillera tout particulièrement à la bonne mise en place des *repositories* :term:`VITAM` associés à la nouvelle version. Se reporter à la section du :term:`DIN` sur la mise en place des *repositories* :term:`VITAM`. 

.. caution:: À l'issue de l'exécution du déploiement de Vitam, les composants *externals* ainsi que les *timers* systemd seront redémarrés. Il est donc recommandé de jouer les étapes de migration suivantes dans la foulée.  

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
