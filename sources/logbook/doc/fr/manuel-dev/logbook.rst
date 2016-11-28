Logbook
#######

Utilisation
###########

Paramètres
**********

Les paramètres sont représentés via une interface **LogbookParameters** sous le package fr.gouv.vitam.logbook.common.parameters.

L'idée est de représenter les paramètres sous forme de Map<LogbookParameterName, String>.

Une methode getMapParameters() permet de récuperer l'ensemble de ces paramètres.
Une methode getMandatoriesParameters() permet de récuperer un set de paramètre qui ne doivent pas être null ni vide.

On retrouve une implémentation dans la classe **LogbookOperationParameters** qui représente les paramètres pour
journaliser une **opération**.

Il existe egalement une Enum **LogbookParameterName** qui permet de définir tous les noms de paramètres possible. Elle permet de remplir la map de paramètres ainsi que le set permettant de tester les paramètres requis.

La factory
**********

Afin de récupérer le client ainsi que la bonne classe de paramètre, une factory a été mise en place.
Actuellement, elle ne fonctionne que pour le journal des opérations.

.. code-block:: java

    // Récupération du client
    LogbookOperationsClientFactory.changeMode(ClientConfiguration configuration)
    LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

    // Récupération de la classe paramètre
    LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    // Utilisation des setter : parameters.putParameterValue(parameterName, parameterValue);
    parameters.putParameterValue(LogbookParameterName.eventTypeProcess,
                LogbookParameterName.eventTypeProcess.name())
              .putParameterValue(LogbookParameterName.outcome,
                LogbookParameterName.outcome.name());
    
    // Usage recommandé : utiliser le factory avec les arguments obligatoires à remplir
    LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters(args);
    
    // Des helpers pour aider
    parameters.setStatus(LogbookOutcome).getStatus();
    parameters.setTypeProcess(LogbookTypeProcess).getTypeProcess();
    parameters.getEventDateTime();
    parameters.setFromParameters(LogbookParameters).getParameterValue(LogbookParameterName);


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
	  LogbookOperationsClientFactory.changeMode(null)
      // Récupération explicite du client mock
      LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

Pour instancier son client en mode Production :

.. code-block:: java

      // Changer la configuration du Factory
      LogbookOperationsClientFactory.changeMode(ClientConfiguration configuration);
      // Récupération explicite du client
      LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

Le client
*********

Le client propose actuellement quatre méthodes : create, update, selectOperation et selectOperationbyId

Le mock de create et upadate ne vérifie pas l'identifiant (eventIdentifier) ni la date (evendDateTime). En effet, il ne doit pas exister pour le create et inversement pour l'update.

Chacune de ces méthodes prend en arguement la classe paramètre instanciée via la factory et peuplée au besoin.

Le mock de selectOperation retourne un JsonNode qui contient MOCK_SELECT_RESULT_1 et MOCK_SELECT_RESULT_2

Le mock de selectOperationbyId retourne un JsonNode qui contient seulement MOCK_SELECT_RESULT_1. En effet, chaque opération a un identifiant unique.

Chacune de ces méthodes prend en arguement une requête select en String

.. code-block:: java

    // Récupération du client
    LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

    // Récupération de la classe paramètre
    LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    // Utilisation des setter : parameters.putParameterValue(parameterName, parameterValue);

    // create
    client.create(parameters);
    // possibilité de réutiliser le même parameters
    // Utilisation des setter : parameters.putParameterValue(parameterName, parameterValue);
    // update
    client.update(parameters);
    
    // select opération
    client.selectOperation(String select);   
    // select opération par id
    client.selectOperationbyId(String select);

Exemple d'usage générique
=========================


