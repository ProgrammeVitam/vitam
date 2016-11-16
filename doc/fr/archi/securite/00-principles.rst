Principes
#########

Les principes de sécurité de VITAM, dans cette version, suivent les directives suivantes :

* Authentification et authorisation systématique des systèmes clients de VITAM basé sur une authentification TLS mutuelle utilisant des certificats (pour les composants de la couche accès) ;
* Validation systématique des entrées du système :

    - Détection et suppression de codes malveillants dans les archives déposées dans VITAM ;
    - Robustesse contre les failles du Top Ten OWASP pour toutes les interfaces REST ;

* Validation périodique des listes de CRL pour toutes les CA trustées par VITAM.

.. Architectes VITAM : d'autres principes de sécurité ?


Principes de cloisonnement
==========================

Les principes de cloisonnement en zones, et notamment les implications en terme de communication entre ces zones ont été décrits dans :doc:`la section dédiée aux principes de déploiement </fonctionnelle-exploitation/30-principles-deployment>`.


Principes de sécurisation des accès externes
============================================

Les services logiciels en contact direct avec les clients du SAE (i.e. les services ``*-external``) implémentent les mesures de sécurité suivantes :

* Chiffrement du transport des données entre les applications externes et VITAM via HTTPS ; par défaut, la configuration suivante est appliquée :

    - Protocoles exclus : ``SSLv2``, ``SSLv3``
    - Ciphers exclus : ``.*NULL.*``, ``.*RC4.*``, ``.*MD5.*``, ``.*DES.*``, ``.*DSS.*``

	.. note:: Pour la bêta VITAM, les ciphers recommandés sont : ``TLS_ECDHE.*``, ``TLS_DHE_RSA.*``

* Authentification par certificat x509 requise des applications externes (authentification M2M) basée sur une liste blanche de certificats valides ;

    - Lors d’une connexion, la vérification synchrone confirme que le certificat proposé n’est pas expiré (not before, not after) et est bien présent dan le référentiel d’authentification des certificats valides (qui est un fichier keystore contenant la liste des certificats valides)

    .. note:: Pour la bêta VITAM, la liste des certificats reconnus est stockée dans un keystore Java

* Filtrage exhaustif des données et requêtes entrant dans le système basés sur :

    - Un WAF applicatif permettant le filtrage d'entrée filtrant les entrées pouvant être une menace pour le système (intégration de la bibliothèque `ESAPI <https://www.owasp.org/index.php/Category:OWASP_Enterprise_Security_API>`_ protégeant notamment contre les attaques de type XSS)
    - Support de l'utilisation d'un ou plusieurs antivirus (configurables et extensibles) dans le composant d'entrée (``ingest``) permettant de valider l'inocuité des données entrantes.

.. note:: Dans cette version du système, le paramétrage de l'antivirus est supporté lors de l'installation, mais pas le paramétrage d'ESAPI (notamment  les filtres appliquées) ; cette possibilité sera néanmoins ouverte dans une version ultérieure.

.. todo Parle-t-on de l'authentification des utisliateurs à l'ihm demo

Principes de sécurisation des bases de données
==============================================

Les bases de données sont sécurisées via un cloisonnement physique et/ou logique des différentes bases de données qui les constituent.

MongoDB
-------

Dans le cas de MongoDB, le cloisonnement est logique. Chaque service hébergeant des données dans MongoDB se voit attribuer une base et un utilisateur dédié. Cet utilisateur a uniquement les droits de lecture / écriture dans les collections de cette base de données, mais ne peut notamment pas modifier la structure des collections de sa base de données ni accéder aux collections d'une autre base de données.

Un utilisateur technique "root" est également créé pour les besoins de l'installation et de la configuration de MongoDB.

Chaque base de données doit être accédée que par les instances d'un seul service (ex: le service logbook est le seul à accéder à la base de données logbook).

Enfin, l'accès anonyme à MongoDB est désactivé, et les utilisateurs sont authentifié par le couple utilisateur / mot de passe.


Elasticsearch
-------------

Dans le cas d'Elasticsearch, le cloisonnement est principalement physique, dans le sens où le cluster hébergeant les données métier est disjoint du cluster hébergeant les données techniques.

.. todo Peut-on dire des trucs en plus ???

.. caution:: L'accès au cluster Elasticsearch est anonyme sans authentification requise ; ceci est dû à une limitation de la version OpenSource d'Elasticsearch, et pourra être réévalué dans les futures versions du système VITAM.
