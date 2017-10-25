Base MetaData
#############

Collections contenues dans la base
===================================

La base Metadata contient les collections relatives aux métadonnées des unités archivistiques (collection Unit) et des groupes d'objets (collection ObjectGroup). 

Collection Unit
===============

Utilisation de la collection Unit
---------------------------------

La collection Unit contient les informations relatives aux unités archivistiques. 

Exemple de XML en entrée
------------------------

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
----------------------------------------------

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
      "_sps": [
          "RATP"
      ],
      "_sp": "RATP",
      "_ops": [
          "aedqaaaaacfeavznabdrgak7pkvyhgiaaaaq"
      ],
      "_unitType": "INGEST",
      "_v": 1,
      "_tenant": 0,
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
          "aeaqaaaaaahccnklabtgyak7pkvypgqaaaba": 3,
          "aeaqaaaaaahccnklabtgyak7pkvypgqaaacq": 2,
          "aeaqaaaaaahccnklabtgyak7pkvypgyaaaba": 1
      },
      "_storage": {
          "_nbc": [
              2
          ],
          "strategyId": "default",
          "offerIds": [
              "vitam-iaas-app-02.int",
              "vitam-iaas-app-03.int"
          ]
      }
  }

Détail du JSON
--------------

La structure de la collection Unit est composée de la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau de transfert conforme au standard SEDA v.2.0., c'est-à-dire toutes les balises se rapportant aux unités archivistiques.

Cette transposition se fait comme suit :

**"_id":** identifiant unique de l'unité archivistique.
    
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_og" (objectGroup):** identifiant du groupe d'objets représentant cette unité archivistique.
    
  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champ _id de la collection objectGroup.
  * Cardinalité : 0-1

**"_sps":** services producteurs liés à l'unité archivistique suite à un rattachement et ayant des droits d'accès sur celle-ci.
  
  * Il s'agit d'un tableau contenant les identifiants de tous les services producteurs référençant l'unité archivistique.
  * Il s'agit d'un tableau de chaînes de caractères.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_sp":** service producteur d'origine déclaré lors de la prise en charge de l'unité archivistique par la solution logicielle Vitam.
  
  * Il s'agit du service producteur inscrit dans le bordereau lié au transfert de l'unité archivistique déclaré via une extension du schéma <OtherManagementAbstract>, la balise <OriginatingAgencyIdentifier>.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"DescriptionLevel":** niveau de description archivistique de l'unité archivistique.
    
  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> dans le bordereau.
  * Cardinalité : 1-1

**"Title":** titre de l'unité archivistique.
  
  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le bordereau de transfert.
  * Cardinalité : 1-1

**"Titles":** titres de l'unité archivistique par langue.
    
  * Il s'agit d'un JSON.
  * Les titres sont organisés sous la forme de clef / valeur, la clef étant l'indicatif de la langue, la valeur le titre. Par exemple : "fr": "Ceci est un titre."
  * Cardinalité : 0-1

**"Description":** description de l'unité archivistique.

  * Il s'agit d'une chaîne de caractères.
  * Ce champ est renseigné avec les informations situées entre les balises <Description> de l'unité archivistique concernée dans le bordereau.
  * Cardinalité : 0-1

**"Descriptions":** description de l'unité archivistique par langue.
    
  * Il s'agit d'un JSON
  * Les descriptions sont organisées sous la forme de clef / valeur, la clef étant l'indicatif de la langue, la valeur la description. Par exemple : "fr": "Ceci est une description."
  * Cardinalité : 0-N

**"XXXXX":** des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le bordereau au niveau du Content de chaque unité archivistique.
    
  * Se reporter à la documentation descriptive du SEDA 2.0 et notamment le schéma ontology.xsd pour connaître la liste des métadonnées facultatives).