.. code-block:: java

    // Récupération du client
    LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();

    // Récupération de la classe paramètre
    LogbookParameters parameters = LogbookParametersFactory.newLogbookOperationParameters();
    // Utilisation des setter : parameters.putParameterValue(parameterName, parameterValue);
    parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
              GUIDFactory.newOperationId(tenant).getId())
            .setStatus(outcome).setTypeProcess(type);

    // create global du processus AVANT toute opération sur ce processus
    parameters.setStatus(LogbookOutcome.STARTED);
    client.create(parameters);

    // et maintenant append jusqu'à la fin du processus global
    LogbookParameters subParameters = LogbookParametersFactory.newLogbookOperationParameters();
    // Récupère les valeurs du parent: attention à resetter les valeurs propres !
    subParameters.setFromParameters(parameters);
    // Event GUID
    subParameters.putParameterValue(LogbookParameterName.eventIdentifier,
        GUIDFactory.newOperationIdGUID(tenantId).getId());
    // Event Type
    subParameters.putParameterValue(LogbookParameterName.eventType,
        "UNZIP");
    subParameters.setStatus(LogbookOutcome.STARTED);
    // Et autres paramètres
    ...
    // Start sous opération
    client.update(subParameters);
    // Unsip
    subParameters.setStatus(LogbookOutcome.OK);
    // Sous opération OK
    client.update(subParameters);

    // Autres Opérations

    // Fin Opération Globale
    // create global du processus AVANT toute opération sur ce processus
    parameters.setStatus(LogbookOutcome.OK);
    client.update(parameters);

    // Quand toutes les opérations sont terminées
    client.close();


Exemple Ingest
==============

.. code-block:: java

        // Available informations
        // TenantId
        int tenantId = 0;
        // Process Id (SIP GUID)
        String guidSip = "xxx";
        // X-Request-Id
        String xRequestId = "yyy";
        // Global Object Id: in ingest = SIP GUID



        // Récupération du client
        LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();



        // Récupération de la classe paramètre avec ou sans argument
        LogbookParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters();
        LogbookParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
              eventType, eventIdentifierProcess, eventTypeProcess, 
              outcome, outcomeDetailMessage, eventIdentifierRequest);


        // Utilisation du setter
        // Event GUID
        parameters.putParameterValue(LogbookParameterName.eventIdentifier,
            GUIDFactory.newOperationIdGUID(tenantId).getId());
        // Event Type
        parameters.putParameterValue(LogbookParameterName.eventType,
            "UNZIP");
        // Event Identifier Process
        parameters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            guidSip);
        // Event Type Process
        parameters.setTypeProcess(LogbookTypeProcess.INGEST);
        // X-Request-Id
        parameters.putParameterValue(LogbookParameterName.eventIdentifierRequest,
            xRequestId);
        // Global Object Id = SIP GUID for Ingest
        parameters.putParameterValue(LogbookParameterName.objectIdentifier,
            guidSip);



        // Lancement de l'opération
        // Outcome: status
        parameters.setStatus(LogbookOutcome.STARTED);
        // Outcome detail message
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "One infotmation to set before starting the operation");



        // 2 possibilities
        // 1) Démarrage de l'Opération globale (eventIdentifierProcess) dans INGEST première fois
        client.create(parameters);
        // 2) update global process Operation (same eventIdentifierProcess) partout ailleurs
        client.update(parameters);



        // Run Operation
        runOperation();



        // Finalisation de l'opération, selon le statut
        // 1) Si OK
        parameters.setStatus(LogbookOutcome.OK);
        // 2) Si non OK
        parameters.setStatus(LogbookOutcome.ERROR);
        parameters.putParameterValue(LogbookParameterName.outcomeDetail,
            "404_123456"); // 404 = code http, 123456 = code erreur Vitam



        // Outcome detail message
        parameters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            "One infotmation to set after the operation");
        // update global process operation
        client.update(parameters);



        // When all client opération is done
        client.close();




Exemple ihm-demo-web-application
================================ 

.. code-block:: java

    	@POST
    	@Path("/logbook/operations")
    	@Produces(MediaType.APPLICATION_JSON)
    	public Response getLogbookResult(String options)
    	
        // Traduction de Mappeur à la requête DSL
        Map<String, String> optionsMap = JsonHandler.getMapStringFromString(options);
        query = CreateDSLClient.createSelectDSLQuery(optionsMap);
        
        // Récupération du client
        LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getLogbookOperationClient();
        
        // Sélection des opérations par la requête DSL
        result = logbookClient.selectOperation(query);
        
    	@POST
    	@Path("/logbook/operations/{idOperation}")
    	@Produces(MediaType.APPLICATION_JSON)
    	public Response getLogbookResultById(@PathParam("idOperation") String operationId, String options)
    	
    	// Récupération du client
    	LogbookClient logbookClient = LogbookClientFactory.getInstance().getLogbookOperationClient();
        
        // Sélection des opérations par ID
        result = logbookClient.selectOperationbyId(operationId);       