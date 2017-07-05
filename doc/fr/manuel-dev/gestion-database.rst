Gestion des bases de données
############################

Ce document présente les points d'attention et une check list quand vous avez
une modification à faire sur un schéma de données d'une base de données ou la
création d'une requête particulière MongoDB.

Gestion de l'ajout d'un champ
=============================

Si ce champ n'est pas "protégé" (non préfixé par "**\_**"), seuls les aspects
indexations sont à suivre.

Si ce champ est "protégé" (préfixé par un "**\_**"), quelques règles d'usages sont
à respecter :

- Il est préfixé en base par "**\_**" afin de ne pas être en conflit avec des métadonnées externes (notamment pour le "*content*" du Unit)
- Le nom dans la base doit être court (exemple: **\_us**) afin de limiter l'empreinte mémoire et disque de ce champs tant pour les index que pour les données, et tant pour MongoDB que pour ElasticSearch
- Le nom du point de vue usage (externe et interne) doit être explicite (exemple: **allunitups**)
- Il est préfixé d'un '**#**' pour permettre son interprétation par Vitam comme un champ protégé
- Il cache l'implémentation réelle du champ

Certains de ces champs sont interdits en update/insert (depuis l'extérieur),
mais autorisés en interne.

La définition d'un tel champ "protégé" s'effectue ainsi :

- common-database-vitam

  - common-database-public

    - BuilderToken.java : il contient un enum simple définisssant le champ (exemple: **ALLUNITUPS("allunitups")**)
    - VitamFieldsHelper.java : il contient des helpers pour accéder directement à la représentation formelle (précédé du '**#**') le champ (exemple: **allunitups()**)

      Le QueryBuilder interdit les champs préfixés par "**\_**". Il impose donc l'usage de la notation '**#**'.

  - commmon-database-private

    - ParserTokens.java : il contient la copie exacte de BulderToken mais y ajoute les méthodes
    
      - **notAllowedOnSet()** qui interdit ou pas l'update/insert depuis l'extérieur. Ce check est réalisé par les API-internal via les VarNameAdapter.
      - **getPROJECTIONARGS()*** qui traduit du champ interne en champ externe. Cette fonction est utilisé par les deux ci-dessous.
      - **isNotAnalyzed()** qui indique si le champ n'est pas indexé
      - **isAnArray()** qui indique si le champ est un tableau 
      - **isSingleProtectedVariable** désigne les variables de collections Single
      - **isAnArrayVariable** désigne les variables de collections Single ou Multiple
      - **isSingleNotAnalyzedVariable** désigne les variables de collections Single

    - VarNameAdapter.java pour Unit/ObjectGroup
    - VarNameInsertAdapter.java pour Unit/ObjectGroup
    - VarNameUpdateAdapter.java pour Unit/ObjectGroup *(devra être dupliqué en usage externe et interne : protection de certains champs)*
    - SingleVarNameAdapter.java pour les collections hors Unit/ObjectGroup pour usage interne
    - SingleVarNameAdapterExternal.java pour usage externe pour les collections hors Unit/ObjectGroup

metadata-core : Unit et ObjectGroup
-----------------------------------

- MongoDbVarNameAdapter.java : autorise les update/insert sur les **#protégés** et trafuit dans les champs définitifs définis dans MetadataDocument.java, Unit.java et ObjectGroup.java (exemple: **#allunitups** en **\_us**)
- MongoDbMetadataResponseFilter.java : récupère la réponse et retraduit en sens inverse un champs "**\_xxx**" en son correspondant "**#xxxxxxxxx**" (exemple: **\_us** en **#allunitups**)
- MetadataDocument.java et Unit.java et ObjectGroup.java pour la définition des champs traduits en interne (formats courts comme "**\_us**" et non "**\_unitsparents**")

Pour les autres collections
---------------------------

Elle s'appuie sur SingleVarNameAdapater et devraient avoir leurs propres extensions
(comme MongoDbVarNameAdapter) ainsi que pour les retours (comme MongoDbMetadataResponseFilter)

Modification d'une collection : check list
==========================================

- Pour les champs protégés (préfix **#**)

  - Ajouter le champ dans les classes BuilderToken, VitamFieldsHelper, ParserTokens
  - Vérifier/Modifier les VarNameAdapter de la collection s'ils sont bien pris en compte (tant pour les cas Insert/Update interdits ou pas que pour la traduction dans le nom du champ final)
  - Modifier le ResponseFilter de la collection pour retraduire en #xxxxx la réponse

- Pour tous les champs

  - Mettre à jour le schéma Json pour prendre en compte le nouveau champ et son type
  - Si ce champ est utilisé dans des requêtes MongoDB et/ou consitue une clef primaire modifier avec l'intégration les index techniques MongoDb (optimisation et unicité)
