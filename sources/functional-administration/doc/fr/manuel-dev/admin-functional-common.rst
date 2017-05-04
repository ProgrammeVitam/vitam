Administration-Management-Common
#######

*Parent package:* **fr.gouv.vitam.functional.administration**

*Package proposition:* **fr.gouv.vitam.functional.administration.common**

Ce package implémente les différentes opérations sur le module functional-administration
 (insert, delete, select pour les formats, les règles de gestion et les registres de fonds)

1. Modules et packages
======================

---fr.gouv.vitam.functional.administration.common: contenant des modèles de document MongoDb

---fr.gouv.vitam.functional.administration.common.client.model: contenant les modèles de la réponse du client

---fr.gouv.vitam.functional.administration.common.exception: contenant les exceptions du module

---fr.gouv.vitam.functional.administration.common.server: contenant les classes pour l'accès aux bases de données

2. Classes
==========

Dans cette section, nous présentons quelques classes principales dans les modules/packages
abordés ci-dessus.

2.1 Class ElasticsearchAccessFunctionalAdmin
-------------------

Class ElasticsearchAccessFunctionalAdmin : il s'agit de la classe qui permet de gérer les requêtes de functional.administration à la base de données ElasticSearch
Les différents traitements sont l'ajout, la recherche et la suppression.

Pour la recherche :

- La méthode search(final FunctionalAdminCollections collection, final QueryBuilder query,
        final QueryBuilder filter) permet de chercher dans l'index Elasticsearch avec le query et le filter.

Pour l'insert :

- La Méthode addIndex(final FunctionalAdminCollections collection) permet d'ajouter un index dans Elasticsearch

- La Méthode addEntryIndexes(final FunctionalAdminCollections collection,
        final Map<String, String> mapIdJson) permet  d'insérer les indexes dans l'index ElasticSearch.

Pour le delete :

- La Méthode deleteIndex(final FunctionalAdminCollections collection) permet  de supprimer un index dans Elasticsearch.

2.2 Class MongoDbAccessAdminImpl
-------------------------------------

- La Méthode insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)
                   permet d'insérer un ensemble d'entrées dans mongodb et les indexe dans ElasticSearch (seulement pour les formats et les règles de gestion) .

- La Méthode MongoCursor<?> findDocuments(JsonNode select, FunctionalAdminCollections collection)
                   permet de chercher les documents dans mongoDb (pour les formats et les règles de gestion.
                   On cherche d'abord dans Elasticsearch pour récupérer identifiant unique puis cherche dans mongoDb).

- La Méthode public void updateDocumentByMap(Map<String, Object> map, JsonNode objNode,
        FunctionalAdminCollections collection, UPDATEACTION operator)
                  permet de mettre à jour un ensemble d'entrées dans les document mongodb et l'index ElasticSearch (seulement pour les formats et les règles de gestion).

- La Méthode public void updateData(JsonNode update, FunctionalAdminCollections collection)
                  permet de mettre à jour une entrée dans un document mongodb via une requête au format json

- La Méthode deleteCollection(FunctionalAdminCollections collection)
                  permet de supprimer un ensemble d'entrées dans monfoDb et l'index ElasticSearch (seulement pour les formats et les règles de gestion).
