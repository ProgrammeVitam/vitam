Access-client
*************
Ce module est utilisé par le module ihm-demo(package fr.gouv.vitam.ihmdemo.core).

Utilisation
**********

- La factory : Afin de récupérer le client-access , une factory a été mise en place.

.. code-block:: java

    // Récupération du client
     final AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

- Le Mock
  Si les paramètres de productions sont introuvables, le client passe en mode Mock par défaut.
  Il est possible de récupérer directement le mock :

	.. code-block:: java

      // Changer la configuration du Factory client
      AccessClientFactory.setConfiguration(AccessClientType.MOCK_OPERATIONS);
      
      // Récupération explicite du client mock
        final AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();
        
	- Pour instancier son client en mode Production :

	.. code-block:: java

      // Changer la configuration du Factory
       AccessClientFactory.setConfiguration(AccessClientType.MOCK_OPERATIONS);
      // Récupération explicite du client
      AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();
      
Le client
*********
	Le client propose actuellement une méthode : selectUnits(String dslQuery);
	Paramètre de la fonction : String dsl
	//TODO (Itérations futures : ajouter méthode modification des métadonnées ?)

	Le client récupère une réponse au format Json.

	.. code-block:: java

	Exemple d'usage générique
	=========================
	.. code-block:: java

    // Récupération du client
     AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

    // Récupération du dsl ( cf ihm-demo documentation)    
    
    // Recherche des Archives Units
    JsonNode selectUnits(String dsl)  
