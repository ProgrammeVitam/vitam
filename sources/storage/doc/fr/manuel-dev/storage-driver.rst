Storage Driver
**************

**Note** : la récupération du bon driver associée à l'offre qui doit être utilisée est la responsabilité du
DriverManager et ne sera pas décrit ici.

Utilisation d'un Driver
-----------------------

Comme expliqué dans la section architecture technique, le driver est responsable de l'établissement d'une connexion
avec une ou plusieurs offres de stockage distantes. Le choix du driver à utiliser est la responsabilité du
DriverManager qui fournit l'implémentation (si elle existe) du bon **Driver** en fonction de l'identifiant de l'offre
de stockage.


Vérifier la disponibilité de l'offre
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    // 1Vérification de la disponibilité de l'offre
    if (myDriver.isStorageOfferAvailable("http://my.storage.offer.com", parameters)) {
        // L'offre est disponible est accessible
    } else {
        // L'offre est indisponible
    }

Vérification de la capacité de l'offre
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        // Le tenantId afin de récupérer la capacité
        Integer tenantId = 0;
        // Récupération de la capacité
        StorageCapacityResult capacity = myConnection.getStorageCapacity(tenantId);
        // On peut ici verifier que l'espace disponible est suffisant par exemple
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }

Put d'un objet dans l'offre de stockage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    String digestAlgorithm = DigestType.MD5.getName();
    InputStream dataStream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        StoragePutRequest request = new StoragePutRequest(tenantId, type, guid, digestAlgorithm, dataStream);
        StoragePutResult result = myConnection.putObject(request);
        // On peut vérifier ici le résultat du put
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }


Get d'un objet dans l'offre de stockage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        StorageObjectRequest request = new StorageObjectRequest(tenantId, type, guid);
        StorageGetResult result = myConnection.getObject(request);
        // On peut vérifier ici le résultat du get
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }


Head d'un objet dans l'offre de stockage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        StorageObjectRequest request = new StorageObjectRequest(tenantId, type, guid);
        Boolean result = myConnection.objectExistsInOffer(request);
        // On peut vérifier ici le résultat du head
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }
    
Delete d'un objet dans l'offre de stockage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    String digestAlgorithm = DigestType.MD5.getName();
    final Digest digest = new Digest(algo);
    InputStream dataStream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
    digest.update(dataStream);
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        StorageRemoveRequest request = new StorageRemoveRequest(tenantId, type, guid, digestType, digest.toString());
        StorageRemoveResult result = myConnection.removeObject(request);
        // On peut vérifier ici le résultat du delete
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }
    
Lister des types d'objets dans l'offre de stockage
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    String digestAlgorithm = DigestType.MD5.getName();
    final Digest digest = new Digest(algo);
    InputStream dataStream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
    digest.update(dataStream);
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        // Construction de l'objet permettant d'effectuer la requete. L'identifiant du curseur n'existe pas et est à
        // null, c'est une demande de nouveau cusreur, x-cursor à vrai.
        StorageListRequest request = new StorageListRequest(tenantId, type, null, true);
        try (CloseableIterator<ObjectEntry> result = myConnection.listObjects(request)) {
            // On peut alors itérer sur le résultat
            while(result.hasNext()) {
                JsonNode json = result.next();
                // Traitement....
            }
        }
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }

Récupérer les metadatas d'un objet    
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code-block:: java

    // Définition des paramètres nécessaires à l'établissement d'une connexion avec l'offre de stockage
    // Note: dans un vrai cas d'utilisation, ces paramètres doivent être récupérés de la configuration de
    // l'offre et ne pourrons pas être défini en dur de cette manière car l'utilisation des drivers est un traitement
    // générique à la fois vis à vis de l'offre et vis à vis du driver.
    Properties parameters = new Properties();
    parameters.put(StorageDriverParameterNames.USER.name(), "bob");
    parameters.put(StorageDriverParameterNames.PASSWORD.name(), "p4ssword");

    Integer tenantId = 0;
    String type = DataCategory.OBJECT.getFolder();
    String guid = "GUID";
    String digestAlgorithm = DigestType.MD5.getName();
    final Digest digest = new Digest(algo);
    InputStream dataStream = new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf"));
    digest.update(dataStream);
    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        // Construction de l'objet permettant d'effectuer la requete. L'identifiant du curseur n'existe pas et est à
        // null, c'est une demande de nouveau cusreur, x-cursor à vrai.
        StorageListRequest request = new StorageListRequest(tenantId, type, null, true);
        try (CloseableIterator<ObjectEntry> result = myConnection.getMetadatas(request)) {

            // On peut alors itérer sur le résultat
            while(result.hasNext()) {
                JsonNode json = result.next();
                // Traitement....
            }
        }
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }
