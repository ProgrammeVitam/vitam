Worker
######

Présentation
^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.worker**

4 modules composent la partie worker : 
- worker-common : incluant la partie common (Utilitaires...), notamment le SedaUtils.
- worker-core : contenant les différents handlers.
- worker-client : incluant le client permettant d'appeler le REST.
- worker-server : incluant la partie REST.

Worker-server
-------------

Rest API
^^^^^^^^

Pour l'instant les uri suivantes sont déclarées : 

| http://server/worker/v1
| POST /tasks -> **POST Permet de lancer une étape à exécuter**

Registration
^^^^^^^^^^^^
Une partie registration permet de gérer la registration du Worker. 

La gestion de l'abonnement du *worker* auprès du serveur *processing* se fait à l'aide d'un ServletContextListener : *fr.gouv.vitam.worker.server.registration.WorkerRegistrationListener*.

Le WorkerRegistrationListener va lancer l'enregistrement du *worker* au démarrage du serveur worker, dans un autre Thread utilisant l'instance *Runnable* : *fr.gouv.vitam.worker.server.registration.WorkerRegister*.

L'execution du *WorkerRegister* essaie d'enregistrer le *worker* suivant un retry paramétrable dans la configuration du serveur avec :
- un délai (registerDelay en secondes) 
- un nombre d'essai (registerTry)

Le lancement du serveur est indépendant de l'enregistrement du *worker* auprès du *processing* : le serveur *worker* ne s'arrêtera pas si l'enregistrement n'a pas réussi.


Worker-core
-----------
Dans la partie Core, sont présents les différents Handlers nécessaires pour exécuter les différentes actions.
- CheckConformityActionHandler
- CheckObjectsNumberActionHandler
- CheckSedaActionHandler
- CheckStorageAvailabilityActionHandler
- CheckVersionActionHandler
- ExtractSedaActionHandler
- IndexObjectGroupActionHandler
- IndexUnitActionHandler
- StoreObjectGroupActionHandler

Ainsi que la classe WorkerImpl permettant de lancer ces différents handlers.

Détail du handler : ExtractSedaActionHandler
''''''''''''''''''''''''''''''''''''''''''''
Ce handler permet d'extraire le contenu du SEDA. Il y a :
- extraction des BinaryDataObject
- extraction des ArchiveUnit
- création des lifes cycles des units
- construction de l'arbre des units et sauvegarde sur le workspace
- sauvegarde de la map des units sur le workspace
- sauvegarde de la map des objets sur le workspace
- sauvegarde de la map des objets groupes sur le workspace

Détail des différentes maps utilisées :
.......................................
binaryDataObjectIdToObjectGroupId :
   contenu : cette map contient les id des objets binaires ainsi que l'id du groupe d'objet de la balise DataObjectGroupId ou DataObjectGroupReferenceId
   création : elle est créé lors de la création du handler
   mise à jour : elle est populée lors de la lecture des BinaryDataObject
   lecture : lecture de la map dans mapNewTechnicalDataObjectGroupToBDO, getNewGdoIdFromGdoByUnit, completeBinaryObjectToObjectGroupMap, checkArchiveUnitIdReference et writeBinaryDataObjectInLocal
   suppression : c'est un clean en fin d'execution du handler


binaryDataObjectIdWithoutObjectGroupId :
   contenu : cette map contient les id des objets binaires ainsi que les groupes d'objets techniques instanciés lors du parcours des objets binaires.
   création : elle est créé lors de la création du handler
   mise à jour : elle est populée lors du parcours des objets binaires dans mapNewTechnicalDataObjectGroupToBDO et extractArchiveUnitToLocalFile (dans on découvre un DataObjectReferenceId) 
   lecture : lecture de la map dans mapNewTechnicalDataObjectGroupToBDO, extractArchiveUnitToLocalFile, getNewGdoIdFromGdoByUnit, 
   suppression : c'est un clean en fin d'execution du handler

Le groupe d'objet technique GotObj contient un guid et un boolean isVisited, initialisé à false lors de la création. Le set à true est fait lors du parcours des units.

TODO contenu des autres maps


Worker-common
-------------

Le worker-common contient majoritairement des classes utilitaires.
A terme, il faudra que SedaUtils notamment soit "retravaillé" pour que les différentes méthodes soit déplacées dans les bons Handlers.

Worker-client
-------------
Le worker client contient le code permettant l'appel vers les API Rest offert par le worker.
Pour le moment une seule méthode est offerte : submitStep. Pour plus de détail, voir la partie worker-client.


