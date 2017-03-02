Processing Management
#####################
:Version: 27/02/2017 

Présentation
^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam.processing**
|  *Package proposition:* **fr.gouv.vitam.processing.management**

4 modules composent la partie processing-management : 
- processing-management : incluant la partie core + la partie REST.
- processing-management-client : incluant le client permettant d'appeler le REST.
- processing-engine : le moteur workflow.
- processing-data : le module de persistance et d'accès aux processus lancés (en éxécution, en pause, annulés, terminés).

Processing-management
=====================

Rest API
------------------

Dans le module Processing-management (package rest) : 
| http://server/processing/v1
| POST /operations -> **absolète**

| GET /status -> **statut du logbook**

| POST /operations/{id} -> **initialiser et/ou lancer un processus workflow**
| PUT /operations/{id} -> **éxécuter une action sur processus existant**
| - Relancer un processus en mode continu avec header X-ACTION==> resume
| - Exécuter l'étape suivante avec header X-ACTION==> next
| - Mettre en pause un processus avec header X-ACTION==> pause
| GET /operations/{id} -> **récupérer les details d'un processus workflow par id et tenant**
| HEAD /operations/{id} -> **récupérer l'état d'éxécution d'un processus workflow par id et tenant**
| DELETE /operations/{id} -> **Annuler un processus**

De plus est ajoutée à la resource existante une resource déclarée dans le module processing-distributor (package rest). 
| http://server/processing/v1/worker_family
| POST /{id_family}/workers/{id_worker} -> **POST Permet d'ajouter un worker à la liste des workers**
| DELETE /{id_family}/workers/{id_worker} -> **DELETE Permet de supprimer un worker**

Core
-----

Dans la partie Core, la classe ProcessManagementImpl propose les méthodes suivantes :

- **submitWorkflow**:Elle permet de lancer/relancer en mode continu ou step by step un processus workflow.
- **pauseProcessWorkFlow**:Elle permet de mettre en pause un processus workflow.
- **cancelProcessWorkflow**:Elle permet d'annuler un processus workflow.

Processing-management-client
----------------------------

Utilisation
------------

Le client propose les méthode suivantes : 
- **initVitamProcess**: initialiser le contexte d'un processus.
- **executeVitamProcess**: ! absolète !.
- **executeOperationProcess**:lancer un processus workflow avec un mode d'éxécution (resume/step by step).
- **updateOperationActionProcess**:relancer un processus workflow pour éxécuter une etape (mode: "next") ou toutes les étapes ("resume").
- **getOperationProcessStatus**:récupérer l'état d'éxécution d'un processus workflow par id et tenant.
- **cancelOperationProcessExecution**:annuler un processus workflow par id et tenant.
- **listOperationsDetails**:récupérer la liste des processus.

- **registerWorker** : permet d'ajouter un nouveau worker à la liste des workers.
- **unregisterWorker** : permet de supprimer un worker à la liste des workers.

Exemple:
---------

.. code-block:: java

            processingClient = ProcessingManagementClientFactory.getInstance().getClient();
           
            Response response = processingClient.executeOperationProcess("containerName", "workflowId",
                logbookTypeProcess.toString(), ProcessAction.START.getValue());
      

Processing-data
===============

Le module Processing data est responsable de la partie persistance ,accès aux données des processus avoir l'état d'exécution et l'oordonnancement des étapes.

Le module processing data propose plusieurs méthodes: 

- **initProcessWorkflow**: initialiser le contexte d'un processus.
- **updateStep**: mettre à jour une étape (les elements éxécutés/restés).
- **updateStepStatus**: mettre à jour le status d'une étape donnée.
- **updateProcessExecutionStatus**: mettre à jour le status d'un processus donné(valeur possibles :PENDING,RUNNING,PAUSE,CANCELLED,FAILED,COMPLETED) .
- **nextStep**: mettre à jour une étape.
- **getFinallyStep**: récupérer l'étape de finalisation pour processus donné.
- **cancelProcessWorkflow**: annuler un processus par son id et son tenant.


Configuration
^^^^^^^^^^^^^^^
1. Configuration du pom
Configuration du pom avec maven-surefire-plugin permet le build sous jenkins. Il permet de configurer le chemin des resources de esapi dans le common private.
