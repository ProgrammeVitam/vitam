
Génération des magasins de certificats
--------------------------------------

En prérequis, les certificats et les autorités de certification doivent être présents dans les répertoires attendus.

.. caution:: Avant de lancer le script de génération des stores, il est nécessaire de modifier le vault contenant les mots de passe des stores: environmements/group_vars/all/vault-keystores.yml

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
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Génération du jks
    Entry for alias ingest-external successfully imported.
    Import command completed:  1 entries successfully imported, 0 entries failed or cancelled
    [INFO] [generate_stores.sh] Suppression du p12
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore de storage-offer-default pour le serveur localhost
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Génération du jks
    Entry for alias storage-offer-default successfully imported.
    Import command completed:  1 entries successfully imported, 0 entries failed or cancelled
    [INFO] [generate_stores.sh] Suppression du p12
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore timestamp de logbook
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore client de ihm-demo
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Ajout du certificat public de ihm-demo dans le grantedstore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore client de ihm-recette
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Ajout du certificat public de ihm-recette dans le grantedstore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore client de reverse
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Ajout du certificat public de reverse dans le grantedstore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Ajout des certificat public du répertoire external dans le grantedstore external
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Génération du truststore client-external
    [INFO] [generate_stores.sh] Ajout des certificats client dans le truststore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/client-external/ca-intermediate.crt dans le truststore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/client-external/ca-root.crt dans le truststore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout des certificats serveur dans le truststore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-intermediate.crt dans le truststore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-root.crt dans le truststore external
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Creation du keystore client de storage-engine
    [INFO] [generate_stores.sh] Génération du p12
    [INFO] [generate_stores.sh] Ajout du certificat public de storage-engine dans le grantedstore storage
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Ajout des certificat public du répertoire external dans le grantedstore storage
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Génération du truststore client-storage
    [INFO] [generate_stores.sh] Ajout des certificats client dans le truststore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/client-storage/ca-intermediate.crt dans le truststore storage
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/client-storage/ca-root.crt dans le truststore storage
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout des certificats serveur dans le truststore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-intermediate.crt dans le truststore storage
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-root.crt dans le truststore storage
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Génération du truststore server
    [INFO] [generate_stores.sh] Ajout des certificats client dans le truststore
    [INFO] [generate_stores.sh] Ajout des certificats serveur dans le truststore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-intermediate.crt dans le truststore server
    Certificate was added to keystore
    [INFO] [generate_stores.sh] Ajout de /home/nico/git/vitam/deployment/pki/ca/server/ca-root.crt dans le truststore server
    Certificate was added to keystore
    [INFO] [generate_stores.sh] -------------------------------------------
    [INFO] [generate_stores.sh] Fin de la génération des stores

Ce script génère sous ``environmements/keystores`` les stores (jks / p12) associés pour un bon fonctionnement dans VITAM.

Il est aussi possible de déposer directement les keystores au bon format en remplaçant ceux fournis par défaut, en indiquant les mots de passe d'accès dans le vault: ``environmements/group_vars/all/vault-keystores.yml``
