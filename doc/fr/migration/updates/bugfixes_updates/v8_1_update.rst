Notes et procédures spécifiques V8.1
####################################

Procédures à exécuter AVANT la montée de version
================================================

Mise à jour de MongoDB vers la version 8.0.23
---------------------------------------------

.. caution:: **Attention**
    Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V8.1.
    Cette opération est à effectuer si vous venez des versions de Vitam suivante: V8.1.2-.
    Il est recommandé d'effectuer un backup des bases de données à l'aide de mongodump avant de poursuivre.

Exécutez le playbook suivant à partir de l'ansiblerie de la V8.1 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_80.yml --ask-vault-pass
