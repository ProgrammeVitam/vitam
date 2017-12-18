ingest-external-client
######################

Utilisation
###########

Paramètres
**********
Les paramètres sont les InputStreams du fichier SIP pour le dépôt dans la base VITAM

La factory
**********

Afin de récupérer le client, une factory a été mise en place.

.. code-block:: java

    // Récupération du client ingest-external 
    IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

    // Changer la configuration du Factory
    IngestExternalClientFactory.setConfiguration(IngestExternalClientFactory.IngestExternalClientType.MOCK_CLIENT, null);
    // Récupération explicite du client mock
    IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();


Le client
*********


Pour instancier son client en mode Production :

.. code-block:: java

    // Ajouter un fichier functional-administration-client.conf dans /vitam/conf
    // Récupération explicite du client
    IngestExternalClient client = IngestExternalClientFactory.getInstance().getIngestExternalClient();


Le client les méthodes suivantes:

.. code-block:: java

    // Upload un SIP
    RequestResponse<JsonNode> upload(InputStream stream, Integer tenantId, String contextId, String action)
        throws IngestExternalException;
    // Télécharger un object du serveur sauvegardé de l'operation upload ci-dessus avec son ID et type
    Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId)
        throws IngestExternalException;
    // Exécuter une action sur un Process Workflow
    RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId, Integer tenantId)
        throws VitamClientException;
    // Exécuter une action sur un Process Workflow
    Response updateOperationActionProcess(String actionId, String operationId, Integer tenantId) throws VitamClientException;
    // Retourne le statut d'un process workflow et son état
    ItemStatus getOperationProcessStatus(String id, Integer tenantId) throws VitamClientException;
    // Retourne le détail d'un process workflow
    ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query, Integer tenantId) throws VitamClientException;
    RequestResponse<JsonNode> cancelOperationProcessExecution(String id, Integer tenantId) throws VitamClientException, BadRequestException;
    @Deprecated //Not used
    ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow,
        Integer tenantId)
        throws InternalServerException, BadRequestException, VitamClientException;
    void initVitamProcess(String contextId, String container, String workFlow, Integer tenantId)
        throws InternalServerException, VitamClientException;
    @Deprecated
    void initWorkFlow(String contextId, Integer tenantId) throws VitamException;
    RequestResponse<JsonNode> listOperationsDetails(Integer tenantId) throws VitamClientException;


Le client implémente aussi l'interface OperationStatusClient ayant la méthode suivante:

.. code-block:: java

    RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext, String id) throws VitamClientException;

Cette interface est passée comme paramètre au client VitamPoolingClient.
