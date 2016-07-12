Indexer les métadonnées des ArchiveUnit et DataObject
############################

Algorithme
*******************

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
