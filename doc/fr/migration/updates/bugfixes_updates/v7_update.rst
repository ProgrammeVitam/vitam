Notes et procédures spécifiques V7
##################################

Procédures à exécuter APRÈS la montée de version
================================================

Migration des mappings elasticsearch
------------------------------------

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration depuis une version 7.0.1- (v7.0.1 ou inférieur) vers une version 7.0.2+ (7.0.2 ou supérieure).

Cette migration de données consiste à mettre à jour le modèle d'indexation elasticsearch-data.

Elle est réalisée en exécutant la procédure suivante sur **tous les sites** (primaire et secondaire(s)) :

- Lancement de la migration du modèles d'indexation des métadonnées sur elasticsearch-data :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_elasticsearch_mapping.yml --ask-vault-pass

..
