Notes et procédures spécifiques R12
###################################

.. caution:: Rappel : la montée de version vers la *release* R11 s’effectue depuis la *release* R9 (LTS V2) ou la *release* R10 (V2, *deprecated*) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 

Prérequis à la montée de version
================================

Mise à jour de l'inventaire
----------------------------

Les versions récentes de ansible préconisent de ne plus utiliser le caractère "-" dans les noms de groupes ansible.

Pour effectuer cette modification, un script de migration est mis à disposition pour mettre en conformité votre "ancien" inventaire dans une forme compatible avec les outils de déploiement de la *release* 12.

La commande à lancer est ::

   cd deployment
   ./upgrade_inventory.sh ${fichier_d_inventaire}


Montée de version MongoDB 4.0 vers 4.2
--------------------------------------

La montée de version R9 (ou R10 ou R11) vers R12 comprend une montée de version de la bases de données MongoDB de la version 4.0 à la version 4.2. 

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

* Arrêt de :term:`VITAM` (`playbook` ``ansible-vitam-exploitation/stop_vitam.yml``)

.. warning:: A partir de là, la solution logicielle :term:`VITAM` est arrêtée ; elle ne sera redémarrée qu'au déploiement de la nouvelle version.

* Démarrage des différents cluster mongodb (playbook ``ansible-vitam-exploitation/start_mongodb.yml``)
* Upgrade de mongodb en version 4.2 (`playbook` ``ansible-vitam-exploitation/migration_mongodb_42.yml``)

Montée de version
=================

La montée de version vers la *release* R12 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`.

.. note:: Rappel : avant de procéder à la montée de version, on veillera tout particulièrement à la bonne mise en place des *repositories* :term:`VITAM` associés à la nouvelle version. Se reporter à la section du :term:`DIN` sur la mise en place des *repositories* :term:`VITAM`.

.. caution:: À l'issue de l'exécution du déploiement de Vitam, les composants *externals* ainsi que les *timers* systemd seront redémarrés. Il est donc recommandé de jouer les étapes de migration suivantes dans la foulée.

Etapes de migration
===================

Dans le cadre d'une montée de version R11 vers R12, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de réinstallation de la solution logicielle :term:`VITAM`.

Nettoyage des DIPs depuis les offres
------------------------------------

La migration s'effectue, uniquement sur le site principal, à l'aide de la commande suivante :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r11_r12_dip_cleanup.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r11_r12_dip_cleanup.yml --ask-vault-pass``

.. warning:: Selon la volumétrie des données précédement chargées, le `playbook` peut durer quelques minutes.
