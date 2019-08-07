Administration-Management-Common
###################################

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
----------------------------------------------

Class ElasticsearchAccessFunctionalAdmin : il s'agit de la classe qui permet de gérer les requêtes de functional.administration à la base de données ElasticSearch
Les différents traitements sont l'ajout, la recherche et la suppression.

Pour la recherche :

- La méthode search(final FunctionalAdminCollections collection, final QueryBuilder query, final QueryBuilder filter) permet de chercher dans l'index Elasticsearch avec le query et le filter.

Pour l'insert :

- La Méthode addIndex(final FunctionalAdminCollections collection) permet d'ajouter un index dans Elasticsearch
- La Méthode addEntryIndexes(final FunctionalAdminCollections collection, final Map<String, String> mapIdJson) permet  d'insérer les indexes dans l'index ElasticSearch.

Pour le delete :

- La Méthode deleteIndex(final FunctionalAdminCollections collection) permet  de supprimer un index dans Elasticsearch.

2.2 Class MongoDbAccessAdminImpl
-------------------------------------

- La Méthode insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection)

  permet d'insérer un ensemble d'entrées dans mongodb et les indexe dans ElasticSearch (seulement pour les formats et les règles de gestion) .

- La Méthode MongoCursor<?> findDocuments(JsonNode select, FunctionalAdminCollections collection)

  permet de chercher les documents dans mongoDb (pour les formats et les règles de gestion. On cherche d'abord dans Elasticsearch pour récupérer identifiant unique puis cherche dans mongoDb).

- La Méthode public void updateDocumentByMap(Map<String, Object> map, JsonNode objNode,FunctionalAdminCollections collection, UPDATEACTION operator)

  permet de mettre à jour un ensemble d'entrées dans les document mongodb et l'index ElasticSearch (seulement pour les formats et les règles de gestion).

- La Méthode public void updateData(JsonNode update, FunctionalAdminCollections collection)

  permet de mettre à jour une entrée dans un document mongodb via une requête au format json

- La Méthode deleteCollection(FunctionalAdminCollections collection)

  permet de supprimer un ensemble d'entrées dans monfoDb et l'index ElasticSearch (seulement pour les formats et les règles de gestion).


3. Mapping elasticsearch des documents (recherche rapprochée)

Cette section concerne le mapping elasticsearch des documents géré au niveau functional administration. Mais c'est la même règle partout ailleur.

Pour qu'un document soit analysé par elasticsearch et que la recherche rapprochée marche il faut ce qui suit :

- Ajouter un paramètre typeunique au document concerné. Ce paramètre est utilisé par elasticsearch.

Exemple: le document profile contient bien un paramètre :

.. sourcecode:: java

   public static final String TYPEUNIQUE = "typeunique";


- Créer dans le dossier resources les fichiers mapping au format json.

profile-es-mapping.json, accesscontract-es-mapping.json, ....

Exemple de fichier json de mapping elasticsearch:

.. sourcecode:: json

   {
    "properties": {
      "Name": {
        "type": "string"
      },
      "Status": {
        "type": "string",
        "index": "not_analyzed"
      },
      "CreationDate": {
        "type": "date",
        "format": "strict_date_optional_time"
      },
      "LastUpdate": {
        "type": "string",
        "index": "not_analyzed"
      }
    }
   }



- Ces fichers sont ensuite chargé au niveau de ElasticsearchAccessFunctionalAdmin.

- Dans la méthode getMapping de ElasticsearchAccessFunctionalAdmin, il faut rajouter le document concerné, ainsi récupérer le mapping correspondant.

.. sourcecode:: java

   private String getMapping(FunctionalAdminCollections collection) throws IOException {
      if (collection.equals(FunctionalAdminCollections.PROFILE)) {
          return ElasticsearchUtil.transferJsonToMapping(FileRules.class.getResourceAsStream(MAPPING_PROFILE_FILE_JSON));
      }
      return "";
   }

- Dans la méthode getTypeUnique ajouter TYPEUNIQUE du document concerné.

.. sourcecode:: java

   private String getTypeUnique(FunctionalAdminCollections collection) {
      if (collection.equals(FunctionalAdminCollections.PROFILE)) {
              return PROFILE.TYPEUNIQUE;
      }
      return "";
   }

Attention:

- Il faut supprimer l'index s'il existe déjà pour qu'il puisse être crée avec les bon `mappings`.
- Si on supprime l'index, il faut ré-indexer les données de la base de données.

