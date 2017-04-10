Workflow d'entrée d'un arbre de positionnement
##############################################

Introduction
============

Ce document décrit le processus (workflow-tree) d'entrée d'un arbre de positionement mis en place dans la solution logicielle Vitam, 
l'implémentation mise en place pour celui-ci dans la version V1 de cette solution.

Processus d'entrée d'un arbre (vision métier)
=============================================

Le processus d'entrée d'un arbre est identique au workflow d'entrée d'un SIP:    

 - Un workflow-tree est un processus composé d’étapes elles-mêmes composées d’une liste d’actions

- Chaque étape, chaque action peut avoir les statuts suivants : OK, KO, Warning, FATAL

- Chaque action peut avoir les modèles d'éxécutions : Bloquant ou Non bloquant

Ce workflow se compose des différentes étapes et actions propres à celui d'un upload de SIP. 
Les sections suivantes présentent également sa structure et des actions non encore abordées pour celui de l'entrée d'un SIP.

Le processus d'entrée débute lors du lancement du chargement d'un Submission Information Package dans la solution Vitam. De plus, toutes les étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent le processus d'entrée (clé et description de la clé associée dans le journal des opérations) tel qu'implémenté dans la version bêta de la solution logicielle :


Contrôle du SIP (STP_INGEST_CONTROL_SIP)
----------------------------------------

  * Vérification globale du SIP (CHECK_SEDA) : Vérification de la cohérence physique d'un arbre
  
  * Vérification d'existe d'objets (CHECK_NO_OBJECT)

    + **Règle** : vérifier s'il y a d'un objet numérique dans le manifest.xml de l'arbre

    + **Type** : bloquant.

    + **Statuts** :
		- OK : s'il y a des objects numériques dans le manifest
		- KO : s'il n'y a pas d'objects numérique dans le manifest
    
  * Vérification du nombre d'objets (CHECK_MANIFEST_OBJECTNUMBER)    

  * Vérification de la cohérence du bordereau (CHECK_MANIFEST)


Rangement des unites archivistiques (STP_UNIT_STORING)
------------------------------------------------------

  * Indexation des métadonnées des unités archivistiques (UNIT_METADATA_INDEXATION)

  * Sécurisation des métadonnées des unités archivistiques (UNIT_METADATA_STORAGE)

  * Sécurisation du journal des cycles de vie des unités archivistiques (COMMIT_LIFE_CYCLE_UNIT)


Finalisation de l'entrée (STP_INGEST_FINALISATION)
--------------------------------------------------

  * Notification de la fin de l'opération d'entrée (ATR_NOTIFICATION)

  * Mise en cohérence des journaux de cycle de vie (ROLL_BACK) 


Structure du Workflow d'un arbre (Implémenté en V1)
========================================

Le workflow actuel mis en place dans la solution Vitam est défini dans le fichier "DefaultHoldingSchemeWorkflow.json".
Il décrit le processus d'entrée pour téléchager un arbre, indexer les métadonnées et stocker les objets contenus dans l'arbre.

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
	                "uri": "VALUE:HOLDING_UNIT"
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


.. figure:: images/Workflow_Tree.jpg
  :align: center
  :height: 22 cm

  Diagramme d'état / transitions du workflow d'un arbre


- **Step 1** - STP_INGEST_CONTROL_SIP : Check d'arbre

  * CHECK_SEDA (CheckSedaActionHandler.java) :

  * CHECK_NO_OBJECT (CheckNoObjectsActionHandler.java) : vérifier s'il y a d'un objet numérique dans le manifest.xml de l'arbre
  
  * CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

  * CHECK_MANIFEST (ExtractSedaActionHandler.java) :


- **Step 2** - STP_OG_STORING : Rangement des objets

  * OG_STORAGE (StoreObjectGroupActionPlugin.java) :

  * OG_METADATA_INDEXATION (IndexObjectGroupActionPlugin.java) :

- **Step 3 et finale** - STP_INGEST_FINALISATION 

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :

