Worker Client
#############

La factory
**********

Afin de récupérer le client une factory a été mise en place.

.. code-block:: java

    // Récupération du client
    WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();

A la demande l'instance courante du client, si un fichier de configuration worker-client.conf est présent dans le classpath le client en mode de production est envoyé, sinon il s'agit du mock.

Le Mock
=======

En l'absence d'une configuration, le client est en mode Mock. Il est possible de récupérer directement le mock :

.. code-block:: java

      // Changer la configuration du Factory
      WorkerClientFactory.setConfiguration(WorkerClientFactory.WorkerClientType.MOCK_WORKER, null);
      // Récupération explicite du client mock
      WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();


Le mode de production
=====================

Pour instancier son client en mode Production :

.. code-block:: java

      // Changer la configuration du Factory
      WorkerClientFactory.setConfiguration(WorkerClientFactory.WorkerClientType.WORKER, configuration);
      // Récupération explicite du client
      WorkerClient client = WorkerClientFactory.getInstance().getWorkerClient();
      
      
Les services
************

Le client propose pour le moment une fonctionnalité :
- Permet de soumettre le lancement d'une étape. Deux paramètres sont nécessaires : un string requestId + un objet DescriptionStep. Voici un exemple d'utilisation : 

.. code-block:: java
   DescriptionStep ds = new DescriptionStep(new Step(), new WorkParams());
   List<EngineResponse> responses =
            client.submitStep("requestId", ds);
   // Now we can check the list of response
