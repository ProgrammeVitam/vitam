API
###

Formation générale des API externes
===================================

Services
---------

* ingest-external : Opérations d’entrées
* access-external : Opérations d'accès et journaux d’opérations
* admin-external : Gestion du référentiel et opérations d’administration

Quelques Ressources
-------------------

* ``/ingest-external/v1/ingests``
* ``/admin-external/v1/formats``
* ``/access-external/v1/units``

Format
---------

::

  POST   /access-external    /v1       /units
  VERB   Endpoint            Version   Ressource

La documentation des API REST décrit en détail les endpoints, les conventions d'appels ainsi que le language de requêtes DSL.


Clients d'appels Java
=====================

Vitam est livré avec des clients d'appels externes en Java. Ils sont notamment accessibles depuis les packages des clients suivants :

* Ingest External Client : ``fr.gouv.vitam.ingest.external.client``
* Access External Client : ``fr.gouv.vitam.access.external.client``

De plus, plusieurs helpers sont disponibles pour la construction des requêtes DSL dans ``common/common-database-vitam/common-database-public`` :

* fr.gouv.vitam.common.database.builder.query; notamment **VitamFieldsHelper** et **QueryHelper**
* fr.gouv.vitam.common.database.builder.query.action; dont **UpdateActionHelper**
* fr.gouv.vitam.common.database.builder.request.multiple; dont **DeleteMultiQuery**, **SelectMultiQuery**, **InsertMultiQuery**, **UpdateMultiQuery**
* fr.gouv.vitam.common.database.builder.request.single; dont **Delete**, **Insert**, **Select**, **Update**

La documentation JavaDoc décrit en détail les API clientes Java.
