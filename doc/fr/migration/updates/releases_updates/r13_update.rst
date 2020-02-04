Notes et procédures spécifiques R13
###################################

.. caution:: Rappel : la montée de version vers la *release* R13 s’effectue depuis la *release* R9 (LTS V2), la *release* R10 (V2, *deprecated*), la *release* R11 (V2, *deprecated*) ou la *release* R12 (V2) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 

Prérequis à la montée de version
================================

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

Pour effectuer cette modification, un script de migration est mis à disposition pour mettre en conformité votre "ancien" inventaire dans une forme compatible avec les outils de déploiement de la *release* 12.

La commande à lancer est ::

   cd deployment
   ./upgrade_inventory.sh ${fichier_d_inventaire}

Montée de version
=================

La montée de version vers la *release* R12 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`.

.. note:: Rappel : avant de procéder à la montée de version, on veillera tout particulièrement à la bonne mise en place des *repositories* :term:`VITAM` associés à la nouvelle version. Se reporter à la section du :term:`DIN` sur la mise en place des *repositories* :term:`VITAM`.

.. caution:: À l'issue de l'exécution du déploiement de Vitam, les composants *externals* ainsi que les *timers* systemd seront redémarrés. Il est donc recommandé de jouer les étapes de migration suivantes dans la foulée.

Etapes de migration
===================

Dans le cadre d'une montée de version R12 vers R13, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de réinstallation de la solution logicielle :term:`VITAM`.

Mise à jour des métadonnées de reconstruction dans mongo data
-------------------------------------------------------------

Le `playbook` ajoute dans les données des collections `Offset` des bases `masterdata`, `logbook` et `metadata` du site secondaire la valeur ` "strategy" : "default" `.

La migration s'effectue, uniquement sur le site secondaire, à l'aide de la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r12_r13_upgrade_offset_strategy.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r12_r13_upgrade_offset_strategy.yml --ask-vault-pass``
