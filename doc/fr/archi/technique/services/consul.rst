Consul
######

Type :
	COTS

Données stockées :
	* Etat du cluster et localisation des services

Typologie de consommation de resources :
	* Serveurs :
	    - CPU : faible
	    - Mémoire : faible
	    - Réseau : faible
	    - Disque : faible
	* Agents :
	    - CPU : faible
	    - Mémoire : faible
	    - Réseau : faible
	    - Disque : faible

.. caution:: Consul est un service critique d'infrastructure ! Un dysfonctionnement de ce service peut rapidement entraîner une panne générale du système.

Architecture de déploiement
===========================

* 2n + 1 noeuds pour les serveurs ; chaque noeud serveur doit répondre aux requêtes RPC des agents et expose l'IHM de suivi de l'état du cluster consul ;
* 1 noeud agent par serveur hébergeant des services VITAM ; chaque noeud agent agit comme serveur DNS local.
  
Les ports utilisés par Consul sont les suivants :

* tcp:8300 : Port RPC ; il permet aux agents d'exécuter des requêtes vers les serveurs.
* tcp:8301 : Port de "gossip" ; il permet la découverte automatique des agents entre eux, et la propagation des évènements du cluster vers tous les noeuds.
* tcp:8400 : Port RPC local ; il est utilisé par la console consul locale (CLI).
* tcp:8500 : Port HTTP ; il est notamment utilisé par les noeuds serveur pour servir l'interface de monitoring et d'administration.
* udp:53 & tcp:53 : Port d'écoute DNS


LB/HA
=====

.. todo:: Expliquer les principes de LB/HA du serveur

