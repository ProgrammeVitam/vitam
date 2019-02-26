Liste des secrets
#################

Les secrets nécessaires au bon déploiement de VITAM sont les suivants :

* Certificat ou mot de passe de connexion SSH à un compte sudoer sur les serveurs cibles (pour le déploiement) ;
* Certificats x509 serveur (comprenant la clé privée) pour les modules de la zone d'accès (services ``*-external``) et pour le module ``storage``, ainsi que les CA (finales et intermédiaires) ;
* Certificats x509 client d'horodatage, pour les modules appliquant l'horodatage sécurisé, ainsi que les CA (finales et intermédiaires) ;
* Certificats x509 client pour les clients du SAE (ex. : les applications métier, le service ``ihm-demo``), ainsi que les CA (finales et intermédiaires) ;
* CA (finales et intermédiaires) éventuels des offres de stockage utilisées (ex. : CA d'une offre de stockage objet swift).

.. note:: Ces certificats x509 seront déployés dans des `keystores java <https://docs.oracle.com/cd/E19509-01/820-3503/ggffo/index.html>`_ en tant qu'éléments de configuration de ces services (cf. le :term:`DIN` pour plus d'informations).


Les secrets définis lors de l'installation de VITAM sont les suivants :

* Mots de passe des keystores ;
* Mots de passe des administrateurs fonctionnels de l'application VITAM ;
* Mots de passe d'administration de base de données MongoDB ;
* Mots de passe des comptes d'accès aux bases de données MongoDB.

Le détail de l'usage des certificats pour le déploiement est donné dans le :term:`DIN`.
