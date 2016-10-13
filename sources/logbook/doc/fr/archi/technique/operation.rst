Opération
*********

Présentation
------------

|  *Parent package:* **fr.gouv.vitam.logbook**
|  *Package proposition:* **fr.gouv.vitam.logbook.operations**

Module pour le module opération : api / rest.

Services
--------

Rest API
--------

| http://server/logbook/v1
| POST /operations/id_op -> **POST nouvelle opération**
| PUT /operations/id_op -> **Append sur une opération existante (ajout d'un item)**
| GET /operations -> **retourne une liste d'opérations sous forme : id + autres infos de la dernière ligne de chaque opération ([ { id_op: id,  last_line_infos }, ... ])**
| GET /operations/id_op -> **accès aux évenements d'une opération**
| GET /status -> **statut du logbook**
