Cucumber (exemples)
###################

Voici quelques exemples de scénarios de tests réalisables avec Cucumber.

Collection Unit
----------------

**Fonctionnalité** Recherche avancée

**Scénario** Recherche avancée d’archives – cas OK d’une recherche multicritères croisant métadonnées techniques et métadonnées descriptives et métadonnées de gestion (API)

::
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

::
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

::
  Etant donné les tests effectués sur le tenant 0
  Quand je modifie l'unité archivistique avec la requete
  """
  {"$query": [],"$filter": {},"$action": [ 		{"$set": { 				"_id" : "toto_id" 			}}]}
  """
  Et le statut de la requete est Bad Request


**Fonctionnalité** Affichage des métadonnées de l'objet physique

**Scénario** CAS OK = import SIP OK et métadonnées de l'objet physique OK

::
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
----------------------

**Fonctionnalité** Recherche de règle de gestion

**Scénario** Vérification et import des règles OK, recherche par id OK

::
  Quand je vérifie le fichier nommé data/rules/jeu_donnees_OK_regles_CSV_regles.csv pour le référentiel RULES                                |   Quand j'utilise le fichier de requête suivant data/queries/select_rule_by_id.json
  Et je recherche les données dans le référentiel RULES
  Alors le nombre de résultat est 1
  Et les metadonnées sont
  | RuleId           | APP-00001
  """"
  { 	"$query": { 		"$eq": { 			"RuleId": "APP-00001" 		} 	}, 	"$projection": { 		"$fields": { 			"#id": 1, 			"RuleId": 1, 			"Name": 1 		} 	}, 	"$filter": {} }
  """"


Collection AccessAccessionRegister
------------------------------------

**Fonctionnalité** Recherche dans les registres de fond

**Contexte** Avant de lancer cette suite de test, je présuppose que les règles de gestions et de formats sont chargés.

**Scénario** Upload d'un SIP et vérification du contenu dans le registre de fonds

::
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
