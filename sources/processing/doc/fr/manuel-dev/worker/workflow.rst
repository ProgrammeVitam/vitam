Workflow
########

DefaultIngestWorkflow
=====================


Un Workflow est défini en JSON avec la structure suivante :


- un identifiant (id)
- une clé (identifier)
- un nom (name)
- une catégorie (typeProc)
- une liste de Steps : La structure d'une step donnée est la suivante

 - un identifiant de famille de Workers (workerGroupId)
 - un identifiant de Step (stepName)

 - un modèle d'exécution (behavior) pouvant être :
   BLOCKING : le traitement est bloqué en cas d'erreur, il est nécessaire de recommencer le workflow
   NOBLOCKING : le traitement peut continuer malgrée les erreurs
   FINALLY: le traitement est executé quelque soit la statut des traitements precedants

 - un modèle de distribution :

    - kind: un type pouvant être :

        - REF : pas de distribution pour ce step et définit une référence vers un fichier à traiter. (Exemple: manifest.xml)
        - LIST :

         - si la valeur de 'element' est 'Units' : la liste des éléments à traiter est incluse dans un fichier ingestLevelStack.json. Ce fichier contient les guid des archive units ordonnés par niveau de graphe.
         - si la valeur de 'element' est autre : la liste des éléments à traiter est représentée par les fichiers présents dans le sous-répertoire représenté par 'element' (ex : 'ObjectGroup')

        - LIST_IN_FILE: Fichier contenant une liste de GUID à traiter dans la distribution

    - l'élément de distribution (element) indiquant l'élément unique (REF) ou le chemin vers un dossier ou un fichier sur le Workspace (LIST, LIST_IN_FILE)
    - type: est-ce une distribution sur des unités archivistiques ou sur des groupes d'objets.
    - statusOnEmptyDistribution: Le statut qu'on attribue au step si jamais la distribution n'a pas eu lieu. Par defaut: WARNING
    - bulkSize: La taille du bulk : c'est à dire le nombre d'élément qui sera envoyé au worker. Par défaut, la valeur est récupérée depuis la configuration avec la variable `workerBulkSize`.

 - une liste d'Actions :


    - un nom d'action (actionKey)
    - un modèle d'exécution (behavior) pouvant être BLOCKING ou NOBLOCKING
    - des paramètres d'entrées (in) :

       - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
       - une cible (uri) comportant un schema (WORKSPACE, MEMORY, VALUE) et un path :


          - WORKSPACE:path indique le chemin relatif sur le workspace
          - WORKSPACE_OBJECT:path indique le chemin relatif sur un dossier intitulé au nom d'objet courant situé au workspace
          - MEMORY:path indique le nom de la clef de valeur
          - MEMORY_SINGLE:path indique le nom de la clef de valeur
          - VALUE:path indique la valeur statique en entrée

       - chaque handler peut accéder à ces valeurs, définies dans l'ordre stricte, via le handlerIO

          - WORKSPACE, WOKSPACE_OBJECT : implicitement un File
          - MEMORY, MEMORY_SINGLE : implicitement un objet mémoire déjà alloué par un Handler précédent
          - VALUE : implicitement une valeur String

    - des paramètres de sortie (out) :

       - un nom (name) utilisé pour référencer cet élément entre différents handlers d'une même étape
       - une cible (uri) comportant un schema (WORKSPACE, MEMORY) et un path :

          - WORKSPACE:path indique le chemin relatif sur le workspace
          - WORKSPACE_OBJECT:path indique le chemin relatif sur un dossier intitulé au nom d'objet courant situé au workspace
          - MEMORY:path indique le nom de la clef de valeur
          - MEMORY_SINGLE:path indique le nom de la clef de valeur

       - chaque handler peut stocker les valeurs finales, définies dans l'ordre stricte, via le handlerIO

          - WORKSPACE, WORKSPACE_OBJECT : implicitement un File local
          - MEMORY, MEMORY_SINGLE : implicitement un objet mémoire


.. literalinclude:: includes/json
   :language: json
   :linenos:



Etapes
------

- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  - PREPARE_STORAGE_INFO :
    - Vérifier que le storage est disponible
    - Récupérer les informations de connection au storage et les offres de stockage.

  - CHECK_SEDA :
    - Test existence manifest.xml
    - Validation XSD SEDA manifest.xml

  - CHECK_HEADER :
    - CHECK_AGENT: Vérifier l'existence des services agents dans le manifest et dans le référentiel des services agents.
    - CHECK_CONTRACT_INGEST : Vérifier l'existence des contrats d'entrée dans le manifest et dans le référentiel des contrats d'entrée
    - CHECK_IC_AP_RELATION: Vérifier le profile d'archivage et sa relation avec le contrat d'entrée
    - CHECK_ARCHIVEPROFILE: valider le manifest avec le fichier XSD/RNG défini dans le profile d'archivage

  - CHECK_DATAOBJECTPACKAGE:

    - Cas 1: arbres et plans d'accès

        - CHECK_NO_OBJECT
        - CHECK_MANIFEST_OBJECTNUMBER
        - CHECK_MANIFEST

    - Cas 2: SIP
        - CHECK_MANIFEST_DATAOBJECT_VERSION
        - CHECK_MANIFEST_OBJECTNUMBER
        - CHECK_MANIFEST
        - CHECK_CONSISTENCY

  - CHECK_NO_OBJECT: Vérifier l'existence ou pas des objets binaires et des objets physiques
    - Vérifier l'égalité du nomber de fichiers binaires dans le workspace par rapport au manifest.
    - Extract seda: créer des fichiers json représentant les méta-données des unités archivistiques et groupes d'objets
    - Si en plus, l'ingest n'est pas un arbre ou un plan de classement, vérifier la cohérence entre les unités archivistiques et les groupes d'objets.

  - CHECK_MANIFEST_OBJECTNUMBER :
    - Comptage BinaryDataObject dans manifest.xml en s'assurant d'aucun doublon :
    - List Workspace GUID/SIP/content/
    - CheckObjectsNumber Comparaison des 2 nombres et des URI

  - CHECK_MANIFEST :
    - Extraction BinaryDataObject de manifest.xml / MAP des Id BDO / Génération GUID
    - Extraction ArchiveUnit de manifest.xml / MAP des id AU / Génération GUID
    - Contrôle des références dans les AU des Id BDO
    - Stockage dans Workspace des BDO et AU

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

  - OG_METADATA_STORAGE : Enregistrement en base des métadonnées des ObjectGroup ainsi que leurs journaux de cycle de vie

  - COMMIT_LIFE_CYCLE_OBJECT_GROUP : Écriture des objets sur l’offre de stockage des BDO des GO

- **Step 8** - STP_UNIT_STORING : Index Units / distribution sur LIST GUID/Units

  - UNIT_METADATA_STORAGE : Enregistrement en base des métadonnées des Units ainsi que leurs journaux de cycle de vie

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


DefaultRulesUpdateWorkflow
==========================
