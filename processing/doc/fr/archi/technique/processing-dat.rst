DAT : module processing
#######################

Ce document présente l'ensemble de manuel développement concernant le développment du module
metadata qui représente le story #70, qui contient :

- modules & parkages
- classes de métiers

--------------------------

1. Module et packages   

   |--- processing-common: contient les méthodes commons: les modèles, les exceptions, SedaUtil ...
   |
   |--- processing-distributor: appelle un worker de processus et distribue le workflow
   |
   |--- processing-distributor-client: client de module processing-distributor
   |
   |--- processing-engine: appelle un distributeur de processus
   |
   |--- processing-engine-client: client de module processing-engine
   |
   |--- processing-management: gestion de workflow
   |
   |--- processing-management-client: client de module processing-management
   |
   |--- processing-worker: exécute les actions step par step
   
2. Classes métiers
	Dans cette section, nous présentons quelques classes principales dans des modules/packages qu'on a abordé ci-dessus.
	
	SedaUtil: contient toutes les méthodes qui lit et extraire les éléments du SEDA
	
	actionHandler: classe abstraite de toutes les actions. 
	
