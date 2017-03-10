Worker
######

1. Présentation
***************

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.worker**

4 modules composent la partie worker :
- worker-common : incluant la partie common (Utilitaires...), notamment le SedaUtils.
- worker-core : contenant les différents handlers.
- worker-client : incluant le client permettant d'appeler le REST.
- worker-server : incluant la partie REST.

2. Worker-server
****************

2.1 Rest API
------------

Pour l'instant les uri suivantes sont déclarées :

| http://server/worker/v1
| POST /tasks -> **POST Permet de lancer une étape à exécuter**

2.2 Registration
----------------
Une partie registration permet de gérer la registration du Worker.

La gestion de l'abonnement du *worker* auprès du serveur *processing* se fait à l'aide d'un ServletContextListener : *fr.gouv.vitam.worker.server.registration.WorkerRegistrationListener*.

Le WorkerRegistrationListener va lancer l'enregistrement du *worker* au démarrage du serveur worker, dans un autre Thread utilisant l'instance *Runnable* : *fr.gouv.vitam.worker.server.registration.WorkerRegister*.

L'execution du *WorkerRegister* essaie d'enregistrer le *worker* suivant un retry paramétrable dans la configuration du serveur avec :
- un délai (registerDelay en secondes)
- un nombre d'essai (registerTry)

Le lancement du serveur est indépendant de l'enregistrement du *worker* auprès du *processing* : le serveur *worker* ne s'arrêtera pas si l'enregistrement n'a pas réussi.

2.3. Configuration de worker

Cela présente la configuration pour un worker quand il est déployé. Deux paramètres importants quand le worker fonctionne en mode parallèle.   

 * WorkerCapacity :
	Cela présente la capacité d'un worker qui réponds au demande de parallélisation de la distribution de tâches du workflow.  
	Il est précisé par le paramètre capacity dans le WorkerConfiguration.    
 
 * WorkerFamily :
 Chaque worker est configuré pour traiter groupe de tâches corresponsant à ses fonctions et on cela permetre de définir les familles de worker. 
 Il est précisé par workerFamily dans le WorkerConfigration.  

2.4. WorkerBean
présente l'information complète sur un worker pour la procédure d'enregistrement d'un worker. Il contient les information sur le nom, 
la famille et la capacité ... d'un worker et présente en mode json. Voici un example :  

.. code-block:: json
    
{ "name" : "workername", "family" : "DefaultWorker", "capacity" : 10, "storage" : 100,
 "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 12345 } }
 
 
2.5. Persistence des workers
----------------------------
 
 La lise de workers est persistée dans une base de données. Pour le moment, la base est un fichier de données qui contient une tableau de 
 workers en format ArrayNode et chaque worker est une élément JsonNode. Exemple ci-dessous est des données d'une liste de workers 

.. code-block:: json

[
  {"workerId": "workerId1", "workerinfo": { "name" : "workername", "family" : "DefaultWorker", "capacity" : 10, "storage" : 100,
 "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 12345 }}}, 
     
 {"workerId": "workerId2", "workerinfo": { "name" : "workername2", "family" : "BigWorker", "capacity" : 10, "storage" : 100,
 "status" : "Active", "configuration" : {"serverHost" : "localhost", "serverPort" : 54321 } }} 
] 

Le fichier nommé "worker.db" qui sera créé dans le /vitam/data/processing   
 
Chaque worker est identifié par workerId et l'information générale du champs workerInfo. L'ensemble des actions suivantes sont traitées : 
  
* Lors du redémarrage du distributor, il recharge la liste des workers enregistrés. Ensuite, il vérifie le status de chaque worker de la liste, 
(serverPort:serverHost) en utilisant le WorkerClient. Si le worker qui n'est pas disponible, il sera supprimé de la liste des workers enregistrés 
et la base sera mise à jour. 

* Lors de l'enregistrement/désenregistrement, la liste des workers enregistrés sera mis à jour (ajout/supression d'un worker).        

.. code-block:: java

	checkStatusWorker(String serverHost, int serverPort) // vérifier le statut d'un worker	
	marshallToDB()   // mise à jour la base de la liste des workers enregistrés
	
	
2.6. Désenregistrement d'un worker
----------------------------------

Lorsque le worker s'arrête ou se plante, ce worker doit être désenregistré. 

* Si le worker s'arrête, la demande de désenregistrement sera lancé pour le contexte "contextDestroyed" de la WorkerRegistrationListener  
(implémenté de ServletContextListener) en utilisant le ProcessingManagementClient pour appeler le service de desenregistrement de distributeur.   

* Si le worker se plante, il ne réponse plus aux requêtes de WorkerClient dans la "run()" WorkerThread et dans le catch() des exceptions de de traitement, 
une demande de désenregistrement doit être appelé dans cette boucle.

 - le distributeur essaie de faire une vérification de status de workers en appelant checkStatusWorker() en plusieurs fois 
 (définit dans GlobalDataRest.STATUS_CHECK_RETRY). 
 - si après l'étape 1 le statut de worker est toujours indisponible, le distributeur va appeler la procédure de désenregistrement de ce worker de la liste 
 de worker enregistrés. 
    
    
                
3. Worker-core
**************
Dans la partie Core, sont présents les différents Handlers nécessaires pour exécuter les différentes actions.
- CheckConformityActionHandler
- CheckObjectsNumberActionHandler
- CheckObjectUnitConsistencyActionHandler
- CheckSedaActionHandler
- CheckStorageAvailabilityActionHandler
- CheckVersionActionHandler
- ExtractSedaActionHandler
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



La classe WorkerImpl permet de lancer ces différents handlers.

3.1 Focus sur la gestion des entrées / sorties  des Handlers
------------------------------------------------------------

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

3.2 Cas particulier des Tests unitaires
---------------------------------------

Afin d'avoir un handlerIO correctement initialisé, il faut redéfinir le handlerIO manuellement comme l'attend le handler :

.. code-block:: java
   // In a common part (@Before for instance)
   HandlerIO handlerIO = new HandlerIO("containerName", "workerid");
   List<IOParameter> out = new ArrayList<>();
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "UnitsLevel/ingestLevelStack.json")));
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/BDO_TO_OBJECT_GROUP_ID_MAP.json")));
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/BINARY_DATA_OBJECT_ID_TO_GUID_MAP.json")));
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/OG_TO_ARCHIVE_ID_MAP.json")));
   out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "Maps/BDO_TO_VERSION_BDO_MAP.json")));
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
..

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
      handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP));
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
..

3.3 Création d'un nouveau handler
---------------------------------
La création d'un nouveaux handler doit être motivée par certaines conditions nécessaires :
    - lorsque qu'il n'y a pas de handler qui répond au besoin
    - lorsque rajouter la fonctionnalité dans un handler existant, le surcharge et le détourne de sa fonctionalité première
    - lorsque l'on veut refactorer un handler existant pour donner des fonctionalités 'un peu' plus 'élémentaires'

Les handlers doivent étendrent la classe ActionHandler et implémenter la méthode execute.
Lors de la création d'un nouveau handler, il faut ajouter une nouvelle instance, dans WorkerImpl.init pour enregistrer le
handler dans le worker et définir le handler id.
Celui ci sert de clé pour :
    - les messages dans logbook (vitam-logbook-messages_fr.properties) en fonction de la criticité
    - les fichiers json de définition des workflows json (exemple : DefaultIngestWorkflow.json)

cf. workflow.rst


4. Details des Handlers
***********************

4.1 Détail du handler : CheckConformityActionHandler
----------------------------------------------------

4.1.1 description
=================

Ce handler permet de contrôle de l'empreinte. Il comprend désormais 2 tâches :

-- Vérification de l'empreinte par rapport à l'empreinte indiquée dans le manifeste (en utilisant algorithme déclaré dans manifeste)
-- Calcul d'une empreinte en SHA-512 si l'empreinte du manifeste est calculée avec un algorithme différent

4.1.2 exécution
===============
CheckConformityActionHandler recupère l'algorithme de Vitam (SHA-512) par l'input dans workflow
et le fichier en InputStream par le workspace.

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

4.1.3 journalisation :
======================
logbook lifecycle
=================
CA 1 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste)

