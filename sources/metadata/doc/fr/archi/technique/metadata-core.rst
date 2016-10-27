metadata-core
*******************

Presentation
------------

|  *Parent package:* **fr.gouv.vitam.metadata**
|  *Package proposition:* **fr.gouv.vitam.metadata.core**

Ce parkage implémente les différentes opérationns sur le métadata
 (insertUnit, insertObjectGroup, selectUnitsByQuery, selectUnitsById )
 
1. Modules et packages

|---fr.gouv.vitam.metadata.core.collections: contenant des classes pour gérer les requetes MongoDb 

|---fr.gouv.vitam.metadata.core.utils
|---fr.gouv.vitam.metadata.core
|---fr.gouv.vitam.metadata.core.database.configuration

2. Classes 
Dans cette section, nous présentons quelques classes principales dans des modules/packages 
abordés ci-dessus.

2.1 Class DbRequest :

la classe qui permet de gérer les requetes de metadata .

***la Méthode execRequest(final RequestParserMultiple requestParser, final Result defaultStartSet)
permet de parser le query et définir le type d'objet(Unit ou Object Group) afin de gérer et exécuter la requete .
Les différents traitements sont l'ajout , update et la suppression .

******Pour l'update : 
      la Méthode  lastUpdateFilterProjection(UpdateToMongodb requestToMongodb, Result last)
      permet de finaliser la requete avec la dernière list de mise à jour en testant sur le type d'objet Unit ou Object 
      group et ajout d'index qui correspond au champ mise à jour.
  
  
********** la Méthode indexFieldsUpdated(Result last) 
           
           permet  de mettre à jour les indexes liées aux champs modifiés .
           fait appel à une méthode qui permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch en se basant 
           sur un Curseur de résultat.

2.2 Class ElasticsearchAccessMetadata
   
***************** la Méthode updateBulkUnitsEntriesIndexes(MongoCursor<Unit>)

                   permet de mettre à jour un ensemble d'entrées dans l'index ElasticSearch en se basant sur un Curseur de résultat.
   


