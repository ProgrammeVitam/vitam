Worker
######

Présentation
============

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.worker**

4 modules composent la partie worker :
- worker-common : incluant la partie common (Utilitaires...), notamment le SedaUtils.
- worker-core : contenant les différents handlers.
- worker-client : incluant le client permettant d'appeler le REST.
- worker-server : incluant la partie REST.

Worker-server
=============

Rest API
--------

Pour l'instant les uri suivantes sont déclarées :

| http://server/worker/v1
| POST /tasks -> **POST Permet de lancer une étape à exécuter**

Registration
------------

Une partie registration permet de gérer la registration du Worker.

La gestion de l'abonnement du *worker* auprès du serveur *processing* se fait à l'aide d'un ServletContextListener : *fr.gouv.vitam.worker.server.registration.WorkerRegistrationListener*.

Le WorkerRegistrationListener va lancer l'enregistrement du *worker* au démarrage du serveur worker, dans un autre Thread utilisant l'instance *Runnable* : *fr.gouv.vitam.worker.server.registration.WorkerRegister*.

L'execution du *WorkerRegister* essaie d'enregistrer le *worker* suivant un retry paramétrable dans la configuration du serveur avec :

- un délai (registerDelay en secondes)
- un nombre d'essai (registerTry)

Le lancement du serveur est indépendant de l'enregistrement du *worker* auprès du *processing* : le serveur *worker* ne s'arrêtera pas si l'enregistrement n'a pas réussi.

Configuration de worker
-----------------------

Cela présente la configuration pour un worker quand il est déployé. Deux paramètres importants quand le worker fonctionne en mode parallèle.   

 * WorkerCapacity :

	Cela présente la capacité d'un worker qui réponds au demande de parallélisation de la distribution de tâches du workflow.  
	Il est précisé par le paramètre capacity dans le WorkerConfiguration.    
 
 * WorkerFamily :

 Chaque worker est configuré pour traiter groupe de tâches corresponsant à ses fonctions et on cela permetre de définir les familles de worker. 
 Il est précisé par workerFamily dans le WorkerConfigration.  

WorkerBean
----------

présente l'information complète sur un worker pour la procédure d'enregistrement d'un worker. Il contient les information sur le nom, 
la famille et la capacité ... d'un worker et présente en mode json. Voici un example :  

.. code-block:: json
    
   { "name" : "workername", "family" : "DefaultWorker", "capacity" : 10, "storage" : 100,
   "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 12345 } }
 
 
Persistence des workers
-----------------------
 
 La lise de workers est persistée dans une base de données. Pour le moment, la base est un fichier de données qui contient une tableau de 
 workers en format ArrayNode et chaque worker est une élément JsonNode. Exemple ci-dessous est des données d'une liste de workers 

.. code-block:: json

  [
    {"workerId": "workerId1", "workerinfo": { "name" : "workername", "family" : "DefaultWorker", "capacity" : 10, "storage" : 100,
    "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 12345 }}},   
    {"workerId": "workerId2", "workerinfo": { "name" : "workername2", "family" : "BigWorker", "capacity" : 10, "storage" : 100,
    "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 54321 } }} 
  ]

Le fichier nommé "worker.db" qui sera créé dans le répertoire ``/vitam/data/processing``.
 
Chaque worker est identifié par workerId et l'information générale du champs workerInfo. L'ensemble des actions suivantes sont traitées : 
  
* Lors du redémarrage du distributor, il recharge la liste des workers enregistrés. Ensuite, il vérifie le status de chaque worker de la liste, 

(serverPort:serverHost) en utilisant le WorkerClient. Si le worker qui n'est pas disponible, il sera supprimé de la liste des workers enregistrés et la base sera mise à jour. 

* Lors de l'enregistrement/désenregistrement, la liste des workers enregistrés sera mis à jour (ajout/supression d'un worker).        

.. code-block:: java

	checkStatusWorker(String serverHost, int serverPort) // vérifier le statut d'un worker	
	marshallToDB()   // mise à jour la base de la liste des workers enregistrés
	
	
Désenregistrement d'un worker
-----------------------------

Lorsque le worker s'arrête ou se plante, ce worker doit être désenregistré. 

* Si le worker s'arrête, la demande de désenregistrement sera lancé pour le contexte "contextDestroyed" de la WorkerRegistrationListener  (implémenté de ServletContextListener) en utilisant le ProcessingManagementClient pour appeler le service de desenregistrement de distributeur.   

* Si le worker se plante, il ne réponse plus aux requêtes de WorkerClient dans la "run()" WorkerThread et dans le catch() des exceptions de de traitement, 

une demande de désenregistrement doit être appelé dans cette boucle.

- le distributeur essaie de faire une vérification de status de workers en appelant checkStatusWorker() en plusieurs fois définit dans GlobalDataRest.STATUS_CHECK_RETRY). 
- si après l'étape 1 le statut de worker est toujours indisponible, le distributeur va appeler la procédure de désenregistrement de ce worker de la liste de worker enregistrés. 


Worker-core
===========

Dans la partie Core, sont présents les différents Handlers nécessaires pour exécuter les différentes actions.

- CheckConformityActionHandler
- CheckObjectsNumberActionHandler
- CheckObjectUnitConsistencyActionHandler
- CheckSedaActionHandler
- CheckStorageAvailabilityActionHandler
- CheckVersionActionHandler
- ExtractSedaActionHandler
- CheckIngestContractActionHandler
- IndexObjectGroupActionHandler
- IndexUnitActionHandler
- StoreObjectGroupActionHandler
- FormatIdentificationActionHandler
- AccessionRegisterActionHandler
- TransferNotificationActionHandler
- UnitsRulesCompteHandler
- DummyHandler

Plugins Worker : les plugins proposent des actions comme les Handler. Quand le service worker démarré, les plugins et leur fichier properties 
sont chargés. Les actions sont cherché d'abord dans le plugin pour le traitement, si l'action ne trouve pas dans plugin, il sera appelé dans 
le Handler correspondant.
 
- CheckConfirmityActionPlugin : pour la vérification de la conformité de document
- FormatIdentificationActionPlugin : pour le vérification de formats de fichiers
- StoreObjectGroupActionPlugin : pour le storage des groupes d'objets
- UnitsRulesComputeActionPlugin :  pour la gestion de règles de gestion
- IndexUnitActionPlugin : pour indexer des unités archivistes
- IndexObjectGroupActionPlugin : pour indexer des groupes d'objets
- ArchiveUnitRulesUpdateActionPlugin : mise à jour des unités archivisitiques
- RunningIngestsUpdateActionPlugin : mise à jour des ingests en cours

La classe WorkerImpl permet de lancer ces différents handlers.

Focus sur la gestion des entrées / sorties  des Handlers
--------------------------------------------------------

Chaque Handler a un constructeur sans argument et est lancé avec la commande :

.. code-block:: java

  CompositeItemStatus execute(WorkerParameters params, HandlerIO ioParam).
  ..

Le HandlerIO a pour charge d'assurer la liaison avec le Workspace et la mémoire entre tous les handlers d'un step.

La structuration du HandlerIO est la suivante :

- des paramètres d'entrées (in) :

   - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
   - une cible (uri) comportant un schema (WORKSPACE, MEMORY, VALUE) et un path :

      - WORKSPACE:path indique le chemin relatif sur le workspace
      - MEMORY:path indique le nom de la clef de valeur
      - VALUE:path indique la valeur statique en entrée

   - chaque handler peut accéder à ces valeurs, définies dans l'ordre stricte, via le handlerIO

      - WORKSPACE : implicitement un File

.. code-block:: java

  File file = handlerIO.getInput(rank);
  ..


      - MEMORY : implicitement un objet mémoire déjà alloué par un Handler précédent

.. code-block:: java

  // Object could be whatever, Map, List, JsonNode or even File
  Object object = handlerIO.getInput(rank);
  ..

      - VALUE : implicitement une valeur String

.. code-block:: java

  String string = handlerIO.getInput(rank);
  ..


- des paramètres d'entrées (out) :

   - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
   - une cible (uri) comportant un schema (WORKSPACE, MEMORY) et un path :

      - WORKSPACE:path indique le chemin relatif sur le workspace
      - MEMORY:path indique le nom de la clef de valeur

   - chaque handler peut stocker les valeurs finales, définies dans l'ordre stricte, via le handlerIO


      - WORKSPACE : implicitement un File local

.. code-block:: java

  // To get the filename as specified by the workflow
  ProcessingUri uri = handlerIO.getOutput(rank);
  String filename = uri.getPath();
  // Write your own file
  File newFile = handlerIO.getNewLocalFile(filename);
  // write it
  ...
  // Now give it back to handlerIO as ouput result,
  // specifying if you want to delete it right after or not
  handlerIO.addOuputResult(rank, newFile, true);
  // or let the handlerIO delete it later on
  handlerIO.addOuputResult(rank, newFile);
  ..

      - MEMORY : implicitement un objet mémoire

.. code-block:: java

  // Create your own Object
  MyClass object = ...
  // Now give it back to handlerIO as ouput result
  handlerIO.addOuputResult(rank, object);
  ..


Afin de vérifier la cohérence entre ce qu'attend le Handler et ce que contient le HandlerIO, la méthode suivante est à réaliser :

.. code-block:: java

  List<Class<?>> clasz = new ArrayList<>();
  // add in order the Class type of each Input argument
  clasz.add(File.class);
  clasz.add(String.class);
  // Then check the conformity passing the number of output parameters too
  boolean check = handlerIO.checkHandlerIO(outputNumber, clasz);
  // According to the check boolean, continue or raise an error
  ..


Cas particulier des Tests unitaires
-----------------------------------

Afin d'avoir un handlerIO correctement initialisé, il faut redéfinir le handlerIO manuellement comme l'attend le handler :

.. code-block:: java

  // In a common part (@Before for instance)
  HandlerIO handlerIO = new HandlerIO("containerName", "workerid");
  List<IOParameter> out = new ArrayList<>();
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UnitsLevel/ingestLevelStack.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_GUID_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OG_TO_ARCHIVE_ID_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "ATR/globalSEDAParameters.json")));
  // Dans un bloc @After, afin de nettoyer les dossiers
  @After
  public void aftertest() {
    handlerIO.close();
  }
  // Pour chaque test
  @Test
  public void test() {
    handlerIO.addOutIOParameters(out);
    ...
  }


