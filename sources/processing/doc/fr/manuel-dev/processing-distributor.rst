Processing Distributor
######################

Présentation
^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam.processing**
|  *Package proposition:* **fr.gouv.vitam.processing.distributor**

2 modules composent la partie processing-distributor : 
- processing-distributor : incluant la partie core + la partie REST.
- processing-distributor-client : incluant le client permettant d'appeler le REST.

Processing-distributor
----------------------

Rest API
^^^^^^^^

Pour l'instant les uri suivantes sont déclarées : 

| http://server/processing/v1/worker_family
| POST /{id_family}/workers/{id_worker} -> **POST Permet d'ajouter un worker à la liste des workers**
| DELETE /{id_family}/workers/{id_worker} -> **DELETE Permet de supprimer un worker**

A noter, que la resource ProcessDistributorResource est utilisée dans la partie Processing-Management.

Core
^^^^
Dans la partie core la classe ProcessDistributorImpl propose une méthode principale : distribute.
Cette méthode permet de lancer des étapes et de les diriger vers différents Workers (pour l'instant un seul worker existe).
De plus, un système de monitoring permet d'enregistrer le statut des étapes lancées par la méthode distribute (cf module ProcessMonitoring).
En attributs de l'implémentation du ProcessDistributor, sont présents 1 map de Workers ainsi qu'une liste de Workers disponibles.
Ces 2 objets permettent (et permettront plus finement dans le futur) de gérer la liste des workers disponibles.
Deux méthodes : registerWorker et unregisterWorker permettent d'ajouter ou de supprimer les workers à la liste des workers disponibles. 

Processing-distributor-client
-----------------------------

Pour le momentn le module est vide, car la partie client permettant d'appeler les méthodes register / unregister est portée par le module processing-management-client.
A terme, il sera souhaité d'avoir 2 clients séparés.
