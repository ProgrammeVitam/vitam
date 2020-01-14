Notes et procédures spécifiques R12
###################################

.. caution:: Rappel : la montée de version vers la *release* R13 s’effectue depuis la *release* R9 (LTS V2), la *release* R10 (V2, *deprecated*), la *release* R11 (V2, *deprecated*) ou la *release* R12 (V2, *deprecated*) et doit être réalisée en s’appuyant sur les dernières versions *bugfixes* publiées. 


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