Si nécessaire et si compatible, il est possible de passer par un mode MEMORY pour les paramètres "in" :

.. code-block:: java

  // In a common part (@Before for instance)
  HandlerIO handlerIO = new HandlerIO("containerName", "workerid");
  // Declare the signature in but instead of using WORKSPACE, use MEMORY
  List<IOParameter> in = new ArrayList<>();
  in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file1")));
  in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file2")));
  in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file3")));
  in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file4")));
  // Dans un bloc @After, afin de nettoyer les dossiers
  @After
  public void aftertest() {
  handlerIO.close();
  }
  // Pour chaque test
  @Test
  public void test() {
  // Use it first as Out parameters
  handlerIO.addOutIOParameters(in);
  // Initialize the real value in MEMORY using those out parameters from Resource Files
  handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
  handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
  handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(DO_TO_DO_INFO_MAP));
  handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS));
  // Reset the handlerIo in order to remove all In and Out parameters
  handlerIO.reset();
  // And now declares the In parameter list, that will use the MEMORY default values
  handlerIO.addInIOParameters(in);
  ...
  }
  // If necessary, delcares real OUT parameters too there
  List<IOParameter> out = new ArrayList<>();
  out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "file5")));
  handlerIO.addOutIOParameters(out);
  // Now handler will have access to in parameter as File as if they were coming from Workspace


Création d'un nouveau handler
-----------------------------

La création d'un nouveaux handler doit être motivée par certaines conditions nécessaires :

- lorsque qu'il n'y a pas de handler qui répond au besoin
- lorsque rajouter la fonctionnalité dans un handler existant, le surcharge et le détourne de sa fonctionalité première
- lorsque l'on veut refactorer un handler existant pour donner des fonctionalités 'un peu' plus 'élémentaires'

Les handlers doivent étendrent la classe ActionHandler et implémenter la méthode execute.
Lors de la création d'un nouveau handler, il faut ajouter une nouvelle instance, dans WorkerImpl.init pour enregistrer le handler dans le worker et définir le handler id.
Celui-ci sert de clé pour :

- les messages dans logbook (vitam-logbook-messages_fr.properties) en fonction de la criticité
- les fichiers json de définition des workflows json (exemple : DefaultIngestWorkflow.json)

cf. workflow


Details des Handlers
====================

Détail du handler : CheckConformityActionHandler
------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de contrôle de l'empreinte. Il comprend désormais 2 tâches :

-- Vérification de l'empreinte par rapport à l'empreinte indiquée dans le manifeste (en utilisant algorithme déclaré dans manifeste)
-- Calcul d'une empreinte en SHA-512 si l'empreinte du manifeste est calculée avec un algorithme différent

Exécution
~~~~~~~~~

CheckConformityActionHandler recupère l'algorithme de Vitam (SHA-512) par l'input dans workflow et le fichier en InputStream par le workspace.

Si l'algorithme est différent que celui dans le manifest, il calcul l'empreinte de fichier en SHA-512

.. code-block:: java

	DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput().get(ALGO_RANK));
  response = handlerIO.getInputStreamNoCachedFromWorkspace(
  IngestWorkflowConstants.SEDA_FOLDER + "/" + binaryObject.getUri());
  InputStream inputStream = (InputStream) response.getEntity();
  final Digest vitamDigest = new Digest(digestTypeInput);
  Digest manifestDigest;
  boolean isVitamDigest = false;
  if (!binaryObject.getAlgo().equals(digestTypeInput)) {
      manifestDigest = new Digest(binaryObject.getAlgo());
      inputStream = manifestDigest.getDigestInputStream(inputStream);
  } else {
      manifestDigest = vitamDigest;
      isVitamDigest = true;
  }
  ......................


Si les empreintes sont différents, c'est le cas KO.
Le message { "MessageDigest": "value", "Algorithm": "algo", "ComputedMessageDigest": "value"} va être stocké dans le journal
Sinon le message { "MessageDigest": "value", "Algorithm": "algo", "SystemMessageDigest": "value", "SystemAlgorithm": "algo"} va être stocké dans le journal
Mais il y a encore deux cas à ce moment:

	si l'empreinte est avec l'algorithme SHA-512, c'est le cas OK.
	sinon, c'est le cas WARNING. le nouveau empreint et son algorithme seront mis à jour dans la collection ObjectGroup.

CheckConformityActionHandler compte aussi le nombre de OK, KO et WARNING.
Si nombre de KO est plus de 0, l'action est KO.

4.1.3 journalisation
~~~~~~~~~~~~~~~~~~~~

logbook lifecycle
=================

CA 1 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste)

Dans le processus d'entrée, l'étape de vérification de la conformité de l'empreinte doit être appelée en position 450.
Lorsque l'étape débute, pour chaque objet du groupe d'objet technique, une vérification d'empreinte doit être effectuée (celle de l'objet avec celle inscrite dans le manifeste SEDA). Cette étape est déjà existante actuellement.
Le calcul d'empreinte en SHA-512 (CA 2) ne doit pas s'effectuer si l'empreinte renseigné dans le manifeste a été calculé en SHA-512. C'est cette empreinte qui sera indexée dans les bases Vitam.

CA 1.1 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste) - OK

- Lorsque l'action est OK, elle inscrit une ligne dans les journaux du cycle de vie des GOT :

* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "OK"
* outcomeDetailMessage FR : « Succès de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>"
* objectIdentifierIncome : MessageIdentifier du manifest

Comportement du workflow décrit dans l'US #680

- La collection ObjectGroup est aussi mis à jour, en particulier le champs : Message Digest : {  empreinte, algorithme utlisé }

CA 1.2 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste) - KO

- Lorsque l'action est KO, elle inscrit une ligne dans les journaux du cycle de vie des GOT :

* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "KO"
* outcomeDetailMessage FR : « Échec de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm> Empreinte calculée : <Empreinte calculée par Vitam>"
* objectIdentifierIncome : MessageIdentifier du manifest

Comportement du workflow décrit dans l'US #680

-----------------------------------

CA 2 : Vérification de la conformité de l'empreinte. (empreinte différent de SHA-512 dans le manifeste)

Si l'empreinte proposé dans le manifeste SEDA n'est pas en SHA-512, alors le système doit calculer l'empreinte en SHA-512. C'est cette empreinte qui sera indexée dans les bases Vitam.
Lorsque l'action débute, pour chaque objet du groupe d'objet technique, un calcul d'empreinte au format SHA-512 doit être effectué. Cette action intervient juste apres le check de l'empreinte dans le manifeste (mais on est toujours dans l'étape du check conformité de l'empreinte).

CA 2.1 : Vérification de la conformité de l'empreinte. (empreinte différent de SHA-512 dans le manifeste) - OK

- Lorsque l'action est OK, elle inscrit une ligne dans les journaux du cycle de vie des GOT :

* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "OK"
* outcomeDetailMessage FR : « Succès de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte Manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>" "Empreinte calculée (<algorithme utilisé "XXX">): <Empreinte calculée par Vitam>"
* objectIdentifierIncome : MessageIdentifier du manifest

modules utilisés
----------------

processing, worker, workspace et logbook

cas d'erreur
~~~~~~~~~~~~

XMLStreamException                          : problème de lecture SEDA
InvalidParseOperationException              : problème de parsing du SEDA
LogbookClientAlreadyExistsException         : un logbook client existe dans ce workflow
LogbookClientBadRequestException            : LogbookLifeCycleObjectGroupParameters est mal paramétré et le logbook client génère une mauvaise requete
LogbookClientException                      : Erreur générique de logbook. LogbookException classe mère des autres exceptions LogbookClient
LogbookClientNotFoundException              : un logbook client n'existe pas pour ce workflow
LogbookClientServerException                : logbook server a un internal error
ProcessingException                         : erreur générique du processing
ContentAddressableStorageException          : erreur de stockage


Détail du handler : CheckObjectsNumberActionHandler
---------------------------------------------------

description
~~~~~~~~~~~

Ce handler permet de comparer le nombre d'objet stocké sur le workspace et le nombre d'objets déclaré dans le manifest.

Détail du handler : CheckObjectUnitConsistencyActionHandler
-----------------------------------------------------------

Ce handler permet de contrôler la cohérence entre l'object/object group et l'ArchiveUnit.

Pour ce but, on détecte les groupes d'object qui ne sont pas référé par au moins d'un ArchiveUnit.
Ce tache prend deux maps de données qui ont été crée dans l'étape précédente de workflow comme input :
objectGroupIdToUnitId
objectGroupIdToGuid
Le ouput de cette contrôle est une liste de groupe d'objects invalide. Si on trouve les groupe d'objects
invalide, le logbook lifecycles de group d'object sera mis à jour.

L'exécution de l'algorithme est présenté dans le code suivant :*

.. code-block:: java 

  while (it.hasNext()) {
    final Map.Entry<String, Object> objectGroup = it.next();
    if (!objectGroupToUnitStoredMap.containsKey(objectGroup.getKey())) {
      itemStatus.increment(StatusCode.KO);
      try {
        // Update logbook OG lifecycle
        final LogbookLifeCycleObjectGroupParameters logbookLifecycleObjectGroupParameters =
            LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters();
        LogbookLifecycleWorkerHelper.updateLifeCycleStartStep(handlerIO.getHelper(),
            logbookLifecycleObjectGroupParameters,
            params, HANDLER_ID, LogbookTypeProcess.INGEST,
            objectGroupToGuidStoredMap.get(objectGroup.getKey()).toString());
        logbookLifecycleObjectGroupParameters.setFinalStatus(HANDLER_ID, null, StatusCode.KO,
            null);
        handlerIO.getHelper().updateDelegate(logbookLifecycleObjectGroupParameters);
        final String objectID =
            logbookLifecycleObjectGroupParameters.getParameterValue(LogbookParameterName.objectIdentifier);
        handlerIO.getLifecyclesClient().bulkUpdateObjectGroup(params.getContainerName(),
            handlerIO.getHelper().removeUpdateDelegate(objectID));
      } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
        LogbookClientServerException | ProcessingException e) {
        LOGGER.error("Can not update logbook lifcycle", e);
      }
      ogList.add(objectGroup.getKey());
    } else {
      itemStatus.increment(StatusCode.OK);
      // Update logbook OG lifecycle
      ....
    }
  }


Détail du handler : CheckSedaActionHandler
------------------------------------------

Ce handler permet de valider la validité du manifest par rapport à un schéma XSD. 
Il permet aussi de vérifier que les informations remplies dans ce manifest sont correctes.

- Le schéma de validation du manifest : src/main/resources/seda-vitam-2.0-main.xsd.

Détail du handler : CheckStorageAvailabilityActionHandler
---------------------------------------------------------

TODO

Détail du handler : CheckVersionActionHandler
---------------------------------------------

TODO

Détail du handler : ExtractSedaActionHandler
--------------------------------------------

description
~~~~~~~~~~~

Ce handler permet d'extraire le contenu du SEDA. Il y a :

- extraction des BinaryDataObject et PhysicalDataObject
- extraction des ArchiveUnit
- création des lifes cycles des units
- construction de l'arbre des units et sauvegarde sur le workspace
- sauvegarde de la map des units sur le workspace
- sauvegarde de la map des objets sur le workspace
- sauvegarde de la map des objets groupes sur le workspace


Détail des différentes maps utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Map<String, String> dataObjectIdToGuid

    contenu         : cette map contient l'id du DO relié à son guid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors de la lecture des BinaryDataObject et PhysicalDataObject
    lecture, get    : saveObjectGroupsToWorkspace, getObjectGroupQualifiers,
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> dataObjectIdToObjectGroupId :

    contenu         : cette map contient l'id du DO relié au groupe d'objet de la balise DataObjectGroupId ou DataObjectGroupReferenceId
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors de la lecture des BinaryDataObject et PhysicalDataObject
    lecture, get    : lecture de la map dans mapNewTechnicalDataObjectGroupToDO, getNewGdoIdFromGdoByUnit, completeDataObjectToObjectGroupMap, checkArchiveUnitIdReference et writeDataObjectInLocal
    suppression     : c'est un clean en fin d'execution du handler

Map<String, GotObj> dataObjectIdWithoutObjectGroupId :

    contenu         : cette map contient l'id du DO relié à un groupe d'objet technique instanciés lors du parcours des objets.
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des DO dans mapNewTechnicalDataObjectGroupToDO et extractArchiveUnitToLocalFile. Dans extractArchiveUnitToLocalFile, quand on découvre un DataObjectReferenceId et que cet Id se trouve dans dataObjectIdWithoutObjectGroupId alors on récupère l'objet et on change le statut isVisited à true.
    lecture, get    : lecture de la map dans mapNewTechnicalDataObjectGroupToDO, extractArchiveUnitToLocalFile, getNewGdoIdFromGdoByUnit,
    suppression     : c'est un clean en fin d'execution du handler

Le groupe d'objet technique GotObj contient un guid et un boolean isVisited, initialisé à false lors de la création. Le set à true est fait lors du parcours des units.

Map<String, String> objectGroupIdToGuid

    contenu         : cette map contient l'id du groupe d'objet relié à son guid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des DO dans writeDataObjectInLocal et mapNewTechnicalDataObjectGroupToDO lors de la création du groupe d'objet technique
    lecture, get    : lecture de la map dans checkArchiveUnitIdReference, writeDataObjectInLocal, extractArchiveUnitToLocalFile, saveObjectGroupsToWorkspace
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> objectGroupIdToGuidTmp

    contenu         : c'est la même map que objectGroupIdToGuid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée dans writeDataObjectInLocal
    lecture, get    : lecture de la map dans writeDataObjectInLocal
    suppression     : c'est un clean en fin d'execution du handler

Map<String, List<String>> objectGroupIdToDataObjectId

    contenu         : cette map contient l'id du groupe d'objet relié à son ou ses DO
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des DO dans writeDataObjectInLocal quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId et qu'il n'existe pas dans objectGroupIdToDataObjectId.
    lecture, get    : lecture de la map dans le parcours des DO dans writeDataObjectInLocal.  La lecture est faite pour ajouter des DO dans la liste.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, List<String>> objectGroupIdToUnitId

    contenu         : cette map contient l'id du groupe d'objet relié à ses AU
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des units dans extractArchiveUnitToLocalFile quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId et qu'il nexiste pas dans objectGroupIdToUnitId sinon on ajoute dans la liste des units de la liste
    lecture, get    : lecture de la map dans le parcours des units. La lecture est faite pour ajouter des units dans la liste.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, DataObjectInfo> objectGuidToDataObject

    contenu         : cette map contient le guid du data object et DataObjectInfo
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populer lors de l'extraction des infos du data object vers le workspace
    lecture, get    : elle permet de récupérer les infos binary data object pour sauver l'object group sur le worskapce
    supression      : c'est un clean en fin d'execution du handler

Map<String, String> unitIdToGuid

    contenu         : cette map contient l'id de l'unit relié à son guid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des units dans extractArchiveUnitToLocalFile
    lecture, get    : lecture de la map se fait lors de la création du graph/level des unit dans createIngestLevelStackFile et dans la sauvegarde des object groups vers le workspace
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> unitIdToGroupId

    contenu         : cette map contient l'id de l'unit relié à son group id
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des DO dans writeDataObjectInLocal quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId
    lecture, get    : lecture de la map se fait lors de l'extraction des unit dans extractArchiveUnitToLocalFile et permettant de lire dans objectGroupIdToGuid.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> objectGuidToUri

    contenu         : cette map contient le guid du BDO relié à son uri définis dans le manifest
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est poppulée lors du parcours des DO dans writeDataObjectInLocal quand il rencontre la balise uri
    lecture, get    : lecture de la map se fait lors du save des objects groups dans le workspace
    suppression     : c'est un clean en fin d'execution du handler

sauvegarde des maps (dataObjectIdToObjectGroupId, objectGroupIdToGuid) dans le workspace

Vérifier les ArchiveUnit du SIP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dans les cas où le SIP contient un objet numérique référencé par un groupe d'objet et qu'une unité archiviste
référence cet objet directement (au lieu de déclarer le GOT), le résultat attendu est un statut KO au niveau de 
l'étape STP_INGEST_CONTROL_SIP dans l'action CHECK_MANIFEST. Ce contrôle est effectué dans la fonction 
checkArchiveUnitIdReference de ExtractSedaHandler.

Pour ce cas, le map unitIdToGroupId contient une référence entre un unitId et groupId et ce groupId est l'id de l'objet numérique.  
Dans le objectGroupIdToGuid, il n'existe pas de lien entre id de groupe d'objet et son guid (parce que c'est un id d'object
numérique).

On vérifie la valeur des groupIds récupérés dans dataObjectIdToObjectGroupId et unitIdToGroupId. Si ils sont différents,
il s'agit du cas abordé ci-dessus, sinon c'est celui des objects numériques sans groupe d'objet technique. Enfin, l'exception
ArchiveUnitContainDataObjectException est déclenchée pour ExtractSeda et dans cette étape, le status KO est mise à jour 
pour l'exécution de l'étape.

L'exécution de l'algorithme est présenté dans le preudo-code ci-dessous:

.. code-block:: text

  Si (map unitIdToGroupId contient des valeurs)    
    Pour (chaque élement ELEM du map unitIdToGroupId)
      Si (la valeur guid de groupe d'object dans objectGroupIdToGuid associé à ELEM) // archiveUnit reference par DO
        Prendre (la valeur groupId dans le maps dataObjectIdToObjectGroupId associé à groupId d'ELEM)
        Si (cette groupId est NULLE) // ArchiveUnit réferencé DO mais il n'existe pas un lien DO à groupe d'objet 
          Délencher (exception ProcessingException)
        Autrement
          Si (cette groupId est différente grouId associé à ELEM)
            Délencher (exception ArchiveUnitContainDataObjectException)
          Fin Si
        Fin Si
      Fin Si
    Fin Pour
  Fin Si


Détails du data dans l'itemStatus retourné
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Le itemStatus est mis à jour avec les objets du manifest.xml remontées pour mettre à jour evDetData.
Il contient dans data le json de evDetData en tant que String.
Les champs récupérés (s'ils existent dans le manifest) sont "evDetailReq", "evDateTimeReq", "ArchivalAgreement", "agIfTrans", "ServiceLevel".

Détail du handler : IndexObjectGroupActionHandler
-------------------------------------------------

4.7.1 description
~~~~~~~~~~~~~~~~~

Indexation des objets groupes en récupérant les objets groupes du workspace. Il y a utilisation d'un client metadata.

.. TODO

4.8 Détail du handler : IndexUnitActionHandler
----------------------------------------------

4.8.1 description
=================

Indexation des units en récupérant les units du workspace. Il y a utilisation d'un client metadata.

.. TODO

4.9 Détail du handler : StoreObjectGroupActionHandler
-----------------------------------------------------

4.9.1 description
=================
Persistence des objets dans l'offre de stockage depuis le workspace.

.. TODO

4.10 Détail du handler : FormatIdentificationActionHandler
----------------------------------------------------------

4.10.1 Description
==================

Ce handler permet d'identifier et contrôler automatiquement le format des objets versés.
Il s'exécute sur les différents ObjectGroups déclarés dans le manifest. Pour chaque objectGroup, voici ce qui est effectué :

- récupération du JSON de l'objectGroup présent sur le Workspace
- transformation de ce Json en une map d'id d'objets / uri de l'objet associée
- boucle sur les objets :

 - téléchargement de l'objet (File) depuis le Workspace
 - appel l'outil de vérification de format (actuellement Siegfried) en lui passant le path vers l'objet à identifier + récupération de la réponse.
 - appel de l'AdminManagement pour faire une recherche getFormats par rapport au PUID récupéré.
 - mise à jour du Json : le format récupéré par Siegfried est mis à jour dans le Json (pour indexation future).
 - construction d'une réponse.

- sauvegarde du JSON de l'objectGroup dans le Workspace.
- aggrégation des retours pour générer un message + mise à jour du logbook.

4.10.2 Détail des différentes maps utilisées :
==============================================

Map<String, String> objectIdToUri

    contenu         : cette map contient l'id du BDO associé à son uri.
    création        : elle est créée dans le Handler après récupération du json listant les ObjectGroups
    MAJ, put        : elle est populée lors de la lecture du json listant les ObjectGroups.
    lecture, get    : lecture au fur et à mesure du traitement des BDO.
    suppression     : elle n'est pas enregistrée sur le workspace et est présente en mémoire uniquement.

4.10.3 exécution
================

Ce Handler est exécuté dans l'étape "Contrôle et traitements des objets", juste après le Handler de vérification des empreintes.

4.10.4 journalisation : logbook operation? logbook life cycle?
==============================================================

Dans le traitement du Handler, sont mis à jour uniquement les journaux de cycle de vie des ObjectGroups.
Les Outcome pour les journaux de cycle de vie peuvent être les suivants :

- Le format PUID n'a pas été trouvé / ne correspond pas avec le référentiel des formats.
- Le format du fichier n'a pas pu être trouvé.
- Le format du fichier a été complété dans les métadonnées (un "diff" est généré et ajouté).
- Le format est correct et correspond au référentiel des formats.

(Note : les messages sont informatifs et ne correspondent aucunement à ce qui sera vraiment inséré en base)

4.10.5 modules utilisés
=======================

Le Handler utilise les modules suivants :

- Workspace (récupération / copie de fichiers)
- Logbook (mise à jour des journaux de cycle de vie des ObjectGroups)
- Common-format-identification (appel pour analyse des objets)
- AdminManagement (comparaison format retourné par l'outil d'analyse par rapport au référentiel des formats de Vitam).

4.10.6 cas d'erreur
===================

Les différentes exceptions pouvant être rencontrées :

- ReferentialException : si un problème est rencontré lors de l'interrogation du référentiel des formats de Vitam
- InvalidParseOperationException/InvalidCreateOperationException : si un problème est rencontré lors de la génération de la requête d'interrogation du référentiel des formats de Vitam
- FormatIdentifier*Exception : si un problème est rencontré avec l'outil d'analyse des formats (Siegfried)
- Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
- Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
- Content*Exception : si un problème est rencontré lors de l'interrogation du workspace
- ProcessingException : si un problème plus général est rencontré dans le Handler


Détail du handler : TransferNotificationActionHandler
-----------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de finaliser le processus d'entrée d'un SIP. Cet Handler est un peu spécifique car il sera lancé même si une étape précédente tombe en erreur.

Il permet de générer un xml de notification qui sera :

- une notification KO si une étape du workflow est tombée en erreur.
- une notification OK si le process est OK, et que le SIP a bien été intégré sans erreur.

La première étape dans ce handler est de déterminer l'état du Workflow : OK ou KO.

Détail des différentes maps utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Map<String, Object> archiveUnitSystemGuid

    contenu         : cette map contient la liste des archives units avec son identifiant tel que déclaré dans le manifest, associé à son GUID.

Map<String, Object> dataObjectSystemGuid

    contenu         : cette map contient la liste Data Objects avec leur GUID généré associé à l'identifiant déclaré dans le manifest.

Map<String, Object> bdoObjectGroupSystemGuid

    contenu         : cette map contient la liste groupes d'objets avec leur GUID généré associé à l'identifiant déclaré dans le manifest.

exécution
~~~~~~~~~

Ce Handler est exécuté en dernière position. Il sera exécuté quoi qu'il se passe avant.
Même si le processus est KO avant, le Handler sera exécuté.

*Cas OK :*
@TODO@

*Cas KO :*
Pour l'opération d'ingest en cours, on va récupérer dans les logbooks plusieurs informations :

- récupération des logbooks operations générés par l'opération d'ingest.
- récupération des logbooks lifecycles pour les archive units présentes dans le SIP.
- récupération des logbooks lifecycles pour les groupes d'objets présents dans le SIP.

Le Handler s'appuie sur des fichiers qui lui sont transmis. Ces fichiers peuvent ne pas être présents si jamais le process est en erreur avec la génération de ces derniers.

- un fichier globalSedaParameters.file contenant des informations sur le manifest (messageIdentifier).
- un fichier mapsUnits.file : présentant une map d'archive unit
- un fichier mapsDO.file : présentant la liste des data objects
- un fichier mapsDOtoOG.file : mappant le data object à son object group

A noter que ces fichiers ne sont pas obligatoires pour le bon déroulement du handler.

Le handler va alors procéder à la génération d'un XML à partir des informationss aggrégées.
Voici sa structure générale :

- MessageIdentifier est rempli avec le MessageIdentifier présent dans le fichier globalSedaParameters. Il est vide si le fichier n'existe pas.
- dans la balise ReplyOutcome :

  - dans Operation, on aura une liste d'events remplis par les différentes opérations KO et ou FATAL. La liste sera forcément remplie avec au moins un event. Cette liste est obtenue par l'interrogation de la collection LogbookOperations.
  - dans ArchiveUnitList, on aura une liste d'events en erreur. Cette liste est obtenue par l'interrogation de la collection LogbookLifecycleUnits.
  - dans DataObjectList, on aura une liste d'events en erreur. Cette liste est obtenue par l'interrogation de la collection LogbookLifecycleObjectGroups.


Le XML est alors enregistré sur le Workspace.

journalisation : logbook operation? logbook life cycle?
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Dans le traitement du Handler, le logbook est interrogé : opérations et cycles de vie.
Cependant aucune mise à jour est effectuée lors de l'exécution de ce handler.


modules utilisés
~~~~~~~~~~~~~~~~

Le Handler utilise les modules suivants :

- Workspace (récupération / copie de fichiers)
- Logbook (partie server) : pour le moment la partie server du logbook est utilisée pour récupérer les différents journaux (opérations et cycles de vie).
- Storage : permettant de stocker l'ATR.

cas d'erreur
~~~~~~~~~~~~

Les différentes exceptions pouvant être rencontrées :

- Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
- Content*Exception : si un problème est rencontré lors de l'interrogation du workspace
- XML*Exception : si un souci est rencontré sur la génération du XML
- ProcessingException : si un problème plus général est rencontré dans le Handler


Détail du handler : AccessionRegisterActionHandler
--------------------------------------------------

Description
~~~~~~~~~~~

AccessionRegisterActionHandler permet de fournir une vue globale et dynamique des archives

sous la responsabilité du service d'archives, pour chaque tenant.

Détail des maps utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~

Map<String, String> objectGroupIdToGuid

    contenu         : cette map contient l'id du groupe d'objet relié à son guid

Map<String, String> archiveUnitIdToGuid

	contenu         : cette map contient l'id du groupe d'objet relié à son guid

Map<String, Object> dataObjectIdToDetailDataObject

	contenu         : cette map contient l'id du data object relié à ses informations


Exécution
~~~~~~~~~

L'alimentation du registre des fonds a lieu pendant la phase de finalisation de l'entrée,

une fois que les objets et les units sont rangés. ("stepName": "STP_INGEST_FINALISATION")

Le Registre des Fonds est alimenté de la manière suivante:

	-- un identifiant unique
	-- des informations sur le service producteur (OriginatingAgency)
	-- des informations sur le service versant (SubmissionAgency), si différent du service producteur

   -- des informations sur le contrat (ArchivalAgreement)

	-- date de début de l’enregistrement (Start Date)
	-- date de fin de l’enregistrement (End Date)
	-- date de dernière mise à jour de l’enregistrement (Last update)
	-- nombre d’units (Total Units)
	-- nombre de GOT (Total ObjectGroups)
	-- nombre d'Objets (Total Objects)
	-- volumétrie des objets (Object Size)
	-- id opération d’entrée associée [pour l'instant, ne comprend que l'evIdProc de l'opération d'entrée concerné]
	-- status (ItemStatus)

Détail du handler : CheckIngestContractActionHandler
----------------------------------------------------

Description
~~~~~~~~~~~

CheckIngestContractHandler permet de vérifier la présence et contrôler le contrat d'entrée  
du SIP à télécharger. 

Détail des données utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

 globalSEDAParameters.json
 Ce handler prend ce fichier comme le parametre d'entrée. Le fichier contient des données gobales sur l'ensemble des 
 parametrès du bordereau et il a été généré à l'étape de l'ExtractSedeActionHandler (CHECK_MANIFEST).    

Exécution
~~~~~~~~~

Le handler cherche d'abord dans globalSEDAParameters.json le nom du contrat déclaré dans le SIP associé au balise <ArchivalAgreement>. 
Si il n'y as pas de déclaration de contrat d'entrée, le handler retourne le status OK. Si il y a un déclaration de contrat, une liste 
des opérations suivantes sera effectué : 

	- recherche du contrat d'entrée déclaré dans la référentiel de contrat  
	- vérification de contrat : 

			si le contrat non trouvé ou contrat trouvé mais en status INACTIVE, le handler retourne le status KO
			si le contrat trouvé et en status ACTIVE, le handler retourne le status OK
   																 
   																 
L'exécution de l'algorithme est présenté dans le preudo-code ci-dessous:

.. code-block:: text

	Si (il y as pas de déclaration de contrat)
		handler retourne OK
	Autrement
		recherche du contrat dans la base via le client AdminManagementClient
		Si (contrat nou trouvé OU contrat trouvé mais INACTIVE)
			 handler retourne KO
		Autrement 
		    handler retourne OK
		Fin Si
	Fin Si


Détail du handler : CheckNoObjectsActionHandler
-----------------------------------------------

Description
~~~~~~~~~~~

CheckNoObjectsActionHandler permet de vérifier s'il y a des objects numériques dans le SIP à verser dans le système.  

Détail des données utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Le handler prend ce fichier manifest extrait du WORKSPACE comme le parametre d'entrée. 

exécution
~~~~~~~~~

Le fichier manifest sera lu pour vérifier s'il y a des TAG "BinaryDataObject" ou "PhysicalDataObject".
S'il en y a, le handler retourne KO, sinon OK.

Détail du plugin : CheckArchiveUnitSchema
-----------------------------------------

Description
~~~~~~~~~~~

CheckArchiveUnitSchema permet d'exécuter un contrôle intelligent des archive unit en vérifiant la conformité du JSON généré dans le process pour chaque archive unit, par rapport à un schéma défini. 

.. only:: html

    .. literalinclude:: includes/archive-unit-schema.json
       :language: json
       :linenos:


.. only:: latex

    Le schéma est disponible dans les sources de VITAM (fichier ``archive-unit-schema.json``)


Détail des données utilisées
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Le plugin récupère l'id de l'Archive Unit à vérifier. 

exécution
~~~~~~~~~

A partir de l'Id de l'id de l'Archive Unit à vérifier, le plugin va télécharger le fichier json associé dans le Workspace.
Par la suite, il va vérifier la validation de ce Json par rapport au schéma json de Vitam.

détail des vérifications
~~~~~~~~~~~~~~~~~~~~~~~~

Dans le schéma Json Vitam défini, voici les spécificités qui ont été ajoutées pour différents champs :

- StartDate pour les Rules : une date contenant une année égale à ou au dessus de l'année 9000 sera refusée.
- Content / Title : peut être de type String, Array ou number (on pourra avoir des titres traduits ainsi que des nombres si besoin) 


Détail du handler : CheckArchiveProfileActionHandler
----------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de vérifier le profil dans manifeste

exécution
~~~~~~~~~

Le format du profil est XSD ou RNG.
L'exécution de l'algorithme est présenté dans le preudo-code ci-dessous:

.. code-block:: text

	Si le format du profil est équal à XSD
		retourne true si XSD valide le fichier manifest.xml
	Fin Si
	Si le format du profil est équal à RNG
		retourne true si RNG valide le fichier manifest.xml
	Fin Si


Détail du handler : CheckArchiveProfileRelationActionHandler
------------------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de vérifier la relation entre le contrat d'entrée et le profil dans manifeste

exécution
~~~~~~~~~

Si le champ "ArchiveProfiles" dans le contrat d'entrée 
contient l'identifiant du profil, retourne true

.. code-block:: java

	Select select = new Select();
    select.setQuery(QueryHelper.eq(IngestContract.NAME, contractName));
    JsonNode queryDsl = select.getFinalSelect();
    RequestResponse<IngestContractModel> referenceContracts = adminClient.findIngestContracts(queryDsl);
    if (referenceContracts.isOk()) {
    	IngestContractModel contract = ((RequestResponseOK<IngestContractModel> ) referenceContracts).getResults().get(0);
        isValid = contract.getArchiveProfiles().contains(profileIdentifier);
    }


Détail du handler : ListArchiveUnitsActionHandler
-------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de lister les unités archivistiques qui devront être mises à jour.

exécution
~~~~~~~~~

Il prend en entrée un fichier json représentant la liste règles de gestion ayant été modifiés dans le référentiel.
Pour chaque règle mise à jour, une requête vers la collection units est effectuée. 
Le but de cette recherche est de générer une liste d'units avec les règles de gestion associées ayant été modifiées.
En sortie, pour chaque unité archivistique, on aura un fichier GUID_AU.json (dans un sous répertoire GUIDOpération/UnitsWithoutLevel/) contenant un tableau des règles de gestion modifiées.


Détail du handler : ListRunningIngestsActionHandler
---------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de lister les ingests toujours en cours d'exécution (processState RUNNING ou PAUSE).


exécution
~~~~~~~~~

Une requête est effectuée sur ProcessManagement, pour récupérer la liste des ingests en cours.

.. code-block:: java

   ProcessQuery pq = new ProcessQuery();
   List<String> listStates = new ArrayList<>();
   listStates.add(ProcessState.RUNNING.name());
   listStates.add(ProcessState.PAUSE.name());
   pq.setStates(listStates);
   List<String> listProcessTypes = new ArrayList<>();
   listProcessTypes.add(LogbookTypeProcess.INGEST.toString());
   listProcessTypes.add(LogbookTypeProcess.HOLDINGSCHEME.toString());
   listProcessTypes.add(LogbookTypeProcess.FILINGSCHEME.toString());
   pq.setListProcessTypes(listProcessTypes);
   RequestResponseOK<ProcessDetail> response =
                (RequestResponseOK<ProcessDetail>) processManagementClient.listOperationsDetails(pq);

Suite à cette requête, la liste des opérations d'Ingest est enregistrée dans un fichier JSON : PROCESSING/runningIngests.json.

Détail du plugin : ArchiveUnitRulesUpdateActionPlugin
-----------------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de mettre à jour les règles de gestion d'une unité archivistique. Il s'agit ici de mettre à jour le champ endDate pour les règles de gestion impactées.
On se trouve ici en mode distribué, cela veut donc dire que l'on traite les mises à jour, unité par unité.


exécution
~~~~~~~~~

Le fichier json pour l'unité archivistique, généré dans le Handler "ListArchiveUnitsActionHandler" est récupéré.
A partir de ce dernier, on va faire une première requète pour récupérer l'unité archivistique telle qu'enregistrée en base.

Ensuite, catégorie par catégorie, des requêtes de mises à jour vont être créées.
Une requête finale sera aggrégée, comprenant les différentes catégories mises à jour.
Enfin, l'update final de la base de données sera exécuté, tel que ci-dessous : 

.. code-block:: java

   query.addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), params.getProcessId()));
   JsonNode updateResultJson = metaDataClient.updateUnitbyId(query.getFinalUpdate(), archiveUnitId);
   String diffMessage = archiveUnitUpdateUtils.getDiffMessageFor(updateResultJson, archiveUnitId);
   itemStatus.setEvDetailData(diffMessage); 
   
