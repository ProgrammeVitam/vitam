DAT : module metadata 
#####################

Ce document présente l'ensemble du manuel développement concernant le développment du module 
metadata qui identifié par la user story #70, qui contient :

- modules & packages
- classes métiers
--------------------------


1. Modules et packages

metadata	
    |--- metadata-builder : contenant des classes pour les fonctionnalités de base 
    |                     pour construire des query élémentaire DSL  
    |--- metadata-parser  : parser pour construire des requêtes DSL
    |--- metadata-api     : définir des APIs de traitement des requêtes un utilisant  
    |			   la base de données choisie
    |--- metadata-core    : implémentation des APIs
    |--- metadata-rest    : le serveur REST de métadata qui donnes des traitement 
    |                       sur les requêtes DSL
    |--- metadata-client  : client métadata qui sera utilisé par les autres modules 
    |                       pour faire des requête DSL sur le métadata

2. Classes métiers 
Dans cette section, nous présentons quelques classes principales dans des modules/packages 
qu'on a abordé ci-dessus.

metadata-api : 
-MetaData.java : définir des interface métiers pour le métadata

metadata-core : 
-MetaDataImpl.java : implémenter des fonctionnalités de traitement sur le métadata, pré-défini
dans -MetaData.java

metadata-rest 
- MetaDataRessource.java : définir des ressources différentes pour le serveur REST métadata
- MetaDataApplication.java : créer & lancer le serveur d'application avec une configuration 

metadata-client 
- MetaDataClient.java : créer le client et des fonctionnalités en se connectant au serveur REST

