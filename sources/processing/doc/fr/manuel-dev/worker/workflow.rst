Workflow
########

DefaultIngestWorkflow
*********************


Un Workflow est défini en JSON avec la structure suivante :


- un identifiant (id)
- une clé (identifier)
- un nom (name)
- une catégorie (typeProc)
- une liste de Steps :

 - un identifiant de famille de Workers (workerGroupId)
 - un identifiant de Step (stepName)

 - un modèle d'exécution (behavior) pouvant être :
   BLOCKING : le traitement est bloqué en cas d'erreur, il est nécessaire de recommencer le workflow
   NOBLOCKING : le traitement peut continuer malgrée les erreurs

 - un modèle de distribution :

    - un type (kind) pouvant être REF ou LIST
    - l'élément de distribution (element) indiquant l'élément unique (REF) ou le chemin sur le Workspace (LIST)

 - une liste d'Actions :


    - un nom d'action (actionKey)
    - un modèle d'exécution (behavior) pouvant être BLOCKING ou NOBLOCKING
    - des paramètres d'entrées (in) :

       - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
       - une cible (uri) comportant un schema (WORKSPACE, MEMORY, VALUE) et un path :


          - WORKSPACE:path indique le chemin relatif sur le workspace
          - MEMORY:path indique le nom de la clef de valeur
          - VALUE:path indique la valeur statique en entrée

       - chaque handler peut accéder à ces valeurs, définies dans l'ordre stricte, via le handlerIO

          - WORKSPACE : implicitement un File
          - MEMORY : implicitement un objet mémoire déjà alloué par un Handler précédent
          - VALUE : implicitement une valeur String

    - des paramètres de sortie (out) :

       - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
       - une cible (uri) comportant un schema (WORKSPACE, MEMORY) et un path :

          - WORKSPACE:path indique le chemin relatif sur le workspace
          - MEMORY:path indique le nom de la clef de valeur

       - chaque handler peut stocker les valeurs finales, définies dans l'ordre stricte, via le handlerIO


          - WORKSPACE : implicitement un File local
          - MEMORY : implicitement un objet mémoire


Exemple:

.. only:: html

        .. literalinclude:: includes/json
           :language: json
           :linenos:

.. only:: latex

        .. literalinclude:: includes/json
           :language: json
           :linenos:
           :dedent: 4

.. dedent ne semble pas marcher.

Etapes
-------

- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  - CHECK_SEDA :
    - Test existence manifest.xml
    - Validation XSD SEDA manifest.xml

  - CHECK_MANIFEST_DATAOBJECT_VERSION :

  - CHECK_MANIFEST_OBJECTNUMBER :
    - Comptage BinaryDataObject dans manifest.xml en s'assurant d'aucun doublon :
    - List Workspace GUID/SIP/content/
    - CheckObjectsNumber Comparaison des 2 nombres et des URI


  - CHECK_MANIFEST :
    - Extraction BinaryDataObject de manifest.xml / MAP des Id BDO / Génération GUID
    - Extraction ArchiveUnit de manifest.xml / MAP des id AU / Génération GUID
    - Contrôle des références dans les AU des Id BDO
    - Stockage dans Workspace des BDO et AU

  - CHECK_CONTRACT_INGEST : Vérification de la présence et contrôle du contrat d'entrée

  - CHECK_CONSISTENCY : vérification de la cohérence objet/unit

- **Step 2** - STP_OG_CHECK_AND_TRANSFORME : Check Objects Compliance du SIP / distribution sur LIST GUID/BinaryDataObject

  - CHECK_DIGEST : Contrôle de l'objet binaire correspondant du BDO taille et empreinte via Workspace

  - OG_OBJECTS_FORMAT_CHECK :
    - Contrôle du format des objets binaires
    - Consolidation de l'information du format dans l'object groupe correspondant si nécessaire

- **Step 3** - STP_UNIT_CHECK_AND_PROCESS : Check des archive unit et de leurs règles associées

  - CHECK_UNIT_SCHEMA : Contrôles intelligents du Json représentant l'Archive Unit par rapport à un schéma Json
  - UNITS_RULES_COMPUTE : Calcul des règles de gestion

- **Step 4** - STP_STORAGE_AVAILABILITY_CHECK : Check Storage Availability / distribution REF GUID/SIP/manifest.xml

  - STORAGE_AVAILABILITY_CHECK : Contrôle de la taille totale à stocker par rapport à la capacité des offres de stockage pour une stratégie et un tenant donnés

- **Step 5** - STP_OBJ_STORING : Rangement et indexation des objets

  - OBJ_STORAGE : Écriture des objets sur l’offre de stockage des BDO des GO

  - OG_METADATA_INDEXATION : Indexation des métadonnées des ObjectGroup

- **Step 6** - STP_UNIT_METADATA : Indexation des métadonnées des Units

  - UNIT_METADATA_INDEXATION : Transformation Json Unit et intégration GUID Unit + GUID GO

- **Step 7** - STP_OG_STORING : Rangement des métadonnées des objets

  - COMMIT_LIFE_CYCLE_OBJECT_GROUP : Écriture des objets sur l’offre de stockage des BDO des GO

  - OG_METADATA_STORAGE : Enregistrement en base des métadonnées des ObjectGroup

  - COMMIT_LIFE_CYCLE_OBJECT_GROUP : Écriture des objets sur l’offre de stockage des BDO des GO

- **Step 8** - STP_UNIT_STORING : Index Units / distribution sur LIST GUID/Units

  - COMMIT_LIFE_CYCLE_UNIT : Écriture des métadonnées des Units sur l’offre de stockage des BDO des GO

  - UNIT_METADATA_STORAGE : Enregistrement en base des métadonnées des Units

  - COMMIT_LIFE_CYCLE_UNIT : Écriture des métadonnées des Units sur l’offre de stockage des BDO des GO

- **Step 9** - STP_ACCESSION_REGISTRATION : Alimentation du registre de fond

  - ACCESSION_REGISTRATION :  enregistrement des archives prises en charge dans le Registre des Fonds

- **Step 10 et finale** - STP_INGEST_FINALISATION : Notification de la fin de l’opération d’entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  - ATR_NOTIFICATION :
    - génération de l'ArchiveTransferReply xml (OK ou KO)
    - enregistrement de l'ArchiveTransferReply xml dans les offres de stockage


Création d'un nouveau step
--------------------------
Un step est une étape de workflow. Il regroupe un ensemble d'actions (handler). Ces steps sont définis dans le workflowJSONvX.json (X=1,2).