Le différentiel (résumant les champs modifiés, principalement les endDate des règles de gestion) sera enregistré également dans les cycles de vie de l'unité archivistique.

.. code-block:: java

   //do some things
   archiveUnitUpdateUtils.logLifecycle(params, archiveUnitId, StatusCode.OK, diffMessage, logbookLifeCycleClient);  

Détail du plugin : RunningIngestsUpdateActionPlugin
---------------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de mettre à jour les règles de gestion des unités archivistiques des ingests en cours.


exécution
~~~~~~~~~

Le fichier json décrivant les ingests en cours, généré dans le Handler "ListRunningIngestsActionHandler" est récupéré.
Il va permettre, de traiter au fur et à mesure les ingests n'ayant pas été encore impactés par la mise à jour du référentiel des règles de gestion.

La manière de procéder est la suivante :

- Une boucle while(true) va permettre de boucler continuellement sur une liste d'ingest.
- Une boucle interne sur un iterator obtenu à partir de la liste des ingests va permettre de traiter les différents processus.

   - Si l'ingest est finalisé (entre le moment de l'exécution du Handler ListRunningIngestsActionHandler, et l'exécution du plugin) alors on va vérifier la liste des règles de gestion pour chaque unité archivistique, puis procéder à des mises à jour (code commun avec le plugin ArchiveUnitRulesUpdateActionPlugin). L'ingest est alors, au final, supprimé de l'iterator.
   - Si l'ingest est toujours en cours, alors on passe au suivant.

