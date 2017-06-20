Base MetaData
#############

Collections contenues dans la base
===================================

La base contient les collections relatives aux métadonnées des unités archivistiques et des groupes d'objets.

Collection Unit
===============

Utilisation de la collection Unit
---------------------------------

La collection unit contient les informations relatives aux unités archivistiques.

Exemple de JSON
---------------

.. code-block:: json

  {
      "_id": "aeaqaaaaaahbjs5eabbboak4educsrqaaaaq",
      "_og": "",
      "_sps": [
        "FRAN_NP_009913"
    ],
    "_sp": "FRAN_NP_009913",
      "_mgt": {
          "StorageRule": [
              {
                  "Rule": "R1",
                  "StartDate": "2017-05-01",
                  "FinalAction": "RestrictAccess",
                  "EndDate": "2018-05-01"
              }
          ],
          "OriginatingAgency": "FRAN_NP_009913"
      },
      "DescriptionLevel": "RecordGrp",
      "Title": "AU5",
      "_ops": [
          "aedqaaaaachpuaosabkcgak4educq4yaaaaq"
      ],
      "_unitType": "INGEST",
      "_tenant": 0,
      "_max": 1,
      "_min": 1,
      "_up": [],
      "_nbc": 1,
      "_us": []
  }

Exemple de XML en entrée
------------------------

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour contribuer les champs du JSON. Il s'agit des informations situées entre les balises <ArchiveUnit>

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?>
  <ArchiveUnit id="AU5">
     <Management>
        <StorageRule>
           <Rule>R1</Rule>
           <StartDate>2017-05-01</StartDate>
           <FinalAction>RestrictAccess</FinalAction>
        </StorageRule>
     </Management>
     <Content>
        <DescriptionLevel>RecordGrp</DescriptionLevel>
        <Title>AU5</Title>
     </Content>
     <ArchiveUnit id="ref3">
        <ArchiveUnitRefId>AU3</ArchiveUnitRefId>
     </ArchiveUnit>
  </ArchiveUnit>

Détail du JSON
--------------

La structure de la collection Unit est composée de la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau conforme au standard SEDA v.2.0., c'est-à-dire toutes les balises se rapportant aux unités archivistiques.

Cette transposition se fait comme suit :

"_id": identifiant unique de l'unité archivistique.
    Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.

"_sps": services producteurs liées à l'unité archivistique.
  Il s'agit d'un tableau contenant tous les services producteurs référençant l'unité archivistique.
  Il s'agit d'un tableau de chaînes de caractère.

"_sp": service producteur d'Origine.
  Il s'agit du service producteur inscrit dans le bordereau lié au transfert d' l'unité archivistique.
  Il s'agit d'une chaîne de caractère.

"DescriptionLevel": niveau de description archivistique de l'unité archivistique.
    Il s'agit d'une chaîne de caractères.
    Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> dans le bordereau.

"Title": titre de l'unité archivistique.
    Il s'agit d'une chaîne de caractères.
    Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le bordereau.

"Description": description de l'unité archivistique.
    Il s'agit d'une chaîne de caractères.
    Ce champ est renseigné avec les informations situées entre les balises <description> de l'unité archivistique concernée dans le bordereau.

"XXXXX": des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le bordereau au niveau du Content de chaque unité archivistique.
    Se reporter à la documentation SEDA 2.0 descriptive et notamment le schéma ontology.xsdpour connaître la liste des métadonnées facultatives)

"_og" (objectGroup): identifiant du groupe d'objets référencé dans cette unité archivistique
    Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champs _id de la collection objectGroup.

"_ops" (operations): tableau contenant les identifiants d'opérations auxquelles cette unité archivistique a participé
    Il s'agit d'une chaîne de 36 caractères correspondant au GUID du champs _id de la collection logBookOpération.

"_unitType": champ indiquant le type d'unité archivistique concerné. Il s'agit d'une chaîne de caractères. La valeur contenue doit être conforme à l'énumération UnitType. Celle-ci peut être :
  * INGEST : unité d'archivistique issue d'un SIP
  * FILING_UNIT : unité d'archivistique issue d'un plan de classement
  * HOLDING_UNIT : unité d'archivistique issue d'un arbre de positionnement

"_tenant" (#tenant): identifiant du tenant
  Il s'agit d'un entier

"_max" : profondeur maximale de l'unité archivistique par rapport à une racine
  Calculée, cette profondeur est le maximum des profondeurs, quelles que soient les racines concernées et les chemins possibles

"_min" : profondeur minimum de l'unité archivistique par rapport à une racine
  Calculé, symétriquement le minimum des profondeurs, quelles que soient les racines concernées et les chemins possibles ;

"_up" : tableau recenssant les _id des unités archivistiques parentes (parents immédiats)
  Il s'agit d'une chaîne de 36 caractères correspondant au GUID. Valeur du champ _id de la collection Unit.

"_nbc" : nombre d'enfants immédiats de l'unité archivistique
  Il s'agit d'une chaîne de 36 caractères

"_us" : tableau contenant la parentalité, indexé [ GUID1, GUID2, ... }
  Tableau de chaînes de 36 caractères

"_uds" : tableau contenant la parentalité ainqi que le niveau de profondeur relative.
  Ces informations sont réunis dans le tableau sous la forme de clef/valeur. Exemple [{GUID1 : depth1}, {GUID2 : depth2}, ... }]
  Il s'agit d'un tableau de JSON

_profil : Type de document utilisé lors de l'entrée, correspond au ArchiveUnitProfile, le profil d'archivage utilisé lors de l'entrée
  Chaîne de caractères

"_mgt" : contient les balises reprises du bloc <Management> du bordereau pour cette unité archivistique :
  * "OriginatingAgency": service producteur déclaré dans le message ArchiveTransfer (OriginatingAgencyIdentifier)
  * "RuleType" [] : règles de gestion appliquées à cette unité archivistiques. Chaque tableau correspond à une catégorie de règle. Pour être valide, la catégorie de règle doit être présente dans la collection FileRules. Chaque tableau, optionnel, contient une à n règles. Chaque règle est composée des champs suivants :
  * "Rule": identifiant de la règle. Pour être valide, elle doit être contenue dans la collection FileRule, et correspondre à la valeur du champ RuleID de la collection FileRule.
  * "StartDate": date de début du calcul de l'échéance. Cette date est déclarée dans le message ArchiveTransfert ou ajoutée *a posteriori* par une modification.
  * "FinalAction": champ décrivant le sort final. Ce champ est disponible pour les règles de catégorie "StorageRule" et "AppraisalRule". La valeur contenue dans le champ doit être disponible soit dans l'énumération FinalActionAppraisalCodeType soit dans FinalActionStorageCodeType
  * "EndDate": Date de fin d'application de la règle; Cette valeur est issue d'un calcul réalisé par la solution logicielle Vitam consistant en l'ajout du délai correspondant à la règle dans la collection FileRules et le champ startDate.

Collection ObjectGroup
======================

Utilisation de la collection ObjectGroup
----------------------------------------

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de Json stocké en base
------------------------------

