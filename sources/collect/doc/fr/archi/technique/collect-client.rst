Access-client
*************
Ce module est utilisé par les test d'integration(package fr.gouv.vitam.collect).

Utilisation
***********

- La factory : Afin de récupérer le client-collect , une factory a été mise en place.

.. code-block:: java

    // Récupération du client
     final CollectClient collectClient = CollectClientFactory.getInstance().getClient();

- Le Mock
  Si les paramètres de productions sont introuvables, le client passe en mode Mock par défaut.
  Il est possible de récupérer directement le mock :

     
- Pour instancier son client en mode Production :

.. code-block:: java

  // Changer la configuration du Factory
   CollectClientFactory.changeMode(COLLECT_CLIENT_CONF);
      
Le client
*********
Le client propose actuellement plusieurs méthodes : 

  initTransaction(TransactionDto transactionDto);
  uploadArchiveUnit(String transactionId, JsonNode unitJsonNode);
  addObjectGroup(String unitId, String usage, Integer version, JsonNode objectJsonNode);
  addBinary(String unitId, String usage, Integer version, InputStream inputStreamUploaded);
  closeTransaction(String transactionId);
  ingest(String transactionId);
  uploadProjectZip(VitamContext vitamContext, String projectId, InputStream inputStreamUploaded);
  selectUnits(VitamContext vitamContext, JsonNode jsonQuery);
  getUnitsByProjectId(VitamContext vitamContext, String projectId, JsonNode dslQuery)
  getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage, int version)

