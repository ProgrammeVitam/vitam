Base MasterData
###############

Collections contenues dans la base
===================================

Il s'agit des collections relatives aux référentiels utilisés par Vitam.

Collection Formats (FileFormat)
===============================

Utilisation de la collection Formats (FileFormat)
--------------------------------------------------

La collection format permet de stocker les différents formats de fichiers ainsi que leur description.

Exemple de JSON stocké en base
------------------------------

::

 {
  "_id": "aeaaaaaaaahbl62nabduoak3jc2zqciaadiq",
  "CreatedDate": "2016-09-27T15:37:53",
  "VersionPronom": "88",
  "Version": "2",
  "HasPriorityOverFileFormatID": [
      "fmt/714"
  ],
  "MIMEType": "audio/mobile-xmf",
  "Name": "Mobile eXtensible Music Format",
  "Group": "",
  "Alert": false,
  "Comment": "",
  "Extension": [
      "mxmf"
  ],
  "PUID": "fmt/961"
}


Exemple de la description d'un format dans le XML d'entrée
----------------------------------------------------------

Ci-après, la portion d'un bordereau (DROID_SignatureFile_VXX.xml) utilisée pour renseigner les champ du JSON

::

   <FileFormat ID="105" MIMEType="application/msword" Name="Microsoft Word for Macintosh Document" PUID="x-fmt/64" Version="4.0">
     <InternalSignatureID>486</InternalSignatureID>
     <Extension>mcw</Extension>
   </FileFormat>

Détail des champs du JSON stocké en base
---------------------------------------

"_id": Il s'agit de l'identifiant unique du format dans VITAM.
    C'est une chaine de caractères composée de 36 signes.

"CreatedDate": Il s'agit la date de création de la version du fichier de signatures PRONOM.
    Il est utilisé pour alimenter l’enregistrement correspondant au format dans Vitam (balise DateCreated dans le fichier).
    Le format de la date correspond à la norme ISO 8601.

"VersionPronom": Il s'agit du numéro de version du fichier de signatures PRONOM utilisé.
    Ce chiffre est toujours un entier. Le numéro de version de pronom est à l'origine déclaré dans le XML au niveau de la balise <FFSignatureFile> au niveau de l'attribut "version ".

Dans cet exemple, le numéro de version est 88 :

::

 <FFSignatureFile DateCreated="2016-09-27T15:37:53" Version="88" xmlns="http://www.nationalarchives.gov.uk/pronom/SignatureFile">

"MIMEType": Ce champ contient le MIMEtype du format de fichier.
    C'est une chaine de caractères renseignée avec le contenu de l'attribut "MIMEType" de la balise <FileFormat>. Cet attribut est facultatif dans le XML.

"HasPriorityOverFileFormatID" : Liste des PUID des formats sur lesquels le format a la priorité.

::

  <HasPriorityOverFileFormatID>1121</HasPriorityOverFileFormatID>

Cet ID est ensuite utilisé dans Vitam pour retrouver le PUID correspondant.
    S'il existe plusieurs balises <HasPriorityOverFileFormatID> dans le xml pour un format donné, alors les PUID seront stocké dans le JSON sou la forme suivante :

::

  "HasPriorityOverFileFormatID": [
      "fmt/714",
      "fmt/715",
      "fmt/716"
  ],

"PUID": ce champ contient le PUID du format.
    Il s'agit de l'identifiant unique du format au sein du référentiel pronom. Il est issu du champ "PUID" de la balise <FileFormat>. La valeur est composée du préfixe fmt ou x-fmt, puis d'un nombre correspondant au numéro d'entrée du format dans le référentiel pronom. Les deuéléments sont séparés par un "/"

Par exemple

::

 x-fmt/64

Les PUID comportant un préfixe "x-fmt" indiquent que ces formats sont en cours de validation par The National Archives (UK). Ceux possédant un préfixe "fmt" sont validés.

"Version": Ce champ contient la version du format.
    Il s'agit d'une chaîne de caractère.

Exemples de formats :

::

 Version="3D Binary Little Endian 2.0"
 Version="2013"
 Version="1.5"

