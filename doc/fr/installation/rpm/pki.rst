Explications relatives à la PKI
###############################

Les commandes sont à passer dans le sous-répertoire ``deployment`` de la livraison.

.. caution:: par la suite, le terme <environnement> correspond à l'extension du nom de fichier d'inventaire.


.. figure:: images/pki-certificats.*
    :align: center

    Vue d'ensemble de la gestion des certificats au déploiement


Génération des autorités de certification
=========================================


Cas d'une PKI inexistante
--------------------------

Dans le répertoire de déploiement, lancer le script :

.. code-block:: bash

   pki/scripts/generate_ca.sh


Ce script génère sous ``pki/ca`` les autorités de certification root et intermédiaires pour clients, serveurs, et timestamping.

Voici ci-dessous un exemple de rendu du script :

.. code-block:: bash

    [INFO] [generate_ca.sh] Lancement de la procédure de création des CA
    [INFO] [generate_ca.sh] ==============================================
    [INFO] [generate_ca.sh] Répertoire /home/nico/git/vitam/deployment/pki/ca absent ; création...
    [INFO] [generate_ca.sh] Création du répertoire de travail temporaire tempcerts sous /home/nico/git/vitam/deployment/pki/tempcerts...
    [INFO] [generate_ca.sh] Création de CA root pour server...
    [INFO] [generate_ca.sh] Create CA request...
    Generating a 2048 bit RSA private key
    ........................+++
    ....................+++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/server/ca-root.key'
    -----
    [INFO] [generate_ca.sh] Create CA certificate...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/server/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_server'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:14 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] Création de la CA intermediate pour server...
    [INFO] [generate_ca.sh] Generate intermediate request...
    Generating a 4096 bit RSA private key
    .................................++
    ..................................................................................................++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/server/ca-intermediate.key'
    -----
    [INFO] [generate_ca.sh] Sign...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/server/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_server'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:14 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] ----------------------------------------------
    [INFO] [generate_ca.sh] Création de CA root pour client-external...
    [INFO] [generate_ca.sh] Create CA request...
    Generating a 2048 bit RSA private key
    .........................................................+++
    ....................................+++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/client-external/ca-root.key'
    -----
    [INFO] [generate_ca.sh] Create CA certificate...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-external/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_client-external'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:14 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] Création de la CA intermediate pour client-external...
    [INFO] [generate_ca.sh] Generate intermediate request...
    Generating a 4096 bit RSA private key
    ......................++
    ....++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/client-external/ca-intermediate.key'
    -----
    [INFO] [generate_ca.sh] Sign...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-external/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_client-external'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:14 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] ----------------------------------------------
    [INFO] [generate_ca.sh] Création de CA root pour client-storage...
    [INFO] [generate_ca.sh] Create CA request...
    Generating a 2048 bit RSA private key
    ...............+++
    ..................................+++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/client-storage/ca-root.key'
    -----
    [INFO] [generate_ca.sh] Create CA certificate...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-storage/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_client-storage'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:14 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] Création de la CA intermediate pour client-storage...
    [INFO] [generate_ca.sh] Generate intermediate request...
    Generating a 4096 bit RSA private key
    ...............................................................................................................................................................++
    ..............................................................................................................................................................................................................................................................................................++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/client-storage/ca-intermediate.key'
    -----
    [INFO] [generate_ca.sh] Sign...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-storage/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_client-storage'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:16 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] ----------------------------------------------
    [INFO] [generate_ca.sh] Création de CA root pour timestamping...
    [INFO] [generate_ca.sh] Create CA request...
    Generating a 2048 bit RSA private key
    .........................+++
    ........................................+++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/timestamping/ca-root.key'
    -----
    [INFO] [generate_ca.sh] Create CA certificate...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/timestamping/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_timestamping'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:16 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] Création de la CA intermediate pour timestamping...
    [INFO] [generate_ca.sh] Generate intermediate request...
    Generating a 4096 bit RSA private key
    ........................++
    .........++
    writing new private key to '/home/nico/git/vitam/deployment/pki/ca/timestamping/ca-intermediate.key'
    -----
    [INFO] [generate_ca.sh] Sign...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/timestamping/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'CA_timestamping'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 26 16:29:16 2027 GMT (3650 days)

    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] ----------------------------------------------
    [INFO] [generate_ca.sh] ==============================================
    [INFO] [generate_ca.sh] Fin de la procédure de création des CA

.. note::  bien noter les dates de création et de fin de validité des CA. En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.

Cas d'une CA déjà existante
----------------------------

Pas de support pour le moment en cas de CA déjà existante uniquement.
Il est nécessaire de générer et déposer manuellement les certificats (voir étape ci-dessous)

Génération des certificats
==========================

Cas de certificats inexistants
-------------------------------

.. warning:: cette étape n'est à effectuer que pour les clients ne possédant pas de certificats.

Editer complètement le fichier ``environments/<fichier d'inventaire>``  pour indiquer les serveurs associés à chaque service.
En prérequis les CA doivent être présentes.

