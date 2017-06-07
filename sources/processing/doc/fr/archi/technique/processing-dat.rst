DAT : module processing
#######################

Ce document présente l'ensemble de manuel développement concernant le développment du module
metadata qui représente le story #70, qui contient :

- modules & parkages
- classes de métiers


1. Module et packages
---------------------

Les principaux modules sont : 

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

2. Modèle
---------

Un modèle a été mis en place pour permettre la remontée et l'agrégation des status des différents item du worflow.

Un état du worflow utilise l'objet **ItemStatus** qui contient :
* itemId : l'identifiant de l'item de processus résponsable du status (identifiant de step, handler, transaction, etc)
* statusMeter : une liste de nombre de code status (nombre de OK, KO, WARNING, etc)
* globalStatus : un status global
* une liste de données remontée par l'item du processus (comme messageIdentifier)

Les états du processus de workflow utilisent un objet composite **CompositeItemStatus** qui est un **ItemStatus** et contient une Map d'états de workflow de ses sous-items.

Un workflow est défini par un fichier json contenant les steps ainsi que toutes les actions qui doivent être exécutées par les steps. Chaque Step et Action doivent être identifiés par un ID unique qui est également utilisé pour récupérer les messages.



3. Process Distributor
----------------------

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

* liste des familles de worker
* ajouter/mettre à jour/effacer une famille de worker
* statut d'une fammile de worker
* liste des workers d'une famille
* effacer les workers d'une famille
* statut d'un worker
* mise à jour d'un worker

4. Parallélisme dans le distributeur
------------------------------------
Les parallélismes suivants sont mis en oeuvre dans le distributeur

* Parallélisme dans l'exécution des steps entre plusieurs workflows : celui-ci est géré de manière naturelle sous la forme de plusieurs requêtes (actuellement Java, demain en HTTP) entre le moteur du processing (process-engine) et le distributeur. 
* Parallélisme dans l'exécution d'un step pour une distribution de type list vers un même worker. Les principes sont les suivants
 
  -> Worker : chaque worker associé à un WorkerConfiguration pré-défini. Chaque worker appartient à une famille correspondant à ses fonctions. 
  et il possède aussi une capacité pour gérer plusieurs threads en parallèle, précisé par le paramètre capacity de WorkerCongiguration, et ces paramètres seront initialisés lors du lancement du Worker.  

  -> Enregistrement/déenregistrement d'un worker : Le principe est un découplage asynchrone basé sur plusieurs queues de messages bloquantes (BlockingQueue en java)   
  Il y a plusieurs famille de worker et chaque famille lié à une queue de messages bloquantes. Pour l'enregistrement du worker, nous faisons aussi un contrôle pour s'assurer que le worker 
  ne peut s'enregistrer qu'à une famille lui appartenant. Au moment de l'enregistrement, si la queue de la famille n'existe pas encore, elle sera créée. 

  -> Opérarations: 
  	- Lors de l'enregistrement d'un worker (voir section ci-dessus), un thread (cf WorkerManager) est crée et se met en écoute sur la blocking queue (Consommateur) correspondante de la famille.                 
  	  Une fois une tâche consommée, s'il a une capacité suffisante (fournie par le worker lors de l'enregistrement), ce thread (WorkerThreadManager) va créer un thread (WorkerThread) pour gérer 
  	 l'envoi de la demande au Worker ainsi que la gestion de la callback vers le producteur.
  	 
  	- Lors de distribution d'un step d'un workflow, 
   + le distributeur pousse les tâches dans la blockingQueue (Producteur) et garde en mémoire les tâches qui sont en cours
   + La queue n'est qu'un élement de découplage et a donc une taile réduite : le thread de distribution est donc bloqué soit lors de son insertion dans la queue soit en attente que toutes les tâches soient terminées 
   + Une callback est exécutée par le consommateur en fin de traitement pour supprimer la tâche terminée des tâches en cours

Le parallélisme entre plusieurs workers sera mis en oeuvre en V1
