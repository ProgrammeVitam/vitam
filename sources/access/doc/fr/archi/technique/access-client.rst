Access-client
*************
Ce module est utilisé par le module ihm-demo(package fr.gouv.vitam.ihmdemo.core).

Utilisation
***********

- La factory : Afin de récupérer le client-access , une factory a été mise en place.

.. code-block:: java

    // Récupération du client
     final AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

- Le Mock
  Si les paramètres de productions sont introuvables, le client passe en mode Mock par défaut.
  Il est possible de récupérer directement le mock :

.. code-block:: java

    // Changer la configuration du Factory client
    AccessClientFactory.setConfiguration(AccessClientType.MOCK);
    // Récupération explicite du client mock
    final AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

     
- Pour instancier son client en mode Production :

.. code-block:: java

  // Changer la configuration du Factory
   AccessClientFactory.setConfiguration(AccessClientType.PRODUCTION);
  // Récupération explicite du client
  AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();
      
Le client
*********
Le client propose actuellement plusieurs méthodes : 

  selectUnits(String dslQuery);
  selectUnitbyId(String sqlQuery, String id);
  updateUnitbyId(String updateQuery, String unitId);
  selectObjectbyId(String selectObjectQuery, String objectId);
  getObjectAsInputStream(String selectObjectQuery, String objectGroupId, String usage, int version);

Paramètre de la fonction : String ds, String Identification

.. //TODO (Itérations futures : ajouter méthode modification des métadonnées ?)

Le client récupère une réponse au format Json ou au format InputStream.
