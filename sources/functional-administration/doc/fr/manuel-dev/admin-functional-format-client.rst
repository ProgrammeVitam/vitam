Administration-Management-format
################################

Utilisation
###########

Paramètres
**********

Les paramètres sont les InputStreams du fichier Pronom pour l'import ou la validation.
Pour la recherche des formats, les paramètres sont les requête DSL construites par les builders de common-database

La factory
**********

Afin de récupérer le client, une factory a été mise en place.

.. code-block:: java

    // Récupération du client 
    AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
      AdminManagementClientFactory.setConfiguration(AdminManagementClientFactory.AdminManagementClientType.MOCK_CLIENT, null);
      // Récupération explicite du client mock
      AdminManagementClient client = AdminManagementClientFactory.getInstance().getLogbookClient();


Le client
*********


Pour instancier son client en mode Production :

.. code-block:: java

      // Ajouter un fichier functional-administration-client.conf dans /vitam/conf
	  // Récupération explicite du client
     AdminManagementClient client = AdminManagementClientFactory.getInstance().getAdminManagementClient();
     

Le client propose actuellement 14 méthodes : 

.. code-block:: java

	  Status status();
	  void checkFormat(InputStream stream);
	  void importFormat(InputStream stream);
	  void deleteFormat();
	  JsonNode getFormatByID(String id);
	  JsonNode getFormats(JsonNode query);
     checkRulesFile(InputStream stream);
     importRulesFile(InputStream stream);
     deleteRulesFile();
     JsonNode getRuleByID(String id);
     JsonNode getRule(JsonNode query);
     createorUpdateAccessionRegister(AccessionRegisterDetail register);
     JsonNode getAccessionRegister(JsonNode query);
     JsonNode getAccessionRegisterDetail(JsonNode query);