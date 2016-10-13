Worker server
#############

Présentation
************

|  *Parent package :* **fr.gouv.vitam.worker**
|  *Package proposition :* **fr.gouv.vitam.worker.server**

Module embarquant la partie server du worker.

Services
========

De manière générale, pour le Worker, les méthodes utilisées sont les suivantes :
 - GET : pour récupérer des infos sur une liste d'étapes, ou sur une étape particulière.
 - POST : pour démarrer le lancement d'une étape.
 - PUT : pour les mises à jour d'étapes.

Rest API
--------

URI d'appel
^^^^^^^^^^^
| http://server/worker/v1

Headers
^^^^^^^
Plusieurs informations sont nécessaires dans la partie header :
 - X-Request-Id : l'identifiant unique de la requête.

Méthodes
^^^^^^^^
| GET /tasks -> **Liste les étapes en cours.**
| POST /tasks -> **Permet de soumettre une étape.**
| GET /tasks/{id_async} -> **Permet de récupérer le statut d'une étape.**
| PUT /tasks/{id_async} -> **Permet d'intéragir avec une étape.** 

| GET /status -> **statut du worker**
