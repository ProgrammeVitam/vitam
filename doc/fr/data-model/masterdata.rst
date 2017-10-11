Base MasterData
###############

Collections contenues dans la base
===================================

La base Masterdata contient les collections relatives aux référentiels utilisés par la solution logicielle Vitam.

Collection FileFormat
=====================

Utilisation de la collection FileFormat
---------------------------------------

La collection FileFormat permet de référencer et décrire les différents formats de fichiers ainsi que leur description. La collection est initialisée à partir de l'import du fichier de signature PRONOM, mis à disposition par The National Archive (UK).

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
    "_id": "aeaaaaaaaahbl62nabduoak3jc2zqciaadiq",
    "CreatedDate": "2016-09-27T15:37:53",
    "VersionPronom": "88",
     "PUID": "fmt/961",
    "Version": "2",
    "Name": "Mobile eXtensible Music Format",
    "Extension": [
        "mxmf"
    ],
    "HasPriorityOverFileFormatID": [
        "fmt/714"
    ],
    "MIMEType": "audio/mobile-xmf", 
    "Group": "",
    "Alert": false,
    "Comment": "",
    "_v": 0
  }


Exemple de la description d'un format dans le XML d'entrée
----------------------------------------------------------

Ci-après, la portion d'un fichier de signature (DROID_SignatureFile_VXX.xml) utilisée pour renseigner les champ du JSON

::

   <FileFormat ID="105" MIMEType="application/msword" Name="Microsoft Word for Macintosh Document" PUID="x-fmt/64" Version="4.0">
     <InternalSignatureID>486</InternalSignatureID>
     <Extension>mcw</Extension>
   </FileFormat>

Détail des champs du JSON stocké en base
------------------------------------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique du format.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"CreatedDate":** date de création de la version du fichier de signatures PRONOM utilisé pour initialiser la collection.

  * Il s'agit d'une date au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"  
  * Cardinalité : 1-1

**"VersionPronom":** numéro de version du fichier de signatures PRONOM utilisé.
    
    * Il s'agit d'un entier.
    * Le numéro de version de PRONOM est à l'origine déclaré dans le fichier de signature au niveau de la balise <FFSignatureFile> au niveau de l'attribut "version ".
    * Cardianlité : 1:1

Dans cet exemple, le numéro de version est 88 :

::

 <FFSignatureFile DateCreated="2016-09-27T15:37:53" Version="88" xmlns="http://www.nationalarchives.gov.uk/pronom/SignatureFile">

**"MIMEType":** MIMEtype correspondant au format de fichier.
    
    * Il s'agit d'une chaîne de caractères.
    * Elle est renseignée avec le contenu de l'attribut "MIMEType" de la balise <FileFormat>. Cet attribut est facultatif dans le fichier de signature.

**"HasPriorityOverFileFormatID":** liste des PUID des formats sur lesquels le format a la priorité.

  * Il s'agit d'un tableau de chaînes de caractères
  * Cardinalité : 0:1

::

  <HasPriorityOverFileFormatID>1121</HasPriorityOverFileFormatID>

Cet ID est ensuite utilisé dans Vitam pour retrouver le PUID correspondant.
    S'il existe plusieurs balises <HasPriorityOverFileFormatID> dans le fichier xml initial pour un format donné, alors les PUID seront stocké dans le JSON sous la forme suivante :

::

  "HasPriorityOverFileFormatID": [
      "fmt/714",
      "fmt/715",
      "fmt/716"
  ],

**"PUID":** identifiant unique du format au sein du référentiel PRONOM.
    
    * Il s'agit d'une chaîne de caractères.
    * Il est issu du champ "PUID" de la balise <FileFormat>. La valeur est composée du préfixe "fmt" ou "x-fmt", puis d'un nombre correspondant au numéro d'entrée du format dans le référentiel PRONOM. Les deux éléments sont séparés par un "/"
  * Cardinalité : 1-1

Par exemple

::

 x-fmt/64

Les PUID comportant un préfixe "x-fmt" indiquent que ces formats sont en cours de validation par The National Archives (UK). Ceux possédant un préfixe "fmt" sont validés.

"Version": version du format.
    
    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1

Exemples de formats :

::

 Version="3D Binary Little Endian 2.0"
 Version="2013"
 Version="1.5"

