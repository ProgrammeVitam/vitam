Processing Management
#####################

Présentation
^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam.processing**
|  *Package proposition:* **fr.gouv.vitam.processing.management**

2 modules composent la partie processing-management : 
- processing-management : incluant la partie core + la partie REST.
- processing-management-client : incluant le client permettant d'appeler le REST.

Processing-management
---------------------

Rest API
^^^^^^^^

Dans le module Processing-management (package rest) : 
| http://server/processing/v1
| POST /operations -> **POST Soumettre un workflow à éxécution**
| GET /status -> **statut du logbook**


De plus est ajoutée à la resource existante une resource déclarée dans le module processing-distributor (package rest). 
| http://server/processing/v1/worker_family
| POST /{id_family}/workers/{id_worker} -> **POST Permet d'ajouter un worker à la liste des workers**
| DELETE /{id_family}/workers/{id_worker} -> **DELETE Permet de supprimer un worker**

Core
^^^^

Dans la partie Core, la classe ProcessManagementImpl propose une méthode : submitWorkflow.
Elle permet de lancer un workflow.

Processing-management-client
----------------------------

Utilisation
^^^^^^^^^^^

Le client propose une méthode principale : executeVitamProcess. Deux autres méthodes ont été ajoutées : registerWorker et unregisterWorker. 

- executeVitamProcess permet de traiter les opérations de "workflow".
- registerWorker : permet d'ajouter un nouveau worker à la liste des workers.
- unregisterWorker : permet de supprimer un worker à la liste des workers.


Configuration
-------------
1. Configuration du pom
Configuration du pom avec maven-surefire-plugin permet le build sous jenkins. Il permet de configurer le chemin des resources de esapi dans le common private.
