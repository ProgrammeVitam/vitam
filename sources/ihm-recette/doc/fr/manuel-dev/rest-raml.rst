ihm-recette
###########

Présentation
------------
Ce document présente le schéma des points d'API défini qui sera appelé par
le frontend de l'application web.

------------

 package:* **fr.gouv.vitam.api**
 |  *Package proposition:* **fr.gouv.vitam.metadata.rest**

Module pour le module opération : api / fr.gouv.vitam.ihmrecette.appserver

Services
--------

Rest API
--------

| URL Path : / 
|
| GET    /messages/logbook -> récupère les traductions liées aux status des journaux d'opération
| GET    /stat/{id_op} -> N'est pas utilisé par le front
| POST   /operations/traceability -> force une sécurisation des journaux d'opération
| POST   /logbooks -> N'est pas utilisé par le front
| GET    /logbooks -> N'est pas utilisé par le front
| GET    /logbooks/{idOperation} -> N'est pas utilisé par le front
| GET    /logbooks/{idOperation} -> N'est pas utilisé par le front
| GET    /logbooks/{idOperation}/content -> N'est pas utilisé par le front
| POST   /accesscontracts -> Récupère les contrats d'accès valides
| POST   /dslQueryTest -> Envoie une requête DSL de test attendant un json de réponse en résultat
|
| DELETE /delete/deleteTnr -> Vide toutes les colelctions sur tous les tenants et sans vérifications pour les TNR
| DELETE /delete -> Vide toutes les collections (sauf formats) pour le tenant donné
|
| DELETE /delete/formats -> Vide la collection des formats sur tout les tenants
| DELETE /delete/rules -> Vide la collection des règles de gestion sur le tenant donné
| DELETE /delete/accessionregisters -> Vide la collection des registres des fonds sur le tenant donné
| DELETE /delete/logbook/operation -> Vide la collection des journaux d'opération sur le tenant donné
| DELETE /delete/logbook/lifecycle/unit -> Vide la collection des cycles de vie des unités archivistiques sur le tenant donné
| DELETE /delete/logbook/lifecycle/objectgroup -> Vide la collection des cycles de vie des groupes d'objets sur le tenant donné
| DELETE /delete/masterdata/ingestContract -> Vide la collection des contrats d'entrée sur le tenant donné
| DELETE /delete/masterdata/accessContract -> Vide la colelction des contrats d'accès sur le tenant donné
| DELETE /delete/metadata/objectgroup -> Vide la collection des groupes d'objets sur le tenant donné
| DELETE /delete/metadata/unit -> Vide la collection des unités archivistiques sur le tenant donné
| DELETE /delete/masterdata/profile -> Vide la collection des profiles sur le tenant donné


