DAT : module ingest-internal 
############################

Ce document présente l'ensemble du manuel développement concernant le développment du module 
ingest-internal qui est identifié par le user story #84, qui contient :

- modules & packages
- classes métiers

--------------------------


1. Modules et packages

ingest-internal

	|--- ingest-internal-common : contenant des classes pour les traitements commons de modules ingest-internal  
	|--- ingest-internal-model  : définir les modèles de données utilisé dans le module
	|--- ingest-internal-api     : définir des APIs de traitement dépôt des SIP vers le base MongoDb	   					
	|--- ingest-internal-core    : implémentation des APIs
	|--- ingest-internal-rest    : le serveur REST de ingest-internal qui donnes des traitement sur dépôt de document SIP.                      
	|--- ingest-internal-client  : client ingest-internal qui sera utilisé par les autres modules interne de VITAM pour le service de dépôt des SIPs                    

2. Classes métiers

Dans cette section, nous présentons quelques classes principales dans des modules/packages 
qu'on a abordé ci-dessus.

ingest-internal-model:

-UploadResponseDTO.java : définir le modèle de réponse sur l'opération de dépôt SIP (upload). Il contient l'information sur le nom de fichier SIP, le code de retour VITAM, le code de retour HTTP, le message et le status.

ingest-internal-api: 

-UploadService.java : interface pour le service de dépôt interne.

ingest-internal-core: 

-MetaDataImpl.java : implémenter des fonctionnalités de traitement sur le métadata, pré-défini dans -MetaData.java

ingest-internal-rest:

- IngestInternalRessource.java : définir des ressources différentes pour le serveur REST ingest-internal
- IngestInternalApplication.java : créer & lancer le serveur d'application avec une configuration 

ingest-internal-client 

- IngestInternalClient.java : interface client IngestInternal
- IngestInternalInternalClientMock.java : mock client ingest-internal
- IngestInternalClientRest.java : le client ingest-internal et des fonctionnalités en se connectant au serveur REST

