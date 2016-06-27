Access-core

Présentation
###########

Implémente les API publique du module access-api

Packages:
**********

fr.gouv.vitam.access.core

*************************



Classes utilisées

AccessModuleImpl

Classe qui dialogue avec le module metadata. C'est une passe-plat. Elle transmet au metadata client une requête dsl.

.. code-block:: java

  public JsonNode selectUnit(String selectRequest){
  
  ...
   // Récupération du client metadata
    metaDataClientFactory = new MetaDataClientFactory();
    metaDataClient = metaDataClientFactory.create(accessConfiguration.getUrlMetaData());
  ...
  
// appel du client metadata
  try {
         jsonNode = metaDataClient.selectUnits(
         accessModuleBean != null ? accessModuleBean.getRequestDsl() : "");

       } 
       ...
 }