L'attribut "version" n'est pas obligatoire dans la balise <fileformat> du XML.

"Name": Il s'agit du nom du format.
    Le champ contient une chaîne de caractère. Le nom du format est issu de la valeur de l'attribut "Name" de la balise <FileFormat> du XML d'entrée.

"Extension" : Ce champ est un tableau.
    Il contient les valeurs situées entre les balises <Extension> elles-mêmes encapsulées entre les balises <FileFormat>. Le champ <Extension> peut-être multivalué. Dans ce cas, les différentes valeurs situées entre les différentes balises <Extensions> sont placées dans le tableau et séparées par une virgule.

Par exemple, pour le format PUID : fmt/918 on la XML suivant :

::

 <FileFormat ID="1723" Name="AmiraMesh" PUID="fmt/918" Version="3D ASCII 2.0">
     <InternalSignatureID>1268</InternalSignatureID>
     <Extension>am</Extension>
     <Extension>amiramesh</Extension>
     <Extension>hx</Extension>
   </FileFormat>

Les valeurs des balises extensions seront stockées de la façon suivante dans le JSON :

::

 "Extension": [
      "am",
      "amiramesh",
      "hx"
  ],

"Alert": Alerte sur l'obsolescence du format.
    C'est un booléen dont la valeur est par défaut placée à False.

"Comment": Ce champ n'est pas renseigné avec une valeur issue du XML.
    C'est un champ propre à VITAM qui contient une chaîne de caractère.

"Group": Ce champ n'est pas renseigné avec une valeur issue du XML.
    C'est un champ propre à VITAM qui contient une chaîne de caractère.

Collection Règles de gestion (FileRules)
=========================================

Utilisation de la collection règles de gestions
-----------------------------------------------

La collection règles de gestion permet de stocker unitairement les différentes règles de gestion du réferentiel.

Exemple de JSON stocké en base
------------------------------

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
   "UpdateDate": "2017-04-07"
}

Colonne du csv comprenant les règles de gestion
-----------------------------------------------

================ ================= ======================= =========================== =============== ===============================
RuleId            RuleType          RuleValue               RuleDescription             RuleDuration     RuleMeasurement
---------------- ----------------- ----------------------- --------------------------- --------------- -------------------------------
Id de la règle    Type de règle     Intitulé de la règle    Description de la règle     Durée            Unité de mesure de la durée
================ ================= ======================= =========================== =============== ===============================

La liste des type de règle disponibles est en annexe 5.4
Les valeurs renseignées dans la colonne unité de mesure doivent correspondre à une valeur de l'énumération RuleMeasurementEnum, à savoir :
* MOUNTH
* DAY
* YEAR
* SECOND

Détail des champs
-----------------

"_id": Identifiant unique par tenant de la règle de gestion généré dans VITAM.
    C'est une chaîne de caractère composée de 36 caractères.

"RuleId": Il s'agit de l'identifiant de la règle dans le référentiel utilisé.
    Par commodité, les exemples sont composés d'un Préfixe puis d'une nombre séparés par un tiret, mais ce formalisme n'est pas obligatoire.

Par exemple :

::

 ACC-00027

Les préfixes indiquent le type de règle dont il s'agit. La liste des valeurs pouvant être utilisée comme préfixe ainsi que les types de règles auxquelles elles font référence sont disponibles en annexe.

"RuleType": *Champ obligatoire* Il s'agit du type de règle.
    Il correspond à la valeur située dans la colonne RuleType du fichier csv référentiel. Les valeurs possibles pour ce champ sont indiquées en annexe.

"RuleValue": *Champ obligatoire* Chaîne de caractères décrivant l'intitulé de la règle.
    Elle correspond à la valeur située dans la colonne RuleValue du fichier csv référentiel.

"RuleDescription": Chaîne de caractère permettant de décrire la règle.
    Elle correspond à la valeur située dans la colonne RuleDescriptionRule du fichier csv référentiel.

"RuleDuration": *Champ obligatoire* Chiffre entier compris entre 0 et 9999.
    Associé à la valeur "RuleMeasurement", il permet de décrire la durée d'application de la règle de gestion. Il correspond à la valeur située dans la colonne RuleDuration du fichier csv référentiel.

