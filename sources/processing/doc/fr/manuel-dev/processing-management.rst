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

| GET /status -> **statut du logbook**

| POST /operations/{id} -> **initialiser et/ou exécuter une action sur un processus workflow existant**
| PUT /operations/{id} -> **exécuter une action sur processus existant**
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


- **init**: Initialiser un processus avec un workflow donné. Dans cette étape on attach avec un cardinalité un-à-un un ProcessEngine et une machine à état à ce processus.
- **next**: Exécute l'action next (exécuter l'étape suivante mode step by step) sur un processus existant.
- **resume**: Exécute l'action resume (exécuter toutes les étapes mode continu) sur un processus existant.
- **pause**: Exécute l'action pause (mettre le processus en état pause dès que possible) sur un processus existant.
- **cancel**: Exécute l'action cancel (annuler un processus dès que possible) sur un processus existant.
- **findAllProcessWorkflow**: Lister tous les processus d'un tenant donné.
- **findOneProcessWorkflow**: Trouver un processus avec son id et son tenant.


La machine à état:
------------------
Dans la partie core on trouve aussi la classe StateMachine. Elle gère toutes les actions sur un processus donné.

- **next**:

Evaluer le passage de l'état actuel du processus vers l'état RUNNING en mode step by step.
On ne peut passer à l'état RUNNING que depuis un état en cours PAUSE.
Si cette évaluation ne lance pas d'exception alors lancer l'exéction d'un processus appel de ma la méthode **doRunning**.

- **resume**:

Evaluer le passage de l'état actuel du processus vers l'état RUNNING en mode continu.
On ne peut passer à l'état RUNNING que depuis un état en cours PAUSE.
Si cette évaluation ne lance pas d'exception alors lancer l'exéction d'un processus appel de ma la méthode **doRunning**.

- **pause**:

Evaluer le passage de l'état actuel du processus vers l'état PAUSE.
On ne peut passer à l'état PAUSE que depuis un état en cours (PAUSE, RUNNING).
Si cette évaluation ne lance pas d'exception alors , dans le cas d'un état en cours RUNNING, finir l'exécution de l'étape en cours et passer à l'état PAUSE, et si c'est la dernière étape, alors passer à l'état COMPLETED. Appel de ma la méthode **doPause**

- **cancel**:

Evaluer le passage de l'état actuel du processus vers l'état COMPLETED.
On ne peut passer à l'état COMPLETED que depuis un état en cours (PAUSE, RUNNING).
Si cette évaluation ne lance pas d'exception alors , dans le cas d'un état en cours RUNNING,  finir l'exécution de l'étape en cours et passer à l'état COMPLETED. Appel de ma la méthode **doCompleted**

-**doRunning**: Appelée depuis **next** ou **resume**.
-**doPause**: Appelée depusi **pause**.
-**doCompleted**: Appelée depuis **cancel**.

-**onComplete**:

Appelée depuis le ProcessEngine quand une étape a été exécuté.
Evaluation sur l'exécution de l'étape suivante selon les informations suivantes:

- Si la dernière étape alors exécuter finaliser le logbook et persister le processus.
- Sinon :

    - Vérifier si le status de l'étape est KO bloquant ou FATAL alors exécuter la dernière étape.
    - Sinon vérifier si une demande d'action est présente (évaluer la targetState):

        - targetState = COMPLETED: Exécuter la dernière étape.
        - targetState = PAUSE: Alors pause
        - Sinon exécuter l'étape suivante.

-**onError**:

Appelée depuis le ProcessEngine quand une exception est levée lors de l'exécution d'un étape.
Si c'est pas la dernière étape alors essayer d'exécuter la dernière étape.
Dans tous les cas, finaliser le logbook et persister le processus.

-**onUpdate**:

Appelée depuis le ProcessEngine pour metter à jour les informations du processus à la volé.


Lors de la finalisation du logbook, la mise à jours des informations sur l'état et le status son effectué au niveau du processus. Une suppression de l'opération depuis le workspace.


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
		logbookTypeProcess.toString(), ProcessAction.RESUME.getValue());


Processing-data
===============

Le module Processing data est responsable de la partie persistance ,accès aux données des processus avoir l'état d'exécution et l'oordonnancement des étapes.

Le module processing data propose plusieurs méthodes: 

- **initProcessWorkflow**: initialiser le contexte d'un processus.
- **updateStep**: mettre à jour une étape (les elements éxécutés/restés).
- **findOneProcessWorkflow**: Trouver depuis la map un processus par son id et son tenant.
- **findAllProcessWorkflow**: Trouver depuis la map tous les processus d'un tenant.
- **addToWorkflowList**: Ajouter un processus à la map (sauvegrade mémoire)


Configuration
^^^^^^^^^^^^^^^
1. Configuration du pom

Configuration du pom avec maven-surefire-plugin permet le build sous jenkins. Il permet de configurer le chemin des resources de esapi dans le common private.
