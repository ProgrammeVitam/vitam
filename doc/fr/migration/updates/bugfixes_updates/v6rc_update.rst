Notes et procédures spécifiques V6
##################################

Procédures à exécuter APRÈS la montée de version
================================================

Migration des mappings elasticsearch
------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 6.rc.3- (v6.rc.3 ou inférieur) vers une version 6.rc.4+ (6.rc.4 ou supérieure).

Cette migration de données consiste à mettre à jour le modèle d'indexation elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

- Les jobs Vitam et les services externals de Vitam doivent être arrêtés sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduling.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/stop_vitam_scheduler.yml --ask-vault-pass

..


- Réindexation des référentiels sur elasticsearch :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --ask-vault-pass --tags "securityprofile, context, ontology, ingestcontract, agencies, accessionregisterdetail, archiveunitprofile, accessionregistersummary, accesscontract, fileformat, filerules, profile, griffin, preservationscenario, managementcontract"

..

- Lancement de la migration du modèles d'indexation des métadonnées sur elasticsearch-data :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..

- Réactivation des services externals ainsi que les timers sur **tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduler.yml --ask-vault-pass
    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_vitam_scheduling.yml --ask-vault-pass

..
