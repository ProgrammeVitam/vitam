Notes et procédures spécifiques V6RC
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

