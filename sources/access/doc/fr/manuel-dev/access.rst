Composant Access
################

Utilisation
===========

Configuration
*************
Le module d'access est configuré par un POM qui contient les informations nécessaires (nom du projet, numéro de version, identifiant du module parent, les sous modules (common, api, core, rest, client) de sous module d'access, etc..).
Ces informations sont contenues dans le fichier pom.xml présent dans le répertoire de base du module Access.

.. code-block:: xml

    <parent>
        <groupId>fr.gouv.vitam</groupId>
        <artifactId>parent</artifactId>
        <version>${vitam.version}</version>
    </parent>

    <artifactId>access</artifactId>
    <packaging>pom</packaging>
    
    <modules>
         <module>access-internal</module>
         <module>access-external</module>
    </modules>


La factory
**********

Afin de récupérer le client une factory a été mise en place.

.. code-block:: java

     // Récupération du client
     final AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock.
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


L'application rest
******************
La méthode run avec l'argument de port permet aux tests unitaires de démarrer sur un port spécifique.
Le premier argument contient le nom du fichier de configuration access.conf (il est templatiser avec ansible)


Le client
*********

    Le client propose actuellement plusieurs méthodes permettant de gérer la lecture et la modification des collections Units, LogbookOperation, ObjectGroup, Lifecycle (Unit et OG) et de gérer l'export DIP.

    Le client récupère une réponse au format Json ou au format InputStream.
    
Le client AdminExternalClient implémente aussi l'interface OperationStatusClient ayant la méthode suivante:

.. code-block:: java

    RequestResponse<ItemStatus> getOperationProcessStatus(VitamContext vitamContext, String id) throws VitamClientException;

Cette interface est passée comme paramètre au client VitamPoolingClient.
    

Exemple d'usage générique
=========================

.. code-block:: java

    // Récupération du client dans le module ihm-demo
     AccessClient client = AccessClientFactory.getInstance().getAccessOperationClient();

    // Récupération du dsl ( cf ihm-demo documentation)   
   
    // Recherche des Archives Units
    JsonNode selectUnits(String dsl) 
    
    // Recherche des Units par Identification
     JsonNode selectUnitbyId(String sqlQuery, String id)
     
     //Recherche d'object par ID + un DSL selectObjectQuery 
     JsonNode jsonObject = client.selectObjectbyId(String selectObjectQuery, String id);
     
     //Récupération d'un objet au format input stream
     InputStream stream = client.getObjectAsInputStream(String selectObjectQuery, String objectGroupId, String usage, int version);

Exemple d'usage générique
=========================
.. code-block:: java

    // Récupération du client
   private static final AccessClient ACCESS_CLIENT = AccessClientFactory.getInstance().getAccessOperationClient();

     ...
  
    // Autres Opérations
    
    public static JsonNode searchUnits(String parameters)
            throws AccessClientServerException, AccessClientNotFoundException, InvalidParseOperationException {
        return ACCESS_CLIENT.selectUnits(parameters);
    }
