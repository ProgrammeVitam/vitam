Mongodb
#######

Base de données dédié aux données métier


Type :
	COTS

Données stockées :
	* Données d'archives
	* Journaux métier
	* Référentiels métier

Typologie de consommation de resources :
	* CPU : moyenne
	* Mémoire : forte
	* Réseau : forte
	* Disque : forte


Architecture de déploiement
===========================

**Architecture 1 noeud:**

* 1 serveur mongodb:

    - 1 noeud mongod    

**Architecture n shards et r noeuds par replica set (cluster):**

* 3 serveurs config / service, chacun hébergeant:

    - 1 noeud mongos (service)
    - 1 noeud mongod (Replica Set de config)

* nr serveurs, chacun hébergeant: 

    - 1 noeud mongod

**Les ports utilisés par mongodb sont les suivants:**

* tcp:27017 : Port de communication par défaut des tous les noeuds
* tcp:27018 : Port d'écoute des noeuds du Replica Set de config (car cohabitation avec le mongos qui écoute sur 27017)

LB/HA
=====

    * Sharding
        - Mongodb utilise la sharding pour scaler la base de données
        - Le sharding distribue les données à travers les n partitions physiques (shards) dont le cluster est composé
        - Bien choisir la clé de sharding est primordial pour une répartition égale des documents insérés dans les différents shards
        - Chaque shard est composé d'un Replica Set

    * Replica Set (RS)
        - Les Replica Set assurent la haute disponibilité de Mongodb
        - Un Replica Set est composé d'un noeud primaire et de deux noeuds secondaires. (Règles Mongodb de production)
        - L'écriture se fait obligatoirement sur le noeud primaire

    * Replica Set de config
        - Un Replica Set est dédié pour le stockage de la config
        - Comme tous les autres Replica Set, il est recommandé de le peupler de 3 noeuds

    * Router (mongos)
        - Le router mongos permet de rediriger une requête sur le bon shard, en fonction de la clé de sharding
        - Décision prise sur Vitam de faire cohabiter mongos et les noeuds du Replica Set de config

Monitoring
==========

.. todo:: ???

