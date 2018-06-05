Collection Unit
###############

Utilisation de la collection Unit
=================================

La collection Unit contient les informations relatives aux unités archivistiques.

Exemple de XML en entrée
========================

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour compléter les champs du JSON. Il s'agit des informations situées entre les balises <ArchiveUnit>.

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <ArchiveUnit id="ID44">
      <Management>
          <DisseminationRule>
              <Rule>DIS-00002</Rule>
              <StartDate>2000-01-01</StartDate>
              <RefNonRuleId>DIS-00001</RefNonRuleId>
          </DisseminationRule>
      </Management>
      <Content>
          <DescriptionLevel>RecordGrp</DescriptionLevel>
          <Title>Gare du Nord</Title>
          <Description>Cette unité de description hérite de son parent de la règle ACC-00003 avec pour StartDate 01/01/2000, bloque l'héritage de la règle DIS-00001 mais déclare la règle DIS-00002 avec pour StartDate 01/01/2000</Description>
          <StartDate>2017-04-05T08:11:56</StartDate>
          <EndDate>2017-04-05T08:11:56</EndDate>
      </Content>
      <ArchiveUnit id="ID75">
          <ArchiveUnitRefId>ID32</ArchiveUnitRefId>
      </ArchiveUnit>
  </ArchiveUnit>

Exemple de Json stocké dans la collection Unit
==============================================

Les champs présentés dans l'exemple ci-après ne fait pas état de l'exhaustivité des champs disponibles dans le SEDA. Ceux-ci sont référencés dans la documentation SEDA disponible au lien suivant : https://redirect.francearchives.fr/seda/api_v2/doc.html


.. code-block:: json

    {
      "_id": "aeaqaaaaaahccnklabtgyak7pkvypgyaaacq",
      "_mgt": {
          "DisseminationRule": {
              "Rules": [
                  {
                      "Rule": "DIS-00002",
                      "StartDate": "2000-01-01",
                      "EndDate": "2075-01-01"
                  }
              ],
              "Inheritance": {
                  "PreventRulesId": [
                      "DIS-00001"
                  ]
              }
          },
          "OriginatingAgency": "RATP"
      },
      "DescriptionLevel": "RecordGrp",
      "Title": "Gare du Nord",
      "Titles": {
          "fr": "Gare du Nord"
      },
      "Description": "Cette unité de description hérite de son parent de la règle ACC-00003 avec pour StartDate 01/01/2000, bloque l'héritage de la règle DIS-00001 mais déclare la règle DIS-00002 avec pour StartDate 01/01/2000",
      "Descriptions": {
          "fr": "Cette unité de description hérite de son parent de la règle ACC-00003 avec pour StartDate 01/01/2000, bloque l'héritage de la règle DIS-00001 mais déclare la règle DIS-00002 avec pour StartDate 01/01/2000"
      },
      "StartDate": "2017-04-05T08:11:56",
      "EndDate": "2017-04-05T08:11:56",
      "_storage": {
          "_nbc": 2,
          "strategyId": "default",
          "offerIds": [
              "vitam-iaas-app-02.int",
              "vitam-iaas-app-03.int"
          ]
      },
      "_sps": [
          "RATP"
      ],
      "_sp": "RATP",
      "_ops": [
          "aedqaaaaacfeavznabdrgak7pkvyhgiaaaaq"
      ],
      "_opi": "aedqaaaaacfeavznabdrgak7pkvyhgiaaaaq",
      "_unitType": "INGEST",

      "_max": 4,
      "_min": 1,
      "_up": [
          "aeaqaaaaaahccnklabtgyak7pkvypgyaaaba"
      ],
      "_nbc": 1,
      "_us": [
          "aeaqaaaaaahccnklabtgyak7pkvypgqaaaba",
          "aeaqaaaaaahccnklabtgyak7pkvypgqaaacq",
          "aeaqaaaaaahccnklabtgyak7pkvypgyaaaba"
      ],

   "_uds": {
       "1": ["aeaqaaaaaahccnklabtgyak7pkvypgyaaaba"],
       "2": ["aeaqaaaaaahjgl36aazigaldnxdkimiaaabq"],
       "3": ["aeaqaaaaaahccnklabtgyak7pkvypgqaaaba"],
   },
	]
      "_v": 1,
      "_tenant": 0
    }


