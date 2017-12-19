Métadata
*********

Présentation
------------

|  *Parent package:* **fr.gouv.vitam.api**
|  *Package proposition:* **fr.gouv.vitam.metadata.rest**

Ce paquet permet de valider les différents paquets.
Module hébergeant le support REST et le jar de lancement du service.

Services
--------

Rest API
--------

URL Path : http://server/metadata/v1

POST /units : **POST nouvelle unit et sélection d'une liste des units avec une réquête**

GET /units  :  **GET sélectionne une liste des units avec une requête**

GET /status : **statut du server rest metadata (available/unavailable)**

POST /objectgroups : **Insérer une nouvelle object groups avec une requête DSL**

GET /objectgroups/{id_og} : **avoir un object groups par id avec une requête DSL**

GET /units/{id_unit} : **POST nouvelle unit et sélection d'une liste des units avec une requête**

PUT /units/{id_unit}  : **mettre à jour une unit par identifiant**

