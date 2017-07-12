
Cas 1: Je ne dispose pas de PKI, je souhaite utiliser celle de Vitam
====================================================================

Dans ce cas, il est nécessaire d'utiliser la :term:`PKI` fournie avec la solution logicielle VITAM.

Procédure générale
------------------

.. danger:: La :term:`PKI` fournie avec la solution logicielle Vitam ne doit être utilisée que pour faire des tests, et ne doit par conséquent surtout pas être utilisée en environnement de production !

La :term:`PKI` de la solution logicielle VITAM est une suite de scripts qui vont générer dans l'ordre ci-dessous:

- Les autorités de certifcation (CA)
- Les certificats (clients, serveurs, de timestamping) à partir des CA
- Les keystores, en important les certificats et CA nécessaires pour chacun des keystores


Génération des CA par les scripts Vitam
---------------------------------------

Il faut faire générer les autorités de certification par le script décrit ci-dessous.


Dans le répertoire de déploiement, lancer le script :

.. code-block:: bash

   pki/scripts/generate_ca.sh


Ce script génère sous ``pki/ca`` les autorités de certification root et intermédiaires pour générer des certificats clients, serveurs, et de timestamping.

.. warning:: Bien noter les dates de création et de fin de validité des CA. En cas d'utilisation de la PKI fournie, la CA root a une durée de validité de 10 ans ; la CA intermédiaire a une durée de 3 ans.

Voici ci-dessous un exemple de rendu du script :

.. code-block:: bash

    [INFO] [generate_ca.sh] Lancement de la procédure de création des CA
    [INFO] [generate_ca.sh] ==============================================
    [INFO] [generate_ca.sh] Répertoire /home/utilisateur/git/vitam/deployment/pki/ca absent ; création...
    [INFO] [generate_ca.sh] Création du répertoire de travail temporaire tempcerts sous /home/utilisateur/git/vitam/deployment/pki/tempcerts...
    [INFO] [generate_ca.sh] Création de CA root pour server...
    [INFO] [generate_ca.sh] Create CA request...
    Generating a 2048 bit RSA private key
    ........................+++
    ....................+++
    writing new private key to '/home/utilisateur/git/vitam/deployment/pki/ca/server/ca-root.key'
    -----
    [INFO] [generate_ca.sh] Create CA certificate...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/server/ca-config
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
         [...]
    Write out database with 1 new entries
    Data Base Updated
    [INFO] [generate_ca.sh] ----------------------------------------------
    [INFO] [generate_ca.sh] ==============================================
    [INFO] [generate_ca.sh] Fin de la procédure de création des CA


Génération des certificats par les scripts Vitam
------------------------------------------------

Le fichier d'inventaire de déploiement ``environments/<fichier d'inventaire>`` (cf. :ref:`inventaire`) doit être correctement renseigné pour indiquer les serveurs associés à chaque service. En prérequis les CA doivent être présentes.

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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/server/hosts/localhost/ingest-external.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/server/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/server/hosts/localhost/access-external.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/server/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/server/hosts/localhost/storage-offer-default.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA server...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/server/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/timestamping/vitam/logbook.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec CA timestamping...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/timestamping/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/client-external/clients/ihm-demo/ihm-demo.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/client-external/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/client-external/clients/ihm-recette/ihm-recette.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/client-external/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/client-external/clients/reverse/reverse.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-external...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/client-external/ca-config
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
    writing new private key to '/home/utilisateur/git/vitam/deployment/environments/certs/client-storage/clients/storage-engine/storage-engine.key'
    -----
    [INFO] [generate_certs.sh] Generation du certificat signé avec client-storage...
    Using configuration from /home/utilisateur/git/vitam/deployment/pki/config/client-storage/ca-config
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
Les mots de passe des clés privées des certificats sont stockés dans le vault ansible environmements/certs/vault-certs.yml

.. caution::  Les certificats générés à l'issue ont une durée de validité de (à vérifier).

.. include:: keystores.rst
