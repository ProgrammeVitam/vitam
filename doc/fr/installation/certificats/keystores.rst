
Génération des magasins de certificats
--------------------------------------

En prérequis, les certificats et les autorités de certification doivent être présents dans les répertoires attendus.

.. caution:: Avant de lancer le script de génération des stores, il est nécessaire de modifier le vault contenant les mots de passe des stores: ``environmements/group_vars/all/vault-keystores.yml``, décrit dans la section :ref:`pkiconfsection`.

Lancer le script :

.. code-block:: bash

   ./generate_stores.sh

Ci-dessous un exemple de sortie du script :

.. code-block:: bash

    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore de access-external pour le serveur localhost
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Génération du jks
    Entry for alias access-external successfully imported.
    Import command completed:  1 entries successfully imported, 0 entries failed or cancelled
    [INFO] [generate_stores.sh] Suppression du p12
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore de ingest-external pour le serveur localhost
        [...]
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Fin de la génération des stores

Ce script génère sous ``environmements/keystores`` les stores (jks / p12) associés pour un bon fonctionnement dans VITAM.

Il est aussi possible de déposer directement les keystores au bon format en remplaçant ceux fournis par défaut, en indiquant les mots de passe d'accès dans le vault: ``environmements/group_vars/all/vault-keystores.yml``
