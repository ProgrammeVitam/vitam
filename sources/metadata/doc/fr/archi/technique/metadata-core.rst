metadata-core
#############

Présentation
============

*Parent package:* **fr.gouv.vitam.metadata**

*Package proposition:* **fr.gouv.vitam.metadata.core**

Ce package implémente les différentes opérations sur le module métadata
 (insertUnit, insertObjectGroup, selectUnitsByQuery, selectUnitsById )

1. Modules et packages
======================

---fr.gouv.vitam.metadata.core.collections: contenant des classes pour gérer les requetes MongoDb

---fr.gouv.vitam.metadata.core.utils

---fr.gouv.vitam.metadata.core

---fr.gouv.vitam.metadata.core.database.configuration

2. Classes
==========

Dans cette section, nous présentons quelques classes principales dans les modules/packages
abordés ci-dessus.

2.1 Class DbRequest
-------------------

La classe qui permet de gérer les requetes de metadata : la Méthode execRequest(final RequestParserMultiple requestParser, final Result defaultStartSet)
permet de parser le query et définir le type d'objet(Unit ou Object Group) afin de gérer et exécuter la requete .
Les différents traitements sont l'ajout, l'update et la suppression.

Pour l'update :
- La Méthode lastUpdateFilterProjection(UpdateToMongodb requestToMongodb, Result last)
  Permet de finaliser la requete avec la dernière list de mise à jour en testant sur le type d'objet Unit ou Object group et ajout d'index qui correspond au champ mise à jour.
- La Méthode indexFieldsUpdated(Result last)
  Permet  de mettre à jour les indexes liées aux champs modifiés de Units. Fait appel à une méthode qui permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch en se basant sur un Curseur de résultat.
- La méthode indexFieldsOGUpdated(Result last)
  Permet  de mettre à jour les indexes liées aux champs modifiés de Object Group.
  fait appel à une méthode qui permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch en se basant
  sur un Curseur de résultat.

Pour l'insert :
- La Méthode  lastInsertFilterProjection(UpdateToMongodb requestToMongodb, Result last)
  Permet de finaliser la requete et ajout d'index qui correspond au champ mise à jour.


- La Méthode insertBulk(InsertToMongodb requestToMongodb, Result result)
  Permet  d'insérer les indexes. Fait appel à une méthode qui permet d'insérer un ensemble d'entrées dans l'index ElasticSearch en se basant sur une requête résultat.


Pour le delete :
- La Méthode  lastDeleteFilterProjection(UpdateToMongodb requestToMongodb, Result last)
  Permet de finaliser la requete et supprimer d'index en se basant sur la requete.
- La Méthode removeOGIndexFields(Result last)
  Permet  de supprimer les indexes des object group existants dans le résultat .
- La Méthode removeUnitIndexFields(Result last)
  Permet  de supprimer les indexes des units existants dans le résultat.

2.2 Class ElasticsearchAccessMetadata
-------------------------------------

- La Méthode updateBulkUnitsEntriesIndexes(MongoCursor<Unit>)
  permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch en se basant sur un Curseur de résultat.
- La Méthode updateBulkOGEntriesIndexes(MongoCursor<ObjectGroup>)
  permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch de Object Group en se basant sur un Curseur de résultat.
- La Méthode insertBulkUnitsEntriesIndexes(MongoCursor<Unit> cursor)
  permet d'insérer un ensemble d'entrées dans l'index ElasticSearch de Units en se basant sur un Curseur de résultat.
- La Méthode updateBulkOGEntriesIndexes(MongoCursor<ObjectGroup> cursor)
  permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch de Object Group en se basant sur un Curseur de résultat.
- La Méthode deleteBulkOGEntriesIndexes(MongoCursor<ObjectGroup> cursor)
  permet de supprimer un ensemble d'entrées dans l'index ElasticSearch de Object Group en se basant sur un Curseur de résultat.
- La Méthode  deleteBulkUnitsEntriesIndexes(MongoCursor<Unit> cursor)
  permet de supprimer un ensemble d'entrées dans l'index ElasticSearch de Unit en se basant sur un Curseur de résultat.


2.3 Class MetaDataImpl
-------------------------------------

- La Méthode insertUnit(JsonNode insertRequest)
                   permet de rechercher un ensemble d'entrée dans la collection Unit en se basant sur la requête DSL.
- La Méthode insertObjectGroup(JsonNode objectGroupRequest)
                   permet de mettre à jour un ensemble d'entrée dans l'index ElasticSearch de Object Group en se basant sur un curseur de résultat.
- La Méthode selectUnitsByQuery(JsonNode selectQuery)
                  permet de rechercher un ensemble d'entrée dans la collection Unit en se basant sur la requête DSL.
- La Méthode selectUnitsById(JsonNode selectQuery, String unitId)
                  permet de rechercher un ensemble d'entrée dans la collection Unit en se basant sur la requête DSL et Id d'un Unit.
- La Méthode selectObjectGroupById(JsonNode selectQuery, String objectGroupId)
                  permet de rechercher un ensemble d'entrée dans la collection ObjectGroup en se basant sur la requête DSL et Id d'un Unit.
- La Méthode selectMetadataObject(JsonNode selectQuery, String unitOrObjectGroupId,
        List<BuilderToken.FILTERARGS> filters)
                  permet de rechercher un ensemble d'entrée dans les collections Unit et ObjectGroup en se basant sur la requête DSL, Id et le filtre.

2.4 Class UnitNode
-------------------------------------

- La Méthode buildAncestors(Map<String, UnitSimplified> parentMap, 
        Map<String, UnitNode> allUnitNode, Set<String> rootList)
                   permet de construire un graphe DAG pour les objets dans Vitam.


2.5 Class UnitRuleCompute
-------------------------------------

- La Méthode computeRule()
  permet de calculer les règles de gestion héritées dans un graphe. Chaque node va calculer un UnitInheritedRule grâce à celui 
  de son parent avec ses propres règles de gestions puis concatener les règles (s'il a plusieurs parents).

2.5 Class UnitInheritedRule
-------------------------------------

- La Méthode createNewInheritedRule(ObjectNode unitManagement, String unitId)
  permet de calculer les règles de gestion héritées en utilisant le règle du parent avec ses propres règles de gestion.
- La Méthode concatRule(UnitInheritedRule parentRule) 
  permet de concaténer les règles de gestion héritées de plusieurs parents.


