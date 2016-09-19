Elasticsearch-log
#################

Cluster dédié aux données métier


Type :
	COTS

Données stockées :
	* Logs des composants déployés dans le cadre de VITAM (services java, bases de données, composants de support (logstash, curator))

Typologie de consommation de resources :
	* CPU : moyenne
	* Mémoire : forte
	* Réseau : forte
	* Disque : forte


Architecture de déploiement
===========================

.. todo:: Expliquer la topologie de déploiement (pas trop compliquée pour ES)

2n + 1 noeuds 

LB/HA
=====

.. todo:: Expliquer les principes de LB/HA

Monitoring
==========

.. todo:: plugin head / kopf / interfaces REST natives / ...