"RuleMeasurement": *Champ obligatoire* Correspond à l'unité de mesure de la durée décrite dans le champ "RuleDuration".

"CreationDate": Date de création de la règle

"UpdateDate": Date de mise à jour de la règle
       - Pour l'instant identique à la date de création. Ces deux dates sont mises à jour à chaque import de référentiel.

Collection IngestContract
=========================

Utilisation de la collection
----------------------------

La collection IngestContract permet de stocker unitairement les contrats d'entrée.

Exemple de JSON stocké en base
------------------------------

::

    {
      "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
      "_tenant": 0,
      "Name": "SIA archives nationales",
      "Description": "Contrat d'accès - SIA archives nationales",
      "Status": "ACTIVE",
      "CreationDate": "2017-04-10T11:30:33.798",
      "LastUpdate": "2017-04-10T11:30:33.798",
      "ActivationDate": "2017-04-10T11:30:33.798",
      "DeactivationDate": null
      "ArchiveProfiles": [
          "ArchiveProfile8"
      ]
    }

Exemple d'un fichier implémentant des contrats d'entrée envoyé au format JSON
------------------------------------------------------------------------------

L'exemple suivant est un JSON contenant deux contrats d'entrée :

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
            ]
        }
    ]

Les champs à renseigner obligatoirement à la création d'un contrat sont :
* Name
* Description

Détail des champs
-----------------

"_id": identifiant unique. Il s'agit d'une chaîne de 36 caractères.

"_tenant": nom du tenant

"Name" : Unique par tenant. nom du contrat d'entrée. Il s'agit d'une chaîne de caractères.

"Description": description du contrat d'entrée. Il s'agit d'une chaîne de caractères.

"Status": statut du contrat. Peut être ACTIVE ou INACTIVE

"CreationDate": date de création du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"LastUpdate": date de dernière mise à jour du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"ActivationDate": date d'activation. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"DeactivationDate": date de désactivation du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"ArchiveProfiles": Tableau de chaînes de caractères. Contient la liste des profils d'archivage pouvant être utilisés par le contrat d'entrée.

Collection AccessContract
=========================

Utilisation de la collection
----------------------------

La collection AccessContract permet de stocker unitairement les contrats d'accès.

Exemple de JSON stocké en base
------------------------------

::

    {
    "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
    "_tenant": 0,
    "Name": "SIA archives nationales",
    "Description": "Contrat d'accès - SIA archives nationales",
    "Status": "ACTIVE",
    "CreationDate": "2017-04-10T11:30:33.798",
    "LastUpdate": "2017-04-10T11:30:33.798",
    "ActivationDate": "2017-04-10T11:30:33.798",
    "DeactivationDate": null,
    "OriginatingAgencies":["FRA-56","FRA-47"]
    }

Les champs à renseigner obligatoirement à la création d'un contrat sont :
* Name
* Description

Exemple d'un fichier implémentant des contrats d'accès envoyé au format JSON
------------------------------------------------------------------------------

L'exemple suivant est un JSON contenant deux contrats d'accès :

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

Détail des champs
-----------------

"_id": identifiant unique. Il s'agit d'une chaîne de 36 caractères.

"_tenant": nom du tenant

"Name" : Unique par tenant. nom du contrat d'accès. Il s'agit d'une chaîne de caractères.

"Description": description du contrat d'accès. Il s'agit d'une chaîne de caractères.

"Status": statut du contrat. Peut être ACTIVE ou INACTIVE

"CreationDate": date de création du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"LastUpdate": date de dernière mise à jour du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"ActivationDate": date d'activation. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"DeactivationDate": date de désactivation du contrat. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"OriginatingAgencies": tableau contenant les services producteurs pour lesquels le détenteur du contrat a accès peut consulter les archives. Il s'agit d'un tableau de chaînes de caractères.

Collection AccessionRegisterSummary
===================================

Utilisation de la collection
----------------------------

Cette collection est utilisée pour l'affichage global du registre des fonds, dans la liste des fonds pour lesquels des AU ont été prises en compte dans Vitam.

