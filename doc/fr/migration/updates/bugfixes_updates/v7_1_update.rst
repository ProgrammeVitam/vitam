Notes et procédures spécifiques V7.1
####################################

Procédures à exécuter APRÈS la montée de version
================================================

Migration unités archivistiques avec demande de transfert non complétée
-----------------------------------------------------------------------

Cette migration permet de corriger des erreurs de sécurisation ou d'audit sur des unités archivistiques pour lesquelles une demande de transfert a été initiée, mais dont le transfert effectif n'a pas été finalisée.

.. caution:: Cette procédure doit être exécutée uniquement en cas de migration mineure depuis une version 7.1.1 (v7.1.1 ou inférieure) vers une version 7.1.2+ (v7.1.2 ou supérieure).

.. caution:: Cette migration est à effectuer **APRES** l'installation et uniquement sur le **site primaire**.

.. caution:: Dans le cas où un tenant comporte un très grand nombre d'unités archivistiques dont la demande de transfert a été initiée pour un tenant donné (plus de 100 000 unités archivistiques), la migration ne pourra alors s'opérer. Le support Vitam devra alors être contacté.

Pour lancer la migration, le playbook suivant doit être exécuté :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_metadata_transfer_request_traceability.yml --ask-vault-pass