**"_ops"** (operations): tableau contenant les identifiants d'opérations auxquelles cette unité archivistique a participé.

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
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.
    
  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_max":** profondeur maximale de l'unité archivistique par rapport à une racine.
      
  * Calculée, cette profondeur correspond au maximum des profondeurs, quelles que soient les racines concernées et les chemins possibles.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_min":** profondeur minimum de l'unité archivistique par rapport à une racine.
      
  * Calculée, cette profondeur correspond au le minimum des profondeurs, quels que soient les racines concernées et les chemins possibles.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_up" (unit up):** tableau recenssant les _id des unités archivistiques parentes (parents immédiats).
      
  * Il s'agit d'une chaîne de 36 caractères correspondant au GUID. Valeur du champ _id de la collection Unit.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_nbc" :** nombre d'enfants immédiats de l'unité archivistique.
      
  * Il s'agit d'une chaîne de 36 caractères.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_us":** tableau contenant la parentalité, indexé de la manière suivante : [ GUID1, GUID2, ... ].
      
  * Tableau de chaînes de 36 caractères.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_uds":** tableau contenant la parentalité ainsi que le niveau de profondeur relative.
      
  * Ces informations sont réunies dans le tableau sous la forme de clef/valeur. Exemple [{GUID1 : depth1}, {GUID2 : depth2}, ... }].   
  * Il s'agit d'un tableau de JSON.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**_profil:** Profil d'archivage utilisé lors de l'entrée.
      
  * Correspond à ArchiveProfile, le profil d'archivage utilisé lors de l'entrée.   
  * Chaîne de caractères.
  * Cardinalité : 0-1

**"_mgt": contient les balises reprises du bloc <Management> du bordereau pour cette unité archivistique**.

  * "OriginatingAgency": service producteur déclaré dans le message ArchiveTransfer (OriginatingAgencyIdentifier)
  * "RuleType" : catégorie de règles de gestion appliquées à cette unité archivistique. Chaque catégorie contient un tableau de règles de gestion et des paramètres d'héritage de règles. Pour être valide, la catégorie de règle doit être présente dans la collection FileRules.
  * "Rules": tableau, optionnel, contient une à n règles. Chaque règle est composée des champs suivants :

      * "Rule": identifiant de la règle. Pour être valide, elle doit être contenue dans la collection FileRules, et correspondre à la valeur du champ RuleId de la collection FileRules.
      * "StartDate": date de début du calcul de l'échéance. Cette date est déclarée dans le message ArchiveTransfer ou ajoutée *a posteriori* par une modification.
      * "FinalAction": champ décrivant le sort final. Ce champ est disponible pour les règles de catégorie "StorageRule" et "AppraisalRule". La valeur contenue dans le champ doit être disponible soit dans l'énumération FinalActionAppraisalCodeType soit dans FinalActionStorageCodeType.
      * "ClassificationLevel" : champ référençant le niveau de protection. Ce champ est disponible pour les règles de la catégorie "ClassificationRule".
      * "ClassificationOwner" : champ indiquant l'émetteur de la classification. Ce champ est disponible pour les règles de la catégorie "ClassificationRule".
      * "ClassificationReassessingDate" : date de réévaluation de la classification. Ce champ est disponible pour les règles de la catégorie "ClassificationRule".
      * "NeedReassessingAuthorization" : champ booléen indiquant si une autorisation humaine est nécessaire pour réévaluer la classification. Ce champ est disponible pour les règles de la catégorie "ClassificationRule".
      * "NeedAuthorization" : champ booléen indiquant si une autorisation humaine est nécessaire pour vérifier ou valider les opérations de gestion des ArchiveUnit.
      * "EndDate": date de fin d'application de la règle. Cette valeur est issue d'un calcul réalisé par la solution logicielle Vitam consistant en l'ajout du délai correspondant à la règle dans la collection FileRules et le champ startDate.

  * "Inheritance" : paramètres d'héritage des règles de gestion.

    * "PreventInheritance" : champ booléen indiquant si les règles de gestion de la même catégorie ne doivent pas être héritées d'un parent.
    * "PreventRulesId" : tableau de d'identifiants de règles de gestion qui ne doivent pas être héritées d'un parent.
      
  * Cardinalité : 1-1

Collection ObjectGroup
======================

Utilisation de la collection ObjectGroup
----------------------------------------

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de XML
--------------

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour compléter les champs du JSON.

::

  <PhysicalDataObject id="ID109">
      <DataObjectGroupReferenceId>ID0009</DataObjectGroupReferenceId>
      <DataObjectVersion>PhysicalMaster</DataObjectVersion>
      <PhysicalId>1 Num 1/191-3</PhysicalId>
      <PhysicalDimensions>
          <Height unit="centimetre">10.5</Height>
          <Length unit="centimetre">14.8</Length>
          <Thickness unit="micrometre">350</Thickness>
          <Weight unit="gram">3</Weight>
      </PhysicalDimensions>
      <Extent>1 carte imprimée</Extent>
      <Dimensions>10,5cm x 14,8cm</Dimensions>
      <Color>Noir et blanc</Color>
      <Framing>Paysage</Framing>
      <Technique>Phototypie</Technique>
  </PhysicalDataObject>
  <BinaryDataObject id="ID9">
      <DataObjectGroupId>ID0009</DataObjectGroupId>
      <DataObjectVersion>BinaryMaster</DataObjectVersion>
      <Uri>Content/1NUM_9.JPG</Uri>
      <MessageDigest algorithm="SHA-512">0e0cec05a1d72ee5610eaa5afbc904c012d190037cbc827d08272102cdecf0226efcad122b86e7699f767c661c9f3702379b8c2cb01c4f492f69deb200661bb9</MessageDigest>
      <Size>7702</Size>
      <FormatIdentification>
          <FormatLitteral>JPEG File Interchange Format</FormatLitteral>
          <MimeType>image/jpeg</MimeType>
          <FormatId>fmt/43</FormatId>
      </FormatIdentification>
      <FileInfo>
          <Filename>1NUM_9.JPG</Filename>
      </FileInfo>
      <Metadata>
          <Image>
              <Dimensions>117x76</Dimensions>
              <Width>117px</Width>
              <Height>76px</Height>
              <VerticalResolution>96ppp</VerticalResolution>
              <HorizontalResolution>96ppp</HorizontalResolution>
              <ColorDepth>24</ColorDepth>
          </Image>
      </Metadata>
  </BinaryDataObject>

Exemple de JSON stocké en base
------------------------------

Les champs présentés dans l'exemple ci-après ne font pas état de l'exhaustivité des champs disponibles dans le SEDA. Ceux-ci sont référencés dans la documentation SEDA disponible au lien suivant : https://redirect.francearchives.fr/seda/api_v2/doc.html

.. code-block:: json

  {
      "_id": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
      "_tenant": 0,
      "_profil": "Image",
      "FileInfo": {
          "Filename": "1NUM_9.JPG"
      },
      "_qualifiers": [
          {
              "qualifier": "PhysicalMaster",
              "_nbc": 1,
              "versions": [
                  {
                      "_id": "aeaaaaaaaafgsz3wabcugak7ube6dzqaaaca",
                      "DataObjectGroupId": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
                      "DataObjectVersion": "PhysicalMaster_1",
                      "PhysicalId": "1 Num 1/191-3",
                      "PhysicalDimensions": {
                          "Height": {
                              "unit": "centimetre",
                              "value": 10.5
                          },
                          "Length": {
                              "unit": "centimetre",
                              "value": 14.8
                          },
                          "Thickness": {
                              "unit": "micrometre",
                              "value": 350
                          },
                          "Weight": {
                              "unit": "gram",
                              "value": 3
                          }
                      },
                      "Extent": "1 carte imprimée",
                      "Dimensions": "10,5cm x 14,8cm",
                      "Color": "Noir et blanc",
                      "Framing": "Paysage",
                      "Technique": "Phototypie",
                      "_storage": {
                          "_nbc": 0,
                          "offerIds": [],
                          "strategyId": "default"
                      }
                  }
              ]
          },
          {
              "qualifier": "BinaryMaster",
              "_nbc": 1,
              "versions": [
                  {
                      "_id": "aeaaaaaaaafgsz3wabcugak7ube6dxyaaaba",
                      "DataObjectGroupId": "aebaaaaaaafgsz3wabcugak7ube6dxyaaabq",
                      "DataObjectVersion": "BinaryMaster_1",
                      "FormatIdentification": {
                          "FormatLitteral": "JPEG File Interchange Format",
                          "MimeType": "image/jpeg",
                          "FormatId": "fmt/43"
                      },
                      "FileInfo": {
                          "Filename": "1NUM_9.JPG"
                      },
                      "Metadata": {
                          "Image": {
                              "Dimensions": "117x76",
                              "Width": "117px",
                              "Height": "76px",
                              "VerticalResolution": "96ppp",
                              "HorizontalResolution": "96ppp",
                              "ColorDepth": 24
                          }
                      },
                      "Size": 7702,
                      "Uri": "Content/1NUM_9.JPG",
                      "MessageDigest": "0e0cec05a1d72ee5610eaa5afbc904c012d190037cbc827d08272102cdecf0226efcad122b86e7699f767c661c9f3702379b8c2cb01c4f492f69deb200661bb9",
                      "Algorithm": "SHA-512",
                      "_storage": {
                          "_nbc": 2,
                          "offerIds": [
                              "vitam-iaas-app-02.int",
                              "vitam-iaas-app-03.int"
                          ],
                          "strategyId": "default"
                      }
                  }
              ]
          }
      ],
      "_up": [
          "aeaqaaaaaafgsz3wabcugak7ube6d4qaaaaq"
      ],
      "_nbc": 0,
      "_ops": [
          "aedqaaaaachxqyktaai4aak7ube557iaaaaq"
      ],
      "_sp": "Vitam",
      "_sps": [
          "Vitam"
      ],
      "_storage": {
          "_nbc": [
              2
          ],
          "offerIds": [
              "vitam-iaas-app-02.int",
              "vitam-iaas-app-03.int"
          ],
          "strategyId": "default"
      },
      "_v": 1
  }