Dans le processus d'entrée, l'étape de vérification de la conformité de l'empreinte doit être appelée en position 450.
Lorsque l'étape débute, pour chaque objet du groupe d'objet technique, une vérification d'empreinte doit être effectuée (celle de l'objet avec celle inscrite dans le manifeste SEDA). Cette étape est déjà existante actuellement.
Le calcul d'empreinte en SHA-512 (CA 2) ne doit pas s'effectuer si l'empreinte renseigné dans le manifeste a été calculé en SHA-512. C'est cette empreinte qui sera indexée dans les bases Vitam.

CA 1.1 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste) - Started
- Lorsque l'action débute, elle inscrit une ligne dans les journaux du cycle de vie des GOT :
* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "Started"
* outcomeDetailMessage FR : « Début de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>"
* objectIdentifierIncome : MessageIdentifier du manifest

CA 1.2 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste) - OK
- Lorsque l'action est OK, elle inscrit une ligne dans les journaux du cycle de vie des GOT :
* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "OK"
* outcomeDetailMessage FR : « Succès de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>"
* objectIdentifierIncome : MessageIdentifier du manifest
Comportement du workflow décrit dans l'US #680

- La collection ObjectGroup est aussi mis à jour, en particulier le champs : Message Digest : {  empreinte, algorithme utlisé }

CA 1.3 : Vérification de la conformité de l'empreinte. (empreinte en SHA-512 dans le manifeste) - KO
- Lorsque l'action est KO, elle inscrit une ligne dans les journaux du cycle de vie des GOT :
* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "KO"
* outcomeDetailMessage FR : « Échec de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>
Empreinte calculée : <Empreinte calculée par Vitam>"
* objectIdentifierIncome : MessageIdentifier du manifest
Comportement du workflow décrit dans l'US #680

****************************
CA 2 : Vérification de la conformité de l'empreinte. (empreinte différent de SHA-512 dans le manifeste)

Si l'empreinte proposé dans le manifeste SEDA n'est pas en SHA-512, alors le système doit calculer l'empreinte en SHA-512. C'est cette empreinte qui sera indexée dans les bases Vitam.
Lorsque l'action débute, pour chaque objet du groupe d'objet technique, un calcul d'empreinte au format SHA-512 doit être effectué. Cette action intervient juste apres le check de l'empreinte dans le manifeste (mais on est toujours dans l'étape du check conformité de l'empreinte).

CA 2.1 : Vérification de la conformité de l'empreinte. (empreinte différent de SHA-512 dans le manifeste) - Started
- Lorsque l'action débute, elle inscrit une ligne dans les journaux du cycle de vie des GOT :
* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "Started"
* outcomeDetailMessage FR : « Début de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>"
* objectIdentifierIncome : MessageIdentifier du manifest

CA 2.2 : Vérification de la conformité de l'empreinte. (empreinte différent de SHA-512 dans le manifeste) - OK
- Lorsque l'action est OK, elle inscrit une ligne dans les journaux du cycle de vie des GOT :
* eventType EN – FR : « Digest Check», « Vérification de l'empreinte des objets»
* outcome : "OK"
* outcomeDetailMessage FR : « Succès de la vérification de l'empreinte »
* eventDetailData FR : "Empreinte Manifeste : <MessageDigest>, algorithme : <MessageDigest attribut algorithm>"
"Empreinte calculée (<algorithme utilisé "XXX">): <Empreinte calculée par Vitam>"
* objectIdentifierIncome : MessageIdentifier du manifest

4.1.5 modules utilisés
======================
processing, worker, workspace et logbook

4.1.4 cas d'erreur
==================
XMLStreamException                          : problème de lecture SEDA
InvalidParseOperationException              : problème de parsing du SEDA
LogbookClientAlreadyExistsException         : un logbook client existe dans ce workflow
LogbookClientBadRequestException            : LogbookLifeCycleObjectGroupParameters est mal paramétré et le logbook client génère une mauvaise requete
LogbookClientException                      : Erreur générique de logbook. LogbookException classe mère des autres exceptions LogbookClient
LogbookClientNotFoundException              : un logbook client n'existe pas pour ce workflow
LogbookClientServerException                : logbook server a un internal error
ProcessingException                         : erreur générique du processing
ContentAddressableStorageException          : erreur de stockage


4.2 Détail du handler : CheckObjectsNumberActionHandler
-------------------------------------------------------

