DAT : module logbook
####################

Ce document présente l'ensemble de manuel développement concernant le développment du module
logbook qui représente le story #33, qui contient :

- modules & parkages
   
	|--- logbook-common: contient les méthodes commons: les modèles, les exceptions
	|
	|--- logbook-common-client: client de module logbook-common
	|
	|--- logbook-common-server: connecter le base des données mongodb et construire les requêtes DSL
	|
	|--- logbook-operations: insérer, mettre à jour, selectionner et supprimer les operations dans logbook
	|
	|--- logbook-operations-client: client de module logbook-operations
	|
	|--- logbook-rest: le serveur REST de logbook qui donnes les opérations
	|
   
	
	
    