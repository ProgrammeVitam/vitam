Storage Engine
##############

Storage Engine Client
#####################

La factory
**********

Afin de récupérer le client une factory a été mise en place.

.. code-block:: java

    // Récupération du client
    StorageClient client = StorageClientFactory.getInstance().getStorageClient();

A la demande l'instance courante du client, si un fichier de configuration storage-client.conf est présent dans le classpath le client en mode de production est envoyé, sinon il s'agit du mock.


Le Mock
=======

En l'absence d'une configuration, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
      StorageClientFactory.setConfiguration(StorageClientFactory.StorageClientType.MOCK_STORAGE, null);
      // Récupération explicite du client mock
      StorageClient client = StorageClientFactory.getInstance().getStorageClient();


Le mode de production
=====================

Pour instancier son client en mode Production :

.. code-block:: java

      // Changer la configuration du Factory
      StorageClientFactory.setConfiguration(StorageClientFactory.StorageClientType.STORAGE, configuration);
      // Récupération explicite du client
      StorageClient client = StorageClientFactory.getInstance().getStorageClient();

Les services
************

Le client propose actuellement des fonctionnalités nécéssitant toutes deux paramètres obligatoires :

- l'identifiant du tenant (valeur de test "0")
- l'identifiant de la stratégie de stockage (valeur de test "default")

Ces fonctionnalités sont :

- la récupération des informations sur une offre de stockage pour une stratégie (disponibilité et capacité) :

.. code-block:: java

	JsonNode result = client.getContainerInformation("0", "default");

- l'envoi d'un objet sur une offre de stockage selon une stratégie donnée :
	- pour les objets contenus dans le workspace (objets binaires) :

.. code-block:: java

	JsonNode result = storeFileFromWorkspace("0", "default", StorageCollectionType.OBJECTS, "aeaaaaaaaaaam7mxaaaamakv3x3yehaaaaaq");

   - pour les metadatas Json (objectGroup, unit, logbook -- pas encore implémenté côté serveur) :

.. code-block:: java

		JsonNode result = storeJson("0", "default", StorageCollectionType.UNIT, "aeaaaaaaaaaam7mxaaaamakv3x3yehaaaaaq");

 - la vérification de l'existance d'un objet dans l'offre de stockage selon  une stratégie donnée :
   - pour les conteneurs (pas encore implémenté côté serveur) :

.. code-block:: java

		boolean exist = existsContainer("0", "default");

   - pour les autres objets (object, objectGroup, unit, logbook -- implémenté côté serveur uniquement pour object) :

.. code-block:: java

		boolean exist = exists("0", "default", StorageCollectionType.OBJECTS, "aeaaaaaaaaaam7mxaaaamakv3x3yehaaaaaq");

 - la suppression d'un objet dans l'offre de stockage selon  une stratégie donnée :
   - pour les conteneurs  (pas encore implémenté côté serveur) :

.. code-block:: java

   boolean deleted = deleteContainer("0", "default");

   - pour les autres objets (object, objectGroup, unit, logbook -- implémenté côté serveur uniquement pour object) :

.. code-block:: java

   boolean deleted = delete("0", "default", StorageCollectionType.OBJECTS, "aeaaaaaaaaaam7mxaaaamakv3x3yehaaaaaq");

- la récupération d'un objet (InputStream) contenu dans un container :

.. code-block:: java

   InputStream stream = client.getContainerObject("0", "default", "aeaaaaaaaaaam7mxaaaamakv3x3yehaaaaaq");

- La récupération du status est également disponible :

.. code-block:: java

	StatusMessage status = client.getStatus();
