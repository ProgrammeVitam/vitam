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
   |--- processing-distributor: appelle un worker de processus et distribue le workflow. Offre la possibilité au worker de s'enregistrer, se désabonner.
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
	
   
3. Process Distributor :
   Le distributor, en plus de lancer les workflow, offre désormais la possibilité aux Workers de s'abonner, se désabonner.
   Lors d'un abonnement, le Worker est ajouté à une liste de workers (regroupés par famille de worker). Pour un désabonnement, il est supprimé.
   Pour le moment, les workers ajoutés ne pourront être appelés, cela sera codé dans une autre itération.
   
   A l'heure actuelle voici les méthodes REST proposées :
   
   POST /processing/v1/worker_family/{id_family}/workers/{id_worker}
      -> permet d'enregistrer un nouveau worker pour la famille donnée.
      -> Une query json est passé en paramètre et correspond à la configuration du worker.
   DELETE /processing/v1/worker_family/{id_family}/workers/{id_worker}
      -> permet de désinscrire un worker pour la famille donnée, selon son id.
   
   Dans les itérations suivantes les autres méthodes suivantes seront implémentées : 
   - liste des familles de worker
   - ajouter/mettre à jour/effacer une famille de worker
   - statut d'une fammile de worker
   - liste des workers d'une famille
   - effacer les workers d'une famille
   - statut d'un worker
   - mise à jour d'un worker
          
   
   
   