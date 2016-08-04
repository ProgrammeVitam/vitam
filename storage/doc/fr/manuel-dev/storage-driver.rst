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
        // Requête contant le tenantId afin de récupérer la capacité (objet permettant d'être enrichi dan le futur)
        StorageCapacityRequest request = new StorageCapacityRequest();
        request.setTenantId("tenantId");
        // Récupération de la capacité
        StorageCapacityResult capacity = myConnection.getStorageCapacity(request);
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

    // Etablissement d'une connexion avec l'offre de stockage et réalisation d'une opération
    try (Connection myConnection = myDriver.connect("http://my.storage.offer.com", parameters)) {
        PutObjectRequest request = new PutObjectRequest();
        request.setDataStream(new FileInputStream(PropertiesUtils.findFile("digitalObject.pdf")));
        request.setDigestAlgorithm(DigestType.MD5.getName());
        request.setGuid("GUID");
        request.setTenantId("0");
        PutObjectResult result = myConnection.putObject(request);
        // On peut vérifier ici le résultat du put
    } catch (StorageDriverException exc) {
        // Un problème est survenu lors de la communication avec le service distant
    }

   
