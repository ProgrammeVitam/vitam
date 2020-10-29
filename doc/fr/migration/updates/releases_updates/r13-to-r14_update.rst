Notes et procédures spécifiques R14
###################################

.. caution:: Rappel : la montée de version vers la *release* R14 s’effectue depuis la *release* R13.

.. note:: Cette *release* de la solution logicielle :term:`VITAM` s'appuie sur la version 11 de Java ainsi que la version 7 d'Elasticsearch. Ces mises à jour sont prises en charge par le processus de montée de version. 

.. caution:: La montée de version vers la *release* R13 a été validée par :term:`VITAM` dans le cadre d'une installation de type Centos 7. L'installation dite *from scracth* a quant à elle été validée pour les installations de type Centos 7 et Debian 10 (l'utilisation de la version 11 de Java impose en effet une installation de type Debian 10). La migration d'OS vers la version Debian 10 n'est pas supportée par :term:`VITAM` dans le cadre de la montée de version vers la *release* R14. 

Étapes préalables à la montée de version - Recreation de R13
============================================================


Mise à jour de l'inventaire R13
--------------------------------

Les versions récentes de ansible préconisent de ne plus utiliser le caractère "-" dans les noms de groupes ansible.

Pour effectuer cette modification, un script de migration est mis à disposition.

La commande à lancer est ::

   cd deployment
   ./upgrade_inventory.sh


Recreation de l'environnement R13
---------------------------------

Script de creation d'environnement ::

   ansible-playbook playbooks_os/create_heat.yml -i <inventaire> -l vitam --vault-password-file password_file.txt -e createHeat=yes ..

Le script est à lancer afin de gérer la création des environnements. create_heat est une nouvelle méthode permettant l'optimisation de la création des environnements nous apportant un important gain de vitesse de création.


Configuration de l'environnement R13
-------------------------------------

Configuration niveau d'exploitation ::
      
   ansible-playbook playbooks_os/bootstrap.yml -i hosts -l vitam --private-key <path_to_private_key> -u centos --vault-password-file password_file.txt

   ansible-playbook playbooks_os/vitam_prereq.yml -i hosts -l vitam --private-key <path_to_private_key> -u centos --vault-password-file password_file.txt

Configuration : ntp, repositories, clé ssh, timezone, controle d'accès


Deployment preparation r13
---------------------------

Génération des certificats ::

   pki/scripts/generate_ca.sh
   
   pki/scripts/generate_certs.sh environments/hosts
   
   ./generate_stores.sh environments/hosts


Génération des variables des environnement ::

   ansible-playbook ansible-vitam/generate_hostvars_for_1_network_interface.yml -i environments/hosts --private-key <path_to_private_key>  -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Mise en place de la configuartion réseaux


Mis à jour des repositories ::

   ./check-repositories.py
   
   ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Mis à jour des repositories servant à gérer la récupération des packages


Vitam deployment r13
---------------------

Configuration du repositorie VITAM ::

   ansible-playbook ansible-vitam-extra/browser.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Déploiement de la version R13 de vitam ::

   ansible-playbook ansible-vitam/vitam.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Reverse proxy pour les composants VITAM ::

   ansible-playbook ansible-vitam-extra/reverse.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Vitam-extra deployment r13
--------------------------

Ce playbook a pour but d'importer un ensemble de playbook pour la solution VITAM ::

   ansible-playbook ansible-vitam-extra/extra.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Installation check r13
----------------------

Ce playbook permet de vérifier le bon fonctionnement de VITAM et ses services installés ::

   ansible-playbook ansible-vitam-exploitation/status_vitam.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml



TNR execution r13
-----------------

Execution des tests ::

   ansible-playbook ansible-vitam-extra/load_tnr.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Get p12 for R13 audit
---------------------

Récupérer les clés de cryptage p12 pour l'audit de cohérence ::
   
   ansible-playbook getKeystoreforAudit.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Audit coherence r13
-------------------

Cette commande réalise un audit sur les tenants et leur contrat d'accès associés ::

   ansible-playbook ansible-vitam-exploitation/audit_coherence.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e access_contract=ContratTNR -e tenants=[0,1] --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml



Étapes préalables à la montée de version - Gestion de l'arrêt de R13
====================================================================


Arrêt des *timers* systemd
--------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur la version VITAM ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --private-key <path_to_private_key> --vault-password-file vault_pass.txt

ou, si ``vault_pass.txt`` n'a pas été renseigné ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass

A l'issue de l'exécution du `playbook`, les *timers* systemd ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé de ne lancer la procédure de migration qu'après s'être assuré que plus aucun `workflow` n'est ni en cours, ni en statut **FATAL**. 


Arrêt des composants *externals*
---------------------------------

Les commandes suivantes sont aussi à lancer depuis le répertoire ``deployment`` ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --private-key <path_to_private_key> --vault-password-file vault_pass.txt

ou, si ``vault_pass.txt`` n'a pas été renseigné ::

   `ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass

A l'issue de l'exécution du `playbook`, les composants *externals* ont été arrêtés, afin de ne pas perturber la migration.


Arrêt de l'ensemble des composants :term:`VITAM`
------------------------------------------------

Les commandes suivantes sont aussi à lancer depuis le répertoire ``deployment``.

Tout d'abord il y l'arrêt des applications vitam ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_apps.yml --private-key <path_to_private_key>  --vault-password-file vault_pass.txt

ou, si ``vault_pass.txt`` n'a pas été renseigné :: 

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_apps.yml --ask-vault-pass

A l'issue de l'exécution du `playbook`, les composants :term:`VITAM` ont été arrêtés, afin de ne pas perturber la migration.


Ensuite il y a l'arrêt des applications et services de supervision des environnements ::

   ansible-playbook ansible-vitam-exploitation/stop_vitam_admin.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt

ou, si ``vault_pass.txt`` n'a pas été renseigné :: 

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_admin.ymll --ask-vault-pass

A l'issue de l'exécution du `playbook`, les composants du domaine de supervision :term:`VITAM` ont été arrêtés, afin de ne pas perturber la migration.


La sauvegarde des éléments R13
------------------------------

La sauvegarde des éléments R13 se traduit par la récupération des tests TNR effectués après le déploiement de R13.
   mkdir testTNRr13
   cp vitam.git/deployment/environments/TNR.xml testTNRr13



Montée de version
=================

La montée de version vers la *release* R14 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`.

.. note:: Rappel : avant de procéder à la montée de version, on veillera tout particulièrement à la bonne mise en place des *repositories* :term:`VITAM` associés à la nouvelle version. Se reporter à la section du :term:`DIN` sur la mise en place des *repositories* :term:`VITAM`.

.. caution:: À l'issue de l'exécution du déploiement de Vitam, les composants *externals* ainsi que les *timers* systemd seront redémarrés. Il est donc recommandé de jouer les étapes de migration suivantes dans la foulée.



Etapes de migration
===================


Mise à jour de l'inventaire R14
--------------------------------

Pour effectuer cette modification, un script de migration est mis à disposition.

Il faut avant ça mettre à jour l'environnement avec les informations associés à nouvelle version. Il faudra changer les version
   
Il faut récupérer les sources de la version R14, changer les version de branche et de griffins

La commande à lancer est ::

   cd deployment
   ./upgrade_inventory.sh


Recreation de l'environnement R14
---------------------------------

Script de creation d'environnement ::

   ansible-playbook playbooks_os/create_heat.yml -i <inventaire> -l vitam --vault-password-file password_file.txt -e createHeat=yes ..

Le script est à lancer afin de gérer la création des environnements. create_heat est une nouvelle méthode permettant l'optimisation de la création des environnements nous apportant un important gain de vitesse de création.


Configuration de l'environnement R14
-------------------------------------

Configuration niveau d'exploitation ::
      
   ansible-playbook playbooks_os/bootstrap.yml -i hosts -l vitam --private-key <path_to_private_key> -u centos --vault-password-file password_file.txt

   ansible-playbook playbooks_os/vitam_prereq.yml -i hosts -l vitam --private-key <path_to_private_key> -u centos --vault-password-file password_file.txt

Configuration : ntp, repositories, clé ssh, timezone, controle d'accès


Deployment preparation R14
---------------------------

Génération des certificats ::

   pki/scripts/generate_ca.sh
   pki/scripts/generate_certs.sh environments/hosts
   ./generate_stores.sh environments/hosts


Génération des variables des environnement ::

   ansible-playbook ansible-vitam/generate_hostvars_for_1_network_interface.yml -i environments/hosts --private-key <path_to_private_key>  -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Mise en place de la configuartion réseaux


Mis à jour des repositories ::

   ./check-repositories.py
   ansible-playbook ansible-vitam-extra/bootstrap.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Mis à jour des repositories servant à gérer la récupération des packages


Vitam deployment R14
---------------------

Configuration du repositorie VITAM ::

   ansible-playbook ansible-vitam-extra/browser.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Déploiement de la version R14 de vitam ::

   ansible-playbook ansible-vitam/vitam.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

Reverse proxy pour les composants VITAM ::

   ansible-playbook ansible-vitam-extra/reverse.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Vitam-extra deployment R14
--------------------------

Ce playbook a pour but d'importer un ensemble de playbook pour la solution VITAM ::

   ansible-playbook ansible-vitam-extra/extra.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Start Cerebro r14 - TEMP
-------------------------

Cette étape est temporaire. Elle a été mise en place suite a des problèmes de démarrage du service Cerebro ::

   ansible-playbook ansible-vitam-exploitation/start_vitam_admin.yml -i environments/hosts -l hosts_cerebro --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Installation check R14
----------------------

Ce playbook permet de vérifier le bon fonctionnement de VITAM et ses services installés ::

   ansible-playbook ansible-vitam-exploitation/status_vitam.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e confirmation=yes --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


TNR execution R14
-----------------

Execution des tests ::

   ansible-playbook ansible-vitam-extra/load_tnr.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Réindexation ES Data
--------------------

La montée de version vers la *release* R14 requiert une réindexation totale d'ElasticSearch. Cette réindexation s'effectue à l'aide du playbook ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --vault-password-file vault_pass.txt

ou, si ``vault_pass.txt`` n'a pas été renseigné ::

   ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml  --ask-vault-pass

.. note:: Ce `playbook` ne supprime pas les anciens indexes pour laisser à l'exploitant le soin de vérifier que la procédure de migration s'est correctement déroulée. A l'issue, la suppression des index devenus inutiles devra être réalisée manuellement.


Get p12 for R14 audit
---------------------

Récupérer les clés de cryptage p12 pour l'audit de cohérence ::
   
   ansible-playbook getKeystoreforAudit.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml


Audit coherence R14
-------------------

Cette commande réalise un audit sur les tenants et leur contrat d'accès associés ::

   ansible-playbook ansible-vitam-exploitation/audit_coherence.yml -i environments/hosts --private-key <path_to_private_key> -u centos --vault-password-file vault_pass.txt -e access_contract=ContratTNR -e tenants=[0,1] --extra-vars=@environments/vitam-pf-vars.yml --extra-vars=@environments/environment_vars.yml

