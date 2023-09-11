collect-client
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

  initProject(VitamContext vitamContext, ProjectDto projectDto)
  updateProject(VitamContext vitamContext, ProjectDto projectDto)
  getProjectById(VitamContext vitamContext, String projectId)
  getTransactionByProjectId(VitamContext vitamContext, String projectId)
  getTransactionById(VitamContext vitamContext, String transactionId)
  getProjects(VitamContext vitamContext)
  deleteTransactionById(VitamContext vitamContext, String transactionId)
  deleteProjectById(VitamContext vitamContext, String projectId)
  getUnitById(VitamContext vitamContext, String unitId)
  getUnitsByTransaction(VitamContext vitamContext, String transactionId, JsonNode query)
  getObjectById(VitamContext vitamContext, String gotId)
  initTransaction(VitamContext vitamContext, TransactionDto transactionDto, String projectId)
  uploadArchiveUnit(VitamContext vitamContext, JsonNode unitJsonNode, String transactionId)
  addObjectGroup(VitamContext vitamContext, String unitId, Integer version, JsonNode objectJsonNode, String usage)
  addBinary(VitamContext vitamContext, String unitId, Integer version, InputStream inputStreamUploaded, String usage)
  closeTransaction(VitamContext vitamContext, String transactionId)
  ingest(VitamContext vitamContext, String transactionId)
  abortTransaction(VitamContext vitamContext, String transactionId)
  reopenTransaction(VitamContext vitamContext, String transactionId)
  uploadProjectZip(VitamContext vitamContext, String transactionId, InputStream inputStreamUploaded)
  getObjectStreamByUnitId(VitamContext vitamContext, String unitId, String usage, int version)
  searchProject(VitamContext vitamContext, CriteriaProjectDto criteria)
  updateTransaction(VitamContext vitamContext, TransactionDto transactionDto)
  updateUnits(VitamContext vitamContext, String transactionId, InputStream is)
  selectUnitsWithInheritedRules(VitamContext vitamContext, JsonNode selectQuery)