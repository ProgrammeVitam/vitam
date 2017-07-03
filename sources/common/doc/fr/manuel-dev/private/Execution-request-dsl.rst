Implémentation de l'éxécution des requêtes mono-query DSL
#########################################################

Implémentation des query builder
********************************

Pour construire dynamiquement une requête mono-query, on peut utiliser les builders proposés ci-dessous :
   
   Insert : [filter, data] 
   
   - Il élabore la requête d'insertion. Il contient le filtre et les données à insérer     
  
   Select : [query, filter, projection]
   
   - Il élabore la requête de recherche. Contient le query, le filtre et la projection 
   
   Update : [query, filter, actions]
   
   - Il élabore la requête de mise à jour. Contient le query, le filtre et les actions
   
   Delete : [query, filter]
   
   - Il élabore la requête de suppression. Contient le query, le filtre
   
   
.. code-block:: java

   Select selectQuery = new Select(requestInJson)
   ou
   Select selectQuery = new Select().setQuery(query).setfilter(filter).setData(data); 
   
   
.. code-block:: java

   Update updateQuery = new Update(requestInJson)
   ou
   Update updateQuery = new Update().setQuery(query).setfilter(filter).addActions(data);
   
   
.. code-block:: java

   Insert insertQuery = new Insert(requestInJson)

ou

.. code-block:: java

   Insert insertQuery = new Insert().setData(data).setfilter(filter); 
   
   
.. code-block:: java

   Delete deleteQuery = new Delete(requestInJson)
   ou
   Delete deleteQuery = new Delete().setQuery(query).setfilter(filter); 
   
Implémentation de DbRequestSingle
*********************************

DbRequestSingle est une classe pour éxécuter les requêtes DSL mono-query. 

Pour l'initialiser, il faut utiliser le constructeur avec une collection de Vitam.

Le résultat de l'éxécution est un objet DbRequestResult qui contient les informations suivantes:

- boolean wasAcknowledged : l'information reconnue pour la suppression et la mise à jour
- long count: le nombre d'éléments insérés, trouvés, supprimés ou mis à jour 
- Map<String, List<String>> diffs : la différence entre ancien et nouvelle valeur de l'action mise à jour
- MongoCursor<VitamDocument<?>> cursor : le cursor mongo de l'opération de recherche
  
.. code-block:: java

   DbRequestSingle dbrequest = new DbRequestSingle(collection.getVitamCollection());
   Insert insertquery = new Insert();
   insertquery.setData(arrayNode);
   DbRequestResult result = dbrequest.execute(insertquery);

L'implémentation du sort est disponible sur les requêtes MongoDB et ElasticSearch.
