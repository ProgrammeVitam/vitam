Collection Griffin
###################

Utilisation de la collection Griffin
=====================================

Cette collection référence et décrit les griffons utilisés dans les opérations de préservations.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection Agencies
=============================================================================================

::

  {
      "_id": "aeaaaaaaaahlopljab2wualhmuydxiaaaaaq",
      "Name": Imgmagic,
      "Identifier": "GRIFFIN1",
      "Description": "Griffon IMG",
      "CreationDate": "2016-12-10T00:00:00.000",
      "LastUpdate": "2018-12-07T04:25:57.510",
      "ExecutableName": "imageMagic",
      "ExecutableVersion": "V1",
      "_tenant": 1,
      "_v": 13
  }

Détail des champs
=================

**"_id":** identifiant unique faisant référence à un exécutable et sa version.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** nom du griffon.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":**  identifiant signifiant donné au griffon.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du griffon.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"CreationDate":** date de création du griffon.

  * La date est au format ISO 8601.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``"CreationDate": "2017-04-10T11:30:33.798"``

**"LastUpdate":** date de dernière mise à jour du griffon dans la collection Griffin.

  * La date est au format ISO 8601.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``"LastUpdate": "2017-04-10T11:30:33.798"``

**"ExecutableName":** nom de l'exécutable.

* Nom technique du griffon. C'est le nom utilisé pour lancer l'exécutable sur le système.
* Il s'agit d'une chaîne de caractères.
* Cardinalité : 1-1

**"ExecutableVersion":** version du griffon.

* Version du griffon utilisé. Un même exécutable (ExecutableName) peut être associé à plusieurs versions.
* Il s'agit d'un entier.
* Cardinalité : 1-1

  "ExecutableName": "imageMagic",
  "ExecutableVersion": "V1",

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant utilisant le griffon.
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
