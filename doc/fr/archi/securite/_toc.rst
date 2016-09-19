########
Securite
########

Principes
=========

Les principes de sécurité de VITAM, dans cette version, suivent les directives suivantes :

* Authentification et authorisation systématique des systèmes clients de VITAM basé sur une authentification TLS mutuelle utilisant des certificats (pour les composants de la couche accès) ;
* Validation systématique des entrées du système :

    - Détection et suppression de codes malveillants dans les archives déposées dans VITAM ;
    - Robustesse contre les failles du Top Ten OWASP pour toutes les interfaces REST ;

* Validation périodique des listes de CRL pour toutes les CA trustées par VITAM.
  
.. Architectes VITAM : d'autres principes de sécurité ?


Principes de cloisonnement
--------------------------

Les principes de cloisonnement en zones, et notamment les implications en terme de communication entre ces zones ont été décrits dans :doc:`la section dédiée aux principes de déploiement <fonctionnelle-exploitation/30-principles-deployment>`.


Principes de sécurisation des accès externes
--------------------------------------------

Les services logiciels en contact direct avec les clients du SAE (i.e. les services ``*-external``) implémentent les mesures de sécurité suivantes :

.. TODO : Préciser les algo & ciphers valides

* Chiffrement du transport des données entre les applications externes et VITAM via HTTPS ;
* Authentification par certificat x509 requise des applications externes (authentification M2M) basée sur une liste blanche de certificats valides ;

    - Lors d’une connexion, la vérification synchrone confirme que le certificat proposé n’est pas expiré (not before, not after) et est bien présent dan le référentiel d’authentification des certificats valides (qui est un fichier keystore contenant la liste des certificats valides) 
    
* Filtrage exhaustif des données et requêtes entrant dans le système basés sur :

    - Un WAF applicatif permettant le filtrage d'entrée filtrant les entrées pouvant être une menace pour le système (intégration de la bibliothèque `ESAPI <https://www.owasp.org/index.php/Category:OWASP_Enterprise_Security_API>`_ )
    - Support de l'utilisation d'un ou plusieurs antivirus (configurables et extensibles) dans le composant d'entrée (``ingest``) permettant de valider l'inocuité des données entrantes.


Principes d'authentification externes
-------------------------------------

Liste des secrets
=================

Les secrets nécessaires au bon déploiement de VITAM sont les suivants :

* Certificat ou mot de passe de connexion SSH à un compte sudoer sur les serveurs cibles (pour le déploiement) ;
* Certificats x509 serveur (comprenant la clé privée) pour les modules de la zone d'accès (services ``*-external``), ainsi que les CA (finales et intermédiaires) et CRL associées

    - Ces certificats seront déployés dans des `keystores java <https://docs.oracle.com/cd/E19509-01/820-3503/ggffo/index.html>`_ en tant qu'élément de configuration de ces services (Cf. le :term:`DIN` pour plus d'information)
      
* Certificats x509 client pour les clients du SAE (ex: les SIA, le service ``ihm-demo``), ainsi que les CA (finales et intermédiaires) et CRL associées 
  
	- Ces certificats seront déployés dans des `keystores java <https://docs.oracle.com/cd/E19509-01/820-3503/ggffo/index.html>`_ en tant qu'élément de configuration de ces services (Cf. le :term:`DIN` pour plus d'information)
 
Les secrets définis lors de l'installation de VITAM sont les suivants :

* Mots de passe des administrateurs fonctionnels de l'application VITAM ;
* Mots de passe d'administration de base de données MongoDB ;
* Mots de passe des comptes d'accès aux bases de données MongoDB.



.. todo:: Intégrer (KWA : dans quelle mesure ?) l'étude ebios "cadre" qui aurait déjà été réalisée (recroiser avec Emmanuel).


.. Sera à compléter au fur et à mesure.


.. Autres points à aborder :

.. * DICT ?
..    - Analyse EBIOS "cadre"
.. * Bonnes pratiques de sécurisation
.. * Gestion des comptes
.. * Gestion des secrets
.. * Principes de cloisonnement
.. * Normes
..    - Normes métier archivistique
..    - Normes SI
..       + Conformité au RGS
.. * Principes de MCS
