Désynchronisation des bases de données
######################################

Afin de vérifier la cohérence des données enregistrées dans MongoDB et ElasticSearch, un contrôle supplémentaire a été mis en place.
Cela a pour but d'alerter les administrateurs de la plate-forme en cas de désynchronisation entre MongoDB et Elasticsearch.

Traitement
==========

Après toutes les opérations possibles par le DSL (Insertion, Mise à jour, Selection, etc...), une vérification a été ajoutée, et permet de vérifier la cohérence entre MongoDB et ElasticSearch.
Le nombre de documents contenus dans ElasticSearch est comparé à celui de MongoDB. En cas de différence, une exception est remontée par Metadata (VitamDBException).
De plus, des logs ERROR sont tracés afin de permettre aux administrateurs (via Kibana) de connaître les éventuels Guid provoquant la différence entre les bases de données. 

L'exception VitamDBException sera remontée jusqu'au module AccessExternal, qui retournera alors un message d'erreur explicite (la désynchronisation y sera bien explicitée).