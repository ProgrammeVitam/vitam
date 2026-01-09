Notes et procédures spécifiques V8.0
####################################

Procédures à exécuter AVANT la montée de version
================================================

Mise à jour de MongoDB vers la version 7.0.28
---------------------------------------------

.. caution:: **Attention**
    Cette opération doit être effectuée après avoir mis à jour les dépôts Vitam en V8.0.
    Cette opération est à effectuer si vous venez des versions de Vitam suivante: V8.0.2-.
    Il est recommandé d'effectuer un backup des bases de données à l'aide de mongodump avant de poursuivre.

Exécutez le playbook suivant à partir de l'ansiblerie de la V8.0 **sur tous les sites** :

.. code-block:: bash

    ansible-playbook -i environments/<inventaire> ansible-vitam-migration/migration_mongodb_70.yml --ask-vault-pass
