Worker
######

1. Présentation
^^^^^^^^^^^^^^^

|  *Parent package:* **fr.gouv.vitam**
|  *Package proposition:* **fr.gouv.vitam.worker**

4 modules composent la partie worker :
- worker-common : incluant la partie common (Utilitaires...), notamment le SedaUtils.
- worker-core : contenant les différents handlers.
- worker-client : incluant le client permettant d'appeler le REST.
- worker-server : incluant la partie REST.

Worker-server
-------------

2. Rest API
^^^^^^^^^^^

Pour l'instant les uri suivantes sont déclarées :

| http://server/worker/v1
| POST /tasks -> **POST Permet de lancer une étape à exécuter**

3. Registration
^^^^^^^^^^^^^^^
Une partie registration permet de gérer la registration du Worker.

La gestion de l'abonnement du *worker* auprès du serveur *processing* se fait à l'aide d'un ServletContextListener : *fr.gouv.vitam.worker.server.registration.WorkerRegistrationListener*.

Le WorkerRegistrationListener va lancer l'enregistrement du *worker* au démarrage du serveur worker, dans un autre Thread utilisant l'instance *Runnable* : *fr.gouv.vitam.worker.server.registration.WorkerRegister*.

L'execution du *WorkerRegister* essaie d'enregistrer le *worker* suivant un retry paramétrable dans la configuration du serveur avec :
- un délai (registerDelay en secondes)
- un nombre d'essai (registerTry)

Le lancement du serveur est indépendant de l'enregistrement du *worker* auprès du *processing* : le serveur *worker* ne s'arrêtera pas si l'enregistrement n'a pas réussi.

4. Worker-core
^^^^^^^^^^^^^^
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

La classe WorkerImpl permet de lancer ces différents handlers.

4.1 Détail du handler : CheckConformityActionHandler
''''''''''''''''''''''''''''''''''''''''''''''''''''
4.1.1 description
'''''''''''''''''

Ce handler permet de contrôle de l'empreinte. Il comprend désormais 2 tâches :

-- Vérification de l'empreinte par rapport à l'empreinte indiquée dans le manifeste (en utilisant algorithme déclaré dans manifeste)
-- Calcul d'une empreinte en SHA-512 si l'empreinte du manifeste est calculée avec un algorithme différent

4.1.2 exécution
'''''''''''''''
CheckConformityActionHandler recupère l'algorithme de Vitam (SHA-512) par l'input dans workflow
et le fichier en InputStream par le workspace.

Si l'algorithme est différent que celui dans le manifest, il calcul l'empreinte de fichier en SHA-512

.. code-block:: java
			DigestType digestTypeInput = DigestType.fromValue((String) handlerIO.getInput().get(ALGO_RANK));
            InputStream inputStream =
                workspaceClient.getObject(containerId,
                    IngestWorkflowConstants.SEDA_FOLDER + "/" + binaryObject.getUri());
            Digest vitamDigest = new Digest(digestTypeInput);
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
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
logbook lifecycle
''''''''''''''''''''''
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
''''''''''''''''''''''
processing, worker, workspace et logbook

4.1.4 cas d'erreur
''''''''''''''''''
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
'''''''''''''''''''''''''''''''''''''''''''''''''''''''
4.2.1 description
'''''''''''''''''
Ce handler permet de comparer le nombre d'objet stocké sur le workspace et le nombre d'objets déclaré dans le manifest.

4.3 Détail du handler : CheckObjectUnitConsistencyActionHandler
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
TODO

4.4 Détail du handler : CheckSedaActionHandler
''''''''''''''''''''''''''''''''''''''''''''''
TODO

4.4 Détail du handler : CheckStorageAvailabilityActionHandler
'''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
TODO

4.5 Détail du handler : CheckVersionActionHandler
'''''''''''''''''''''''''''''''''''''''''''''''''
TODO

4.6 Détail du handler : ExtractSedaActionHandler
''''''''''''''''''''''''''''''''''''''''''''''''
4.6.1 description
'''''''''''''''''
Ce handler permet d'extraire le contenu du SEDA. Il y a :
- extraction des BinaryDataObject
- extraction des ArchiveUnit
- création des lifes cycles des units
- construction de l'arbre des units et sauvegarde sur le workspace
- sauvegarde de la map des units sur le workspace
- sauvegarde de la map des objets sur le workspace
- sauvegarde de la map des objets groupes sur le workspace


4.6.2 Détail des différentes maps utilisées :
'''''''''''''''''''''''''''''''''''''''''''''
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
'''''''''''''''''''''''''''''''''''''''''''''''''''''
4.7.1 description
Indexation des objets groupes en récupérant les objets groupes du workspace. Il y a utilisation d'un client metadata.
TODO

4.8 Détail du handler : IndexUnitActionHandler
''''''''''''''''''''''''''''''''''''''''''''''
4.8.1 description
Indexation des units en récupérant les units du workspace. Il y a utilisation d'un client metadata.
TODO

