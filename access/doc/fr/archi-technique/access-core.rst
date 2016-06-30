Access-core
***********

Présentation
************
Ce module permet d'implémenter les API publique du module access-api

Packages:
**********
fr.gouv.vitam.access.core

*************************

Classes utilisées

AccessModuleImpl

Classe qui dialogue avec le module métadata. Elle transmet au métadata client d'une requête dsl.

.. code-block:: java

  public JsonNode selectUnit(String selectRequest){
  
  ...
   // Récupération du client métadata
    metaDataClientFactory = new MetaDataClientFactory();
    metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());
  ...
  
// appel du client métadata
  try {
         jsonNode = metaDataClient.selectUnits(
         accessModuleBean != null ? accessModuleBean.getRequestDsl() : "");

       } 
       ...
 }