Exemple de JSON stocké en base
------------------------------

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
      "creationDate": "2017-04-12T17:01:11.764"
  }

Exemple de la description dans le XML d'entrée
----------------------------------------------

Les seuls élements issus des bordereaux (manifest.xml), utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés entre les balisés <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

"_id": Identifiant unique. Il s'agit d'une chaine de 36 caractères.

"_tenant": 0

"OriginatingAgency": La valeur de ce champ est une chaîne de caractère.
Ce champ est la clef primaire et sert de concaténation pour toutes les entrées effectuées sur ce producteur d'archives. Il est contenu entre les baslises <OriginatinAgencyIdentifier> du bordereau.

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état
    (total, deleted et remained)

    - "total": Nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état
    (total, deleted et remained)

    - "total": Nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état
    (total, deleted et remained)

    - "total": Nombre total d'unités archivistiques pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état
    (total, deleted et remained)

    - "total": Volume total en octets des fichiers pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimées ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": Volume actualisé en octets des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.

"creationDate":  Date d'inscription du producteur d'archives concerné dans le registre des fonds. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

Collection AccessionRegisterDetail
==================================

Utilisation de la collection
----------------------------

Cette collection a pour vocation de stocker l'ensemble des informations sur les opérations d'entrées réalisées pour un service producteur. A ce jour, il y a autant d'enregistrements que d'opérations d'entrées effectuées pour ce service producteur, mais cela doit évoluer.

Exemple de JSON stocké en base
------------------------------

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
      ]
  }

Exemple de la description dans le XML d'entrée
----------------------------------------------

Les seuls élements issus des bordereaux (manifest.xml) utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés entre les balisés <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Détail des champs
-----------------

"_id": Identifiant unique.
    Il s'agit d'une chaine de 36 caractères.

"_tenant": 0, Identifiant du tenant
    *Utilisation post-béta*

"OriginatingAgency": Contient l'identifiant du service producteur du fonds.
    Il est contenu entre les baslises <OriginatinAgencyIdentifier>.

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

on récupère la valeur FRAN_NP_051314
La valeur est une chaîne de caractère.

"SubmissionAgency": Contient l'identifiant du service versant.
    Il est contenu entre les baslises <SubmissionAgencyIdentifier>.

Par exemple pour

::

  <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>

on récupère la valeur FRAN_NP_005761
La valeur est une chaîne de caractère.

Ce champ est facultatif dans le bordereau. Si elle est absente ou vide, alors la valeur contenue dans le champ <OriginatingAgencyIdentifier>. est reportée dans ce champ

"ArchivalAgreement": Contient le contrat utilisé pour réaliser l'entrée. 
  Il est contenu entre les balises <ArchivalAgreement>

Par exemple pour

::

  <ArchivalAgreement>ArchivalAgreement0</ArchivalAgreement>

on récupère la valeur ArchivalAgreement0
La valeur est une chaîne de caractère.

"StartDate": date de la première opération d'entrée correspondant à l'enregistrement concerné. La date est au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00".

"EndDate": Date de la dernière opération d'entrée correspondant à l'enregistrement concerné. au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"

"Status": Indication sur l'état des archives concernées par l'enregistrement.
La liste des valeurs possibles pour ce champ se trouve en annexe

"TotalObjectGroups": Contient la répartition du nombre de groupes d'objets du fonds par état pour l'opération journalisée
    (total, deleted et remained)
    - "total": Nombre total de groupes d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre de groupes d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre de groupes d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalUnits": Contient la répartition du nombre d'unités archivistiques du fonds par état pour l'opération journalisée
    (total, deleted et remained)
    - "total": Nombre total d'unités archivistiques pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'unités archivistiques supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'unités archivistiques conservées dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"TotalObjects": Contient la répartition du nombre d'objets du fonds par état pour l'opération journalisée
    (total, deleted et remained)
    - "total": Nombre total d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Nombre d'objets supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Nombre d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

"ObjectSize": Contient la répartition du volume total des fichiers du fonds par état pour l'opération journalisée
    (total, deleted et remained)
    - "total": Volume total en octet des fichiers pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": Volume total en octets des fichiers supprimées ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": Volume total en octets des fichiers conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
