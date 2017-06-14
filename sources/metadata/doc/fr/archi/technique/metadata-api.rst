Metadata-api
*******************

Présentation
------------

|  *Parent package:		* **fr.gouv.vitam.metadata**
|  *Package proposition:* **fr.gouv.vitam.metadata.api**

- Le package fr.gouv.vitam.api permet d'interagir avec le moteur de données à travers la description du métadata

pour les opérations: insertUnit,  insertObjectGroup, selectUnitsByQuery, selectUnitsById
Le format utilisé pour la description du metadonnees : Json.
 
- Le package fr.gouv.vitam.api.config permet de configurer la connexion de la base de données (Mongo DB)

 en utilisant les paramètres:  host database server IP address, le port database server port, le nom de la BDD, le nom de la collection.

- Le parkage fr.gouv.vitam.api.exception gère les exceptions issues des opérations des demandes d'acess à travers de  métadata.

	les exceptions gerées sont :
	
.. code-block:: java

	MetaDataAlreadyExistException(String message)
	MetaDataAlreadyExistException(Throwable cause)
	MetaDataAlreadyExistException(String message, Throwable cause)

- Le parkage fr.gouv.vitam.api.model permet de la gestion d'interrogation de la base de donnees.
