Notes et procédures spécifiques R13
###################################

.. caution:: Rappel : la montée de version vers la *release* R13 s’effectue depuis la *release* R9 (LTS V2), la *release* R10 (V2, *deprecated*), la *release* R11 (V2, *deprecated*) ou la *release* R12 (V2) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 

.. note:: Cette *release* de la solution logicielle :term:`VITAM` s'appuie sur la version 11 de Java ainsi que la version 7 d'Elasticsearch. Ces mises à jour sont prises en charge par le processus de montée de version. 

.. caution:: La montée de version vers la *release* R13 a été validée par :term:`VITAM` dans le cadre d'une installation de type Centos 7. L'installation dite *from scracth* a quant à elle été validée pour les installations de type Centos 7 et Debian 10 (l'utilisation de la version 11 de Java impose en effet une installation de type Debian 10). La migration d'OS vers la version Debian 10 n'est pas supportée par :term:`VITAM` dans le cadre de la montée de version vers la *release* R13. 

Étapes préalables à la montée de version
========================================

Gestion du référentiel ontologique 
-----------------------------------

.. caution:: En lien avec la *User Story* #5928 (livrée avec la *release* R11) et les changements de comportement de l'API d'import des ontologies associés, si un référentiel ontologique personnalisé est utilisé avec la solution logicielle :term:`VITAM`, il faut impérativement, lors d'une montée de version vers la *release* R11 ou supérieure, modifier manuellement le fichier d'ontologie livré par défaut avant toute réinstallation afin d'y réintégrer les modifications. A défaut, l'ontologie sera remplacée en mode forcé (sans contrôle de cohérence). 

Il faut pour cela éditer le fichier situé à l'emplacement ``deployment/ansible-vitam/roles/init_contexts_and_security_profiles/files/VitamOntology.json`` afin d'y réintégrer les éléments du référentiel ontologique personnalisés.  

.. note:: Lors de la montée de version, une sauvegarde du référentiel ontologique courant est réalisée à l'emplacement ``environments/backups/ontology_backup_<date>.json`` 

Gestion du référentiel des formats 
-----------------------------------

.. caution:: Si un référentiel des formats personnalisé est utilisé avec la solution logicielle :term:`VITAM`, il faut impérativement, lors d'une montée de version, modifier manuellement le fichier des formats livré par défaut avant toute réinstallation afin d'y réintégrer les modifications. A défaut, le référentiel des formats sera réinitialisé. 

Il faut pour cela éditer le fichier situé à l'emplacement ``environments/DROID_SignatureFile_<version>.xml`` afin d'y réintégrer les éléments du référentiel des formats personnalisés.  

Mise à jour de l'inventaire
----------------------------

Les versions récentes de ansible préconisent de ne plus utiliser le caractère "-" dans les noms de groupes ansible.

Pour effectuer cette modification, un script de migration est mis à disposition pour mettre en conformité votre "ancien" inventaire dans une forme compatible avec les outils de déploiement de la *release* R12.

La commande à lancer est ::

   cd deployment
   ./upgrade_inventory.sh ${fichier_d_inventaire}

Arrêt des *timers* systemd
--------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les *timers* systemd ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé de ne lancer la procédure de migration qu'après s'être assuré que plus aucun `workflow` n'est ni en cours, ni en statut **FATAL**. 

Arrêt des composants *externals*
---------------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les composants *externals* ont été arrêtés, afin de ne pas perturber la migration.

Montée de version MongoDB 4.0 vers 4.2
--------------------------------------

La montée de version vers la *release* R12 comprend une montée de version de la bases de données MongoDB de la version 4.0 à la version 4.2. 

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

* Arrêt de :term:`VITAM` (`playbook` ``ansible-vitam-exploitation/stop_vitam.yml``)

.. warning:: A partir de là, la solution logicielle :term:`VITAM` est arrêtée ; elle ne sera redémarrée qu'au déploiement de la nouvelle version.

* Démarrage des différents cluster mongodb (playbook ``ansible-vitam-exploitation/start_mongodb.yml``)
* Upgrade de mongodb en version 4.2 (`playbook` ``ansible-vitam-exploitation/migration_mongodb_42.yml``)

Arrêt de l'ensemble des composants :term:`VITAM`
------------------------------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné : 

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les composants :term:`VITAM` ont été arrêtés, afin de ne pas perturber la migration.

Montée de version
=================

La montée de version vers la *release* R13 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`.

.. note:: Rappel : avant de procéder à la montée de version, on veillera tout particulièrement à la bonne mise en place des *repositories* :term:`VITAM` associés à la nouvelle version. Se reporter à la section du :term:`DIN` sur la mise en place des *repositories* :term:`VITAM`.

.. caution:: À l'issue de l'exécution du déploiement de Vitam, les composants *externals* ainsi que les *timers* systemd seront redémarrés. Il est donc recommandé de jouer les étapes de migration suivantes dans la foulée.

Etapes de migration
===================

Migration des données de certificats
------------------------------------

La *release* R11 apporte une modification quant à la déclaration des certificats. En effet, un bug empêchait l'intégration dans la solution :term:`VITAM` de certificats possédant un serial number long. 

La commande suivante est à exécuter depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/R10_upgrade_serial_number.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/R10_upgrade_serial_number.yml --ask-vault-pass``

Migration des contrats d'entrée
-------------------------------

La montée de version vers la *release* R11 requiert une migration de données (contrats d'entrée) suite à une modification sur les droits relatifs aux rattachements. Cette migration s'effectue à l'aide du playbook :


``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r9_r10_ingestcontracts.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r9_r10_ingestcontracts.yml --ask-vault-pass``

Le template ``upgrade_contracts.js`` contient : 

.. literalinclude::  ../../../../../deployment/ansible-vitam-exploitation/roles/upgrade_R10_contracts/templates/upgrade_contracts.js.j2
   :language: javascript

Nettoyage des DIPs depuis les offres
------------------------------------

Dans le cadre d'une montée de version vers la *release* R12, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de réinstallation de la solution logicielle :term:`VITAM`.

La migration s'effectue, uniquement sur le site principal, à l'aide de la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r11_r12_dip_cleanup.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r11_r12_dip_cleanup.yml --ask-vault-pass``

.. warning:: Selon la volumétrie des données précédement chargées, le `playbook` peut durer quelques minutes.

Réindexation ES Data
--------------------

La montée de version vers la *release* R11 requiert une réindexation totale d'ElasticSearch. Cette réindexation s'effectue à l'aide du playbook :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml  --ask-vault-pass``

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de vérifier que la procédure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être réalisée manuellement.

Mise à jour des métadonnées de reconstruction (cas d'un site secondaire)
------------------------------------------------------------------------

Dans le cadre d'une montée de version vers R13 sur un site secondaire, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de réinstallation de la solution logicielle :term:`VITAM`.

Le `playbook` ajoute dans les données des collections `Offset` des bases `masterdata`, `logbook` et `metadata` du site secondaire la valeur ` "strategy" : "default" `.

La migration s'effectue, uniquement sur le site secondaire, à l'aide de la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r12_r13_upgrade_offset_strategy.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r12_r13_upgrade_offset_strategy.yml --ask-vault-pass``

Vérification de la bonne migration des données
----------------------------------------------

A l'issue de la migration, il est fortement conseillé de lancer un "Audit de cohérence" sur les différents tenants. Pour rappel du :term:`DEX`, pour lancer un audit de cohérence, il faut lancer le *playbook* comme suit :

   ansible-playbook -i <inventaire> ansible-playbok-exploitation/audit_coherence.yml --ask-vault-pass -e "access_contract=<contrat multitenant>"

Ou, si un fichier vault-password-file existe ::

    ansible-playbook -i <inventaire> ansible-playbok-exploitation/audit_coherence.yml --vault-password-file vault_pass.txt -e "access_contract=<contrat multitenant>"

.. note:: L'audit est lancé sur tous les *tenants* ; cependant, il est nécessaire de donner le contrat d'accès adapté. Se rapprocher du métier pour cet *id* de contrat. Pour limiter la liste des *tenants*, il faut rajouter un *extra var* à la ligne de commande ansible. Exemple ::

   -e vitam_tenant_ids=[0,1]

   pour limiter aux `tenants` 0 et 1.