L'attribut "version" n'est pas obligatoire dans la balise <fileformat> du fichier de signature.

**"Name":** nom du format.
    
    * Il s'agit d'une chaîne de caractères.
    * Le nom du format est issu de la valeur de l'attribut "Name" de la balise <FileFormat> du fichier de signature.
    * Cardinalité : 1-1

**"Extension":** Extension(s) du format.
    
    * Il s'agit d'un tableau de chaînes de caractères.
    * Il contient les valeurs situées entre les balises <Extension> elles-mêmes encapsulées entre les balises <FileFormat>. Le champ <Extension> peut-être multivalué. Dans ce cas, les différentes valeurs situées entre les différentes balises <Extensions> sont placées dans le tableau et séparées par une virgule.
    * Cardinalité : 1-1

Par exemple, pour le format dont le PUID est : fmt/918 on la XML suivant :

::

 <FileFormat ID="1723" Name="AmiraMesh" PUID="fmt/918" Version="3D ASCII 2.0">
     <InternalSignatureID>1268</InternalSignatureID>
     <Extension>am</Extension>
     <Extension>amiramesh</Extension>
     <Extension>hx</Extension>
   </FileFormat>

Les valeurs des balises <Extensions> seront stockées de la façon suivante dans le JSON :

::

 "Extension": [
      "am",
      "amiramesh",
      "hx"
  ],

**"Alert":** alerte sur l'obsolescence du format.
    
  * Il s'agit d'un booléen dont la valeur est par défaut placée à false.
  * Cardinalité : 0-1

**"Comment":** commentaire.
	
  * Il s'agit d'une chaîne de caractères.
	* C'est un champ propre à la solution logicielle Vitam.
  * Cardinalité : 0-1

**"Group":** Champ permettant d'indiquer le nom d'une famille de format.
	
  * Il s'agit d'une chaîne de caractères.
  * C'est un champ propre à la solution logicielle Vitam.
  * Cardinalité : 0-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection FileRules
====================

Utilisation de la collection FileRules
--------------------------------------

La collection FileRules permet de stocker unitairement les différentes règles de gestion utilisées dans la solution logicielle Vitam pour calculer les échéances associées aux unités archivistiques.

Cette collection est alimentée par l'import d'un fichier CSV contenant l'ensemble des règles.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

 {
   "_id": "aeaaaaaaaahbl62nabduoak3jc4avsyaaaha",
   "_tenant": 0,
   "RuleId": "ACC-00011",
   "RuleType": "AccessRule",
   "RuleValue": "Communicabilité des informations portant atteinte au secret de la défense nationale",
   "RuleDescription": "Durée de communicabilité applicable aux informations portant atteinte au secret de la défense nationale\nL’échéance est calculée à partir de la date du document ou du document le plus récent inclus dans le dossier",
   "RuleDuration": "50",
   "RuleMeasurement": "YEAR",
   "CreationDate": "2017-04-07",
   "UpdateDate": "2017-04-07",
   "_v": 0
  }


Structure du fichier d'import
-----------------------------

.. csv-table::
  :header: "RuleId","RuleType","RuleValue","RuleDescription","RuleDuration","RuleMeasurement"

  "Id de la règle","Type de règle","Intitulé de la règle","Description de la règle","Durée","Unité de mesure de la durée"

La liste des type de règles disponibles est en annexe 5.4.

Les valeurs renseignées dans la colonne unité de mesure doivent correspondre à une valeur de l'énumération RuleMeasurementEnum, à savoir :

  * MONTH
  * DAY
  * YEAR
  * SECOND

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"RuleId":** identifiant unique par tenant de la règle dans le référentiel utilisé.
    
  * Il s'agit d'une chaîne de caractères.
  * La valeur est reprise du champs RuleId du fichier d'import. Par commodité, les exemples sont composés d'un préfixe puis d'une nombre, séparés par un tiret, mais ce formalisme n'est pas obligatoire.
  * Cardinalité : 1-1

Par exemple :

::

 ACC-00027

Les préfixes indiquent le type de règle dont il s'agit. La liste des valeurs pouvant être utilisées comme préfixes ainsi que les types de règles auxquelles elles font référence sont disponibles en annexe.

**"RuleType":** Type de règle.

  * Il s'agit d'une chaîne de caractères.
  * Il correspond à la valeur située dans la colonne RuleType du fichier d'import. Les valeurs possibles pour ce champ sont indiquées en annexe.
  * Cardinalité : 1-1

