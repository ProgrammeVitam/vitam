Liste des secrets
#################

Les secrets nécessaires au bon déploiement de VITAM sont les suivants :

* Certificat ou mot de passe de connexion SSH à un compte sudoer sur les serveurs cibles (pour le déploiement) ;
* Certificats x509 serveur (comprenant la clé privée) pour les modules de la zone d'accès (services ``*-external``), ainsi que les CA (finales et intermédiaires) et CRL associées

    - Ces certificats seront déployés dans des `keystores java <https://docs.oracle.com/cd/E19509-01/820-3503/ggffo/index.html>`_ en tant qu'élément de configuration de ces services (Cf. le :term:`DIN` pour plus d'information)

* Certificats x509 client pour les clients du SAE (ex: les applciations métier, le service ``ihm-demo``), ainsi que les CA (finales et intermédiaires) et CRL associées

	- Ces certificats seront déployés dans des `keystores java <https://docs.oracle.com/cd/E19509-01/820-3503/ggffo/index.html>`_ en tant qu'élément de configuration de ces services (Cf. le :term:`DIN` pour plus d'information)

Les secrets définis lors de l'installation de VITAM sont les suivants :

* Mots de passe des keystores ;
* Mots de passe des administrateurs fonctionnels de l'application VITAM ;
* Mots de passe d'administration de base de données MongoDB ;
* Mots de passe des comptes d'accès aux bases de données MongoDB.