Détail des champs du JSON
---------------------------

**"_id":** identifiant du groupe d'objet.
      
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.
      
  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_profil":** catégorie de l'objet.
      
  * Repris du nom de la balise présente dans la partie <Metadata> du <DataObjectPackage> du bordereau au niveau du le BinaryMaster.
  Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
  Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...).
  * Cardinalité : 1-1

**"FileInfo":** : informations sur le fichier constituant l'objet-données numérique de référence.

  * reprend le bloc FileInfo du BinaryMaster.
  * L'objet de ce bloc est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait éliminé (selon les règles de conservation).
  * Cardinalité : 1-1

**"_qualifiers":** tableau de structures décrivant les objets inclus dans ce groupe d'objets. Il est composé comme suit :

  - "qualifier": usage de l'objet.
    Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>, c'est la valeur "BinaryMaster" qui est reportée.

    - "nb": nombre d'objets correspondant à cet usage.
    - "versions": tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du bordereau.

      - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères corresppondant à un GUID.
      - "DataObjectGroupId": identifiant du groupe d'objets, composé d'une chaîne de 36 caractères.
      - "DataObjectVersion": version de l'objet par rapport à son usage.

      Par exemple, si on a *binaryMaster* sur l'usage, on aura au moins un objet *binarymaster_1*. Ces champs sont renseignés avec les valeurs récupérées dans les balises <DataObjectVersion> du bordereau.

      - "FormatIdentification": contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel pronom et les valeurs que porte le document est faite.

        - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du message ArchiveTransfer.
        - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du message ArchiveTransfer ou des valeurs correspondant au format tel qu'identifié par la solution logicielle Vitam.
        - "FormatId" : PUID du format de l'objet. Il est défini par la solution logicielle Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK) et correspondant à la valeur du champ PUID de la collection FileFormat.

      - "FileInfo": Contient les informations sur les fichiers.
          
          - "Filename": nom de l'objet.
          - "CreatingApplicationName": nom de l'application avec laquelle l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*.
          - "CreatingApplicationVersion": numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "CreatingOs": système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "CreatingOsVersion": Version du système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. ``Exemple : 2016-08-19T16:36:07.942+02:00`` Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.
          - "Size": taille de l'objet (en octet). Ce champ contient un nombre entier.
      
      - "OtherMetadata": Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>, de l'une extension du schéma SEDA du message  ArchiveTransfer.
      - "Uri": localisation du fichier correspondant à l'objet dans le SIP.
        Chaîne de caractères
      - "MessageDigest": empreinte du fichier correspondant à l'objet. La valeur est calculée par la solution logicielle Vitam.
        Chaîne de caractères
      - "Algorithm": algorithme utilisé pour réaliser l'empreinte du fichier correspondant à l'objet.
        Chaîne de caractères
      - "_storage": contient trois champs qui permettent d'identifier les offres  de stockage.
          
          - "strategyId": identifiant de la stratégie de stockage.
          - "offerIds": liste des offres de stockage pour une stratégie donnée
          - "_nbc": nombre d'offres.

**"_up" (unitup):** tableau identifiant les unités archivistiques représentée par ce groupe d'objets.
        
  * Il s'agit d'un tableau de chaînes de 36 caractères correspondant au GUID contenu dans le champ _id de la collection Unit.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_nbc" (nbobjects):** nombre d'objets dans le groupe d'objets.
        
  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_ops" (operations):** tableau des identifiants d'opérations auxquelles ce GOT a participé.
        
  * Il s'agit d'un tableau de chaînes de 36 caractères correspondant au GUID contenu contenue dans le champ _id de la collection LogBookOperation.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_sp":** service producteur déclaré dans le message ArchiveTransfer (OriginatingAgencyIdentifier)
        
  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_sps":** service producteur d'origine déclaré lors de la prise en charge du groupe d'objet par la solution logicielle Vitam.
        
  * Il s'agit d'un tableau contenant tous les services producteurs référençant le groupe d'objet.    
  * Il s'agit d'un tableau de chaînes de caractère.
  * Champ peuplé par Vitam.
  * Ne peut être vide
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit
        
  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1