**"RuleValue":** Intitulé de la règle.

  * Il s'agit d'une chaîne de caractères.
  * Elle correspond à la valeur de la colonne RuleValue du fichier d'import.
  * Cardinalité : 1-1

**"RuleDescription":** description de la règle.
    
  * Il s'agit d'une chaîne de caractères.
  * Elle correspond à la valeur de la colonne RuleDescriptionRule du fichier d'import.
  * Cardinalité : 1-1

**"RuleDuration":**  Durée de la règle.
    
  * Il s'agit d'un entier compris entre 0 et 999.
  * Associé à la valeur indiqué dans RuleMeasurement, il permet de décrire la durée d'application de la règle de gestion. Il correspond à la valeur de la colonne RuleDuration du fichier d'import.
  * Cardinalité : 1-1

**"RuleMeasurement":**  Unité de mesure de la durée décrite dans la colonne RuleDuration du fichier d'import.
    
    * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de l'énumération RuleMeasurementEnum, à savoir :

      * MONTH
      * DAY
      * YEAR
      * SECOND
        
  * Cardinalité : 1-1

**"CreationDate":** date de création de la règle dans la collection FileRule.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"UpdateDate":** Date de dernière mise à jour de la règle dans la collection FileRules.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection IngestContract
=========================

Utilisation de la collection
----------------------------

La collection IngestContract permet de référencer et décrire unitairement les contrats d'entrée.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
--------------------------------------------------------------------

::

    {
      "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
      "_tenant": 0,
      "Name": "SIA archives nationales",
      "Identifier": "IC-000012",
      "Description": "Contrat d'accès - SIA archives nationales",
      "Status": "ACTIVE",
      "CreationDate": "2017-04-10T11:30:33.798",
      "LastUpdate": "2017-04-10T11:30:33.798",
      "ActivationDate": "2017-04-10T11:30:33.798",
      "DeactivationDate": null,
      "ArchiveProfiles": [
          "ArchiveProfile8"
      ],
      "FilingParentId": "aeaqaaaaaagbcaacaax56ak35rpo6zqaaaaq",
      "_v": 0
    }


Exemple d'un fichier d'import de contrat
----------------------------------------

Les contrats d'entrée sont importés dans la solution logicielle Vitam sous la forme d'un fichier JSON.

::

    [
        {
            "Name":"Contrat Archives Départementales",
            "Description":"Test entrée - Contrat Archives Départementales",
            "Status" : "ACTIVE",
        },
        {
            "Name":"Contrat Archives Nationales",
            "Description":"Test entrée - Contrat Archives Nationales",
            "Status" : "INACTIVE",
            "ArchiveProfiles": [
              "ArchiveProfile8"
            ],
            "FilingParentId" : "aeaqaaaaaahkwxukabcg2ak4u2qq7eaaaaaq"
        }
    ]

Les champs à renseigner obligatoirement à la création d'un contrat sont :

* Name
* Description

Un fichier d'import peut décrire plusieurs contrats.

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name":** Nom du contrat d'entrée, unique par tenant.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier": Champ obligatoire peuplé par Vitam** Identifiant signifiant donné au contrat.
  
  * Il est constitué du préfixe "IC-" suivi d'une suite de 6 chiffres. Par exemple : IC-007485.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du contrat d'entrée.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Status":** statut du contrat.

  * Il s'agit d'une chaîne de caractères.
  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"CreationDate":** date de création du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"LastUpdate":** date de dernière mise à jour du contrat dans la collection IngestContract.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 0-1

**"DeactivationDate":** date de désactivation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 0-1

**"ArchiveProfiles":** liste des profils d'archivage pouvant être utilisés par le contrat d'entrée.
  
  * Tableau de chaînes de caractères correspondant à la valeur du champ Identifier de la collection Profile.
  * Cardinalité : 0-1

**"FilingParentId":** point de rattachement automatique des SIP en application de ce contrat correspondant à l'id d’une unité archivistique dans le plan de classement
  
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID dans le champ _id de la collection Unit.
  * Cardinalité : 0-1


**L'unité archivistique concernée doit être de type FILING_UNIT afin que l'opération aboutisse**

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection AccessContract
=========================

Utilisation de la collection
----------------------------

