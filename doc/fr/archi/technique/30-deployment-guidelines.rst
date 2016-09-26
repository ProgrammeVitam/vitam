Guidelines de déploiement
#########################

Les principes de zoning associés à l'architecture du systèmes VITAM ont été présentés :doc:`lors de la description des principes de déploiement </fonctionnelle-exploitation/30-principles-deployment>` ; cette section a pour but de compléter ces principes par des recommandations concernant la colocalisation des composants.

De manière générale, pour des raisons de sécurité, il est déconseillé de colocaliser des composants appartenant à des zones différentes. Il est par contre possible de colocaliser des composants appartenant à des sous-zones différentes dans la zone des services internes ; ainsi, les colocalisations des composants suivants sont relativement pertinentes :

* ingest-external, access-external et administration-external ;
* ingest-internal et access-internal ;
* elasticsearch-data et mongod ;
* mongos et mongoc ;
* logstash, elasticsearch-log, kibana (pour les déploiements de taille limitée) ; elasticsearch-log et consul (serveur) (pour des déploiements de taille moyenne)
* workspace et storage ;


.. caution::
	Il est recommandé de ne pas colocaliser les composants restants :

	* storage-offer-default, étant dans une zone logique particulière ;
	* worker, ayant une consommation de resources système potentiellement importante.

.. note:: Ces principes de colocation sont les préconisations initiales relatives à cette version du système VITAM ; ils seront revus suite aux campagnes de tests de performance en cours.