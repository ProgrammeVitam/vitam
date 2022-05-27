Notes et procédures spécifiques V6RC
<<<<<<< HEAD
##################################

.. caution:: Pour une montée de version depuis la V5RC de Vitam, veuillez appliquer les procédures spécifiques de la V5 en complément des procédures suivantes. Pour une montée de version depuis la V5, vous pouvez appliquer la procédure suivante directement.

Migration des groupes d'objets
-----------------------------------

.. caution:: Cette migration doit être effectuée après la montée de version V6RC mais avant la réouverture du service aux utilisateurs.

Cette migration de données consiste à :

- Ajouter les champs ``_acd`` (date de création approximative) et ``_aud`` (date de modification approximative) dans la collection ObjectGroup.

Elle est réalisée en exécutant la commande suivante (sur tous les sites, dans le cas d'une installation multi-sites) :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_v6rc.yml --ask-vault-pass

Après le passage du script de migration, il faut procéder à la réindexation de toutes les groupes d'objets :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/reindex_es_data.yml --tags objectgroup --ask-vault-pass

Puis redémarrer les externals qui ont été coupés durant la migration :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-exploitation/start_external.yml --ask-vault-pass

=======
####################################

Montée de version vers mongo 4.4
----------------------------------------------

.. caution:: Cette opération doit être effectuée avant la montée de version vers la V6RC et si la version Mongodb est inférieure à une version 4.4 (ex: 4.2)

.. caution:: Sans cette opération, la montée de version d'une version existante vers une v6rc sera bloquée au démarrage des instances mongod par une incompatibilité.

Executez le playbook suivant:

.. code-block:: bash

     ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_44.yml --ask-vault-pass

Ce playbook effectue la montée de version de mongodb d'une version 4.2 vers une version 4.4 selon la procédure indiquée dans la documentation Mongodb
https://www.mongodb.com/docs/v4.4/release-notes/4.4-upgrade-replica-set/ . Cette procédure n'a pas été testée avec une version mongodb inférieure à 4.2.


Montée de version vers mongo 5.0.9 
-----------------------------------

.. caution:: Cette montée de version doit être effectuée avant la montée de version V6RC de vitam et après la montée de version en mongodb 4.4 ci dessus.

Executez le playbook suivant:

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_50.yml --ask-vault-pass

Ce playbook change le "Read and write Concern" des replicaset par reconfiguration, il désinstalle et réinstalle les binaires et . Il change également le paramètre
"SetFeatureCompatibility" à 5.0.

Une fois ces montées de version de Mongodb réalisées la montée de version Vitam classique peut être réalisée.
>>>>>>> 788aa0fdae (add documentation for mongodb update)