4.2.1 description
=================
Ce handler permet de comparer le nombre d'objet stocké sur le workspace et le nombre d'objets déclaré dans le manifest.

4.3 Détail du handler : CheckObjectUnitConsistencyActionHandler
---------------------------------------------------------------


Ce handler permet de contrôler la cohérence entre l'object/object group et l'ArchiveUnit.

Pour ce but, on détecte les groupes d'object qui ne sont pas référé par au moins d'un ArchiveUnit.
Ce tache prend deux maps de données qui ont été crée dans l'étape précédente de workflow comme input :
objectGroupIdToUnitId
objectGroupIdToGuid
Le ouput de cette contrôle est une liste de groupe d'objects invalide. Si on trouve les groupe d'objects
invalide, le logbook lifecycles de group d'object sera mis à jour.

L'exécution de l'algorithme est présenté dans le code suivant :*
.. code-block:: java ..................................................................
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

........................................................................................


4.4 Détail du handler : CheckSedaActionHandler
----------------------------------------------

Ce handler permet de valider la validité du manifest par rapport à un schéma XSD. 
Il permet aussi de vérifier que les informations remplies dans ce manifest sont correctes.

 - Le schéma de validation du manifest : src/main/resources/seda-vitam-2.0-main.xsd.

4.4 Détail du handler : CheckStorageAvailabilityActionHandler
-------------------------------------------------------------

TODO

4.5 Détail du handler : CheckVersionActionHandler
-------------------------------------------------

TODO

4.6 Détail du handler : ExtractSedaActionHandler
------------------------------------------------

4.6.1 description
=================
Ce handler permet d'extraire le contenu du SEDA. Il y a :
- extraction des BinaryDataObject
- extraction des ArchiveUnit
- création des lifes cycles des units
- construction de l'arbre des units et sauvegarde sur le workspace
- sauvegarde de la map des units sur le workspace
- sauvegarde de la map des objets sur le workspace
- sauvegarde de la map des objets groupes sur le workspace


4.6.2 Détail des différentes maps utilisées :
=============================================
Map<String, String> binaryDataObjectIdToGuid
    contenu         : cette map contient l'id du BDO relié à son guid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors de la lecture des BinaryDataObject
    lecture, get    : saveObjectGroupsToWorkspace, getObjectGroupQualifiers,
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> binaryDataObjectIdToObjectGroupId :
    contenu         : cette map contient l'id du BDO relié au groupe d'objet de la balise DataObjectGroupId ou DataObjectGroupReferenceId
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors de la lecture des BinaryDataObject
    lecture, get    : lecture de la map dans mapNewTechnicalDataObjectGroupToBDO, getNewGdoIdFromGdoByUnit, completeBinaryObjectToObjectGroupMap, checkArchiveUnitIdReference et writeBinaryDataObjectInLocal
    suppression     : c'est un clean en fin d'execution du handler

Map<String, GotObj> binaryDataObjectIdWithoutObjectGroupId :
    contenu         : cette map contient l'id du BDO relié à un groupe d'objet technique instanciés lors du parcours des objets binaires.
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des BDO dans mapNewTechnicalDataObjectGroupToBDO et extractArchiveUnitToLocalFile. Dans extractArchiveUnitToLocalFile, quand on découvre un DataObjectReferenceId et que cet Id se trouve dans binaryDataObjectIdWithoutObjectGroupId alors on récupère l'objet et on change le statut isVisited à true.
    lecture, get    : lecture de la map dans mapNewTechnicalDataObjectGroupToBDO, extractArchiveUnitToLocalFile, getNewGdoIdFromGdoByUnit,
    suppression     : c'est un clean en fin d'execution du handler

Le groupe d'objet technique GotObj contient un guid et un boolean isVisited, initialisé à false lors de la création. Le set à true est fait lors du parcours des units.

Map<String, String> objectGroupIdToGuid
    contenu         : cette map contient l'id du groupe d'objet relié à son guid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des BDO dans writeBinaryDataObjectInLocal et mapNewTechnicalDataObjectGroupToBDO lors de la création du groupe d'objet technique
    lecture, get    : lecture de la map dans checkArchiveUnitIdReference, writeBinaryDataObjectInLocal, extractArchiveUnitToLocalFile, saveObjectGroupsToWorkspace
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> objectGroupIdToGuidTmp
    contenu         : c'est la même map que objectGroupIdToGuid
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée dans writeBinaryDataObjectInLocal
    lecture, get    : lecture de la map dans writeBinaryDataObjectInLocal
    suppression     : c'est un clean en fin d'execution du handler

