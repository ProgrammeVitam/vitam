Notes et procédures spécifiques R7
##################################

.. caution:: Rappel : la montée de version vers la release R7 s'effectue depuis la release R6 (V1, *deprecated*) et doit être réalisée en s'appuyant sur les dernières versions bugfixes publiées. 

Prérequis à la montée de version
================================

Montée de version Consul
------------------------

Le composant ``vitam-consul`` a été monté de version ; la procédure ci-dessous permet la mise en conformité des fichiers de configuration du service afin qu'ils soient compatibles avec la nouvelle version Consul. 

La montée de version Consul s'effectue à l'aide du `playbook` d'installation (:term:`VITAM` et/ou extra) au lancement duquel il est nécessaire d'ajouter la directive ``--tags consul_conf``. 

Exemples :

``ansible-playbook ansible-vitam/vitam.yml -i environments/<ficher d'inventaire> --vault-password-file vault_pass.txt --tags consul_conf``

``ansible-playbook ansible-vitam-extra/extra.yml -i environments/<ficher d'inventaire> --ask-vault-pass --tags consul_conf``

A l'issue de l'exécution du `playbook`, s'assurer que la montée de version Consul a été réalisée avec succès en vérifiant que l'ensemble des services Consul est à l'état **OK**. 

Reprise des données des contextes applicatifs
---------------------------------------------

En prérequis, il est également nécéssaire d'effectuer une reprise des données des contextes applicatifs (base de données MongoDB masterdata, collection ``Context``). 

Deux champs liés aux contextes applicatifs ont été mis à jour en version R7 et doivent être migrés avant le déploiement de la nouvelle version de la solution logicielle :term:`VITAM`.

Sous ``deployment``, exécuter la commande suivante :

``ansible-playbook ansible-vitam-exploitation/preinstall_r7.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook ansible-vitam-exploitation/preinstall_r7.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, si le `playbook` ne remonte pas d'erreur, la reprise des contextes applicatifs a été réalisée avec succès. 

Montée de version
=================

La montée de version vers la release R7 est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux *playbooks* ansible fournis, et selon la procédure d'installation classique décrite dans le :term:`DIN`. 

Etapes de migration 
===================

Dans le cadre d'une montée de version R6 vers R7, il est nécessaire d'appliquer un `playbook` de migration de données à l'issue de la réinstallation de la solution logicielle :term:`VITAM`. Ceci est dû à des changements de modèles de données suite à la mise en place de l’ontologie. 

Avant de procéder à la migration
--------------------------------

Les commandes suivantes sont à lancer depuis le répertoire ``deployment`` sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_timers.yml --ask-vault-pass``

A l'issue de l'exécution du `playbook`, les *timers* systemd ont été arrêtés, afin de ne pas perturber la migration.

Il est également recommandé de ne lancer la procédure de migration qu'après s'être assuré que plus aucun `workflow` n'est en cours de traitement. 

Procédure de migration des données
----------------------------------

La migration des données est réalisée en exécutant la commande suivante (sur le site primaire uniquement, dans le cas d'une installation multi-sites) :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r6_r7.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/migration_r6_r7.yml --ask-vault-pass``

.. warning:: Selon la volumétrie des données précédement chargées, le `playbook` peut durer jusqu'à plusieurs heures.

.. note:: Durant la migration, il est fortement recommandé de ne pas procéder à des versements de données. En effet, le `playbook` se charge d'arrêter les composants ``ingest-external`` et ``access-external``, avant de réaliser les opérations de migration de données, puis de redémarrer les composants ``ingest-external`` et ``access-external``.

Les opérations de migration réalisées portent, entre autres, sur les éléments suivants :

    - Graph / SEDA
    - Mise à jour d'un champ des contextes applicatifs
    - Réindexations Elasticsearch

Après la migration
------------------

Exécuter la commande suivante afin de réactiver les timers systemd sur les différents sites hébergeant la solution logicielle :term:`VITAM` :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --vault-password-file vault_pass.txt``

ou, si ``vault_pass.txt`` n'a pas été renseigné :

``ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_timers.yml --ask-vault-pass``

A l’issue de l’exécution du `playbook`, les *timers* systemd ont été redémarrés. 

Vérification de la bonne migration des données
----------------------------------------------

A l'issue de la migration, il est fortement conseillé de lancer un "Audit de cohérence" sur les différents *tenants*. 