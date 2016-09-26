Elasticsearch-data
##################

Cluster d'indexation dédié aux données métier


Type :
	COTS

Données stockées :
	* Index de recherche des données d'archive

Typologie de consommation de resources :
	* CPU : moyenne
	* Mémoire : forte
	* Réseau : forte
	* Disque : forte


Architecture de déploiement
===========================

.. todo:: Une présentation de l'architecture de déploiement (avec le rôle des différents noeuds), ainsi que des principes de LB/HA d'elasticsearch sera incluse dans une prochaine version de ce document.

Dans le déploiement actuel, tous les noeuds sont considérés comme des noeuds "master" et "data" ; par conséquent, le nombre de noeuds du cluster doit être impair (i.e. 2n + 1 noeuds, n > 1). 

.. todo Dans une prochaine version, affiner l'exposition des ports http


.. Monitoring
.. ==========

.. .. todo:: plugin head / kopf / interfaces REST natives / ...

