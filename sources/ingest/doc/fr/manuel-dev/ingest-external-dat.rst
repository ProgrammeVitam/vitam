 DAT : module ingest-external 
############################

Ce document présente l'ensemble du manuel développement concernant le développment du module 
ingest-external qui identifié par la user story #777 (refacto ingest), qui contient :

- modules & packages
- classes métiers
--------------------------


1. Modules et packages

ingest-external	
    |--- ingest-external-common : contenant des classes pour les traitements commons de modules ingest-external :   
    |    	                      code d'erreur, configuration et le script de scan antivirus		
    |--- ingest-external-api     : définir des APIs de traitement dépôt des SIP vers le base   
    |			   				   MongoDb 
    |--- ingest-external-core    : implémentation des APIs
    |--- ingest-external-rest    : le serveur REST de ingest-external qui donnes des traitement  
    |                       sur dépôt de document SIP.
    |--- ingest-external-client  : client ingest-external qui sera utilisé par les autres application externe de VITAM

2. Classes métiers 
Dans cette section, nous présentons quelques classes principales dans des modules/packages 
qu'on a abordé ci-dessus.

2.1 ingest-external-common: 
fr.gouv.vitam.ingest.external.common.util
-JavaExecuteScript.java : classe java exécute l'anti-virus pour détecter des virus de fichiers. 

fr.gouv.vitam.ingest.external.common.model.response
- IngestExternalError.java : modèle de réponse d'erreur sur la request de dépôt ingest 

ingest-external-api: 
-IngestExternal.java : interface pour le service de dépôt externe.
- IngestExternalOutcomeMessage.java : définir message de réponse du résultat de scan virus

ingest-external-core: 
-IngestExternalImpl.java : implémenter des fonctionnalités de traitement sur le dépôt SIP , pré-défini
dans -IngestExternal.java

ingest-external-rest:
- IngestExternalRessource.java : définir des ressources différentes pour le serveur REST ingest-external
- IngesteEternalApplication.java : créer & lancer le serveur d'application avec une configuration 

ingest-external-client 
- IngestExternalClient.java : interface client Ingestexternal
- IngestExternalexternalClientMock.java : mock client ingest-external
- IngestExternalClientRest.java : le client ingest-external et des fonctionnalités en se connectant 
au serveur REST ingest-external

