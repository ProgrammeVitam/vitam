.. _upgrade_r6_r7:

Migration R6 vers R7
####################



.. caution:: la migration n'est possible qu'en partant de la version la plus récente de la version "R6" (1.0.3).

Dans le cadre d'une montée de version de :term:`VITAM` depuis la version 1.0.3 (version la plus récente de la "R6"), il est nécessaire d'appliquer un `playbook` de migration de données.


Les commandes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass``

A l'issue de ce playbook, les timer systemD ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé :

    - d'arrêter les composants :term:`VITAM` "\*-external"
    - de ne lancer la procédure de migration qu'une fois s'être assuré qu'aucun `workflow` n'est actuellement en cours de traitement

Il faut alors procéder à la migration des données avec la commande suivante (sur le site primaire uniquement) :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r6_r7.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r6_r7.yml --ask-vault-pass``

.. warning:: Selon la volumétrie des données précédement chargées, le `playbook` peut durer jusqu'à plusieurs heures.

.. note:: Durant le temps des migrations, il est fortement recommandé de ne pas procéder à des injections de données. Le `playbook` se charge d'arrêter les composants "ingest-external" et "access-external".

A l'issue de la bonne exécution du `playbook`, il faut lancer la commande suivante pour réactiver les timers systemD sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si vault_pass.txt n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --ask-vault-pass``


A l'issue de la migration, si le `playbook` a correctement terminé, il est fortement conseillé de lancer un "Audit de cohérence" sur les différents tenants.
