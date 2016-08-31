Workflow
########

DefaultIngestWorkflow
*********************

| {
|    "id": "DefaultIngestWorkflow",
|
|   "comment": "Default Ingest Workflow V3",
|    "steps": [
|        {
|            "workerGroupId": "DefaultWorker",
|            "stepName": "Check SIP",
|            "distribution": {
|                "kind": "REF",
|                "element": "SIP/manifest.xml"
|            },
|            "actions": [
|                {
|                    "action": "CheckSeda"
|                },
|                {
|                    "action": "CheckObjectsNumber"
|                },
|                {
|                    "action": "ExtractSeda"
|                }
|            ]
|        },
|        {
|            "workerGroupId": "DefaultWorker",
|            "stepName": "Check Objects Compliance",
|            "distribution": {
|                "kind": "LIST",
|                "element": "DataObjects"
|            },
|            "actions": [
|                {
|                    "action": "CheckObjectsCompliance"
|                }
|            ]
|        },
|        {
|            "workerGroupId": "DefaultWorker",
|            "stepName": "Index seda",
|            "distribution": {
|                "kind": "List",
|                "element": "Units"
|            },
|            "actions": [
|               {
|                    "action": "IndexUnit"
|                }
|            ]
|        }
|    ]
| }


- **Step 1** : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  - CheckSeda : Test existence manifest.xml

  - CheckSeda : Validation XSD SEDA manifest.xml

  - CheckObjectsNumber : Comptage BinaryDataObject dans manifest.xml en s'assurant d'aucun doublon :

  - CheckObjectsNumber : List Workspace GUID/SIP/content/

  - CheckObjectsNumber Comparaison des 2 nombres et des URI

  - ExtractSeda : Extraction BinaryDataObject de manifest.xml / MAP des Id BDO / Génération GUID

  - ExtractSeda : Extraction ArchiveUnit de manifest.xml / MAP des id AU / Génération GUID

  - ExtractSeda : Contrôle des références dans les AU des Id BDO

  - ExtractSeda : Stockage dans Workspace des BDO et AU

- **Step 2** : Check Objects Compliance du SIP / distribution sur LIST GUID/BinaryDataObject

  - CheckObjectsCompliance: Contrôle de l'objet binaire correspondant du BDO taille et empreinte via Workspace

- **Step 3** : Check Storage Availability / distribution REF GUID/SIP/manifest.xml

  - CheckStorageAvailability: Contrôle de la taille totale à stocker par rapport à la capacité des offres de stockage pour une stratégie et un tenant donnés
  
- **Step 4** : Index Units / distribution sur LIST GUID/Units

  - IndexUnit : Transformation Json Unit et intégration GUID Unit + GUID GO

  - IndexUnit : Enregistrement en base U

- **Step 5** : Rangement des objets

  - StoreObjectGroup : Écriture des objets sur l’offre de stockage des BDO des GO

  - IndexObjectGroup : Enregistrement en base des ObjectGroup
