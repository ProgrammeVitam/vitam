Notes et procédures spécifiques V8.0
####################################

.. caution:: Veuillez appliquer les procédures spécifiques à chacune des versions précédentes en fonction de la version de départ selon la suite suivante: V6 -> V7.0 -> V7.1 -> V8.0

Adaptation des sources de déploiement ansible
=============================================

Configuration des jobs d'audit
-----------------------------------------------

Il est maintenant possible de configurer les jobs d'audit d'intégrité et d'existence par tenant, et par conséquent la définition des jobs d'audit a changé.

L'ancienne configuration définissait la fréquence d'exécution et les paramètres des jobs dans le fichier ``deployment/environments/group_vars/all/advanced/vitam_vars.yml`` :

.. code-block:: yaml

vitam_timers:
  functional_administration:
    frequency_integrity_audit: "0 0 0 1 JAN ? 2020"
    frequency_existence_audit: "0 0 0 1 JAN ? 2020"

scheduler:
  job_parameters:
    integrity_audit:
      operations_delay_in_minutes: 1440
    existence_audit:
      operations_delay_in_minutes: 1440

..

La nouvelle configuration n'utilise plus les valeurs de ``vitam_timers.functional_administration.frequency_integrity_audit`` et ``vitam_timers.functional_administration.frequency_existence_audit``, et prend la forme suivante :

.. code-block:: yaml

scheduler:
  job_parameters:
    integrity_audit:
      - key: SYSTEM
        selected_tenants: [1]
        operations_delay_in_minutes: 1440
        frequency: "0 0 2 ? * * *" # Every day at 2am
      - key: TENANT2
        selected_tenants: [2]
        operations_delay_in_minutes: 1440
        frequency: "0 0 4 ? * * *" # Every day at 4am
    existence_audit:
      - key: SYSTEM
        selected_tenants: [1,2]
        operations_delay_in_minutes: 1440
        frequency: "0 0 6 ? * * *" # Every day at 6am

..

Pour ``integrity_audit`` et ``existence_audit``, il conviendra de définir une entrée par tenant ou groupe de tenant devant s'exécuter à la même fréquence. Les différents champs sont décrits ci-dessous :

Le paramètre ``key`` doit être unique et sera ajouté au nom du job. Il doit donc être de forme alphanumérique, sans espaces.

Le paramètre ``selected_tenants`` liste les identifiants de tenants auquel la fréquence s'applique. S'il est vide ou non renseigné, la fréquence s'appliquera à tous les tenants.

Le paramètre ``operations_delay_in_minutes`` correspond au paramètre de la version précédente.

Le paramètre ``frequency`` correspond aux fréquences précédemment définies dans les paramètres ``vitam_timers.functional_administration.frequency_integrity_audit`` et ``vitam_timers.functional_administration.frequency_existence_audit``.

Déploiement des exporters pour VitamUI
--------------------------------------

Si vous utilisez VitamUI et que vous souhaitez déployer les exporters permettant de mettre en place la remontée de métriques dans Prometheus, vous devrez ajouter les groupes suivants au fichier d'inventaire (cf. inventaire d'exemple: ``environments/hosts.example``):

.. code-block:: ini

[hosts:children]
hosts_vitamui

################################################################################
# ZONE VITAMUI
################################################################################
[hosts_vitamui]
# optional: To deploy exporters on VitamUI


[hosts_vitamui:children]
hosts_vitamui_mongod

[hosts_vitamui_mongod]
# optional: To deploy mongodb-exporter on VitamUI

..

Pour permettre de déployer l'exporter mongodb, vous devrez ajouter au fichier ``environments/group_vars/all/main/vault-vitam.yml`` les paramètres associés à la BDD mongo-vitamui:

.. code-block:: yaml

mongodb:
  mongo-vitamui:
    mongod_port: 27017
    admin:
      user: vitamdb-admin
      password: admin1234

..

Avant de pouvoir exécuter le playbook de configuration des exporters pour VitamUI, il faudra au préalable récupérer les host_vars à l'aide du playbook ``ansible-vitam-exploitation/generate_hostvars_for_2_network_interfaces.yml`` ou ``ansible-vitam-exploitation/generate_hostvars_for_1_network_interface.yml``.