La collection AccessContract permet de référencer et de décrire unitairement les contrats d'accès.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

    {
    "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
    "_tenant": 0,
    "Name": "SIA archives nationales",
    "Identifier": "AC-000009",
    "Description": "Contrat d'accès - SIA archives nationales",
    "Status": "ACTIVE",
    "CreationDate": "2017-04-10T11:30:33.798",
    "LastUpdate": "2017-04-10T11:30:33.798",
    "ActivationDate": "2017-04-10T11:30:33.798",
    "DeactivationDate": null,
    "OriginatingAgencies":["FRA-56","FRA-47"],
    "DataObjectVersion": ["PhysicalMaster", "BinaryMaster", "Dissemination", "Thumbnail", "TextContent"],
    "WritingPermission": true,
    "EveryOriginatingAgency": false,
    "EveryDataObjectVersion": true,
    "_v": 0,
    "RootUnits": [
        "aeaqaaaaaahxunbaabg3yak6urend2yaaaaq",
        "aeaqaaaaaahxunbaabg3yak6urendoqaaaaq"
    ]
    }

Exemple d'un fichier d'import de contrat d'accès
------------------------------------------------

Les contrats d'entrée sont importés dans la solution logicielle Vitam sous la forme d'un fichier Json.

::

    [
        {
            "Name":"Archives du Doubs",
            "Description":"Accès Archives du Doubs",
            "Status" : "ACTIVE",
            "ActivationDate":"10/12/2016",
            "OriginatingAgencies":["FRA-56","FRA-47"]
        },
        {
            "Name":"Archives du Calvados",
            "Description":"Accès Archives du Calvados",
            "Status" : "ACTIVE",
            "ActivationDate":"10/12/2016",
            "DeactivationDate":"10/12/2016",
            "OriginatingAgencies":["FRA-54","FRA-64"]
        }
    ]

Les champs à renseigner obligatoirement à la création d'un contrat sont :

* Name
* Description

Un fichier d'import peut décrire plusieurs contrats.

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique par tenant.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"_tenant": Champs obligatoire peuplé par Vitam** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Cardinalité : 1-1

**"Name": Champ obligatoire** Nom du contrat d'entrée unique par tenant.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier" : Champs obligatoire peuplé par Vitam ou par l'application créant le contrat selon la configuration** identifiant signifiant donné au contrat.

  * Il est consituté du préfixe "AC-" suivi d'une suite de 6 chiffres s'il est peuplé par Vitam. Par exemple : AC-001223.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description": Champ obligatoire** Description du contrat d'accès.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Status": Champ bligatoire** statut du contrat.

  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"CreationDate": Champ obligatoire peuplé par Vitam** date de création du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"LastUpdate": Champ obligatoire peuplé par Vitam**  date de dernière mise à jour du contrat dans la collection AccesContrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"ActivationDate": Champ obligatoire peuplé par Vitam** date d'activation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"DeactivationDate": Champ obligatoire peuplé par Vitam** date de désactivation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"OriginatingAgencies":** services producteurs pour lesquels le détenteur du contrat peut consulter les archives.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Cardinalité : 0-n

**"DataObjectVersion":** usages d'un groupe d'objet auxquels le détenteur d'un contrat a access.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Cardinalité : 0-1

**"WritingPermission": Champ obligatoire** droit d'écriture. 

  * Peut être true ou false. S'il est true, le détenteur du contrat peut effectuer des mises à jour.
  * Cardinalité : 1-1

**"EveryOriginatingAgency": Champ obligatoire** droit de consultation sur tous les services producteurs.

  * Il s'agit d'un booléen.
  * Si la valeur est à true, alors le détenteur du contrat peut accéder aux archives de tous les services producteurs.
  * Cardinalité : 1-1

**"EveryDataObjectVersion": Champ obligatoire** droit de consultation sur tous les usages.

  * Il s'agit d'un booléen.
  * Si la valeur est à true, alors le détenteur du contrat peut accéder à tous les types d'usages.
  * Cardinalité : 1-1

**"_v": Champs obligatoire peuplé par Vitam**  version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"RootUnits":** Liste des noeuds de consultation auxquels le détenteur du contrat a accès. Si aucun noeud n'est spécifié, alors l'utilisateur a accès à tous les noeuds.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Cardinalité : 0-1

Collection Profile
===================