- Tant que l'iterator contient des éléments, la boucle continue. (une pause de 10 secondes est prévue avant de reboucler sur l'iterator)
- Enfin quand l'iterator est vide, le plugin, renverra un statut OK notifiant la gestion de tous les ingests.

A l'heure actuelle, pour éviter un nombre d'essais illimité, une limite d'essais à été positionné (NB_TRY = 600). 
A l'avenir, il conviendra certainement de ne pas avoir cette limite.

Il est aussi prévu d'améliorer les performances de l'exécution de ce plugin. 
Il apparait pertinent de rendre parallélisable le traitement des ingests en cours.       


Détail du handler : ListLifecycleTraceabilityActionHandler
----------------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de préparer les listes de cycles de vie des groupes d'objets, et des unités archivistiques.
Il permet aussi la récupération des informations de la dernière opération de sécurisation des cycles de vie.


exécution
~~~~~~~~~

Une première requête permet de récupérer la dernière opération de sécurisation des cycles de vie.
S'il en existe une, on en tire les informations importantes (date d'exécution, etc.), l'opération sera exportée dans un fichier json. 
S'il n'en existe pas, une date minimale (LocalDateTime.MIN) sera utilisée pour la suite du process.


A partir de cette date obtenue, on va interroger Mongo et récupérer 2 listes de cycles de vie (groupes d'objets et units) qui n'ont pas encore été sécurisés.

.. code-block:: java

   final Query parentQuery = QueryHelper.gte("evDateTime", startDate.toString());
   final Query sonQuery = QueryHelper.gte(LogbookDocument.EVENTS + ".evDateTime", startDate.toString());
   final Select select = new Select();
   select.setQuery(QueryHelper.or().add(parentQuery, sonQuery));
   select.addOrderByAscFilter("evDateTime");

A partir de ces 2 listes, on va créer X (X étant le nombre de GoT ou d'units) fichiers dans les sous répertoires GUID/ObjectGroup et GUID/UnitsWithoutLevel.
Ces fichiers json seront utilisés plus tard dans le workflow, dans le cadre de la distribution.

En traitant les différents cycles de vie, on en conclut les informations suivantes :

- date maximum d'un cycle de vie traité
- nombre de cycles de vie liés aux groupes d'objets traités
- nombre de cycles de vie liés aux units traités

Ces informations, combinées à la startDate obtenue précédemment, sont enregistrées dans un fichier json Operations/traceabilityInformation.json.

En résumé, voici les output de ce handler : 

- GUID/Operations/lastOperation.json -> informations sur la dernière opération de sécurisation des cycles de vie
- GUID/Operations/traceabilityInformation.json -> informations sur la sécurisation en cours
- GUID/ObjectGroup/GUID_OG_n.json -> n fichiers json représentant n cycles de vie des groupes d'objets
- GUID/UnitsWithoutLevel/GUID_AU_n.json -> n fichiers json représentant n cycles de vie des units. 


Détail du plugin : CreateObjectSecureFileActionPlugin
-----------------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de traiter, groupe d'objet par groupe d'objet, et de créer un fichier sécurisé. 
Chaque fichier sécurisé créé, sera par la suite, dans l'étape de finalisation, traité et intégré dans un fichier global. 


exécution
~~~~~~~~~

La première étape de ce plugin, consiste à récupérer le fichier json GUID/ObjectGroup/GUID_OG_n.json.
A partir de ce json, représentant le cycle de vie devant être traité, on va créer un fichier sécurisé.
Ce fichier sécurisé contient une ligne unique, organisée de la façon suivante : 

[ID de l'opération provoquant la création du cycle de vie] | [Type du process (INGEST / UPDATE)] | [Date de l'évenement] | [ID du cycle de vie]
 | [Statut final du cycle de vie] | [Hash global du cycle de vie] | [Hash du groupe d'objet associé] | [Liste des versions de l'objet]

Ce fichier généré est ensuite sauvegardé sur le workspace dans : LFCObjects.

Voici l'output de ce plugin :
- GUID/LFCObjects/GUID_OG.json


Détail du plugin : CreateUnitSecureFileActionPlugin
---------------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de traiter, cycle de vie unit par cycle de vie unit, et de créer un fichier sécurisé. 
Chaque fichier sécurisé créé, sera par la suite, dans l'étape de finalisation, traité et intégré dans un fichier global. 


exécution
~~~~~~~~~

La première étape de ce plugin, consiste à récupérer le fichier json GUID/UnitsWithoutLevel/GUID_AU_n.json.
A partir de ce json, représentant le cycle de vie devant être traité, on va créer un fichier sécurisé.
Ce fichier sécurisé contient une ligne unique, organisée de la façon suivante : 

[ID de l'opération provoquant la création du cycle de vie] | [Type du process (INGEST / UPDATE)] | [Date de l'évenement] | [ID du cycle de vie]
 | [Statut final du cycle de vie] | [Hash global du cycle de vie] | [Hash de l'archive unit associé] |

Ce fichier généré est ensuite sauvegardé sur le workspace dans : LFCObjects.

Voici l'output de ce plugin :

- GUID/LFCUnits/GUID_AU.json

Détail du plugin : CheckClassificationLevelActionPlugin
-------------------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de vérifier que le niveau de classification déclaré par les ArchiveUnit du manifeste est conforme à ceux attendus dans la configuration de la plate-forme

exécution
~~~~~~~~~

A partir de l'Id de l'id de l'Archive Unit à vérifier, le plugin va télécharger le fichier json associé dans le Workspace.
Par la suite, il va vérifier le champ ClassificationLevel par rapport au celui dans ClassificationLevelService


Détail du handler : FinalizeLifecycleTraceabilityActionHandler
--------------------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de finaliser la sécurisation des cycles de vie, en générant un fichier zip, et en le sauvegardant sur les offres de stockage.

exécution
~~~~~~~~~

Le Handler va tout d'abord récupérer les fichiers json qui ont été générés dans l'étape 1 : 

- le fichier json de la dernière opération de sécurisation
- le fichier json contenant les informations de la sécurisation en cours

Ensuite, un objet TraceabilityFile va être généré. Cet objet représente un ZipArchiveOutputStream contenant 4 fichiers : 

- global_lifecycles.txt : contenant l'aggrégation des informations des cycles de vie sécurisés.
- additional_information.txt : contenant des informations génériques (nombre de cycles de vie traités, startDate + endDate)
- computing_information.txt : contenant les informations de hachage (hash actuel, hash de la dernière opération de sécurisation, hash d'il y a un mois, et d'il y a un an)
- token.tsp : tampon d'horodatage du fichier de sécurisation

Les informations nécessaires sont récupérées pour générer et remplir les 4 différents fichiers : 

**global_lifecycles.txt :**
Ce fichier va être obtenu de la manière suivante : 

- On récupère la liste des fichiers présents dans les 2 sous-répertoires (GUID/LFCUnits/ et GUID/LFCObjects/).
- Pour chaque fichier récupéré, on récupère son contenu et on ajoute une ligne au fichier global_lifecycles.txt
- Le premier élément traité sera utilisé pour en conclure un hash, qui sera identifié étant comme le hashRoot du fichier.

**additional_information.txt :**
Le fichier json Operations/traceabilityInformation.json va être utilisé pour construire le fichier de la manière suivante : 

- numberOfElements : nombre de cycles de vie traités
- startDate : startDate (soit égale à LocalDateTime.MIN, soit à la plus petite date des cycles de vie traités)
- endDate : plus grande date des cycles de vie traités.
- securisationVersion : version du format du fichier de traçabilité

**computing_information.txt :**
Ce fichier va être rempli de la manière suivante :
- currentHash : le hash du cycle de vie traité en premier
- previousTimestampToken : le tampon d'horodatage de la dernière opération de sécurisation (sera obtenu en analysant le fichier json Operations/lastOperation.json) - peut être vide.
- previousTimestampTokenMinusOneMonth : le tampon d'horodatage de la dernière opération de sécurisation datant d'un mois. Une recherche dans la base LogbookOperations est effectuée.
- previousTimestampTokenMinusOneYear : le tampon d'horodatage de la dernière opération de sécurisation datant d'un an. Une recherche dans la base LogbookOperations est effectuée.

**token.tsp :**
Le fichier token.tsp, contiendra simplement le tampon d'horodatage de l'opération de sécurisation en cours.
Le tampon d'horodatage est obtenu en utilisant le timestampGenerator de Vitam. Cela nécéssite d'avoir un certificat présent dans la configuration du worker (configuration via verify-timestamp.conf spécifiant le p12 + le password).
Les différents hash nécessaires sont : 
- rootHash : hash du premier cycle de vie traité dans l'opération en cours
- hash1 : hash de la dernière opération de sécurisation
- hash2 : hash de la dernière opération de sécurisation datant d'un mois
- hash3 : hash de la dernière opération de sécurisation datant d'un an
(hash1, hash2 et hash3 peuvent être null, si aucune opération n'a été effectué dans le passé)

.. code-block:: java

   final String hash = joiner.join(rootHash, hash1, hash2, hash3);
   final DigestType digestType = VitamConfiguration.getDefaultTimestampDigestType();
   final Digest digest = new Digest(digestType);
   digest.update(hash);
   final byte[] hashDigest = digest.digest();
   final byte[] timeStampToken = timestampGenerator.generateToken(hashDigest, digestType, null);  


Le fichier zip est finalement créé et sauvegardé sur le Workspace. Ensuite, il sera sauvegardé sur les offres de stockage.

Bien évidemment l'opération est enregistré dans le logbook. Les informations de Traceability sont enregistrés dans le champ evDetData. 
Elles seront utilisés par la suite, pour les sécurisations futures. 

Détail du handler : GenerateAuditReportActionHandler
----------------------------------------------------

Description
~~~~~~~~~~~

Ce handler permet de générer le rapport d'audit

exécution
~~~~~~~~~

La rapport commence par une partie généraliste contenant :
* Le GUID de l'opération d'audit à l'origine de ce rapport
* Le tenant sur lequel s'est exécuté l'audit
* Le message (outMessg) du JDO de l'opération de la dernière étape (succès ou échec de l'audit)
* Le statut final (outcome) de l'opération
* La date et l'heure du début de la génération du rapport (evDateTime de l'evénement)
* L'identifiant de ce sur quoi porte l'audit (tenant/SP/opération) 

Deuxièmement, la rapport contient les cas OK, KO, Warning et Fatal de toutes les actions d'audit sur les objets

.. code-block:: java

	//le cas OK
	source.add(JsonHandler.createObjectNode().put(_TENANT, res.get(_TENANT).asText())
    	.put(ORIGINATING_AGENCY, agIdExtNode.get("originatingAgency").asText())
        .put(EV_ID_PROC, res.get(EV_ID_PROC).asText()));

	//le cas KO
	reportKO.add(JsonHandler.createObjectNode().put("IdOp", event.get(EV_ID_PROC).asText())
    	.put(ID_GOT, event.get("obId").asText())
        .put(ID_OBJ, error.get(ID_OBJ).asText())
        .put(USAGE, error.get(USAGE).asText())
        .put(ORIGINATING_AGENCY, originatingAgency)
        .put(OUT_DETAIL, event.get("outDetail").asText()));
        


Détail du plugin : AuditCheckObjectPlugin
-----------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de contrôler les objets dans le cadre d'un audit consultatif

exécution
~~~~~~~~~

Selon le parametre auditActions, il va appeler le plugin,
soit CheckExistenceObjectPlugin, soit CheckIntegrityObjectPlugin



Détail du plugin : CheckExistenceObjectPlugin
---------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de contrôler l'existence d'un objet dans le cadre d'un audit

exécution
~~~~~~~~~

Le plugin va tester l'existence de la cohérence entre les offres de stockages déclarées dans un GOT 
et les offres de stockages relatives à la stratégie de stockage connue du moteur de stockage

.. code-block:: java

	JsonNode storageInformation = version.get("_storage");
    final String strategy = storageInformation.get("strategyId").textValue();
    final List<String> offerIds = new ArrayList<>();
    for (JsonNode offerId : storageInformation.get("offerIds")) {
    	offerIds.add(offerId.textValue());
    }

    if (!storageClient.exists(strategy, StorageCollectionType.OBJECTS,
    	version.get("_id").asText(), offerIds)) {
        nbObjectKO += 1;
    } else {
    	nbObjectOK += 1;
    }



Détail du plugin : CheckIntegrityObjectPlugin
---------------------------------------------

Description
~~~~~~~~~~~

Ce plugin permet de contrôler l'intégrité d'un objet archivé dans le cadre d'un audit

exécution
~~~~~~~~~

Dans le cadre de l'audit, on va vérifier une empreinte d'un objet est bien celle de l'objet audité, 
en fonction de son offre de stockage.

.. code-block:: java

	JsonNode offerToMetadata = storageClient.getObjectInformation(strategy, version.get("_id").asText(), offerIds);
    for (String offerId : offerIds) {
    	String digest = null;
        JsonNode metadata = offerToMetadata.findValue(offerId);
        if (metadata != null){
        	digest = metadata.get("digest").asText();
        } else {
        	checkDigest = false;
            continue;
        }
                        
        if (messageDigest.equals(digest)) {
        	checkDigest = true;
        } else {
        	checkDigest = false;
        }
	}


Worker-common
=============

Le worker-common contient majoritairement des classes utilitaires.
A terme, il faudra que SedaUtils notamment soit "retravaillé" pour que les différentes méthodes soit déplacées dans les bons Handlers.

Worker-client
=============

Le worker client contient le code permettant l'appel vers les API Rest offert par le worker.
Pour le moment une seule méthode est offerte : submitStep. Pour plus de détail, voir la partie worker-client.
