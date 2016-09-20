Guidelines de déploiement
#########################

Les principes de zoning associés à l'architecture du systèmes VITAM ont été présentés :doc:`lors de la description des principes de déploiement <fonctionnelle-exploitation/30-principles-deployment>` ; cette section a pour but de compléter ces principes par des recommandations concernant la colocalisation des composants.

De manière générale, pour des raisons de sécurité, il est déconseillé de colocaliser des composants appartenant à des zones différentes. Il est par contre possible de colocaliser des composants appartenant à des sous-zones différentes dans la zone des services internes ; ainsi, les colocalisations des composants suivants sont relativement pertinentes :

* ingest-internal et processing ;
* workspace et storage-engine ;
* logbook et functional-administration ;
* access-internal et metadata ;
* ingest-external, access-external et administration-external ;
* logstash, elasticsearch-log, kibana (pour les déploiements de taille limitée).

Il est recommandé de ne pas colocaliser les composants de type worker, ceux-ci ayant une consommation de resources système potentiellement importante.
