metadata-core
#############

Presentation
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
           permet  d'insérer les indexes. Fait appel à une méthode qui permet d'insérer un ensemble d'entrées dans l'index ElasticSearch en se basant sur une requête résultat.


Pour le delete :
- La Méthode  lastDeleteFilterProjection(UpdateToMongodb requestToMongodb, Result last)
      permet de finaliser la requete et supprimer d'index en se basant sur la requete.

- La Méthode removeOGIndexFields(Result last)
            Permet  de supprimer les indexes des object group existants dans le résultat .

La Méthode removeUnitIndexFields(Result last)
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