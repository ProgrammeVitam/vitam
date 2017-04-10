Workflow d'entrée d'un plan de classement
##############################################

Introduction
============

Ce document décrit le processus (workflow-plan) d'entrée d'un plan de classement mis en place dans la solution logicielle Vitam, 
l'implémentation mise en place pour celui-ci dans la version V1 de cette solution.

Processus d'entrée d'un plan de classement (vision métier)
=============================================

Le processus d'entrée d'un plan est identique au workflow d'entrée d'un SIP:    

 - Un workflow-plan est un processus composé d’étapes elles-mêmes composées d’une liste d’actions

- Chaque étape, chaque action peut avoir les statuts suivants : OK, KO, Warning, FATAL

- Chaque action peut avoir les modèles d'éxécutions : Bloquant ou Non bloquant

Ce workflow se compose des différentes étapes et actions propres à celui d'un upload de SIP. 
Les sections suivantes présentent également sa structure et des actions non encore abordées pour celui de l'entrée d'un SIP.

Le processus d'entrée débute lors du lancement du chargement d'un Submission Information Package dans la solution Vitam. De plus, toutes les étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent le processus d'entrée (clé et description de la clé associée dans le journal des opérations) tel qu'implémenté dans la version bêta de la solution logicielle :


Contrôle du SIP (STP_INGEST_CONTROL_SIP)
----------------------------------------

  * Vérification globale du SIP (CHECK_SEDA) : Vérification de la cohérence physique d'un plan
  
  * Vérification d'existe d'objets (CHECK_NO_OBJECT)

    + **Règle** : vérifier s'il y a d'un objet numérique dans le manifest.xml du plan

    + **Type** : bloquant.

    + **Statuts** :
		- OK : s'il y a des objects numériques dans le manifest
		- KO : s'il n'y a pas d'objects numérique dans le manifest
    
  * Vérification du nombre d'objets (CHECK_MANIFEST_OBJECTNUMBER)    

  * Vérification de la cohérence du bordereau (CHECK_MANIFEST)
  
  * Vérification de la présence et contrôle du contrat d'entrée (CHECK_CONTRACT_INGEST)


Contrôle et traitements des unités archivistiques (STP_OG_CHECK_AND_TRANSFORME)
-------------------------------------------------------------------------------
  * Application des règles de gestion et calcul des dates d'échéances (UNITS_RULES_COMPUTE)


Rangement des unites archivistiques (STP_UNIT_STORING)
------------------------------------------------------

  * Indexation des métadonnées des unités archivistiques (UNIT_METADATA_INDEXATION)

  * Sécurisation des métadonnées des unités archivistiques (UNIT_METADATA_STORAGE)

  * Sécurisation du journal des cycles de vie des unités archivistiques (COMMIT_LIFE_CYCLE_UNIT)


Registre des fonds (STP_ACCESSION_REGISTRATION)
-----------------------------------------------

  * Alimentation du registre des fonds (ACCESSION_REGISTRATION)
  
Finalisation de l'entrée (STP_INGEST_FINALISATION)
--------------------------------------------------

  * Notification de la fin de l'opération d'entrée (ATR_NOTIFICATION)

  * Mise en cohérence des journaux de cycle de vie (ROLL_BACK) 


Structure du Workflow d'un plan (Implémenté en V1)
========================================

Le workflow actuel mis en place dans la solution Vitam est défini dans le fichier "DefaultFilingSchemeWorkflow.json".
Il décrit le processus d'entrée pour télécharger un plan, indexer les métadonnées et stocker les objets contenus dans le plan.

.. code-block:: json

