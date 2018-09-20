Collection ArchiveUnitProfile
#############################

Utilisation de la collection
============================

La collection ArchiveUnitProfile permet de référencer et décrire unitairement les documents type.


Exemple d'un fichier d''import de document type
===============================================

Les documents type sont importés dans la solution logicielle Vitam sous la forme d'un fichier JSON.

::

    {   
        "Name":"Facture",
        "Description":"Document type d''une facture associée à un dossier de marché",
        "Identifier":"AUP_IDENTIFIER_0",
        "Status":"ACTIVE",
        "ControlSchema":"{}",
        "LastUpdate":"10/12/2016",
        "CreationDate":"10/12/2016",
        "ActivationDate":"10/12/2016",
        "DeactivationDate":"10/12/2016"
    }


Les champs à renseigner obligatoirement à l'import d''un document type sont :

* Name
* Description
* ControlSchema ( même si le champ est vide ) 

Un fichier JSON peut décrire plusieurs documents type.


Exemple de JSON stocké en base comprenant l''exhaustivité des champs de la collection ArchiveUnitProfile
========================================================================================================

::

 {
   "_id": "aegaaaaabmhdh434aapnqalcd7mufiyaaaaq",
   "Identifier": "AUP_IDENTIFIER_0",
   "Name":"Facture",
   "Description":"Document type d''une facture associée à un dossier de marché",
   "Status":"ACTIVE",
   "ControlSchema":"{}",
   "Fields":[],
   "LastUpdate":"10/12/2016",
   "CreationDate":"10/12/2016",
   "ActivationDate":"10/12/2016",
   "DeactivationDate":"10/12/2016"
   "_tenant": 11,
   "_v": 0
 }

Détail des champs de la collection ArchiveUnitProfile
=====================================================

**"_id":** identifiant unique du Document type.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** Nom du Document type.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":** Identifiant signifiant donné au Document type.

  * Il est constitué du préfixe "IC-" suivi d'une suite de 6 chiffres dans le cas ou la solution logicielle Vitam peuple l'identifiant. Par exemple : IC-007485. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du Document type.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du Document type.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Status":** statut du Document type.

  * Il s'agit d'une chaîne de caractères.
  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"ControlSchema":** Schema de contrôle du document type 

  * Il s'agit d'un bloc JSON.
  * Peut être vide
  * Cardinalité : 1-1

:: 

 {
 "$schema": "http://vitam-json-schema.org/draft-04/schema#",
 "id": "http://example.com/root.json",
 "type": "object",
 "additionalProperties": true,
 "properties": {
 
   "DescriptionLevel": {
     "type": "string",
     "enum": [
       "RecordGrp",
       "SubGrp",
       "File"
     ]
 }
 

**"Fields"** : liste des champs contrôlés

    * Il s'agit d'un tableau de chaînes de caractères
    * Liste les champs déclarés dans le schéma de contrôle
    * Renseigné automatiquement par la solution logicielle Vitam
    * Cardinalité 0-1

**"CreationDate":** date de création du Document type.

  * La date est au format ISO 8601

  ``Exemple : "CreationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1

**"LastUpdate":** date de dernière mise à jour du Document type dans la collection ArchiveUnitProfile.

  * La date est au format ISO 8601

  ``Exemple : "LastUpdate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du Document type.

  * La date est au format ISO 8601

  ``Exemple : "ActivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1

**"DeactivationDate":** date de désactivation du Document type.

  * La date est au format ISO 8601

  ``Exemple : "DeactivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1


**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