Utilisation de la collection
----------------------------

La collection Profile permet de référencer et décrire unitairement les profils SEDA.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
    "_id": "aegaaaaaaehlfs7waax4iak4f52mzriaaaaq",
    "_tenant": 1,
    "Identifier": "PR-000003",
    "Name": "ArchiveProfile0",
    "Description": "aDescription of the Profile",
    "Status": "ACTIVE",
    "Format": "XSD",
    "CreationDate": "2016-12-10T00:00",
    "LastUpdate": "2017-05-22T09:23:33.637",
    "ActivationDate": "2016-12-10T00:00",
    "DeactivationDate": "2016-12-10T00:00",
    "_v": 1,
    "Path": "1_profile_aegaaaaaaehlfs7waax4iak4f52mzriaaaaq_20170522_092333.xsd"
  }

Exemple d'un fichier d'import de profils
----------------------------------------

Un fichier d'import peut décrire plusieurs profils.

::

  [
    {
      "Name":"ArchiveProfile0",
      "Description":"Description of the Profile",
      "Status":"ACTIVE",
      "Format":"XSD"
    },
      {
      "Name":"ArchiveProfile1",
      "Description":"Description of the profile 2",
      "Status":"ACTIVE",
      "Format":"RNG"
    }
  ]

Les champs à renseigner obligatoirement à la création d'un contrat sont :

* Name
* Description
* Format

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"_tenant": Champs obligatoire peuplé par Vitam** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Cardinalité : 1-1

"Identifier": Indique l'identifiant signifiant du profil SEDA.

  * Si Vitam est maître dans la création de cet identifiant, il est alors consituté du préfixe "PR-" suivi d'une suite de 6 chiffres. Par exemple : PR-001573.
  * Si Vitam est esclave, a
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Name": Indique le nom du profil SEDA.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Description": Description du profil SEDA.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Status": statut du contrat.

  * Peut être ACTIVE ou INACTIVE
  * Si ce champ n'est pas défini lors de la création de l'enregistrement
  * Cardinalité : 1-1

**"Format": Champ obligatoire** Indique le format attendu pour le fichier décrivant les règles du profil d'archivage.
  
  * Il s'agit d'une chaîne de caractères devant correspondre à l'énumération ProfileFormat.
  * Cardinalité : 1-1
  
**"CreationDate": Champ obligatoire peuplé par Vitam** date de création du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"LastUpdate": Champ obligatoire peuplé par Vitam**  date de dernière mise à jour du contrat dans la collection AccesContrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"ActivationDate": Champ obligatoire peuplé par Vitam** date d'activation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"DeactivationDate": Champ obligatoire peuplé par Vitam** date de désactivation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

**"_v": Champ obligatoire peuplé par Vitam**  version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"Path": Champ contribué par Vitam lors d'un import de fichier xds ou RNG** Indique le chemin pour accéder au fichier du profil d'archivage.

  * Chaîne de caractères.
  * Le type de fichier doit être 
  * Cardinalité : 0-1 

Collection Context
==================

Utilisation de la collection
----------------------------

La collection Context permet de stocker unitairement les contextes applicatifs

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
      "_id": "aegqaaaaaahkwxukabjosak4rp3kqkaaaaaq",
      "Name": "Contexte pour application 1",
      "Status": true,
      "Permissions": [
          {
              "_tenant": 1,
              "AccessContracts": [
                  "AC-000017",
                  "AC-000060"
              ],
              "IngestContracts": [
                  "IC-000060"
              ]
          },
          {
              "_tenant": 0,
              "AccessContracts": [],
              "IngestContracts": []
          }
      ],
      "CreationDate": "2017-10-02T13:00:15.742",
      "LastUpdate": "2017-10-02T13:00:15.744",
      "SecurityProfile": "admin-security-profile",
      "Identifier": "CT-000001",
      "_v": 3
  }

Il est possible de mettre plusieurs contextes dans un même fichier, sur le même modèle que les contrats d'entrées ou d'accès par exemple. On pourra noter que le contexte est multi-tenant et définit chaque tenant de manière indépendante.

Les champs à renseigner obligatoirement à la création d'un contexte sont :

* Name
* Permissions. La valeur de Permissions peut cependant être vide : "Permissions : []"

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name": Champ obligatoire** nom du contexte, qui doit être unique sur la plateforme.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":** identifiant signifiant donné au contexte.
  
  * Il est consituté du préfixe "CT-" suivi d'une suite de 6 chiffres. Par exemple : CT-001573.
  * Il s'agit d'une chaîne de caractères. 
  * Cardinalité : 1-1

**"Status":** statut du contexte. Il peut être "true" ou "false" et a la valeur par défaut : "false". Selon son statut :

  * Il s'agit d'un booléen
  * "true" : le contexte est actif
  * "false" : le contexte est inactif
  * Cardinalité : 1-1

**"Permissions":** Début du bloc appliquant les permissions à chaque tenant.

  * C'est un mot clé qui n'a pas de valeur associée.
  * Il s'agit d'une chaîne de caractères. 
  * Cardinalité : 1-1 

**"_tenant": Champs obligatoire peuplé par Vitam** Il s'agit de l'identifiant du tenant dans lequel vont s'appliquer des permissions.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1 

**"AccessContracts":** tableau d'identifiants de contrats d'accès appliqués sur le tenant.

  * Il s'agit d'un tableau de chaines de caractères
  * Cardinalité : 0-1

**"IngestContracts":** tableau d'identifiants de contrats d'entrées appliqués sur le tenant.

  * Il s'agit d'un tableau de chaines de caractères
  * Cardinalité : 0-1

**"CreationDate":** "CreationDate": date de création du contexte. 
  
  * Il s'agit d'une date au format ISO 8601
  * Cardinalité : 1-1 

**"LastUpdate":** date de dernière modification du contexte. 
  
  * Il s'agit d'une date au format ISO 8601
  * Cardinalité : 1-1 

**"SecurityProfile":** Nom du profil de sécurité utilisé par le contexte. Ce nom doit correspondre à celui d'un profil de sécurité enregistré dans la collection SecurityProfile.

  * Il s'agit d'une chaîne de caractères
  * Cardinalité : 1-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection AccessionRegisterSummary
===================================

Utilisation de la collection
----------------------------

Cette collection contient une vue macroscopique des fonds pris en charge dans la solution logicielle Vitam.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
      "_id": "aefaaaaaaahkkoiuabp4sak3mmoj5vaaaaaq",
      "_tenant": 0,
      "OriginatingAgency": "Vitam",
      "TotalObjects": {
          "total": 27,
          "deleted": 0,
          "remained": 27
      },
      "TotalObjectGroups": {
          "total": 27,
          "deleted": 0,
          "remained": 27
      },
      "TotalUnits": {
          "total": 57,
          "deleted": 0,
          "remained": 57
      },
      "ObjectSize": {
          "total": 18292981,
          "deleted": 0,
          "remained": 18292981
      },
      "creationDate": "2017-04-12T17:01:11.764",
      "_v": 1
  }

Exemple de la description dans le XML d'entrée
-----------------------------------------------

Les seuls élements issus du  message ArchiveTransfer, utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés dans le bloc <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"_tenant": Champs obligatoire peuplé par Vitam** Il s'agit de l'identifiant du tenant dans lequel vont s'appliquer des permissions.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1 

**"OriginatingAgency":** la valeur de ce champ est une chaîne de caractère.
  
  * Ce champ est la clef primaire et sert de concaténation pour toutes les entrées effectuées sur ce producteur d'archives. Récupère la valeur contenue dans le bloc <OriginatinAgencyIdentifier> du message ArchiveTransfer.
  * Cardinalité : 1-1 

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

On récupère la valeur FRAN_NP_051314.

**"TotalObjectGroups": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre de groupes d'objets du service producteur par état
    (total, deleted et remained)

    - "total": Nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1 

**"TotalObjects": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre d'objets du service producteur par état
    (total, deleted et remained)

    - "total": Nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1 

**"TotalUnits": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre d'unités archivistiques du service producteur par état
    (total, deleted et remained)

    - "total": Nombre total d'unités archivistiques pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1 
  
**"ObjectSize": Champs obligatoire peuplé par Vitam** Contient la répartition du volume total des fichiers du service producteur par état
    (total, deleted et remained)

    - "total": Volume total en octets des fichiers pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Volume actualisé en octets des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1 
    
**"creationDate": Champs obligatoire peuplé par Vitam**  Date d'inscription du producteur d'archives concerné dans le registre des fonds. 

  * La date est au format ISO 8601
  * Cardinalité : 0-1
    
**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection AccessionRegisterDetail
==================================

Utilisation de la collection
----------------------------

Cette collection a pour vocation de référencer l'ensemble des informations sur les opérations d'entrées réalisées pour un service producteur. A ce jour, il y a autant d'enregistrements que d'opérations d'entrées effectuées pour ce service producteur, mais des évolutions sont d'ores et déjà prévues.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
      "_id": "aedqaaaaakhpuaosabkcgak4ebd7deiaaaaq",
      "_tenant": 2,
      "OriginatingAgency": "FRAN_NP_009734",
      "SubmissionAgency": "FRAN_NP_009734",
      "ArchivalAgreement": "ArchivalAgreement0",
      "EndDate": "2017-05-19T12:36:52.572+02:00",
      "StartDate": "2017-05-19T12:36:52.572+02:00",
      "Status": "STORED_AND_COMPLETED",
      "LastUpdate": "2017-05-19T12:36:52.572+02:00",
      "TotalObjectGroups": {
          "total": 0,
          "deleted": 0,
          "remained": 0
      },
      "TotalUnits": {
          "total": 11,
          "deleted": 0,
          "remained": 11
      },
      "TotalObjects": {
          "total": 0,
          "deleted": 0,
          "remained": 0
      },
      "ObjectSize": {
          "total": 0,
          "deleted": 0,
          "remained": 0
      },
      "OperationIds": [
          "aedqaaaaakhpuaosabkcgak4ebd7deiaaaaq"
      ],
    "_v": 5
  }

Exemple de la description dans le XML d'entrée
----------------------------------------------

Les seuls élements issus du message ArchiveTransfer utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés dans le bloc <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"_tenant": Champs obligatoire peuplé par Vitam** identifiant du tenant.

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"OriginatingAgency":** Contient l'identifiant du service producteur.
  Il est issu du le bloc <OriginatinAgencyIdentifier>.

Par exemple :

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314
La valeur est une chaîne de caractère.

"SubmissionAgency": Contient l'identifiant du service versant.
    Il est contenu entre les baslises <SubmissionAgencyIdentifier>.

Par exemple pour

::

  <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>

On récupère la valeur FRAN_NP_005761.
La valeur est une chaîne de caractères.

Ce champ est facultatif dans le bordereau. S'il' est absente ou vide, alors la valeur contenue dans le champ <OriginatingAgencyIdentifier> est reportée dans ce champ.

"ArchivalAgreement": Contient le contrat utilisé pour réaliser l'entrée.
  Il est contenu entre les balises <ArchivalAgreement> et correspond à la valeur contenue dans le champ Name de la collection IngestContract.

Par exemple pour

::

  <ArchivalAgreement>ArchivalAgreement0</ArchivalAgreement>

On récupère la valeur ArchivalAgreement0.
La valeur est une chaîne de caractères.

**"StartDate": Champs obligatoire peuplé par Vitam** date de la première opération d'entrée correspondant à l'enregistrement concerné. 

  * La date est au format ISO 8601
  * Cardinalité : 1-1
  
**"EndDate": Champs obligatoire peuplé par Vitam** date de la dernière opération d'entrée correspondant à l'enregistrement concerné. 

  * La date est au format ISO 8601
  * Cardinalité : 1-1
  
**"Status": Champs obligatoire peuplé par Vitam** indication sur l'état des archives concernées par l'enregistrement.

  * Il s'agit d'une chaîne de caractères
  * Cardinalité : 1-1
    
**"TotalObjectGroups": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre de groupes d'objets du fonds par état pour l'opération journalisée (total, deleted et remained) :
    - "total": Nombre total de groupes d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimés ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre de groupes d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1

**"TotalUnits": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre d'unités archivistiques du fonds par état pour l'opération journalisée (total, deleted et remained) :
    - "total": Nombre total d'unités archivistiques pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'unités archivistiques conservées dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1

**"TotalObjects": Champs obligatoire peuplé par Vitam** Contient la répartition du nombre d'objets du fonds par état pour l'opération journalisée (total, deleted et remained) :
    - "total": Nombre total d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimés ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un Json
  * Cardinalité : 1-1

**"ObjectSize": Champs obligatoire peuplé par Vitam** Contient la répartition du volume total des fichiers du fonds par état pour l'opération journalisée (total, deleted et remained) :
    - "total": Volume total en octet des fichiers pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimés ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Volume total en octets des fichiers conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    
  * Il s'agit d'un Json
  * Cardinalité : 1-1

Collection VitamSequence
=========================

Utilisation de collection
-------------------------

Cette collection permet de générer des identifiants signifiants pour les enregistrements des collections suivantes :

* IngestContract
* AccesContract
* Context
* Profil
  
Ces identifiants sont composés d'un préfixe de deux lettres, d'un tiret et d'une suite de six chiffres. Par exemple : IC-027593. Il sont reportés dans les champs Identifier des collections concernées. 

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

::

  {
    "_id": "aeaaaaaaaahkwxukabqteak4q5mtmdyaaaaq",
    "Name": "AC",
    "Counter": 44,
    "_tenant": 1,
    "_v": 0
  }

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name": Champs obligatoire peuplé par Vitam** préfixe. Il s'agit du préfixe utilisé pour générer un identifiant signifiant. La valeur contenue dans ce champ doit correspondre à la map du service VitamCounterService.java. La liste des valeurs possibles est détaillée en annexe 5.6.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Counter": Champs obligatoire peuplé par Vitam** numéro incrémental. Il s'agit du dernier numéro utilisé pour générer un identifiant signifiant.

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"_tenant": Champs obligatoire peuplé par Vitam** information sur le tenant. Il s'agit de l'identifiant du tenant utilisant l'enregistrement

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection SecurityProfile
==========================

Utilisation de collection
-------------------------

Cette collection contient les profils de sécurité mobilisés par les contextes.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

{
    "_id": "aegqaaaaaaeucszwabglyak64gjmgbyaaaba",
    "Identifier": "SEC_PROFILE-000002",
    "Name": "demo-security-profile",
    "FullAccess": false,
    "Permissions": [
        "securityprofiles:create",
        "securityprofiles:read",
        "securityprofiles:id:read",
        "securityprofiles:id:update",
        "accesscontracts:read",
        "accesscontracts:id:read",
        "contexts:id:update"
    ],
    "_v": 1
}

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Identifier": Champs obligatoire peuplé par Vitam** identifiant signifiant donné au profil de sécurité.
  
  * Il est consituté du préfixe "SEC_PROFILE-" suivi d'une suite de 6 chiffres. Par exemple : SEC_PROFILE-001573.
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name":** nom du profil de sécurité, qui doit être unique sur la plateforme.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"FullAccess":** mode super-administrateur. Donne toutes les permissions.
  
  * Il s'agit d'un booléen.
  * S'il est à false, le mode super-admin n'est pas activé et les valeurs du champ permission sont utilisées. S'il est à true, le champ permission doit être vide.
  * Cardinalité : 1-1

"Permissions": décrit l'ensemble des permissions auxquelles le profil de sécurité donne accès. Chaque API externe contient un verbe OPTION qui retourne la liste des services avec leur description et permissions associées.
  
  * Il s'agit d'un tableau de chaînes de caractères.
  * Cardinalité : 0-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

Collection Agencies
===================

Utilisation de collection
-------------------------

La collection IngestContract permet de référencer et décrire unitairement les services agent.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
--------------------------------------------------------------------

::

  {
    "_id": "aehqaaaaaaehiikjabagmak6zz4tzpqaaaaq",
    "Name": "Identifier1",
    "Description": "une description de service agent",
    "Identifier": "Identifier1",
    "_tenant": 0,
    "_v": 0
  }

Détail des champs
-----------------

**"_id": Champs obligatoire peuplé par Vitam** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cardinalité : 1-1

**"Name":** nom du service agent, qui doit être unique sur la plateforme.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Description": Description du service agent.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

"Identifier":  identifiant signifiant donné au service agent.
  Le contenu de ce champs est obligatoirement renseignée dans le Json permettant de créer le contrat. En aucun cas Vitam peut être maître sur la création de cet identifiant comme cela peut être le cas pour d'autres données référentielles.
  Il s'agit d'une chaîne de caractères. 

**"_tenant": Champs obligatoire peuplé par Vitam** information sur le tenant. Il s'agit de l'identifiant du tenant utilisant l'enregistrement

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"_v": Champs obligatoire peuplé par Vitam** version de l'objet décrit

  * Il s'agit d'un entier.
  * Cardinalité : 1-1
