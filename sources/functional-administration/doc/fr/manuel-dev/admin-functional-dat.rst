DAT : module functional-administration
######################################

Ce document présente l'ensemble du manuel développement concernant le développment du module 
functional-administration qui identifie par la user story #71, qui contient :

- modules & packages
- classes métiers
-----------------


1. Modules et packages

functional-administration	
	    |--- functional-administration-common : contenant des classes pour des traitements communs concernant 
	    |    								  le format référentiels, l'opération auprès de la base de données 	
	    |--- functional-administration-format : fournir des traitements de base pour les formats référentiels de VITAM   	 
	    			  |--- functional-administration-format-api  : définitions des APIs 
	    			  |--- functional-administration-format-core : implémentations des APIs 
	    			  |--- functional-administration-format-import 
	    			  
	    |--- functional-administration-rule : fournir des traitements de base pour la gestion de règles administratives
                 |--- functional-administration-rule-api  : Définition des APIs
                 |--- functional-administration-rule-core : Impélmentation des APIs
                 
       |--- functional-administration-accession-register : fournir des traitements de base pour la gestion des registres de fonds
                 |--- functional-administration-accession-register-core : Impélmentation des traitements des registres de fonds

	    |--- functional-administration-rest   : le serveur REST de functional-administration qui donnes des traitement 
	    |                       sur les traitements de format référentiel et gestion de règles administratives.
	    |--- functional-administration-client  : client functional-administration qui sera utilisé par les autres modules 
	    |                       pour les appels de traitement sur le format référentiel & gestion de règles. 


2. Classes métiers 
Dans cette section, nous présentons quelques classes principales dans des modules/packages 
abordés ci-dessus.

2.1. functional-administration-common :

fr.gouv.vitam.functional.administration.common 
-FileFormat.java : une extension de VitamDocument définissant le référentiel des formats.
-ReferentialFile.java : interface définissant des opérations liées au référentiel des format : importation du fichier 
PRONOM, vérificaton du fichier PRONOM soumis, recherche d'un format existant et suppression du référentiel des formats.

fr.gouv.vitam.functional.administration.common.exception : définir des exceptions concernant de opération sur le 
référentiel des formats

fr.gouv.vitam.functional.administration.common.server
les classe de traitement auprès de la base de données mongodb pour les opérations de référentiel de format.

- FunctionalAdminCollections.java : définir la collection dans mongodb pour des données de formats référentiels
- MongoDbAccessReferential.java : interface définissant des opérations sur le format de fichier auprès de la base 
mongodb: insert d'une base de PRONOM, delete de la collection, recherche d'un format par son Id dans la base, 
recherche des format par conditions      
- MongoDbAccessAdminImpl.java : une implémentation de l'interface MongoDbAccessReferential en extension le traitement 
MongoDbAccess commun pour mongodb  

2.2. functional-administration-format
	+ functional-administration-format-api
	+ functional-administration-format-core
	- PronomParser.java : le script de traitement permettant de de récupérer l'ensemble de format en format json depuis 
	d'un fichier PRONOM stantard en format XML contient des différents formats référentiels 
	- ReferentialFormatFileImpl.java : implémentation de base des opération sur le format référentiel de fichier à partir 
	d'un fichier PRONOM jusqu'à la base MongoDB.  
	+ functional-administration-format-import

2.3. functional-administration-rest 
- AdminManagementResource.java : définir des ressources différentes pour le serveur REST functional-administration
- AdminManagementApplication.java : créer & lancer le serveur d'application avec une configuration 

2.4. functional-administration-client 
- AdminManagementClientRest.java : créer le client de et des fonctionnalités en se connectant au serveur REST
- AdminManagementClientMock.java : créer le client et des fonctionnalités en se connectant au mock de serveur