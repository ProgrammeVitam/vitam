Exemples
########

Recherche par ArchivalAgencyArchiveUnitIdentifier
=================================================

EndPoint: /access-external/v1/units

**Client Java**

.. code-block:: java

    try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
         Integer tenantId = 0; // à titre d'exemple
         String contract = "myContract"; // à titre d'exemple
         final String selectQuery = "{\"$query\": [{\"$eq\": {\"ArchivalAgencyArchiveUnitIdentifier\" : \"20130456/3\"}}]}";
         final JsonNode queryJson = JsonHandler.getFromString(selectQuery);            
         client.selectUnits(new VitamContext(tenantId).setAccessContract(contract), queryJson);
     } catch (InvalidParseOperationException | VitamClientException e) {
         ///Log ...
     }

**Client Java avec construction DSL**

EndPoint: access-external/v1/units

..  code-block:: java

    JsonNode queryDsql = null;
    Integer tenantId = 0; // à titre d'exemple
    String contract = "myContract"; // à titre d'exemple
    try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {            
        Query query = QueryHelper.eq("ArchivalAgencyArchiveUnitIdentifier", "20130456/3");
        SelectMultiQuery select = new SelectMultiQuery()
         .addQueries(query)
         .setLimitFilter(0, 100);    
        client.selectUnits(new VitamContext(tenantId).setAccessContract(contract), select.getFinalSelect());
    } catch (InvalidCreateOperationException | InvalidParseOperationException | VitamClientException e) {
        ///Log ...
    }

**Postman**

POST /access-external/v1/units

Indiquer pour la requête POST :  
 
 * Header :  
  * X-Http-Method-Override : GET
  * X-Tenant-Id : 0
  * X-Access-Contract-Id : myContract
  * Accept: application/json
  * Content-Type: application/json
 
 * Body :
.. code-block:: json
   
   {
     "$roots": [],
     "$query": [
       {
         "$eq": {
           "ArchivalAgencyArchiveUnitIdentifier": "20130456/3"
         }
       }
     ],
     "$filter": {},
     "$projection": {}
   } 


Recherche par producteur (FRAN_NP_005568)
=========================================

**Client Java**

Endpoint : /admin-external/v1/accessionregisters

.. code-block:: java

     Integer tenantId = 0; // à titre d'exemple
     String contract = "myContract"; // à titre d'exemple
     final String queryDsl = "{\"$query\": [{\"$eq\": {\"OriginatingAgency\" : \"FRAN_NP_005568\"}}]}";
     try (AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
         final JsonNode queryJson = JsonHandler.getFromString(queryDsl);
         client.findAccessionRegister(new VitamContext(tenantId).setAccessContract(contract), queryJson);
     } catch (InvalidParseOperationException | VitamClientException e) {
         // LOG
     }

**Client Java avec construction DSL**

Endpoint : /admin-external/v1/accessionregisters

.. code-block:: java

     Integer tenantId = 0; // à titre d'exemple
     String contract = "myContract"; // à titre d'exemple=
     Select select = new Select();
     try (AdminExternalClient client = AdminExternalClientFactory.getInstance().getClient()) {
         select.setQuery(QueryHelper.eq("OriginatingAgency", "FRAN_NP_005568"));
         client.findAccessionRegister(new VitamContext(tenantId).setAccessContract(contract),
             select.getFinalSelect());
     } catch (VitamClientException | InvalidCreateOperationException e) {
         // LOG
     }

     
**Postman**

POST /admin-external/v1/accessionregisters

Indiquer pour la requête POST :  
 
 * Header :  
  * X-Http-Method-Override : GET
  * X-Tenant-Id : 0
  * X-Access-Contract-Id : myContract
  * Accept: application/json
  * Content-Type: application/json
 
 * Body :
 
.. code-block:: json
   
   {
      "$query" : { 
         "$eq" : { "OriginatingAgency" : "FRAN_NP_005568" }
      },
      "$filter":{},
      "$projection":{}      
   }


Recherche par titre AND description AND dates
=============================================

**Client Java**

Endpoint : /access-external/v1/units

.. code-block:: java

     Integer tenantId = 0; // à titre d'exemple
     String contract = "myContract"; // à titre d'exemple
     Select select = new Select();
     try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
         MatchQuery titleQ = QueryHelper.match("Title", "myTitle");
         CompareQuery dateQ = QueryHelper.eq("StartDate", "2015-07-24T02:15:28.28Z");
         MatchQuery descQ = QueryHelper.match("Description", "myDescription");
         select.setQuery(QueryHelper.and().add(titleQ, dateQ, descQ));            
         client.selectUnits(new VitamContext(tenantId).setAccessContract(contract), select.getFinalSelect());
     } catch (InvalidCreateOperationException | VitamClientException e) {
         ///Log ...
     }

**Postman**

GET /access-external/v1/units
Indiquer pour la requête POST :  
 
 * Header :  
  * X-Http-Method-Override : GET
  * X-Tenant-Id : 0
  * X-Access-Contract-Id : myContract
  * Accept: application/json
  * Content-Type: application/json
 
 * Body :
.. code-block:: json
   
   {
     "$roots": [],
     "$query": [
       {
         "$and": [
           {
             "$match": {
               "Title" : "myTitle"
             }
           },
           {
             "$match": {
               "Description" : "myDescription"
             }
           }, 
           {
             "$eq" : { 
               "StartDate" : "2015-07-24T02:15:28.28Z" 
             }
           }
         ]
       }
     ],
     "$filter": {},
     "$projection": {}
   } 

Recherche libre titre OR description
====================================

**Client Java**

Endpoint : /access-external/v1/units

.. code-block:: java

     Integer tenantId = 0; // à titre d'exemple
     String contract = "myContract"; // à titre d'exemple
     Select select = new Select();
     try (AccessExternalClient client = AccessExternalClientFactory.getInstance().getClient()) {
         MatchQuery titleQ = QueryHelper.match("Title", "myTitle");
         MatchQuery descQ = QueryHelper.match("Description", "myDescription");
         select.setQuery(QueryHelper.or().add(titleQ, descQ));            
         client.selectUnits(new VitamContext(tenantId).setAccessContract(contract), select.getFinalSelect());
     } catch (InvalidCreateOperationException | VitamClientException e) {
         ///Log ...
     }


**Postman**

GET /access-external/v1/units

Indiquer pour la requête POST :  
 
 * Header :  
  * X-Http-Method-Override : GET
  * X-Tenant-Id : 0
  * X-Access-Contract-Id : myContract
  * Accept: application/json
  * Content-Type: application/json
 
 * Body :
.. code-block:: json
  
   {
     "$roots": [],
     "$query": [
       {
         "$or": [
           {
             "$match": {
               "Title" : "myTitle"
             }
           },
           {
             "$match": {
               "Description" : "myDescription"
             }
           }
         ]
       }
     ],
     "$filter": {},
     "$projection": {}
   }

