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


Liste des secrets
=================

Les secrets nécessaires au bon déploiement de VITAM sont les suivants :

* Certificat ou mot de passe de connexion SSH à un compte sudoer sur les serveurs cibles (pour le déploiement) ;
* Certificats serveur pour les modules de la zone d'accès (composants ``*-external``), ainsi que les CA (finales et intermédiaires) associées ; listes de CRL associées ;
* Mots de passe d'administration de base de données MongoDB ;
 
Les secrets définis lors de l'installation de VITAM sont les suivants :

* Mots de passe des administrateurs fonctionnels de l'application VITAM ;
* Mots de passe des comptes d'accès aux bases de données MongoDB.
  
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