Map<String, List<String>> objectGroupIdToBinaryDataObjectId
    contenu         : cette map contient l'id du groupe d'objet relié à son ou ses BDO
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des BDO dans writeBinaryDataObjectInLocal quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId et qu'il n'existe pas dans objectGroupIdToBinaryDataObjectId.
    lecture, get    : lecture de la map dans le parcours des BDO dans writeBinaryDataObjectInLocal.  La lecture est faite pour ajouter des BDO dans la liste.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, List<String>> objectGroupIdToUnitId
    contenu         : cette map contient l'id du groupe d'objet relié à ses AU
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populée lors du parcours des units dans extractArchiveUnitToLocalFile quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId et qu'il nexiste pas dans objectGroupIdToUnitId sinon on ajoute dans la liste des units de la liste
    lecture, get    : lecture de la map dans le parcours des units. La lecture est faite pour ajouter des units dans la liste.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, BinaryObjectInfo> objectGuidToBinaryObject
    contenu         : cette map contient le guid du binary data object et BinaryObjectInfo
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est populer lors de l'extraction des infos du binary data object vers le workspace
    lecture, get    : elle permet de récupérer les infos binary data object pour sauver l'object group sur le worskapce et
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
    MAJ, put        : elle est populée lors du parcours des BDO dans writeBinaryDataObjectInLocal quand il y a une balise DataObjectGroupId ou DataObjectGroupReferenceId
    lecture, get    : lecture de la map se fait lors de l'extraction des unit dans extractArchiveUnitToLocalFile et permettant de lire dans objectGroupIdToGuid.
    suppression     : c'est un clean en fin d'execution du handler

Map<String, String> objectGuidToUri
    contenu         : cette map contient le guid du BDO relié à son uri définis dans le manifest
    création        : elle est créé lors de la création du handler
    MAJ, put        : elle est poppulée lors du parcours des BDO dans writeBinaryDataObjectInLocal quand il rencontre la balise uri
    lecture, get    : lecture de la map se fait lors du save des objects groups dans le workspace
    suppression     : c'est un clean en fin d'execution du handler

sauvegarde des maps (binaryDataObjectIdToObjectGroupId, objectGroupIdToGuid) dans le workspace

4.7 Détail du handler : IndexObjectGroupActionHandler
-----------------------------------------------------

4.7.1 description
=================

Indexation des objets groupes en récupérant les objets groupes du workspace. Il y a utilisation d'un client metadata.
TODO

4.8 Détail du handler : IndexUnitActionHandler
----------------------------------------------

4.8.1 description
=================

Indexation des units en récupérant les units du workspace. Il y a utilisation d'un client metadata.
TODO

4.9 Détail du handler : StoreObjectGroupActionHandler
-----------------------------------------------------

4.9.1 description
=================
Persistence des objets dans l'offre de stockage depuis le workspace.

TODO

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


4.11 Détail du handler : TransferNotificationActionHandler
----------------------------------------------------------

4.11.1 Description
==================

Ce handler permet de finaliser le processus d'entrée d'un SIP. Cet Handler est un peu spécifique car il sera lancé même si une étape précédente tombe en erreur.
Il permet de générer un xml de notification qui sera :
 - une notification KO si une étape du workflow est tombée en erreur.
 - une notification OK si le process est OK, et que le SIP a bien été intégré sans erreur.

La première étape dans ce handler est de déterminer l'état du Workflow : OK ou KO.

4.11.2 Détail des différentes maps utilisées :
==============================================
Map<String, Object> archiveUnitSystemGuid
    contenu         : cette map contient la liste des archives units avec son identifiant tel que déclaré dans le manifest, associé à son GUID.

Map<String, Object> binaryDataObjectSystemGuid
    contenu         : cette map contient la liste Data Objects avec leur GUID généré associé à l'identifiant déclaré dans le manifest.

