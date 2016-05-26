Logbook
#######

Utilisation
###########

Paramètres
==========

Les paramètres sont représentés via une interface **LogbookParameters** sous le package fr.gouv.vitam.logbook.common
.parameters.

L'idée est de représenter les paramètres sous forme de Map<String, String>.

Une methode getMapParameters() permet de récuperer l'ensemble de ces paramètres.
Une methode getMandatoriesParameters() permet de récuperer un set de paramètre qui ne doivent pas être null ni vide.

On retrouve une implémentation dans la classe **LogbookOperationParameters** qui représente les paramètres pour
journaliser une **opération**.

Il existe egalement une Enum **ParameterName** qui permet de définir tous les noms de paramètres possible. Elle permet
 de remplir la map de paramètres ainsi que le set permettant de tester les paramètres requis.

La factory
==========

Afin de récupérer le client ainsi que la bonne classe de paramètre, une factory a été mise en place.
Actuellement, elle ne fonctionne que pour le journal des opérations.

Ex:
.. code-block:: java

    // Récupération du client
    LogbookClient client = LogbookClientFactory.getLogbookClient(LogbookClientFactory.OPERATIONS);

    // Récupération de la classe paramètre
    LogbookParameters parameters = LogbookClientFactory.newOperationParameters();
    // Utilisation des setter : parameters.setValue(parameterName, parameterValue);
    parameters.setValue(LogbookParameterName.eventTypeProcess.name(),
            LogbookParameterName.eventTypeProcess.name()).setValue(LogbookParameterName.outcome.name(), LogbookParameterName.outcome.name());

Il est possible de récupérer directement le mock :

.. code-block:: java

    // Récupération explicite du client mock
    LogbookClient client = LogbookClientFactory.getLogbookClient(LogbookClientFactory.MOCK_OPERATIONS);

Le client
=========

Le client propose actuellement deux méthode : create et update.

Le mock ne vérifie pas les identifiants (eventIdentifier). En effet, il ne doit pas exister pour le create et
inversement pour l'update.

Chacune de ces méthodes prend en arguement la classe paramètre instanciée via la factory et peuplée au besoin.

Ex:
.. code-block:: java

    // Récupération du client
    LogbookClient client = LogbookClientFactory.getLogbookClient(LogbookClientFactory.OPERATIONS);

    // Récupération de la classe paramètre
    LogbookParameters parameters = LogbookClientFactory.newOperationParameters();
    // Utilisation des setter : parameters.setValue(parameterName, parameterValue);
    parameters.setValue(LogbookParameterName.eventTypeProcess.name(),
            LogbookParameterName.eventTypeProcess.name()).setValue(LogbookParameterName.outcome.name(), LogbookParameterName.outcome.name());

    // create
    String ret = client.create(parameters);