{
  "id": "DefaultFilingSchemeWorkflow",
  "comment": "Default Filing Scheme Workflow V6",
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
            "actionKey": "CHECK_NO_OBJECT",
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
            "actionKey": "CHECK_MANIFEST",
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
                "uri": "MEMORY:MapsMemory/OG_TO_ARCHIVE_ID_MAP.json"
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
              },
              {
                "name": "mapsObjectGroup.file",
                "uri": "MEMORY:MapsMemory/OBJECT_GROUP_ID_TO_GUID_MAP.json"
              }
            ]
          }
        },
        {
          "action": {
            "actionKey": "CHECK_CONTRACT_INGEST",
            "behavior": "BLOCKING",
            "in": [
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
      "stepName": "STP_UNIT_CHECK_AND_PROCESS",
      "behavior": "BLOCKING",
      "distribution": {
        "kind": "LIST",
        "element": "Units"
      },
      "actions": [
        {
          "action": {
            "actionKey": "UNITS_RULES_COMPUTE",
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
            "behavior": "BLOCKING",
             "in": [
              {
                "name": "UnitType",
                "uri": "VALUE:CLASSIFICATION_UNIT"
              }
            ]
          }
        },
        {
          "action": {
            "actionKey": "UNIT_METADATA_STORAGE",
            "behavior": "BLOCKING"
          }
        }
        ,
        {
          "action": {
            "actionKey": "COMMIT_LIFE_CYCLE_UNIT",
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
                "uri": "WORKSPACE:Maps/BDO_TO_VERSION_BDO_MAP.json"
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
            "behavior": "NOBLOCKING",
            "in": [
              {
                "name": "mapsUnits.file",
                "uri": "WORKSPACE:Maps/ARCHIVE_ID_TO_GUID_MAP.json",
                "optional": true
              },
              {
                "name": "mapsBDO.file",
                "uri": "WORKSPACE:Maps/BINARY_DATA_OBJECT_ID_TO_GUID_MAP.json",
                "optional": true
              },
              {
                "name": "mapsBDOtoOG.file",
                "uri": "WORKSPACE:Maps/BDO_TO_OBJECT_GROUP_ID_MAP.json",
                "optional": true
              },
              {
                "name": "mapsBDOtoVersionBDO.file",
                "uri": "WORKSPACE:Maps/BDO_TO_VERSION_BDO_MAP.json",
                "optional": true
              },
              {
                "name": "globalSEDAParameters.file",
                "uri": "WORKSPACE:ATR/globalSEDAParameters.json",
                "optional": true
              },
              {
                "name": "mapsOG.file",
                "uri": "WORKSPACE:Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json",
                "optional": true
              }
            ],
            "out": [
              {
                "name": "atr.file",
                "uri": "WORKSPACE:ATR/responseReply.xml"
              }
            ]
          }
        },
        {
          "action": {
            "actionKey": "ROLL_BACK",
            "behavior": "BLOCKING"
          }
        }
      ]
    }
  ]
}


D'une façon synthétique, le workflow est décrit de cette façon :


##############################################
# Image diagramme workflow-plan sera ajouté
##############################################


- **Step 1** - STP_INGEST_CONTROL_SIP : Check d'plan

  * CHECK_SEDA (CheckSedaActionHandler.java) :

  * CHECK_NO_OBJECT (CheckNoObjectsActionHandler.java) : vérifier s'il y a des objets numériques dans le manifest.xml du plan
  
  * CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

  * CHECK_MANIFEST (ExtractSedaActionHandler.java) :

  * CHECK_CONTRACT_INGEST (CheckIngestContractActionHandler.java) :

- **Step 2** - STP_UNIT_CHECK_AND_PROCESS : Contrôle et traitements des units / distribution sur LIST GUID

  * UNITS_RULES_COMPUTE (UnitsRulesComputePlugin.java) :


- **Step 3** - STP_OG_STORING : Rangement des objets

  * OG_STORAGE (StoreObjectGroupActionPlugin.java) :

  * OG_METADATA_INDEXATION (IndexObjectGroupActionPlugin.java) :

- **Step 4** - STP_ACCESSION_REGISTRATION : Alimentation du registre des fonds

  * ACCESSION_REGISTRATION (AccessionRegisterActionHandler.java) :

- **Step 5 et finale** - STP_INGEST_FINALISATION 

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :