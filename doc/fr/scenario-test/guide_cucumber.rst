Guide d'écriture des tests Cucumber
###################################

Voici des exemples sur les types de test qui peuvent être écrits avec l'outil Cucumber paramétré sur le projet Vitam.

Guide technique
---------------

Les exemples suivants présentent l'exhaustivité des phrases définies par fonctionnalité.

Fonctionnalité : ingest
~~~~~~~~~~~~~~~~~~~~~~~

**Scénario** : Envoi d'une archive et vérification du journal de l'opération, de l'ATR, des journaux de cycle de vie d'un Unit et d'un GOT

**Contexte** Avant de lancer ce scénario, je présuppose que les contrats d'entrée, les contrats d'accès, les référentiels des règles de gestions et des formats sont chargés.

.. code-block:: cucumber

  Scénario: Guide ingest
  # Execution d'un ingest
    Etant donné un fichier SIP nommé data/SIP_GUIDE_INGEST_OK.zip
    Quand je télécharge le SIP
  # Vérification du journal des opérations
    Et je recherche le journal des opérations
    Alors le statut final du journal des opérations est KO
    Et les statuts des événements CHECK_DIGEST, STP_OG_CHECK_AND_TRANSFORME sont KO
    Et l'outcome détail de l'événement CHECK_DIGEST est CHECK_DIGEST.KO
    Et l'outcome détail de l'événement STP_OG_CHECK_AND_TRANSFORME est STP_OG_CHECK_AND_TRANSFORME.KO
  # Vérification de l'ATR
    Quand je télécharge son fichier ATR
    Alors l'état final du fichier ATR est KO
    Et le fichier ATR contient 1 balise de type Date
    Et le fichier ATR contient les valeurs STP_OG_CHECK_AND_TRANSFORME, CHECK_DIGEST, LFC.CHECK_DIGEST, LFC.CHECK_DIGEST.CALC_CHECK
    Et le fichier ATR contient la  chaîne de caractères
  """
  <BinaryDataObject id="ID018">
  """
    Et le fichier ATR contient la  chaîne de caractères
  """
  <ArchiveUnit id="ID019">
  """
  # Vérification du JCV d'un des Units
    Quand je recherche le JCV de l'unité archivistique dont le titre est Fichier 2 nouveau jeu de test
    Alors les statuts des événements LFC.UNITS_RULES_COMPUTE, LFC.UNIT_METADATA_INDEXATION, LFC.UNIT_METADATA_STORAGE sont OK
  # Vérification du JCV d'un des GOTs
    Quand je recherche le JCV du groupe d'objet de l'unité archivistique dont le titre est Historique de la station Gambetta
    Alors les statuts des événements LFC.OG_OBJECTS_FORMAT_CHECK.FILE_FORMAT,LFC.OG_OBJECTS_FORMAT_CHECK est OK


Fonctionnalité : recherche simple des métadonnées d'AUs et de GOTs
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Scénario** Recherche de toutes les unités archivistiques et de groupes d'objets liés à un ingest

**Contexte** Avant de lancer ce scénario, je présuppose que les contrats d'entrée, les contrats d'accès, les référentiels des règles de gestions et des formats sont chargés.

.. code-block:: cucumber

  Scénario: Guide recherche simple
  # Execution d'un ingest
    Etant donné les tests effectués sur le tenant 0
    Et les données du jeu de test du SIP nommé data/SIP_GUIDE_OK.zip
  # Rechercher des AUs
    Quand j'utilise la requête suivante
  """
  { "$roots": [],
    "$query": [{"$in":{"#operations":["Operation-Id"]}}],
    "$filter": {
      "$orderby": { "TransactedDate": 1}
    },
    "$projection": {}
  }
  """
    Et je recherche les unités archivistiques
  # Vérification des résultats
    Alors le nombre de résultat est 5
    Alors les metadonnées pour le résultat 0
      | inheritedRule.StorageRule.R3.{{unit:AU13}}.path                 | [["{{unit:AU13}}","{{unit:AU14}}"]]                            
      | inheritedRule.AccessRule.ACC-00002.{{unit:2_Front Populaire}}.path.array[][]        | [["{{unit:2_Front Populaire}}"]] |      |
      | inheritedRule.AccessRule.ACC-00001.{{unit:AU51}}.EndDate   | "2017-01-01"                                                       |
      | #management.AccessRule.Inheritance.PreventRulesId.array[]                                   | "ACC-00002" |
      | #management.DisseminationRule.Inheritance.PreventInheritance                   | true |
    Quand je recherche les groupes d'objets de l'unité archivistique dont le titre est ID8
    Alors les metadonnées sont
      | #qualifiers.1.versions.0.DataObjectVersion                      | BinaryMaster_1      |
      | #qualifiers.1.versions.0.FileInfo.Filename                      | Filename0           |
      | #qualifiers.1.versions.0.FormatIdentification.FormatId          | fmt/18              |



Fonctionnalité : recherche complexe d'une AU
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Scénario** Recherche d'une unités archivistique particulière et de son groupe d'objet

**Contexte** Avant de lancer ce scénario, je présuppose que les contrats d'entrée, les contrats d'accès, les référentiels des règles de gestions et des formats sont chargés.

.. code-block:: cucumber

  Scénario: Guide recherche avancée
  # Execution d'un ingest
    Etant donné les tests effectués sur le tenant 0
    Et les données du jeu de test du SIP nommé data/SIP_GUIDE_OK.zip
  # Rechercher complexe avec requête dans un fichier et remplacement de paramètres
    Quand j'utilise le fichier de requête suivant data/queries/query.json
    Et j'utilise dans la requête le GUID de l'unité archivistique pour le titre Archive unit ID1
    Et j'utilise dans la requête le paramètre SEDA-ID-UNIT avec la valeur ID1
    Et j'utilise dans la requête le paramètre DEPTH avec la valeur 0
    Et je recherche les unités archivistiques
  # Vérification des résultats
    Alors le nombre de résultat est 1
    Alors les metadonnées sont
      | Title            | Archive unit ID0101 |
      | StartDate        | 2012-06-20T18:58:18 |
      | EndDate          | 2014-12-07T09:52:56 |

Le fichier *data/queries/query.json* contient :

.. code-block:: json
  
  {
    "$roots": ["{{guid}}"],
    "$query": [
      {
        "$and": [
          {
            "$in": { "#operations": ["Operation-Id"] }
          },
          {
            "$eq": { "Title": "Archive unit SEDA-ID-UNIT" }
          }
        ],
        "$depth": DEPTH
      }
    ],
    "$projection": {
      "$fields": {
        "#id": 1,
        "Title": 1
      }
    }
  }


Fonctionnalité : recherche d'un registre des fonds
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Scénario** Recherche d'un registre des fonds et de son détail pour une opération d'ingest

**Contexte** Avant de lancer ce scénario, je présuppose que les contrats d'entrée, les contrats d'accès, les référentiels des règles de gestions et des formats sont chargés.

.. code-block:: cucumber
  
  Scénario: Guide registre de fonds
  # Execution d'un ingest
    Etant donné un fichier SIP nommé data/SIP_GUIDE_OK.zip
    Quand je télécharge le SIP
    Et je recherche le journal des opérations
    Alors le statut final du journal des opérations est OK
  # Rechercher du registre de fonds
    Quand j'utilise la requête suivante
  """
  {
    "$query": { "$eq": { "OriginatingAgency": "FRAN_NP_009913" } },
    "$projection": {}
  }
  """
    Et je recherche les registres de fond
  # Vérification du registre de fonds
    Et le nombre de registres de fond est 1
    Et les metadonnées pour le registre de fond sont
      | OriginatingAgency        | FRAN_NP_009913              |
      | TotalObjects.ingested        | 4 |
      | TotalObjectGroups.ingested        | 4 |
      | TotalUnits.ingested        | 7 |
  # Rechercher du détail du registre de fonds pour l'ingest
    Quand j'utilise la requête suivante
  """
  {
    "$query": {
      "$and": [ { "$in": { "OperationIds": [ "Operation-Id" ] } } ]
    },
    "$projection": {}
  }
  """
    Et je recherche les détails des registres de fond pour le service producteur FRAN_NP_009913
  # Vérification du détail du registre de fonds
    Et le nombre de détails du registre de fond est 1
    Et les metadonnées pour le détail du registre de fond sont
      | OriginatingAgency                 | FRAN_NP_009913              |
      | TotalObjects.ingested             | 4 |
      | TotalObjectGroups.ingested        | 4 |
      | TotalUnits.ingested               | 7 |


Scénarios fonctionnels
----------------------

Collection Unit
~~~~~~~~~~~~~~~

**Fonctionnalité** Recherche avancée

**Scénario** Recherche avancée d’archives – cas OK d’une recherche multicritère croisant métadonnées techniques, métadonnées descriptives et métadonnées de gestion (API)

.. code-block:: cucumber

  Etant donné les tests effectués sur le tenant 0
  Et un fichier SIP nommé data/SIP_OK/ZIP/OK-RULES_TEST.zip
  Et je télécharge le SIP
  Quand  j'utilise le fichier de requête suivant data/queries/select_multicriteres_md.json
  Et je recherche les unités archivistiques
  Alors les metadonnées sont
  | Title            | titre20999999  |
  | StartDate        | 2012-06-20T18:58:18 |
  | EndDate          | 2014-12-07T09:52:56 |


**Scénario** Recherche avancée d’archives – recherche d’archives dans un tenant sur la base de critères correspondant à des archives conservées dans un autre tenant (manuel)

.. code-block:: cucumber

  Etant donné les tests effectués sur le tenant 0
  Quand  j'utilise le fichier de requête suivant data/queries/select_multicriteres_md.json
  Et je recherche les unités archivistiques
  Alors les metadonnées sont
  | Title            | titre20999999  |
  | StartDate        | 2012-06-20T18:58:18 |
  | EndDate          | 2014-12-07T09:52:56 |
  Mais les tests effectués sur le tenant 1
  Et je recherche les unités archivistiques
  Alors le nombre de résultat est 0
  """
  {   "$roots": [],   "$query": [     {           "$and": [             {               "$eq": {                 "#management.AccessRule.Rules.Rule": "ACC-00002"               }             },             {               "$match": {                 "Title": "titre20999999"               }             }           ],       "$depth": 20     }   ],   "$filter": {     "$orderby": {       "TransactedDate": 1     }   },   "$projection": {   } }
  """


**Fonctionnalité** Modification interdite via API

**Scénario** KO_UPDATE_UNIT__ID : Vérifier la non modification de _id

.. code-block:: cucumber

  Etant donné les tests effectués sur le tenant 0
  Quand je modifie l'unité archivistique avec la requete
  """
  {"$query": [],"$filter": {},"$action": [ 		{"$set": { 				"_id" : "toto_id" 			}}]}
  """
  Et le statut de la requete est Bad Request


**Fonctionnalité** Affichage des métadonnées de l'objet physique

**Scénario** CAS OK = import SIP OK et métadonnées de l'objet physique OK

.. code-block:: cucumber

  Etant donné les tests effectués sur le tenant 0
  Et un fichier SIP nommé data/SIP_OK/ZIP/OK_ArchivesPhysiques.zip
  Quand je télécharge le SIP
  Alors le statut final du journal des opérations est OK
  Quand j'utilise la requête suivante
  """
  { "$roots": [],   "$query": [{"$and":[{"$eq":{"Title":"Sed blandit mi dolor"}},{"$in":{"#operations":["Operation-Id"]}}],       "$depth": 0}],     "$projection": {     "$fields": {       "TransactedDate": 1, "#id": 1, "Title": 1, "#object": 1, "DescriptionLevel": 1, "EndDate": 1, "StartDate": 1 }}}
  """
  Et je recherche les groupes d'objets des unités archivistiques
  Alors les metadonnées sont
  | #qualifiers.PhysicalMaster.versions.0.DataObjectVersion                      | PhysicalMaster_1    | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Height.value        | 21                  | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Height.unit         | centimetre          | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Length.value        | 29.7                | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Length.unit         | centimetre          | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Weight.value        | 1                   | 	      | #qualifiers.PhysicalMaster.versions.0.PhysicalDimensions.Weight.unit         | kilogram            | 	      | #qualifiers.BinaryMaster.versions.0.DataObjectVersion                        | BinaryMaster_1      | 	      | #qualifiers.BinaryMaster.versions.0.FileInfo.Filename                        | Filename0           | 	      | #qualifiers.BinaryMaster.versions.0.FormatIdentification.FormatId            | fmt/18              |


Collection FileRules
~~~~~~~~~~~~~~~~~~~~

**Fonctionnalité** Recherche de règle de gestion

**Scénario** Vérification et import des règles OK, recherche par id OK

.. code-block:: cucumber

  Quand je vérifie le fichier nommé data/rules/jeu_donnees_OK_regles_CSV_regles.csv pour le référentiel RULES                                |   Quand j'utilise le fichier de requête suivant data/queries/select_rule_by_id.json
  Et je recherche les données dans le référentiel RULES
  Alors le nombre de résultat est 1
  Et les metadonnées sont
  | RuleId           | APP-00001
  """"
  { 	"$query": { 		"$eq": { 			"RuleId": "APP-00001" 		} 	}, 	"$projection": { 		"$fields": { 			"#id": 1, 			"RuleId": 1, 			"Name": 1 		} 	}, 	"$filter": {} }
  """"


Collection AccessAccessionRegister
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Fonctionnalité** Recherche dans les registres des fonds

**Contexte** Avant de lancer ce scénario, je présuppose que les contrats d'entrée, les contrats d'accès, les référentiels des règles de gestions et des formats sont chargés.

**Scénario** Upload d'un SIP et vérification du contenu dans le registre de fonds

.. code-block:: cucumber

  Etant donné les tests effectués sur le tenant 0
  Et un fichier SIP nommé data/SIP_OK/ZIP/OK_ARBO-COMPLEXE.zip
  Quand je télécharge le SIP
	Et j'utilise le fichier de requête suivant data/queries/select_accession_register_by_id.json
  Et je recherche les détails des registres de fond pour le service producteur Vitam
  Alors les metadonnées sont
    | OriginatingAgency        | Vitam              |
    | SubmissionAgency         | Vitam              |
    | ArchivalAgreement        | ArchivalAgreement0 |
    """"
  {
  	"$query": {
  		"$eq": {
  			"#id": "Operation-Id"
  		}
  	},
  	"$projection": {},
  	"$filter": {}
  }
  """"


Tests de stockage
~~~~~~~~~~~~~~~~~

Ces tests permettent de vérifier qu'un objet est bien stocké plusieurs fois sur la plateforme, afin d'assurer sa pérennité.

Ce test vérifie :

 - Le tenant sur lequel est stocké l'objet
 - Le nom de l'objet stocké
 - La strategie de stockage
 - La liste des stratégies où est stocké l'objet
 - La présence de l'objet dans ces stratégies