Voici un autre extrait de détail JSON :


.. code-block:: json

   { .....
   "_sp": "SP1",
   "_up": [
       "aeaqaaaaaahjgl36aazigaldnxdkivyaaabq",
       "aeaqaaaaaahjgl36aazigaldnxdkivaaaaba"
   ],

   "_us": [
       "aeaqaaaaaahjgl36aazigaldnxdkivyaaabq",
       "aeaqaaaaaahjgl36aazigaldnxdkimiaaabq",
       "aeaqaaaaaahjgl36aazigaldnxdkivaaaaba"
   ],
   "_sps": [
       "SP1",
       "SP2"
   ],
   "_graph": [
       "aeaqaaaaaahjgl36aazigaldnxdkivyaaabq/aeaqaaaaaahjgl36aazigaldnxdkimiaaabq",
       "aeaqaaaaaahjgl36aazigaldnxdkiviaaaba/aeaqaaaaaahjgl36aazigaldnxdkivaaaaba",
       "aeaqaaaaaahjgl36aazigaldnxdkiviaaaba/aeaqaaaaaahjgl36aazigaldnxdkivyaaabq",
       "aeaqaaaaaahjgl36aazigaldnxdkivaaaaba/aeaqaaaaaahjgl36aazigaldnxdkimiaaabq"
   ],
   "_uds": {
       "1": [
           "aeaqaaaaaahjgl36aazigaldnxdkivaaaaba",
           "aeaqaaaaaahjgl36aazigaldnxdkivyaaabq"
       ],
       "2": [
           "aeaqaaaaaahjgl36aazigaldnxdkimiaaabq"
       ]
   },
   "_us_sp": {
       "SP1": [
           "aeaqaaaaaahjgl36aazigaldnxdkivaaaaba",
           "aeaqaaaaaahjgl36aazigaldnxdkivyaaabq"
       ],
       "SP2": [
           "aeaqaaaaaahjgl36aazigaldnxdkimiaaabq",
       ]
   },
   "_min": 1,
   "_max": 3,
   "_glpd": "2018-05-17T11:05:38.705"
   }



Détail du JSON
==============

La structure de la collection Unit est composée de la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau de transfert conforme au standard SEDA v.2.1., c'est-à-dire toutes les balises se rapportant aux unités archivistiques.

Cette transposition se fait comme suit :

**"_id":** identifiant unique de l'unité archivistique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_og" (objectGroup):** identifiant du groupe d'objets représentant cette unité archivistique.

  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champ _id du groupe d'objets de la collection objectGroup.
  * Cardinalité : 0-1

**"DescriptionLevel":** niveau de description archivistique de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> présentes dans le bordereau de transfert.
  * Cardinalité : 1-1

**"Title":** titre de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le bordereau de transfert.
  * Cardinalité : 1-1

**"Titles":** titres de l'unité archivistique par langue.

  * Il s'agit d'un tableau JSON.
  * Les titres sont organisés sous la forme de clef / valeur, la clef étant l'indicatif de la langue, la valeur le titre. Par exemple : "fr": "Ceci est un titre."
  * Cardinalité : 0-1

**"Description":** description de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les informations situées entre les balises <Description> de l'unité archivistique concernée dans le bordereau de transfert.
  * Cardinalité : 0-1

**"Descriptions":** description de l'unité archivistique par langue.

  * Il s'agit d'un tableau JSON
  * Les descriptions sont organisées sous la forme de clef / valeur, la clef étant l'indicatif de la langue, la valeur la description. Par exemple : "fr": "Ceci est une description."
  * Cardinalité : 0-n

**"XXXXX":** des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le bordereau au niveau du Content de chaque unité archivistique.

  * Se reporter à la documentation descriptive du SEDA 2.1 et notamment le schéma ontology.xsd pour connaître la liste des métadonnées facultatives.

**"_storage":** contient trois champs qui permettent d'identifier les offres  de stockage.

  * Il s'agit d'un JSON constitué des champs suivants :

    * "strategyId": identifiant de la stratégie de stockage.
    * "offerIds": liste des offres de stockage pour une stratégie donnée
    * "_nbc": nombre d'offres.

  * Ne peut être vide
  * Cardinalité : 1-1

**"_sps":** services producteurs liés à l'unité archivistique suite à un rattachement et ayant des droits d'accès sur celle-ci.

  * Il s'agit d'un tableau contenant les identifiants de tous les services producteurs référençant l'unité archivistique.
  * Il s'agit d'un tableau de chaînes de caractères.
  * Ne peut être vide. Il comprend au minimum le service versant déclaré dans le bordereau de transfert.
  * Cardinalité : 1-1

**"_sp":** service producteur d'origine déclaré lors de la prise en charge de l'unité archivistique par la solution logicielle Vitam.

  * Il s'agit du service producteur inscrit dans le bordereau lié au transfert de l'unité archivistique et déclaré via une extension du schéma <OtherManagementAbstract>, la balise <OriginatingAgencyIdentifier>.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"_ops"** (operations): tableau contenant les identifiants d'opérations auxquelles cette unité archivistique a participé.

  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champ _id de l'opération enregistré dans la collection LogBookOperation.
  * Ne peut être vide.
  * Cardinalité : 1-1

**"_opi"** : identifiant de l'opération à l'origide de la création de cette unité archivistique.

  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champs _id de la collection LogBookOperation.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_unitType":** champ indiquant le type d'unité archivistique concerné.

  * Il s'agit d'une chaîne de caractères.
  * La valeur contenue doit être conforme à l'énumération UnitType. Celle-ci peut être :

      * INGEST : unité archivistique issue d'un SIP
      * FILING_UNIT : unité archivistique issue d'un plan de classement
      * HOLDING_UNIT : unité archivistique issue d'un arbre de positionnement

  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_max":** profondeur maximale de l'unité archivistique par rapport à une racine.

  * Calculée, cette profondeur correspond au maximum des profondeurs, quels que soient les racines concernées et les chemins possibles.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_min":** profondeur minimum de l'unité archivistique par rapport à une racine.

  * Calculée, cette profondeur correspond au le minimum des profondeurs, quels que soient les racines concernées et les chemins possibles.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_up" (unit up):** tableau recenssant les _id des unités archivistiques parentes (parents immédiats).

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID. Valeur du champ _id d'une unité archivistique enregistré dans la collection Unit.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_nbc" :** nombre d'enfants immédiats de l'unité archivistique.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_us":** tableau contenant la parentalité, c'est à dire l'ensemble des unités archivistiques parentes, indexé de la manière suivante : [ GUID1, GUID2, ... ].

  * Tableau de chaînes de 36 caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_uds":** tableau contenant la parentalité, c'est à dire l'ensemble des unités archivistiques parentes, ainsi que le niveau de profondeur relative.

  * Ces informations sont réunies dans le tableau sous la forme de clef/valeur. Exemple [{GUID1 : depth1}, {GUID2 : depth2}, ... }].
  * Il s'agit d'un tableau de JSON.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_us_sp":** Liste des AU parents concernant tous les niveaux de parentalité

  * Il s'agit d'un tableau contenant les identifiants de tous les services producteurs de tous les parents liées à l'unité archivistique.
  * Il s'agit d'un tableau de chaînes de caractères.
  * Ne peut être vide. Il comprend au minimum le service versant déclaré dans le bordereau de transfert.
  * Cardinalité : 1-1

**"_glpd":** Date de la dernière modification de la partie _graph

  * Il s'agit d'une chaîne de 36 caractères correspondant à une date.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1


**_profil:** Profil d'archivage utilisé lors de l'entrée.

  * Correspond à ArchiveProfile, le profil d'archivage utilisé lors de l'entrée. Sa valeur correspond à l'identifiant métier d'un profil enregistré dans la collection ArchiveProfil.
  * Chaîne de caractères.
  * Cardinalité : 0-1

**"_mgt":** contient les balises contenues dans le bloc <Management> du bordereau de tranfert pour cette unité archivistique.

Une catégorie de règles de gestion appliquées à cette unité archivistique. Ces catégories sont, exhaustivement (cardinalité 0-1 pour chaque catégorie) :

  - AccessRule (délai de communicabilité)
  - AppraisalRule (durée d'utilité administrative)
  - ClassificationRule (durée de classification)
  - DisseminationRule (durée de diffusion)
  - ReuseRule (durée de réutilisation)
  - StorageRule (durée d'utilité courante)

Chaque catégorie peut contenir :

    1. Un tableau de règles de gestion (tableau d'objets, cardinalité 0-1)

      Chacune des règles de ce tableau est elle-même composée de plusieurs informations :

      + **"Rule"**: identifiant de la règle, qui correspond à une valeur du champ RuleId de la collection FileRules. (cardinalité 1-1)
      + **"StartDate"** : "StartDate": date de début du calcul de l'échéance. Cette date est déclarée dans le message ArchiveTransfer ou ajoutée *a posteriori* par une modification de l'unité archivistique. (cardinalité 1-1)
      + **"EndDate**": date de fin d'application de la règle. Cette valeur est issue d'un calcul réalisé par la solution logicielle Vitam. Celui ci consiste en l'ajout du délai correspondant à la règle dans la collection FileRules à la valeur du champ startDate (EndDate = StartDate + Durée) (cardinalité 1-1)

    2. Des données spécifiques aux catégories :

      - Pour les catégories "StorageRule" et "AppraisalRule" uniquement : le champ **"FinalAction"** décrit le sort final des règles dans ces catégories (cardinalité 1-1). La valeur contenue dans le champ peut être :

        + Pour StorageRule : "Transfer", "Copy" ou "RestrictAccess" (énumaration issue du FinalActionStorageCodeType en SEDA 2.1)
        + Pour AppraisalRule : "Keep" ou "Destroy" (énumaration issue du FinalActionAppraisalCodeType en SEDA 2.1)
      - Pour ClassificationRule uniquement :

        + **"ClassificationLevel"** : niveau de classification, obligatoire et systématiquement renseigné (cardinalité 1-1)
        + **"ClassificationOwner"**: propriétaire de la classification, obligatoire et systématiquement renseigné (cardinalité 1-1)
        + **"ClassificationAudience"** : permet de gérer les questions de "diffusion restreinte", "spécial France" et "Confidentiel Industrie", champ optionnel (cardinalité 0-1)
        + **"ClassificationReassessingDate"** : date de réévaluation de la classification, optionnelle. (cardinalité 0-1)
        + **"NeedReassessingAuthorization"** : indique si une autorisation humaine est nécessaire pour réévaluer la classification, optionnel (cardinalité 0-1)

    3. Des paramètres de gestion d'héritage de règles, dans un objet nommé **"Inheritance"** (cardinalité 0-1). Cet objet peut avoir comme valeur :

      + **"PreventInheritance"** : "true" ou "false", utilisé pour bloquer l'héritage de toutes les règles de gestion de la même catégorie (cardinalité 0-1)
      + **"PreventRulesId"** : est un tableau d'identifiants de règles de gestion qui ne doivent pas être héritées d'un parent (cardinalité 0-1)


Extrait d'une unité archivistique ayant un bloc "_mgt" possédant des règles de gestions :

.. code-block:: json

  "_mgt": {
          "AppraisalRule": {
              "Rules": [
                  {
                      "Rule": "APP-00001",
                      "StartDate": "2015-01-01",
                      "EndDate": "2095-01-01"
                  },
                  {
                      "Rule": "APP-00002"
                  }
              ],
              "Inheritance": {
                  "PreventInheritance": true,
                  "PreventRulesId": []
              },
              "FinalAction": "Keep"
          },
          "AccessRule": {
              "Rules": [
                  {
                      "Rule": "ACC-00001",
                      "StartDate": "2016-06-03",
                      "EndDate": "2016-06-03"
                  }
              ]
          },
          "DisseminationRule": {
              "Inheritance": {
                  "PreventInheritance": true,
                  "PreventRulesId": []
              }
          },
          "ReuseRule": {
              "Inheritance": {
                  "PreventRulesId": [
                      "REU-00001", "REU-00002"
                  ]
              }
          },
          "ClassificationRule": {
              "ClassificationLevel": "Secret Défense",
              "ClassificationOwner": "Projet_Vitam",
              "Rules": [
                  {
                      "ClassificationReassessingDate": "2025-06-03",
                      "NeedReassessingAuthorization": true,
                      "Rule": "CLASS-00001"
                  }
              ]
          }
      },
