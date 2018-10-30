Collection Unit
###############

Utilisation de la collection Unit
=================================

La collection Unit contient les informations relatives aux unités archivistiques.

Exemple de XML en entrée
========================

Ci-après, la portion d'un bordereau de transfert (manifest.xml) utilisée pour compléter les champs du JSON. Il s'agit des informations situées entre les balises <ArchiveUnit>.

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

Exemple de JSON stocké dans la collection Unit
==============================================

Les champs présentés dans l'exemple ci-après ne font pas état de l'exhaustivité des champs disponibles dans le SEDA. Ceux-ci sont référencés dans la documentation SEDA disponible au lien suivant : https://redirect.francearchives.fr/seda/


.. code-block:: json

  {
      "_id": "aeaqaaaaamhad455abcwsalep4lzf2iaaaea",
      "_og": "aebaaaaaamhad455abcwsalep4lzfvaaaaca",
      "_mgt": {
          "AccessRule": {
              "Rules": [
                  {
                      "Rule": "ACC-00002",
                      "StartDate": "2000-01-01",
                      "EndDate": "2025-01-01"
                  }
              ]
          }
      },
      "DescriptionLevel": "Item",
      "Title": "Stalingrad.txt",
      "TransactedDate": "2017-04-04T08:07:06",
      "SedaVersion": "2.1",
      "ImplementationVersion": "1.7.0-SNAPSHOT",
      "_storage": {
          "_nbc": 2,
          "offerIds": [
              "offer-fs-1.service.int.consul",
              "offer-fs-2.service.int.consul"
          ],
          "strategyId": "default"
      },
      "_sps": [
          "RATP"
      ],
      "_sp": "RATP",
      "_ops": [
          "aeeaaaaaaohi422caa4paalep4lxwoyaaaaq",
          "aeeaaaaaaohi422caaieaalesqjo5hqaaaaq",
          "aeeaaaaaaohi422caaieaalesqkbhnaaaaaq",
          "aeeaaaaaaohi422caaieaalesqml2vyaaaaq"
      ],
      "_opi": "aeeaaaaaaohi422caa4paalep4lxwoyaaaaq",
      "_unitType": "INGEST",
      "_up": [
          "aeaqaaaaamhad455abcwsalep4lzf2iaaada"
      ],
      "_us": [
          "aeaqaaaaamhad455abcwsalep4lzf2aaaaeq",
          "aeaqaaaaamhad455abcwsalep4lzf2iaaada",
          "aeaqaaaaamhad455abcwsalep4lzf2iaaabq"
      ],
      "_graph": [
          "aeaqaaaaamhad455abcwsalep4lzf2iaaabq/aeaqaaaaamhad455abcwsalep4lzf2aaaaeq",
          "aeaqaaaaamhad455abcwsalep4lzf2iaaaea/aeaqaaaaamhad455abcwsalep4lzf2iaaada",
          "aeaqaaaaamhad455abcwsalep4lzf2iaaada/aeaqaaaaamhad455abcwsalep4lzf2iaaabq"
      ],
      "_uds": {
          "1": [
              "aeaqaaaaamhad455abcwsalep4lzf2iaaada"
          ],
          "2": [
              "aeaqaaaaamhad455abcwsalep4lzf2iaaabq"
          ],
          "3": [
              "aeaqaaaaamhad455abcwsalep4lzf2aaaaeq"
          ]
      },
      "_us_sp": {
          "RATP": [
              "aeaqaaaaamhad455abcwsalep4lzf2aaaaeq",
              "aeaqaaaaamhad455abcwsalep4lzf2iaaada",
              "aeaqaaaaamhad455abcwsalep4lzf2iaaabq"
          ]
      },
      "_min": 1,
      "_max": 4,
      "_glpd": "2018-07-09T12:50:30.733",
      "_v": 3,
      "_tenant": 3,
      "Description": "",
      "_history": [
       {
         "ud": "2018-07-25T15:28:49.040",
         "data": {
           "_v": 0,
           "_mgt": {
             "ClassificationRule": {
               "ClassificationAudience": "ClassificationAudience0",
               "ClassificationLevel": "Secret Défense",
               "ClassificationOwner": "ClassificationOwner0",
               "ClassificationReassessingDate": "2016-06-03",
               "NeedReassessingAuthorization": true,
               "Rules": [
                 {
                   "Rule": "CLASS-00001",
                   "StartDate": "2015-06-03",
                   "EndDate": "2025-06-03"
                 }
               ]
             }
           }
         }
       }
     ]
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

**"_mgt":** contient les balises contenues dans le bloc <Management> du bordereau de transfert pour cette unité archivistique (le champ peut donc être vide).

  * Il contient un tableau de catégories de règles de gestion appliquées à cette unité archivistique. Ces catégories sont, exhaustivement :

    - AccessRule (délai de communicabilité)
    - AppraisalRule (durée d'utilité administrative)
    - ClassificationRule (durée de classification)
    - DisseminationRule (durée de diffusion)
    - ReuseRule (durée de réutilisation)
    - StorageRule (durée d'utilité courante)

  * Cardinalité 0-1, pour chaque catégorie.

Chaque catégorie peut contenir :

    1. Un tableau de règles de gestion (tableau d'objets, cardinalité 0-1)

      Chacune des règles de ce tableau est elle-même composée de plusieurs informations :

      + **"Rule"**: identifiant de la règle, qui correspond à une valeur du champ RuleId de la collection FileRules. (cardinalité 0-1)
      + **"StartDate"** : "StartDate": date de début du calcul de l'échéance. Cette date est déclarée dans le message ArchiveTransfer ou ajoutée *a posteriori* par une modification de l'unité archivistique. (cardinalité 0-1)
      + **"EndDate**": date de fin d'application de la règle. Cette valeur est issue d'un calcul réalisé par la solution logicielle Vitam. Celui-ci consiste en l'ajout du délai correspondant à la règle dans la collection FileRules à la valeur du champ startDate (EndDate = StartDate + Durée) (cardinalité 0-1)

    2. Des données spécifiques aux catégories :

      - Pour les catégories "StorageRule" et "AppraisalRule" uniquement : le champ **"FinalAction"** décrit le sort final des règles dans ces catégories (cardinalité 1-1). La valeur contenue dans le champ peut être :

        + Pour StorageRule : "Transfer", "Copy" ou "RestrictAccess" (énumaration issue du FinalActionStorageCodeType en SEDA 2.1)
        + Pour AppraisalRule : "Keep" ou "Destroy" (énumaration issue du FinalActionAppraisalCodeType en SEDA 2.1)
      - Pour ClassificationRule uniquement :

        + **"ClassificationLevel"** : niveau de classification, obligatoire et systématiquement renseigné (cardinalité 1-1)
        + **"ClassificationOwner"**: propriétaire de la classification, obligatoire et systématiquement renseigné (cardinalité 1-1)
        + **"ClassificationAudience"** : permet de gérer les mentions additionnelles de limitation du champ de diffusion (exemple : "spécial France"), champ optionnel (cardinalité 0-1)
        + **"ClassificationReassessingDate"** : date de réévaluation de la classification, optionnelle. (cardinalité 0-1)
        + **"NeedReassessingAuthorization"** : indique si une autorisation humaine est nécessaire pour réévaluer la classification, optionnel (cardinalité 0-1)

    3. Des paramètres de gestion d'héritage de règles, dans un objet nommé **"Inheritance"** (cardinalité 0-1). Cet objet peut avoir comme valeur :

      + **"PreventInheritance"** :

        * "true" ou "false", utilisé pour bloquer l'héritage de toutes les règles de gestion de la même catégorie
        * Cardinalité 1-1 à partir du moment où le champ Inheritance existe

      + **"PreventRulesId"** :

        * Tableau d'identifiants de règles de gestion qui ne doivent pas être héritées d'un parent
        * A l'entrée il s'agit de la valeur de la balise <RefNonRuleId> du SEDA
        * Cardinalité 1-1 à partir du moment où le champ Inheritance existe


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


**"DescriptionLevel":** niveau de description archivistique de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> présentes dans le bordereau de transfert.
  * Cardinalité : 1-1

**"Title":** titre de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le bordereau de transfert.
  * Cardinalité : 0-1, le modèle d'une unité archivistique doit comporter au moins un champ Title et/ou au moins un champ Title\_

**"Title_":** titres de l'unité archivistique par langue

  * Il s'agit d'un tableau JSON.
  * Les titres sont organisés sous la forme de clef : valeur, la clef étant l'indicatif de la langue en xml:lang et la valeur le titre. Par exemple : "fr": "Ceci est un titre."
  * Cardinalité : 0-1, le modèle d'une unité archivistique doit comporter au moins un champ Title et/ou au moins un champ Title\_

.. code-block:: json

  {
      "fr": "FrenchMySIP",
      "en": "EnglishMySIP"
  },


**"Description":** description de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les informations situées entre les balises <Description> de l'unité archivistique concernée dans le bordereau de transfert.
  * Cardinalité : 0-1, le modèle d'une unité archivistique doit comporter au moins un champ Description et/ou au moins un champ Description\_

**"Description_":** description de l'unité archivistique par langue.

  * Il s'agit d'un tableau JSON
  * Les titres sont organisés sous la forme de clef : valeur, la clef étant l'indicatif de la langue en xml:lang et la valeur la description. Par exemple : "fr": "Ceci est une description."
  * Cardinalité : 0-1, le modèle d'une unité archivistique doit comporter au moins un champ Description et/ou au moins un champ Description\_

.. code-block:: json

  "Description_": {
      "fr": "Une autre description",
      "en": "another description"
  },

**"XXXXX":** des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le bordereau de transfert au niveau du Content de chaque unité archivistique.

  * Se reporter à la documentation descriptive du SEDA 2.1 et notamment le schéma ontology.xsd pour connaître la liste des métadonnées facultatives.

**ArchiveUnitProfile:** profil d'archivage de l'unité archivistique utilisé lors de l'entrée.

  * Correspond à l'identifiant du profil d'archivage associé à l'unité archivistique
  * Chaîne de caractères.
  * Cardinalité : 0-1

**"SedaVersion":** version du SEDA utilisé lors de l'entrée de cette unité archivistique.

    * Champ peuplé par la solution logicielle Vitam.
    * Cardinalité : 1-1
    * Exemple de valeur : "2.1"

**"ImplementationVersion":** version du modèle de donnée actuellement utilisé par l'unité archivistique.

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * Exemple de valeur : "1.7.0-SNAPSHOT"

**"_history"** : données historiques de l'unité archivistique

  * Champ peuplé par la solution logicielle Vitam au moment d'une mise à jour d'une unité archivistique, uniquement si la mise à jour déclenche une historisation
  * Cardinalité : 0-1
  * Ce champ contient les clés suivantes :

    + **"ud"** : date du changement de la métadonnée
    + **"data"** : les données qui sont historisées. Dans l'exemple ci dessous, on constate qu'au 25 juillet 2018, l'unité archivistique a historisé une règle de classification située dans le bloc management (_mgt) de son modèle.

      - Le champ **data** contient de plus le champ **_v**  qui est la version de l'enregistrement de l'unité archivistique avant modification. Ce champ est repris du champ "_v" à la racine du modèle de données de l'unité archivistique


.. code-block:: json

  "_history": [
   {
     "ud": "2018-07-25T15:28:49.040",
     "data": {
       "_v": 0,
       "_mgt": {
         "ClassificationRule": {
           "ClassificationAudience": "ClassificationAudience0",
           "ClassificationLevel": "Secret Défense",
           "ClassificationOwner": "ClassificationOwner0",
           "ClassificationReassessingDate": "2016-06-03",
           "NeedReassessingAuthorization": true,
           "Rules": [
             {
               "Rule": "CLASS-00001",
               "StartDate": "2015-06-03",
               "EndDate": "2025-06-03"
             }
           ]
         }
       }
     }
   }
  ]

Le champ **_history** peut également être créé depuis les données contenues dans un bordereau de transfert :

.. code-block:: xml

  <History>
                <UpdateDate>2018-08-02T14:06:23.374</UpdateDate>
                <Data>
                    <Version>0</Version>
                    <Management>
                        <ClassificationRule>
                            <ClassificationLevel>Secret Défense</ClassificationLevel>
                            <ClassificationOwner>ClassificationOwner0</ClassificationOwner>
                        </ClassificationRule>
                    </Management>
                </Data>
            </History>
            <History>
                <UpdateDate>2018-08-02T14:30:20.137</UpdateDate>
                <Data>
                    <Version>1</Version>
                    <Management>
                        <ClassificationRule>
                            <ClassificationLevel>Confidentiel Défense</ClassificationLevel>
                            <ClassificationOwner>ClassificationOwner0</ClassificationOwner>
                        </ClassificationRule>
                    </Management>
                </Data>
    </History>

Le mapping est le suivant :

    - La balise <History> du bordereau devient le tableau "_history" dans la base de données
    - <Data> devient "data"
    - <Version> devient "_v"
    - <Management> devient "_mgt"


**"_storage":** contient trois champs qui permettent d'identifier les offres  de stockage.

  * Il s'agit d'un JSON constitué des champs suivants :

    * "strategyId": identifiant de la stratégie de stockage.
    * "offerIds": liste des offres de stockage pour une stratégie donnée
    * "_nbc": nombre d'offres.

  * Ne peut être vide
  * Cardinalité : 1-1

**"_sps":** services producteurs auxquels l'unité archivistique a été rattachée (au titre de leurs fonds symboliques)

  * Il s'agit d'un tableau contenant les identifiants de tous les services producteurs référençant l'unité archivistique.
  * Il s'agit d'un tableau de chaînes de caractères.
  * Ne peut être vide. Il comprend au minimum le service versant déclaré dans le bordereau de transfert.
  * Cardinalité : 1-1

**"_sp":** service producteur responsable de l'unité archivistique, qui appartient à son fond propre.

  * Il s'agit du service producteur inscrit dans le bordereau de transfert lié au transfert de l'unité archivistique et déclaré dans la balise <OriginatingAgencyIdentifier> du message ArchiveTransfer.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"_ops" (operations)**: tableau contenant les identifiants d'opérations auxquelles cette unité archivistique a participé.

  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champ _id de l'opération enregistré dans la collection LogbookOperation.
  * Ne peut être vide.
  * Cardinalité : 1-1

**"_opi"** : identifiant de l'opération à l'origine de la création de cette unité archivistique.

  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champs _id de la collection LogbookOperation.
  * Ne peut être vide.
  * Cardinalité : 1-1

**"_unitType":** champ indiquant le type d'unité archivistique concerné.

  * Il s'agit d'une chaîne de caractères.
  * La valeur contenue doit être conforme à l'énumération UnitType. Celle-ci peut être :

      * INGEST : unité archivistique issue d'un SIP
      * FILING_UNIT : unité archivistique issue d'un plan de classement
      * HOLDING_UNIT : unité archivistique issue d'un arbre de positionnement

  * Cardinalité : 1-1

**"_up" (unit up):** tableau recensant les _id des unités archivistiques parentes (parents immédiats).

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID. Valeur du champ _id d'une unité archivistique enregistré dans la collection Unit.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_us":** tableau contenant la parentalité, c'est à dire l'ensemble des unités archivistiques parentes, indexé de la manière suivante : [ GUID1, GUID2, ... ].

  * Tableau de chaînes de 36 caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Vide pour la racine uniquement
  * Cardinalité : 1-1

**"_graph" :** Tableau des chemins de l'unité archivistique

  * Il s'agit d'un tableau contenant tous les chemins pour accéder à l'unité archivistique depuis les racines. Ces chemins sont composés sous la forme id1/id2/id3/.../idn Où chaque id est un identifiant d'unité archivistique. id1 étant l'unité courante et où idn est l'identifiant de l'unité de plus haut niveau.
  * Cardinalité 1-1

**"_us_sp":** Liste des unités archivistique parentes concernant tous les niveaux de parentalité

  * Il s'agit d'un tableau contenant les identifiants de tous les services producteurs de tous les parents liées à l'unité archivistique.
  * Il s'agit d'un tableau de chaînes de caractères.
  * Vide uniquement si l'unité archivistique n'a pas de parents
  * Cardinalité : 1-1

**"_uds":** tableau contenant la parentalité, c'est à dire l'ensemble des unités archivistiques parentes, ainsi que le niveau de profondeur relative.

  * Il s'agit d'un tableau de JSON.
  * Ces informations sont réunies dans le tableau sous la forme de clef/valeur, la clé étant la profondeur du parent (de type entier), la valeur étant elle même un tableau d'identifiant d'unité archivistique. Exemple d'une unité qui a un parent direct, lui même ayant deux parents.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

.. code-block:: json

  "1": [
      "aeaqaaaaamhad455abcwsalep4lzf2iaaada"
  ],
  "2": [
      "aeaqaaaaamhad455abcwsalep4lzf2iaaabq",
      "aeaqaaaaamhad455abcwsalep4lzf2iaaabq"
  ],


**"_min":** profondeur minimum de l'unité archivistique par rapport à une racine.

  * Calculée, cette profondeur correspond au minimum des profondeurs, quels que soient les racines concernées et les chemins possibles.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1


**"_max":** profondeur maximale de l'unité archivistique par rapport à une racine.

  * Calculée, cette profondeur correspond au maximum des profondeurs, quels que soient les racines concernées et les chemins possibles.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_glpd":** Date de la dernière modification du graph dont l'unité dépend

  * Il s'agit d'une date.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_elimination":** tableau contenant les résultats pour l'unité archivistique d'une opération d'analyse d'élimination

  * Champ peuplé par la solution logicielle Vitam au moment d’une indexation lors d'une phase d'analyse d'élimination.
  * Cardinalité : 1-1

  Ce bloc contient les clés suivantes :

  * "OperationId": GUID de l'opération d'élimination

     - Tableau de chaînes de 36 caractères
     - Ne peut être vide
     - Cardinalité : 1-1

  * "GlobalStatus": ce champ indique le statut de l'unité archivistique lors de son indexation

    - les valeurs ne peuvent être que DESTROY ou CONFLICT
    - Ne peut être vide.
    - Cardinalité : 1-1

  * "DestroyableOriginatingAgencies" : Service(s) producteur(s) pour lesquel(s) l'unité archivistique est éliminable

    - Il s’agit d’une chaîne de caractères.
    - Cardinalité : 0-n

  * "NonDestroyableOriginatingAgencies": Service(s) producteur(s) pour lesquel(s) l'unité archivistique n'est pas éliminable

    - Il s’agit d’une chaîne de caractères.
    - Cardinalité : 0-n

  * "ExtendedInfo" : tableau donnant des informations complémentaires dans les cas de CONFLICT

    - Cardinalité : 0-n

  * "ExtendedInfoType": ce champ indique les situations impliquant un CONFLICT

    - Il s’agit d’une chaîne de caractères.
    - Cardinalité : 0-n

      - les valeurs attendues dans ce tableau sont soit :

        - "KEEP_ACCESS_SP" l'unité archivistique n'est pas éliminable car l'accès est conservé pour un service producteur autre que le service producteur principal. Pour chaque cas de KEEP_ACCESS_SP l'unité parente est obligatoirement spécifiée avec son GUID, ainsi que le service producteur concerné.

          - "ParentUnitId": "guid",
          - "DestroyableOriginatingAgencies"

        - "ACCESS_LINK_INCONSISTENCY" l'unité archivistique n'est pas éliminable car sa suppression occasionnerait une incohérence dans le fonds d'archives. Pour chaque cas de ACCESS_LINK_INCONSISTENCY l'unité parente est obligatoirement spécifiée avec son GUID, ainsi que le service producteur concerné.

          - "ParentUnitId": "guid",
          - "DestroyableOriginatingAgencies":
