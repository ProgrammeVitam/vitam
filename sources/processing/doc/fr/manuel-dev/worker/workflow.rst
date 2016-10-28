Workflow
########

DefaultIngestWorkflow
*********************

.. code-block:: json

  {
    "id": "DefaultIngestWorkflow",
    "comment": "Default Ingest Workflow V6",
    "steps": [
      {
        "workerGroupId": "DefaultWorker",
        "stepName": "STP_INGEST_CONTROL_SIP",
        "behavior": "BLOCKING",
        "distribution": {
          "kind": "REF",
          "element": "SIP/manifest.xml"
        },
        "actions": [
          {
            "action": {
              "actionKey": "CHECK_SEDA",
              "behavior": "BLOCKING"
            }
          },
          {
            "action": {
              "actionKey": "CHECK_MANIFEST_DATAOBJECT_VERSION",
              "behavior": "BLOCKING"
            }
          },
          {
            "action": {
              "actionKey": "CHECK_MANIFEST_OBJECTNUMBER",
              "behavior": "NOBLOCKING"
            }
          },
          {
            "action": {
              "actionKey": "CHECK_CONSISTENCY",
              "behavior": "BLOCKING",
              "out": [
                {
                  "name": "unitsLevel.file",
                  "uri": "WORKSPACE:UnitsLevel/ingestLevelStack.json"
                },
                {
                  "name": "mapsBDOtoOG.file",
                  "uri": "WORKSPACE:Maps/BDO_TO_OBJECT_GROUP_ID_MAP.json"
                },
                {
                  "name": "mapsBDO.file",
                  "uri": "WORKSPACE:Maps/BINARY_DATA_OBJECT_ID_TO_GUID_MAP.json"
                },
                {
                  "name": "mapsObjectGroup.file",
                  "uri": "WORKSPACE:Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json"
                },
                {
                  "name": "mapsObjectGroup.file",
                  "uri": "WORKSPACE:Maps/OG_TO_ARCHIVE_ID_MAP.json"
                },
                {
                  "name": "mapsBDOtoVersionBDO.file",
                  "uri": "WORKSPACE:Maps/BDO_TO_VERSION_BDO_MAP.json"
                },
                {
                  "name": "mapsUnits.file",
                  "uri": "WORKSPACE:Maps/ARCHIVE_ID_TO_GUID_MAP.json"
                },
                {
                  "name": "globalSEDAParameters.file",
                  "uri": "WORKSPACE:ATR/globalSEDAParameters.json"
                }
              ]
            }
          },
          {
            "action": {
              "actionKey": "CHECK_CONSISTENCY_POST",
              "behavior": "NOBLOCKING",
              "in": [
                {
                  "name": "mapsBDOtoOG.file",
                  "uri": "WORKSPACE:Maps/OG_TO_ARCHIVE_ID_MAP.json"
                },
                {
                  "name": "mapsBDOtoOG.file",
                  "uri": "WORKSPACE:Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json"
                }
              ]
            }
          }
        ]
      },
      {
        "workerGroupId": "DefaultWorker",
        "stepName": "STP_OG_CHECK_AND_TRANSFORME",
        "behavior": "BLOCKING",
        "distribution": {
          "kind": "LIST",
          "element": "ObjectGroup"
        },
        "actions": [
        	{
            "action": {
              "actionKey": "CHECK_DIGEST",
              "behavior": "BLOCKING",
              "in": [
                {
                  "name": "algo",
                  "uri": "VALUE:SHA-512"
                }
              ],
              "out": [
                {
                  "name": "seda.file",
                  "uri": "WORKSPACE:Maps/BDO_TO_BDO_INFO_MAP.json"
                }
              ]
            }
          },
          {
            "action": {
              "actionKey": "OG_OBJECTS_FORMAT_CHECK",
              "behavior": "BLOCKING"
            }
          }
        ]
      },
      {
        "workerGroupId": "DefaultWorker",
        "stepName": "STP_STORAGE_AVAILABILITY_CHECK",
        "behavior": "BLOCKING",
        "distribution": {
          "kind": "REF",
          "element": "SIP/manifest.xml"
        },
        "actions": [
          {
            "action": {
              "actionKey": "STORAGE_AVAILABILITY_CHECK",
              "behavior": "BLOCKING"
            }
          }
        ]
      },
      {
        "workerGroupId": "DefaultWorker",
        "stepName": "STP_OG_STORING",
        "behavior": "BLOCKING",
        "distribution": {
          "kind": "LIST",
          "element": "ObjectGroup"
        },
        "actions": [
          {
            "action": {
              "actionKey": "OG_STORAGE",
              "behavior": "BLOCKING"
            }
          },
          {
            "action": {
              "actionKey": "OG_METADATA_INDEXATION",
              "behavior": "BLOCKING"
            }
          }
        ]
      },
      {
        "workerGroupId": "DefaultWorker",
        "stepName": "STP_UNIT_STORING",
        "behavior": "BLOCKING",
        "distribution": {
          "kind": "LIST",
          "element": "Units"
        },
        "actions": [
          {
            "action": {
              "actionKey": "UNIT_METADATA_INDEXATION",
              "behavior": "BLOCKING"
            }
          }
        ]
      },
      {
           "workerGroupId": "DefaultWorker",
           "stepName": "STP_ACCESSION_REGISTRATION",
           "behavior": "BLOCKING",
           "distribution": {
             "kind": "REF",
             "element": "SIP/manifest.xml"
           },
           "actions": [
             {
               "action": {
                 "actionKey": "ACCESSION_REGISTRATION",
                 "behavior": "BLOCKING",
                 "in": [
                   {
                     "name": "mapsUnits.file",
                     "uri": "WORKSPACE:Maps/ARCHIVE_ID_TO_GUID_MAP.json"
                   },
                   {
                     "name": "mapsBDO.file",
                     "uri": "WORKSPACE:Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json"
                   },
                   {
                     "name": "mapsBDO.file",
                     "uri": "WORKSPACE:Maps/BDO_TO_BDO_INFO_MAP.json"
                   },
                   {
                     "name": "globalSEDAParameters.file",
                     "uri": "WORKSPACE:ATR/globalSEDAParameters.json"
                   }
                 ]
               }
             }
           ]
         },
         {
           "workerGroupId": "DefaultWorker",
           "stepName": "STP_INGEST_FINALISATION",
           "behavior": "FINALLY",
           "distribution": {
             "kind": "REF",
             "element": "SIP/manifest.xml"
           },
           "actions": [
             {
               "action": {
                 "actionKey": "ATR_NOTIFICATION",
                 "behavior": "BLOCKING",
                 "in": [
                   {
                     "name": "mapsUnits.file",
                     "uri": "WORKSPACE:Maps/ARCHIVE_ID_TO_GUID_MAP.json",
                     "optional": "true"
                   },
                   {
                     "name": "mapsBDO.file",
                     "uri": "WORKSPACE:Maps/BINARY_DATA_OBJECT_ID_TO_GUID_MAP.json",
                     "optional": "true"
                   },
                   {
                     "name": "mapsBDOtoOG.file",
                     "uri": "WORKSPACE:Maps/BDO_TO_OBJECT_GROUP_ID_MAP.json",
                     "optional": "true"
                   },
                   {
                     "name": "mapsBDOtoVersionBDO.file",
                     "uri": "WORKSPACE:Maps/BDO_TO_VERSION_BDO_MAP.json",
                     "optional": "true"
                   },
                   {
                     "name": "globalSEDAParameters.file",
                     "uri": "WORKSPACE:ATR/globalSEDAParameters.json",
                     "optional": "true"
                   }
                 ]
               }
             }
           ]
         }
       ]
      }



- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  - CHECK_SEDA :
    - Test existence manifest.xml
    - Validation XSD SEDA manifest.xml

  - CHECK_MANIFEST_DATAOBJECT_VERSION :

  - CHECK_MANIFEST_OBJECTNUMBER :
    - Comptage BinaryDataObject dans manifest.xml en s'assurant d'aucun doublon :
    - List Workspace GUID/SIP/content/
    - CheckObjectsNumber Comparaison des 2 nombres et des URI

  - CHECK_CONSISTENCY :
    - Extraction BinaryDataObject de manifest.xml / MAP des Id BDO / Génération GUID
    - Extraction ArchiveUnit de manifest.xml / MAP des id AU / Génération GUID
    - Contrôle des références dans les AU des Id BDO
    - Stockage dans Workspace des BDO et AU

  - CHECK_CONSISTENCY_POST : vérification de la cohérence objet/unit

- **Step 2** - STP_OG_CHECK_AND_TRANSFORME : Check Objects Compliance du SIP / distribution sur LIST GUID/BinaryDataObject

  - CHECK_DIGEST : Contrôle de l'objet binaire correspondant du BDO taille et empreinte via Workspace

  - OG_OBJECTS_FORMAT_CHECK :
    - Contrôle du format des objets binaires
    - Consolidation de l'information du format dans l'object groupe correspondant si nécessaire

- **Step 3** - STP_STORAGE_AVAILABILITY_CHECK : Check Storage Availability / distribution REF GUID/SIP/manifest.xml

  - STORAGE_AVAILABILITY_CHECK : Contrôle de la taille totale à stocker par rapport à la capacité des offres de stockage pour une stratégie et un tenant donnés

- **Step 5** - STP_OG_STORING : Rangement des objets

  - OG_STORAGE : Écriture des objets sur l’offre de stockage des BDO des GO

  - OG_METADATA_INDEXATION : Enregistrement en base des ObjectGroup

- **Step 4** - STP_UNIT_STORING : Index Units / distribution sur LIST GUID/Units

  - UNIT_METADATA_INDEXATION :
    - Transformation Json Unit et intégration GUID Unit + GUID GO
    - Enregistrement en base Units

- **Step 5** - STP_ACCESSION_REGISTRATION : Alimentation du registre de fond

  - ACCESSION_REGISTRATION :  enregistrement des archives prises en charge dans le Registre des Fonds

- **Step 6 et finale** - STP_INGEST_FINALISATION : Notification de la fin de l’opération d’entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  - ATR_NOTIFICATION :
    - génération de l'ArchiveTransferReply xml (OK ou KO)
    - enregistrement de l'ArchiveTransferReply xml dans les offres de stockage
    