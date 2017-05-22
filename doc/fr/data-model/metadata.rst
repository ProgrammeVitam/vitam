Base MetaData
#############

Collections contenues dans la base
===================================

Il s'agit des collections relatives aux métadonnées des archives et des objets numériques.

Collection Unit
===============

Utilisation de la collection Unit
---------------------------------

La colection unit contient les informations relatives aux ArchiveUnit.

Exemple de JSON
---------------

::

  {
      "_id": "aeaqaaaaaahbjs5eabbboak4educsrqaaaaq",
      "_og": "",
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

::

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

La structure de la collection Unit est composée de la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau conforme au standard SEDA v.2.0., c'est-à-dire toutes les balises se rapportant aux ArchiveUnit. Cette transposition se fait comme suit :

"_id" (#id): Identifiant unique de l'unité archivistique.
    Chaîne de 36 caractères.

"DescriptionLevel": La valeur de champ est une chaine de caractères.
    Il s'agit du niveau de description archivistique de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <DescriptionLevel> dans le manifest.

"Title": La valeur de ce champ est une chaine de caractères. Il s'agit du titre de l'ArchiveUnit.
    Ce champ est renseigné avec les valeurs situées entre les balises <Title> dans le manifest.

"Description": La valeur contenue dans ce champ est une chaîne de caractères.
    Ce champ est renseigné avec les informations situées entre les balises <description> de l'archiveUnit concernée dans le manifest.

"XXXXX" : Des champs facultatifs peuvent être contenus dans le JSON lorsqu'ils sont renseignés dans le boredereau SEDA au niveau du Content de chaque unité archivistique.
    (CF SEDA 2.0 descriptive pour connaître la liste des métadonnées facultatives)

"_og" (#object): identifiant du groupe d'objets référencé dans cette unité archivistique
    Chaîne de 36 caractères.

"_ops" (#operations): tableau contenant les identifiants d'opérations auxquelles ce Unit a participé

"_unitType": champs indiquant le type d'unité archivistique concerné. La valeur contenue doit être conforme à l'énumération UnitType. Celle-ci peut être :
  * INGEST
  * FILING_UNIT
  * HOLDING_UNIT

"_tenant" (#tenant): il s'agit de l'identifiant du tenant

"_max" (ne devrait pas être visible): profondeur maximale de l'unité archivistique par rapport à une racine
    Calculé, cette profondeur est le maximum des profondeurs, quelles que soient les racines concernées et les chemins possibles

"_min" (ne devrait pas être visible): profondeur minimum de l'unité archivistique par rapport à une racine
    Calculé, symétriquement le minimum des profondeurs, quelles que soient les racines concernées et les chemins possibles ;

"_up" (#unitups): est un tableau qui recense les _id des unités archivistiques parentes (parents immédiats)

"_nbc" (#nbunits): nombre d'enfants immédiats de l'unité archivistique

"_uds" (ne devrait pas être visible): tableau contenant la parentalité, non indexé et pas dans Elasticseatch exemple { GUID1 : depth1, GUID2 : depth2, ... } ; chaque depthN indique la distance relative entre l'unité archivistique courante et l'unité archivistique parente dont le GUID est précisé.

"_us" (#allunitups): tableau contenant la parentalité, indexé [ GUID1, GUID2, ... }

_profil (#type): Type de document utilisé lors de l'entrée, correspond au ArchiveUnitProfile, le profil d'archivage utilisé lors de l'entrée

"_mgt" (#management): contient les balises reprises du bloc <Management> du bordereau pour cette unité archivistique :
  * "OriginatingAgency": le service producteur déclaré dans le bordereau
  * "RuleType" [] : ce tablleau est optionnel. Il contient les règles de gestions appliquées à cette unité archivistiques. Chaque tableau correspond à une catégorie de règle. Pour être valide, la catégorie de règle doit être présente dans la collection Rules. Chaque tableau contient une à n règles. Chaque règle est composée des champs suivants :
  *  "Rule": ce champ contient le nom de la règle. Pour être valide, elle doit être contenue dans la collection Rule, et correspondre à la catégorie déclarée par le nom du tableau qui la contient
  *  "StartDate": date de début. Cette date est déclarée dans le bordereau. Si aucune date n'est renseigne, c'est la date de l'import qui sera ajouté ici.
  *  "FinalAction": champ décrivant le sort final. Ce champ est disponible pour les règles de type "StorageRule" et "AppraisalRule". La valeur contenu doit le champ doit être disponible soit dans l'énumération FinalActionAppraisalCodeType soit dans FinalActionStorageCodeType
  *  "EndDate": Date de fin d'application de la règle; Cette valeur est issue d'un calcul réalisé par VITAM consistant en l'ajout du délais correspondant à la règle dans la collection Rules et le champ startDate.

Collection ObjectGroup
======================

Utilisation de la collection ObjectGroup
----------------------------------------

La collection ObjectGroup contient les informations relatives aux groupes d'objets.

Exemple de Json stocké en base
------------------------------

::

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
    "_qualifiers": {
        "PhysicalMaster": {
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
        "BinaryMaster": {
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
    },
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
------------------------

"_id" (#id): identifiant du groupe d'objet. Il s'agit d'une chaîne de 36 caractères.
Cet id est ensuite reporté dans chaque structure inculse

"_tenant" (#tenant): identifiant du tenant

"_profil" (#type): repris du nom de la balise présente dans le <Metadata> du <DataObjectPackage> du manifeste qui concerne le BinaryMaster.
Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur.
Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...)

"FileInfo" : reprend le bloc FileInfo du BinaryMaster ; l'objet de cette copie est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait détruite (selon les règles de conservation), car ces informations ne sauraient être maintenues de manière garantie dans les futures versions.

"_qualifiers" (#qualifiers): est une structure qui va décrire les objets inclus dans ce groupe d'objet. Il est composé comme suit :

- [Usage de l'objet. Ceci correspond à la valeur contenue dans le champ <DataObjectVersion> du bordereau. Par exemple pour <DataObjectVersion>BinaryMaster_1</DataObjectVersion>. C'est la valeur "BinaryMaster" qui est reportée.
    - "nb": nombre d'objets de cet usage
    - "versions" : tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du bordereau
        - "_id": identifiant de l'objet. Il s'agit d'une chaîne de 36 caractères.
        - "DataObjectGroupId" : Référence à l'identifiant objectGroup. Chaine de 36 caractères.
        - "DataObjectVersion" : version de l'objet par rapport à son usage.

    Par exemple, si on a *binaryMaster* sur l'usage, on aura au moins un objet *binarymaster_1*, *binaryMaster_2*. Ces champs sont renseignés avec les valeurs situées entre les balises <DataObjectVersion>.

    - "FormatIdentification": Contient trois champs qui permettent d'identifier le format du fichier. Une vérification de la cohérence entre ce qui est déclaré dans le XML, ce qui existe dans le référentiel pronom et les valeurs que porte le document est faite.
      - "FormatLitteral" : nom du format. C'est une reprise de la valeur située entre les balises <FormatLitteral> du XML
      - "MimeType" : type Mime. C'est une reprise de la valeur située entre les balises <MimeType> du XML.
      - "FormatId" : PUID du format de l'objet. Il est défini par Vitam à l'aide du référentiel PRONOM maintenu par The National Archives (UK).
    - "FileInfo" :
      - "Filename" : nom de l'objet
      - "CreatingApplicationName": Chaîne de caractères. Contient le nom de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingApplicationVersion": Chaîne de caractères. Contient le numéro de version de l'application avec laquelle le document a été créé. Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOs": Chaîne de caractères. Contient le nom du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnée correspondante portée par le fichier. *Ce champ est facultatif et n'est pas présent systématiquement*
      - "CreatingOsVersion": Chaîne de caractères. Contient le numéro de version du système d'exploitation avec lequel le document a été créé.  Ce champ est renseigné avec la métadonnées correspondante portée par le fichier. *Ce champ et facultatif est n'est pas présent systématiquement*
      - "LastModified" : date de dernière modification de l'objet au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"Ce champ est optionnel, et est renseigné avec la métadonnée correspondante portée par le fichier.
      - "Size": Ce champ contient un nombre entier. taille de l'objet (en octets).
    - "OtherMetadata": Contient une chaîne de caractères. Champ disponible pour ajouter d'autres métadonnées metier (Dublin Core, IPTC...). Ce champ est renseigné avec les valeurs contenues entre les balises <OtherMetadata>. Ceci correspond à une extension du SEDA.
    - "Uri": localisation du fichier dans le SIP
    - "MessageDigest": empreinte du fichier. La valeur est calculé par Vitam.
    - "Algorithm": ce champ contient le nom de l'algorithme utilisé pour réaliser l'empreinte du document.

- "_up" (#unitup): [] : tableau d'identifiant des unités archivistiques parentes
- "_tenant" (#tenant): identifiant du tenant
- "_nbc" (#nbobjects): nombre d'objets dans ce groupe d'objet
- "_ops" (#operations): [] tableau des identifiants d'opérations pour lesquelles ce GOT a participé