4.9 Détail du handler : StoreObjectGroupActionHandler
'''''''''''''''''''''''''''''''''''''''''''''''''''''
4.9.1 description
'''''''''''''''''
Persistence des objets dans l'offre de stockage depuis le workspace.

TODO


4.10 Détail du handler : FormatIdentificationActionHandler
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
4.10.1 Description
''''''''''''''''''

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
''''''''''''''''''''''''''''''''''''''''''''''
Map<String, String> objectIdToUri
    contenu         : cette map contient l'id du BDO associé à son uri.
    création        : elle est créée dans le Handler après récupération du json listant les ObjectGroups
    MAJ, put        : elle est populée lors de la lecture du json listant les ObjectGroups.
    lecture, get    : lecture au fur et à mesure du traitement des BDO.
    suppression     : elle n'est pas enregistrée sur le workspace et est présente en mémoire uniquement.

4.10.3 exécution
''''''''''''''''
Ce Handler est exécuté dans l'étape "Contrôle et traitements des objets", juste après le Handler de vérification des empreintes.

4.10.4 journalisation : logbook operation? logbook life cycle?
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
Dans le traitement du Handler, sont mis à jour uniquement les journaux de cycle de vie des ObjectGroups.
Les Outcome pour les journaux de cycle de vie peuvent être les suivants :
 - Le format PUID n'a pas été trouvé / ne correspond pas avec le référentiel des formats.
 - Le format du fichier n'a pas pu être trouvé.
 - Le format du fichier a été complété dans les métadonnées (un "diff" est généré et ajouté).
 - Le format est correct et correspond au référentiel des formats.

(Note : les messages sont informatifs et ne correspondent aucunement à ce qui sera vraiment inséré en base)

4.10.5 modules utilisés
'''''''''''''''''''''''
Le Handler utilise les modules suivants :
 - Workspace (récupération / copie de fichiers)
 - Logbook (mise à jour des journaux de cycle de vie des ObjectGroups)
 - Common-format-identification (appel pour analyse des objets)
 - AdminManagement (comparaison format retourné par l'outil d'analyse par rapport au référentiel des formats de Vitam).

4.10.6 cas d'erreur
'''''''''''''''''''
Les différentes exceptions pouvant être rencontrées :
 - ReferentialException : si un problème est rencontré lors de l'interrogation du référentiel des formats de Vitam
 - InvalidParseOperationException/InvalidCreateOperationException : si un problème est rencontré lors de la génération de la requête d'interrogation du référentiel des formats de Vitam
 - FormatIdentifier*Exception : si un problème est rencontré avec l'outil d'analyse des formats (Siegfried)
 - Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
 - Logbook*Exception : si un problème est rencontré lors de l'interrogation du logbook
 - Content*Exception : si un problème est rencontré lors de l'interrogation du workspace
 - ProcessingException : si un problème plus général est rencontré dans le Handler


Worker-common
-------------

Le worker-common contient majoritairement des classes utilitaires.
A terme, il faudra que SedaUtils notamment soit "retravaillé" pour que les différentes méthodes soit déplacées dans les bons Handlers.

Worker-client
-------------
Le worker client contient le code permettant l'appel vers les API Rest offert par le worker.
Pour le moment une seule méthode est offerte : submitStep. Pour plus de détail, voir la partie worker-client.


4.11 Détail du handler : AccessionRegisterActionHandler
''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
4.11.1 Description
''''''''''''''''''
AccessionRegisterActionHandler permet de fournir une vue globale et dynamique des archives 
sous la responsabilité du service d'archives, pour chaque tenant.

4.11.2 Détail des maps utilisées
''''''''''''''''''''''''''''''''
Map<String, String> objectGroupIdToGuid
    contenu         : cette map contient l'id du groupe d'objet relié à son guid

Map<String, String> archiveUnitIdToGuid
	contenu         : cette map contient l'id du groupe d'objet relié à son guid
    
Map<String, Object> bdoToBdoInfo
	contenu         : cette map contient l'id du binary data object relié à son information
	
4.11.3 exécution
''''''''''''''''
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

