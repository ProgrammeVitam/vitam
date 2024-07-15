Logbook-lifecycle
##################

Utilisation
###########

Paramètres
**********

Les paramètres sont représentés via une interface **LogbookParameters** sous le package fr.gouv.vitam.logbook.common.parameters.

L'idée est de représenter les paramètres sous forme de Map<LogbookParameterName, String>.

Une methode getMapParameters() permet de récuperer l'ensemble de ces paramètres.
Une methode getMandatoriesParameters() permet de récuperer un set de paramètre qui ne doivent pas être null ni vide.

On retrouve une implémentation dans la classe
**LogbookLifeCycleObjectGroupParameters** qui représente les paramètres pour journaliser un cycle de vie d'object group.
**LogbookLifeCycleUnitParameters** qui représente les paramètres pour journaliser un cycle de vie d'archive unit.

Il existe egalement une Enum **LogbookParameterName** qui permet de définir tous les noms de paramètres possible. Elle permet de remplir la map de paramètres ainsi que le set permettant de tester les paramètres requis.

La factory
**********

Afin de récupérer le client ainsi que la bonne classe de paramètre, une factory a été mise en place.

.. code-block:: java

  // Récupération du client
  LogbookLifeCyclesClientFactory.changeMode(ClientConfiguration configuration)
  LogbookLifeCycleClient client = LogbookLifeCyclesClientFactory.getInstance().getClient();
  // Récupération de la classe paramètre pour Object Group
  LogbookParameters parameters = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
  // Récupération de la classe paramètre pour Archive Unit
  LogbookParameters parameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters();
  // Utilisation des setters pour Object Group et Archive Unit : parameters.putParameterValue(parameterName, parameterValue);
  parameters.putParameterValue(LogbookParameterName.agentIdentifier,
      SERVER_IDENTITY.getJsonIdentity());
  parameters.putParameterValue(LogbookParameterName.eventDateTime,
      LocalDateUtil.nowFormated());
  // Usage recommandé : utiliser le factory avec les arguments obligatoires à remplir
	// Object Group
  LogbookParameters parameters = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(args);
  // Archive Unit
  LogbookParameters parameters = LogbookParametersFactory.newLogbookLifeCycleUnitParameters(args);
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
	LogbookLifeCyclesClientFactory.changeMode(null)
  // Récupération explicite du client mock
  LogbookClient client = LogbookLifeCyclesClientFactory.getInstance().getClient();

Pour instancier son client en mode Production :

.. code-block:: java

  // Changer la configuration du Factory
  LogbookLifeCyclesClientFactory.changeMode(ClientConfiguration configuration)
  // Récupération explicite du client
  LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient();

Le client
*********

Le client propose actuellement six méthodes : create, update, commit, rollback, selectOperation et selectLifeCycles et selectLifeCyclesById

// TODO

Cas d'usage provenant de processing.
