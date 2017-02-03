Métadata
*********

Présentation
------------

|  *Parent package:* **fr.gouv.vitam.api**
|  *Package proposition:* **fr.gouv.vitam.metadata.rest**

Ce parquet permet de valider les diffiréntes parquets.
Module hébergeant le support REST et le jar de lancement du service.

Services
--------

Rest API
--------

| URL Path : http://server/metadata/v1
|
| POST /units -> **POST nouvelle unit et selection d'une liste des units avec une réquête **
|
|GET /units  ->  **GET sélectionne une liste des units avec une requete 
|
| GET /status -> **statut du server rest metadata (available/unavailable)**
|
| POST /objectgroups -> **POST : insérer une nouvelle object groups avec une requête DSL**
|
|GET /objectgroups/{id_og} -> avoir un object groups par id avec une requete DSL
|
|GET /units/{id_unit} -> **POST nouvelle unit et selection d'une liste des units avec une réquête **
|
|PUT /units/{id_unit}  -> mettre à jour une unit par identifiant