.. code-block:: json

  {
    "_id": "aebaaaaaaahbjs5eabbboak4d7shg4aaaaba",
    "_tenant": 0,
    "_profil": "",
    "FileInfo": {
        "Filename": "Filename0",
        "CreatingApplicationName": "CreatingApplicationName0",
        "CreatingApplicationVersion": "CreatingApplicationVersion0",
        "DateCreatedByApplication": "2006-05-04T18:13:51.0",
        "CreatingOs": "CreatingOs0",
        "CreatingOsVersion": "CreatingOsVersion0",
        "LastModified": "2006-05-04T18:13:51.0"
    },
    "_qualifiers": [{
        "qualifier": "PhysicalMaster",
            "_nbc": 1,
            "versions": [
                {
                    "_id": "aeaaaaaaaahbjs5eabbboak4d7shg7iaaaaq",
                    "DataObjectGroupId": "aebaaaaaaahbjs5eabbboak4d7shg4aaaaba",
                    "DataObjectVersion": "PhysicalMaster_1",
                    "PhysicalId": 123456789,
                    "PhysicalDimensions": {
                        "Width": {
                            "unit": "centimetre",
                            "value": 1.7
                        },
                        "Height": {
                            "unit": "centimetre",
                            "value": 21
                        },
                        "Diameter": {
                            "unit": "centimetre",
                            "value": 22
                        },
                        "Length": {
                            "unit": "centimetre",
                            "value": 29.7
                        },
                        "Thickness": {
                            "unit": "centimetre",
                            "value": 1.4
                        },
                        "Weight": {
                            "unit": "kilogram",
                            "value": 1
                        },
                        "NumberOfPage": 20
                    }
                }
            ]
        },
        {
            "qualifier": "BinaryMaster",
            "_nbc": 1,
            "versions": [
                {
                    "_id": "aeaaaaaaaahbjs5eabbboak4d7shg4aaaaaq",
                    "DataObjectGroupId": "aebaaaaaaahbjs5eabbboak4d7shg4aaaaba",
                    "DataObjectVersion": "BinaryMaster_1",
                    "FormatIdentification": {
                        "FormatLitteral": "Acrobat PDF 1.4 - Portable Document Format",
                        "MimeType": "application/pdf",
                        "FormatId": "fmt/18"
                    },
                    "FileInfo": {
                        "Filename": "Filename0",
                        "CreatingApplicationName": "CreatingApplicationName0",
                        "CreatingApplicationVersion": "CreatingApplicationVersion0",
                        "DateCreatedByApplication": "2006-05-04T18:13:51.0",
                        "CreatingOs": "CreatingOs0",
                        "CreatingOsVersion": "CreatingOsVersion0",
                        "LastModified": "2006-05-04T18:13:51.0"
                    },
                    "Size": 29403,
                    "Uri": "Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf",
                    "MessageDigest": "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266
                    c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7",
                    "Algorithm": "SHA-512"
                }
            ]
        }
    ],
    "_up": [
        "aeaqaaaaaahbjs5eabbboak4d7shg7qaaaaq"
    ],
    "_nbc": 0,
    "_ops": [
        "aedqaaaaachpuaosabkcgak4d7shenaaaaaq"
    ],
    "OriginatingAgency": "FRAN_NP_050056"
  }

Exemple de XML
--------------

Ci-après, la portion d'un bordereau (manifest.xml) utilisée pour contribuer les champ du JSON

::

  <BinaryDataObject id="ID8">
      <DataObjectGroupReferenceId>ID4</DataObjectGroupReferenceId>
      <DataObjectVersion>BinaryMaster_1</DataObjectVersion>
      <Uri>Content/ID8.txt</Uri>
      <MessageDigest algorithm="SHA-512">8e393c3a82ce28f40235d0870ca5b574ed2c90d831a73cc6bf2fb653c060c7f094fae941dfade786c826
      f8b124f09f989c670592bf7a404825346f9b15d155af</MessageDigest>
      <Size>30</Size>
      <FormatIdentification>
          <FormatLitteral>Plain Text File</FormatLitteral>
          <MimeType>text/plain</MimeType>
          <FormatId>x-fmt/111</FormatId>
      </FormatIdentification>
      <FileInfo>
          <Filename>BinaryMaster.txt</Filename>
          <LastModified>2016-10-18T21:03:30.000+02:00</LastModified>
      </FileInfo>
  </BinaryDataObject>

Détail des champs du JSON
---------------------------

"_id": identifiant du groupe d'objet.
  Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  Cet id est ensuite reporté dans chaque structure inculse

"_tenant": identifiant du tenant
  Il s'agit d'un entier