Map<String, Object> bdoObjectGroupSystemGuid
    contenu         : cette map contient la liste groupes d'objets avec leur GUID généré associé à l'identifiant déclaré dans le manifest.

4.11.3 exécution
================
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
 - un fichier mapsBDO.file : présentant la liste des binary data objects
 - un fichier mapsBDOtoOG=.file : mappant le binary data object à son object group

A noter que ces fichiers ne sont pas obligatoires pour le bon déroulement du handler.

Le handler va alors procéder à la génération d'un XML à partir des informationss aggrégées.
Voici sa structure générale :
 - MessageIdentifier est rempli avec le MessageIdentifier présent dans le fichier globalSedaParameters. Il est vide si le fichier n'existe pas.
 - dans la balise ReplyOutcome :
   - dans Operation, on aura une liste d'events remplis par les différentes opérations KO et ou FATAL. La liste sera forcément remplie avec au moins un event. Cette liste est obtenue par l'interrogation de la collection LogbookOperations.
   - dans ArchiveUnitList, on aura une liste d'events en erreur. Cette liste est obtenue par l'interrogation de la collection LogbookLifecycleUnits.
   - dans DataObjectList, on aura une liste d'events en erreur. Cette liste est obtenue par l'interrogation de la collection LogbookLifecycleObjectGroups.

Le XML est alors enregistré sur le Workspace.

4.11.4 journalisation : logbook operation? logbook life cycle?
==============================================================
Dans le traitement du Handler, le logbook est interrogé : opérations et cycles de vie.
Cependant aucune mise à jour est effectuée lors de l'exécution de ce handler.


4.11.5 modules utilisés
=======================

Le Handler utilise les modules suivants :

 - Workspace (récupération / copie de fichiers)
 - Logbook (partie server) : pour le moment la partie server du logbook est utilisée pour récupérer les différents journaux (opérations et cycles de vie).
 - Storage : permettant de stocker l'ATR.

4.11.6 cas d'erreur
===================

Les différentes exceptions pouvant être rencontrées :

 - Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
 - Content*Exception : si un problème est rencontré lors de l'interrogation du workspace
 - XML*Exception : si un souci est rencontré sur la génération du XML
 - ProcessingException : si un problème plus général est rencontré dans le Handler


4.12 Détail du handler : AccessionRegisterActionHandler
-------------------------------------------------------

4.12.1 Description
==================

AccessionRegisterActionHandler permet de fournir une vue globale et dynamique des archives

sous la responsabilité du service d'archives, pour chaque tenant.

4.12.2 Détail des maps utilisées
================================
Map<String, String> objectGroupIdToGuid
    contenu         : cette map contient l'id du groupe d'objet relié à son guid

Map<String, String> archiveUnitIdToGuid
	contenu         : cette map contient l'id du groupe d'objet relié à son guid

Map<String, Object> bdoToBdoInfo
	contenu         : cette map contient l'id du binary data object relié à son information


4.12.3 exécution
================
L'alimentation du registre des fonds a lieu pendant la phase de finalisation de l'entrée,

une fois que les objets et les units sont rangés. ("stepName": "STP_INGEST_FINALISATION")

Le Registre des Fonds est alimenté de la manière suivante:
	-- un identifiant unique
	-- des informations sur le service producteur (OriginatingAgency)
	-- des informations sur le service versant (SubmissionAgency), si différent du service producteur
	-- date de début de l’enregistrement (Start Date)
	-- date de fin de l’enregistrement (End Date)
	-- date de dernière mise à jour de l’enregistrement (Last update)
	-- nombre d’units (Total Units)
	-- nombre de GOT (Total ObjectGroups)
	-- nombre d'Objets (Total Objects)
	-- volumétrie des objets (Object Size)
	-- id opération d’entrée associée [pour l'instant, ne comprend que l'evIdProc de l'opération d'entrée concerné]
	-- status (ItemStatus)

5. Worker-common
****************

Le worker-common contient majoritairement des classes utilitaires.
A terme, il faudra que SedaUtils notamment soit "retravaillé" pour que les différentes méthodes soit déplacées dans les bons Handlers.

6. Worker-client
****************

Le worker client contient le code permettant l'appel vers les API Rest offert par le worker.
Pour le moment une seule méthode est offerte : submitStep. Pour plus de détail, voir la partie worker-client.
