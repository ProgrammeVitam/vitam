Extraire les métadonnées des ArchiveUnit et DataObject
######################################################

Général
*******

L'extraction du bordereau SEDA tranforme le fichier manifest.xml en plusieurs fichiers contenant les informations du manifest, les définitions des Archives Units et des Groupes d'Objets Techniques, ainsi que la structure des objets.

Workspace avant extraction :
============================
containerId/SIP
containerId/SIP/Content/
containerId/SIP/manifest.xml

Workspace après extraction :
============================
containerId/SIP
containerId/SIP/Content/
containerId/SIP/manifest.xml
containerId/Units
containerId/Units/AU_GUID.xml
containerId/Uits/...
containerId/ObjectGroup
containerId/ObjectGroup/GOT_GUID.json
containerId/ObjectGroup/...
containerId/Maps/
containerId/Maps/ARCHIVE_ID_TO_GUID_MAP.json
containerId/Maps/BDO_TO_OBJECT_GROUP_ID_MAP.json
containerId/Maps/BDO_TO_VERSION_BDO_MAP.json
containerId/Maps/BINARY_DATA_OBJECT_ID_TO_GUID_MAP.json
containerId/Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json
containerId/UnitsLevel/
containerId/UnitsLevel/ingestLevelStack.json
containerId/ATR/
containerId/ATR/globalSEDAParameters.json


Algorithme
**********

1. Récupération du GUID/objects/SIP/manifest.xml

       * Voir RAML WEB/Internal_Workspace.html#containers__cid__objects__id_object__get

2. Extraction SEDA

      1. Extraction des DataObject (Physical et Binary) dans workspace depuis manifest.xml (GUID/DataObject/GUID) (SedaUtils->extractSEDA)

        * En lisant le fichier XML, extraire les DataObject depuis xml (SedaUtils->writeBinaryDataObjectInLocal et extractArchiveUnitToLocalFile)
        * Mettre en place des MAP utiles
           * Liaisons DataObject -> object (MAP<idDo, path>)
           * Liaisons DataObject <-> ObjectGroup (MAP<idDo, idOg> et MAP<idOg, List<idDo>>)
           * Liaisons Unit -> Unit (MAP<idUFils, List<idUPere>>)
           * Liaisons Unit <-> ObjectGroup (MAP<idx, idy> avec x et y à décider)

      2. Construction des ObjectGroup depuis les DataObject (SedaUtils->saveObjectGroupsToWorkspace)

        * A partir le map ObjectGroup -> DataObject: construire l'objet ObjectGroup en Json
        * Sauvegarde ces ObjectGroups dans workspace

      3. Sauvegarde des ArchiveUnit dans workspace depuis manifest.xml (GUID/Units/GUID) (SedaUtils->writeArchiveUnitToWorkspace)

      4. L'autre worker va chercher les ObjectGroups dans workspace puis indexer dans metadata (SedaUtils->indexObjectGroup)

      5. L'autre worker va traduire Unit XML -> Json puis indexer dans metadata  (SedaUtils->indexArchiveUnit)


3. Journalisation de fin de l'action extraction SEDA (fait par le Distributeur)

Algorithme d'update
===================

Après la création de l'Archives Unit temporaire extraite du manifest.xml si une balise *<SystemId>EXISTING_GUID</SystemId>* a été rencontrée les traitement suivant sont fait :
* l'Archive Unit existant est récupéré en base à partir du EXISTING_GUID fourni dans le fichier, si il n'est pas trouvé l'extraction est arrêtée
* un nouveau fichier d'archive temporaire *EXISTING_GUID.xml* est créé à partir du fichier extrait (*GUID.xml*) en changeant l'attribut d'id de la balise *<ArchiveUnit id="GUID">...</ArchiveUnit>*
* l'ancien fichier *GUID.xml* est supprimé
* le nouveau guid *EXISTING_GUID* remplace l'ancien *GUID* dans la données temporaires d'extraction (correspondance des Id VITAM/SEDA, liste des GUID de unit extrait) et ajouté dans la liste des GUID existants
* préparation du lifecycle de l'archive unit spécifique à la mise à jour (*message à définir*)

Lors de la finalisation de l'extraction des units, si le unit est déclaré comme pré-existant on ajoute :
* on ajoute une balise *<existing>true</existing>* dans la balise *<work>...</work>* pour indiquer aux prochaines étapes que l'archive unit manipulé est une mise à jour
