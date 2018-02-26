Collection FileFormat
#####################

Utilisation de la collection FileFormat
=======================================

La collection FileFormat permet de référencer et décrire les différents formats de fichiers ainsi que leur description. La collection est initialisée à partir de l'import du fichier de signature PRONOM, mis à disposition par The National Archive (UK).

Cette collection est commune à tous les tenants. Elle est enregistré sur le tenant d'administration.

Exemple de la description d'un format dans le XML d'entrée
==========================================================

Ci-après, la portion d'un fichier de signature (DROID_SignatureFile_VXX.xml) utilisée pour renseigner les champs du JSON.

::

   <FileFormat ID="105" MIMEType="application/msword" Name="Microsoft Word for Macintosh Document" PUID="x-fmt/64" Version="4.0">
     <InternalSignatureID>486</InternalSignatureID>
     <Extension>mcw</Extension>
   </FileFormat>

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection FileFormat
===============================================================================================

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

Détail des champs du JSON stocké en base
========================================

**"_id":** identifiant unique du format.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"CreatedDate":** date de création de la version du fichier de signatures PRONOM utilisé pour initialiser la collection.

  * Il s'agit d'une date au format ISO 8601 YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm.

  ``Exemple : "2016-08-19T16:36:07.942+02:00"``

  * Cardinalité : 1-1

**"VersionPronom":** numéro de version du fichier de signatures PRONOM utilisé pour créer l'enregistrement.
    
    * Il s'agit d'un entier.
    * Le numéro de version de PRONOM est à l'origine déclaré dans le fichier de signature au niveau de la balise <FFSignatureFile> au niveau de l'attribut "version ".
    * Cardinalité : 1-1

Dans cet exemple, le numéro de version est 88 :

::

 <FFSignatureFile DateCreated="2016-09-27T15:37:53" Version="88" xmlns="http://www.nationalarchives.gov.uk/pronom/SignatureFile">

**"PUID":** identifiant unique du format au sein du référentiel PRONOM.
    
    * Il s'agit d'une chaîne de caractères.
    * Il est issu du champ "PUID" de la balise <FileFormat>. La valeur est composée du préfixe "fmt" ou "x-fmt", puis d'un nombre correspondant au numéro d'entrée du format dans le référentiel PRONOM. Les deux éléments sont séparés par un "/".
    * Cardinalité : 1-1

Par exemple :

::

 x-fmt/64

Les PUID comportant un préfixe "x-fmt" indiquent que ces formats sont en cours de validation par The National Archives (UK). Ceux possédant un préfixe "fmt" sont validés.

**"Version":** version du format.
    
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

**"MIMEType":** Type MIME correspondant au format de fichier.
    
    * Il s'agit d'une chaîne de caractères.
    * Il est renseigné avec le contenu de l'attribut "MIMEType" de la balise <FileFormat>. Cet attribut est facultatif dans le fichier de signature.
    * Cardinalité : 0-1

**"HasPriorityOverFileFormatID":** liste des PUID des formats sur lesquels le format a la priorité.

  * Il s'agit d'un tableau de chaînes de caractères
  * Peut être vide.
  * Cardinalité : 0-1

::

  <HasPriorityOverFileFormatID>1121</HasPriorityOverFileFormatID>

Cet identifiant est ensuite utilisé dans Vitam pour retrouver le PUID correspondant.

S'il existe plusieurs balises <HasPriorityOverFileFormatID> dans le fichier XML initial pour un format donné, alors les PUID seront stockés dans le JSON sous la forme suivante :

::

  "HasPriorityOverFileFormatID": [
      "fmt/714",
      "fmt/715",
      "fmt/716"
  ],

**"Group":** Champ permettant d'indiquer le nom d'une famille de format.
	
  * Il s'agit d'une chaîne de caractères.
  * C'est un champ propre à la solution logicielle Vitam.
  * Cardinalité : 0-1

**"Alert":** alerte sur l'obsolescence du format.
    
  * Il s'agit d'un booléen dont la valeur est par défaut placée à false.
  * Cardinalité : 0-1

**"Comment":** commentaire.
  
  * Il s'agit d'une chaîne de caractères.
  * C'est un champ propre à la solution logicielle Vitam.
  * Cardinalité : 0-1

**"Extension":** Extension(s) du format.
    
    * Il s'agit d'un tableau de chaînes de caractères.
    * Ne peut être vide
    * Il contient les valeurs situées entre les balises <Extension> elles-mêmes encapsulées entre les balises <FileFormat>. Le champ <Extension> peut-être multivalué. Dans ce cas, les différentes valeurs situées entre les différentes balises <Extension> sont placées dans le tableau et séparées par une virgule.
    * Cardinalité : 1-1

Par exemple, pour le format dont le PUID est fmt/918 la représentation XML est la suivante :

::

 <FileFormat ID="1723" Name="AmiraMesh" PUID="fmt/918" Version="3D ASCII 2.0">
     <InternalSignatureID>1268</InternalSignatureID>
     <Extension>am</Extension>
     <Extension>amiramesh</Extension>
     <Extension>hx</Extension>
   </FileFormat>

Les valeurs des balises <Extension> seront stockées de la façon suivante dans le JSON :

::

 "Extension": [
      "am",
      "amiramesh",
      "hx"
  ],
    
**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1