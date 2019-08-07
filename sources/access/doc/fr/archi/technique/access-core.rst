Access-core
############

Présentation
************
Ce module permet d'implémenter les :term:`API` publiques du module access-api

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

Récupération d'un objet spécifique
==================================

Il faut utiliser la méthode getOneObjectFromObjectGroup() pour récupérer un objet binaire.

Exemple :

.. code-block:: java

  try {
    InputStream objectData = getOneObjectFromObjectGroup("idObjectGroup", queryAsJsonNode, "BinaryMaster", 0, "0");
  } catch (MetaDataNotFoundException exc) {
    // Handle objectGroup not found
  } catch (StorageNotFoundException exc) {
    // Object with given qualifier and version was not found in storage offer
  } catch (InvalidParseOperationException exc) {
    // Handle badly formatted json query
  } catch (AccessExecutionException exc) {
    // Technical exception that should not happen. The message give details on the error
  }