"_profil": typologie de document.
  Repris du nom de la balise présente dans le <Metadata> du <DataObjectPackage> du bordereau qui concerne le BinaryMaster.
  Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
  Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...)

"FileInfo": reprend le bloc FileInfo du BinaryMaster.
 L'objet de cette copie est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait détruite (selon les règles de conservation), car ces informations ne sauraient être maintenues de manière garantie dans les futures versions.

"_qualifiers": tableau de structures décrivant les objets inclus dans ce groupe d'objets.
  Il est composé comme suit :

  - "qualifier": Usage de l'objet.
    Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>. C'est la valeur "BinaryMaster" qui est reportée.
      - "nb": nombre d'objets correspondant à cet usage
      - "versions": tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du bordereau
          - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères corresppondant à un GUID.
          - "DataObjectGroupId": identifiant du groupe d'objets. Chaîne de 36 caractères.
          - "DataObjectVersion": version de l'objet par rapport à son usage.

      Par exemple, si on a *binaryMaster* sur l'usage, on aura au moins un objet *binarymaster_1*. Ces champs sont renseignés avec les valeurs récupérées dans les balises <DataObjectVersion> du bordereau.

      - "FormatIdentification": Contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel pronom et les valeurs que porte le document est faite.
          - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du message ArchiveTransfer.
          - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du message ArchiveTransfer ou des valeurs correspondant au format tel qu'identifié par la solution logicielle Vitam.
          - "FormatId" : PUID du format de l'objet. Il est défini par la solution logicielle Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK) et correspondant à la valeur du champ PUID de la collection FileFormat.

      - "FileInfo" : Contient les informations sur les fichiers.
          - "Filename": nom de l'objet
          - "CreatingApplicationName": Nom de l'application avec laquelle l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "CreatingApplicationVersion": Numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "CreatingOs": Système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le message ArchiveTransfer. *Ce champ est facultatif et n'est pas présent systématiquement*
          - "CreatingOsVersion": Version du système d'exploitation avec lequel l'objet a été créé. Ce champ est renseigné avec la métadonnées correspondante portée par le message ArchiveTransfer. *Ce champ et facultatif est n'est pas présent systématiquement*
          - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.
          - "Size": taille de l'objet (en octets). Ce champ contient un nombre entier.
      - "OtherMetadata": Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>.
        Ceci correspond à une extension du schéma SEDA du message  ArchiveTransfert.
      - "Uri": localisation du fichier correspondant à l'objet dans le SIP.
        Chaîne de caractères
      - "MessageDigest": empreinte du fichier correspondant à l'objet. La valeur est calculé par la solution logicielle Vitam.
        Chaîne de caractères
      - "Algorithm": Algorithme utilisé pour réaliser l'empreinte du fichier correspondant à l'objet.
        Chaîne de caractères
      - "_storage": Contient trois champs qui permettent d'identifier les offres  de stockage.
          - "strategyId": Identifiant de la stratégie de stockage.
          - "offerIds": Liste des offres de stockage pour une stratégie donnée
          - "_nbc": Nombre d'offres

"_up" (#unitup): tableau identifiant les unités archivistiques parentes
  Il s'agit d'un tableau de chaînes de 36 caractères correspondant à un GUID contenu à la valeur contenue dans le champ _id de la collection Unit.

"_nbc" (#nbobjects): nombre d'objets dans le groupe d'objet
  Il s'agit d'un entier.

"_ops" (#operations): tableau des identifiants d'opérations auxquelles ce GOT a participé
  Il s'agit d'un tableau de chaînes de 36 caractères correspondant à un GUID contenu à la valeur contenue dans le champ _id de la collection LogBookOperation.

"OriginatingAgency": service producteur déclaré dans le message ArchiveTransfer (OriginatingAgencyIdentifier)
  Il s'agit d'une chaîne de caractères.

"_sps": services producteurs liées au groupe d'objet.
  Il s'agit d'un tableau contenant tous les services producteurs référençant le groupe d'objet.
  Il s'agit d'un tableau de chaînes de caractère.
