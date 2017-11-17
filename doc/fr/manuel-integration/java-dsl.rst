DSL Java Vitam
##############

Cette partie va essayer de montrer quelques exemples d'usages du DSL à l'aide de la librairie DSL Java Vitam dans différentes conditions.

Génération de requêtes DSL en Java
==================================

Les clients externes java Vitam offrent la possibilité de créer les requêtes DSL à partir des librairies DSL. Il existent 4 types de requêtes DSL au format Json :

- requêtes DSL de recherche (SELECT SINGLE)
- requêtes DSL de recherche de type graphe (SELECT MULTIPLE) **EXPERIMENTAL**
- requête DSL d'accès unitaire (GET BY ID) qui peut se générer de deux manières différentes
- requête DSL de modification unitaire (UPDATE BY ID) qui peut se générer de deux manières différentes

Pour le choix de la requête nécessaire, se référer à la document de l'API rest Vitam.
Exemples de code de génération :

- requête DSL graphe pour recherche sur métadonnées : Select Multi Query (collections multi-query : Unit et Objects)

::

   include fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   Query query1 = match("Title", "titre").setDepthLimit(4);
   Query query2 = exists("FilePlanPosition").setDepthLimit(3);
   SelectMultiQuery select = new SelectMultiQuery().addRoots("id0")
         .addQueries(query1, query2)
         .setLimitFilter(0, 100)
         .addProjection(id(), "Title", type(), parents(), object());
   JsonNode json = select.getFinalSelect();


- requête DSL unitaire d'accès pour les métadonnées : Select By Id (collections multi-query : Unit et Objects)

::

   include fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   SelectMultiQuery select = new SelectMultiQuery()
         .addProjection(id(), "Title");
   JsonNode json = select.getFinalSelectById();


- requête DSL graphe pour recherche sur les données référentiel et logbook : Select Single Query

::

   include fr.gouv.vitam.common.database.builder.request.single.Select;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   Query query = eq("Identifier", "ID");
   Select select = new Select()
         .setQuery(query)
         .setLimitFilter(0, 100)
         .addProjection();
   JsonNode json = select.getFinalSelect();


- requête DSL unitaire d'accès pour les données référentiel et logbook : Select By Id

::

   include fr.gouv.vitam.common.database.builder.request.single.Select;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;

   Select select = new Select()
         .addProjection(id(), "Name");
   JsonNode json = select.getFinalSelectById();


- requête DSL de modification unitaire pour les métadonnées : Update By Id (collection multi-query : Unit et Objects)

::

   include fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;

   Action action = set("Description", "Ma nouvelle description");
   UpdateMultiQuery update = new UpdateMultiQuery()
           .addAction(action);
   JsonNode json = update.getFinalUpdateById();


- requête DSL de modification unitaire pour les données référentiel et logbook : Update By Id (collection single)

::

   include fr.gouv.vitam.common.database.builder.request.single.Update;
   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;

   Action action = set("Name", "Mon nouveau nom");
   Update update = new Update().addActions(action);
   JsonNode json = update.getFinalUpdateById();



Exemples d'usages du DSL
========================

Partie $query
-------------


- $and, $or, $not

::

   { "$and" : [ { "$gte" : { "StartDate" : "2014-03-23T00:00:00" } }, { "$lt" : { "StartDate" : "2014-04-23T00:00:00" } } ] }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = and().add(gte("StartDate", dateFormat.parse("2014-03-23T00:00:00")), 
            lt("StartDate", dateFormat.parse("2014-04-23T00:00:00"));


- $eq, $ne, $lt, $lte, $gt, $gte

::

   { "$gte" : { "StartDate" : "2014-03-23T00:00:00" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = gt("StartDate", dateFormat.parse("2014-03-23T00:00:00"));


- $range

::

   { "$range" : { "StartDate" : { "$gte" : "2014-03-23T00:00:00", "$lt" : "2014-04-23T00:00:00" } } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = range("StartDate", dateFormat.parse("2014-03-23T00:00:00"), true, 
         dateFormat.parse("2014-04-23T00:00:00"), true);


- $exists

::

   { "$exists" : "StartDate" }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = exists("StartDate");


- $in, $nin

::

   { "$in" : { ""#unitups" : ["id1", "id2"] } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = in(unitups(), "id1", "id2");


- $wildcard

::

   { "$wildcard" : { "#type" : "FAC*01" } }

   static include fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.*;
   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = wildcard(type(), "FAC*01");


- $match, $match\_all, $match\_phrase, $match\_phrase\_prefix

::

   { "$match" : { "Title" : "Napoléon Waterloo" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = match("Title", "Napoléon Waterloo");


::

   { "$match_phrase" : { "Description" : "le petit chat est mort" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = matchPhrase("Description", "le petit chat est mort");


- $regex

::

   { "$regex" : { "Identifier" : "AC*" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = regex("Title", "AC*");


- $search

::

   { "$search" : { "Title" : "\"oeufs cuits\" +(tomate | patate) + -frite" } }

   static include fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
   Query query = search("Title", "\"oeufs cuits\" +(tomate | patate) + -frite");



Partie $action dans la fonction Update
--------------------------------------


- $set

::

   { "$set" : { "Title" : "Mon nouveau titre", "Description" : "Ma nouvelle description" }" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = set("Title", "Mon nouveau titre").add("Description", "Ma nouvelle description");


- $unset

::

   { "$unset" : [ "StartDate", "EndDate" ]" }

   static include fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.*;
   Action action = unset("StartDate", "EndDate");

