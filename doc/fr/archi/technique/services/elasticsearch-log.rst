Elasticsearch-log
#################

Cluster dédié aux données métier


Type :
	COTS

Données stockées :
	* Logs techniques des composants déployés dans le cadre de VITAM (services java, bases de données, composants de support (logstash, curator))

Typologie de consommation de resources :
	* CPU : moyenne
	* Mémoire : forte
	* Réseau : forte
	* Disque : forte


Architecture de déploiement
===========================

.. seealso:: Se reporter à :doc:`elasticsearch-data` pour les informations générales concernant elasticsearch.

Dans le déploiement actuel, tous les noeuds sont considérés comme des noeuds "master" et "data" ; par conséquent, le nombre de noeuds du cluster doit être impair (i.e. 2n + 1 noeuds, n > 1).

.. todo Dans une prochaine version, affiner l'exposition des ports http

.. Monitoring
.. ==========

.. todo plugin kopf / interfaces REST natives / ...