Puis, dans le répertoire de déploiement, lancer le script :


.. code-block:: bash

   pki/scripts/generate_certs.sh <fichier d'inventaire>

Ci-dessous un exemple de sortie du script :

.. code-block:: bash

    [INFO] [generate_certs.sh] Suppression de l'ancien vault
    [INFO] [generate_certs.sh] Recopie des clés publiques des CA
    [INFO] [generate_certs.sh] Copie de la CA (root + intermediate) de client-external
    [INFO] [generate_certs.sh] Copie de la CA (root + intermediate) de client-storage
    [INFO] [generate_certs.sh] Copie de la CA (root + intermediate) de server
    [INFO] [generate_certs.sh] Copie de la CA (root + intermediate) de timestamping
    [INFO] [generate_certs.sh] Génération des certificats serveurs
    [INFO] [generate_certs.sh] Création du certificat server pour ingest-external hébergé sur localhost...
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    ................................................................................................................................................++
    .........................................++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/server/hosts/localhost/ingest-external.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/server/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'ingest-external.service.consul'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:00 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Encryption successful
    [INFO] [generate_certs.sh] Création du certificat server pour access-external hébergé sur localhost...
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    ..........++
    ...................++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/server/hosts/localhost/access-external.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/server/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'access-external.service.consul'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:01 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Création du certificat server pour storage-offer-default hébergé sur localhost...
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    .....................++
    ........++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/server/hosts/localhost/storage-offer-default.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/server/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'storage-offer-default.service.consul'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:02 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Génération des certificats timestamping
    [INFO] [generate_certs.sh] Création du certificat timestamping pour logbook
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    .........................................................................................................++
    ...........++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/timestamping/vitam/logbook.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA timestamping...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/timestamping/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'logbook.service.consul'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:04 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Génération des certificats clients
    [INFO] [generate_certs.sh] Création du certificat client pour ihm-demo
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    ....++
    ........++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/client-external/clients/ihm-demo/ihm-demo.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-external/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'ihm-demo'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:04 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Création du certificat client pour ihm-recette
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    .......................................................................++
    ...............................................................................................................................................................++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/client-external/clients/ihm-recette/ihm-recette.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-external/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'ihm-recette'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:06 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Création du certificat client pour reverse
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    ...............................++
    .................................................................................................++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/client-external/clients/reverse/reverse.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-external/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'reverse'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:07 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Création du certificat client pour storage-engine
    [INFO] [generate_certs.sh] Generation de la clé...
    Generating a 4096 bit RSA private key
    ...........++
    ..........................................................................................++
    writing new private key to '/home/nico/git/vitam/deployment/environments/certs/client-storage/clients/storage-engine/storage-engine.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-storage...
    Using configuration from /home/nico/git/vitam/deployment/pki/config/client-storage/ca-config
    Check that the request matches the signature
    Signature ok
    The Subject's Distinguished Name is as follows
    commonName            :ASN.1 12:'storage-engine'
    organizationName      :ASN.1 12:'Vitam.'
    countryName           :PRINTABLE:'FR'
    stateOrProvinceName   :ASN.1 12:'idf'
    localityName          :ASN.1 12:'paris'
    Certificate is to be certified until Feb 29 09:37:08 2020 GMT (1095 days)

    Write out database with 1 new entries
    Data Base Updated
    Decryption successful
    Encryption successful
    [INFO] [generate_certs.sh] Fin de script


Ce script génère sous ``environmements/certs`` les certificats (format crt & key) nécessaires pour un bon fonctionnement dans VITAM.

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

Cas de certificats déjà créés par le client
--------------------------------------------

Si le client possède déjà une :term:`PKI`, ou ne compte pas utiliser la :term:`PKI` fournie par VITAM, il convient de positionner les certificats et CA sous ``environmements/certs/....`` en respectant la structure indiquée ci-dessous

- cert
    - client-external
        - ca: CA(s) des certificats clients external
        - clients
            - external: Certificats des SIAs
            - ihm-demo: Certificat de ihm-demo
            - ihm-recette: Certificat de ihm-recette
            - reverse: Certificat du reverse
    - client-storage
        - ca: CA(s) des certificats clients storage
        - clients
            - storage-engine: Certificat de storage-engine
    - server
        - ca: CA(s) des certificats côté serveurs
        - hosts
            - [nom_serveur]: certificats des composants installés sur le serveur donné, [nom_serveur] doit être identique à ce qui est référencé dans le ficheir d'inventaire
    - timestamping
        - ca: CA des certificats de timestamping
        - vitam: Certificats de timestamping

Il est aussi nécessaire de renseigner le vault contenant les passphrases des clés des certificats: ``environmements/certs/vault-certs.yml``

Génération des stores
=====================

.. caution:: Avant de lancer le script de génération des stores, il est nécessaire de modifier le vault contenant les mots de passe des stores


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
