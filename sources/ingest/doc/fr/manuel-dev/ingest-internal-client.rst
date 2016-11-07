
ingest-internal-client
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

    // Récupération du client ingest-internal 
    IngestInternalClient client = IngestInternalClientFactory.getInstance().getIngestInternalClient();


Le Mock
=======

Par défaut, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
      IngestInternalClientFactory.setConfiguration(IngestInternalClientFactory.IngestInternalClientType.MOCK_CLIENT, null);
      // Récupération explicite du client mock
      IngestInternalClient client = IngestInternalClientFactory.getInstance().getIngestInternalClient();


Le client
*********


Pour instancier son client en mode Production :

.. code-block:: java

      // Ajouter un fichier functional-administration-client.conf dans /vitam/conf
	  // Récupération explicite du client
      IngestInternalClient client = IngestInternalClientFactory.getInstance().getIngestInternalClient();
     

Le client propose deux méthodes : 

.. code-block:: java

	  Status status();
	  UploadResponseDTO upload(String archiveMimeType,List<LogbookParameters> logbookParametersList, InputStream inputStream);



 Cette méthde ( à la version 0.9.0) capable de télécharger un sip compressé en 3 formats (zip, tar, tar.gz)

- Paramètres :
    - archiveMimeType :: String (mimetype de l'archive ;par exemple application/x-tar)
    - logbookParametersList :: List<LogbookParameters>
    - inputStream : InputStream (stream de sip compressé dont le format doit être zip, tar ou tar.gz)
- Retourne : ATR en format xml
- Exceptions : 