Ensuite, ces exporters seront déployés à l'aide du playbook: ``ansible-vitam-extra/vitamui_prometheus.yml``.

Procédures à exécuter AVANT la montée de version
================================================

Arrêt des timers et des accès externes à Vitam
----------------------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V8.0.

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Les timers et les externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..

Mise à jour des dépôts (YUM/APT)
--------------------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version

Afin de pouvoir déployer la nouvelle version, vous devez mettre à jour la variable ``vitam_repositories`` sous ``environments/group_vars/all/main/repositories.yml`` afin de renseigner les dépôts à la version cible.

Pour le dépôt vitam-external, vous devez renseigner la version adaptée à votre système d'exploitation (par exemple pour la version 8.0.0):

* AlmaLinux 9: https://download.programmevitam.fr/vitam_repository/8.0.0/rpm/vitam-external/9/
* Debian 12: https://download.programmevitam.fr/vitam_repository/8.0.0/deb/vitam-external/12/

Puis exécutez le playbook suivant **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/bootstrap.yml --ask-vault-pass

..

Mise à jour de MongoDB 7.0.28
-----------------------------

.. caution:: **Attention**
    Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V8.0.
    Cette opération est à effectuer si vous venez des versions de Vitam suivante: V7.1.4-.
    Il est recommandé d'effectuer un backup des bases de données à l'aide de mongodump avant de poursuivre.

Exécutez le playbook suivant à partir de l'ansiblerie de la V8.0 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_70.yml --ask-vault-pass

..

Arrêt complet de Vitam
----------------------

.. caution:: Cette opération doit être effectuée AVANT la montée de version vers la V8.0

.. caution:: Cette opération doit être effectuée avec les sources de déploiements de l'ancienne version.

Vitam doit être arrêté sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam.yml --ask-vault-pass

..

Application de la montée de version
===================================

.. caution:: L'application de la montée de version s'effectue d'abord sur les sites secondaires puis sur le site primaire.

Lancement du master playbook vitam
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam/vitam.yml --ask-vault-pass

..

Lancement du master playbook extra
----------------------------------

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-extra/extra.yml --ask-vault-pass

..

Procédures à exécuter APRÈS la montée de version
================================================

Migration des mappings elasticsearch pour les métadonnées
---------------------------------------------------------

Cette migration de données consiste à mettre à jour le modèle d'indexation des métadonnées sur elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..

Migration des profiles
----------------------

.. caution:: Cette migration est à effectuer APRÈS la montée de version vers Vitam V8.0 et uniquement sur **site primaire**.

Ce playbook de migration a pour but de mettre à jour les notices en y intégrant la notion de **SedaVersion**.

Le script effectue les actions suivantes :
- Définit par défaut la version **SEDA 2.3** pour les AUP (*Archive Unit Profile*).
- Si un fichier est présent pour les AP (*Archive Profile*), il tente de déterminer la version à partir de celui-ci. Dans le cas contraire, la version par défaut sera appliquée.

Pour lancer ce playbook, utilisez la commande suivante :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migrate_profiles_v7.1_to_v8.0.yml --ask-vault-pass

Migration unités archivistiques avec demande de transfert non complétée
-----------------------------------------------------------------------

Cette migration permet de corriger des erreurs de sécurisation ou d'audit sur des unités archivistiques pour lesquelles une demande de transfert a été initiée, mais dont le transfert effectif n'a pas été finalisée.

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration majeure vers Vitam V8.0+ depuis :
    - Une version 7.1.1 ou inférieure
    - Une version 7.0.1 ou inférieure
    - Une version 6.3-1 ou inférieure

.. caution:: Cette migration est à effectuer **APRES** l'installation et uniquement sur le **site primaire**.

.. caution:: Dans le cas où un tenant comporte un très grand nombre d'unités archivistiques dont la demande de transfert a été initiée pour un tenant donné (plus de 100 000 unités archivistiques), la migration ne pourra alors s'opérer. Le support Vitam devra alors être contacté.

Pour lancer la migration, le playbook suivant doit être exécuté :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_metadata_transfer_request_traceability.yml --ask-vault-pass
