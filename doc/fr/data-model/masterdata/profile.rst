Collection Profile
##################

Utilisation de la collection Profile
====================================

La collection Profile permet de référencer et décrire unitairement les notices de profil d'archivage.

Exemple d'un fichier d'import de notices de Profils d'archivage
===============================================================

Un fichier d'import peut décrire plusieurs notices de profil d'archivage.

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

Les champs à renseigner obligatoirement à la création d'un profil d'archivage sont :

* Name
* Description
* Format

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection Profile
============================================================================================

::

  {
    "_id": "aegaaaaaaehlfs7waax4iak4f52mzriaaaaq",
    "Identifier": "PR-000003",
    "Name": "ArchiveProfile0",
    "Description": "Description of the Profile",
    "Status": "ACTIVE",
    "Format": "XSD",
    "CreationDate": "2016-12-10T00:00",
    "LastUpdate": "2017-05-22T09:23:33.637",
    "ActivationDate": "2016-12-10T00:00",
    "DeactivationDate": "2016-12-10T00:00",
    "_v": 1,
    "_tenant": 1,
    "Path": "1_profile_aegaaaaaaehlfs7waax4iak4f52mzriaaaaq_20170522_092333.xsd"
  }

Détail des champs
=================

**"_id":** identifiant unique de la notice de profil d'archivage.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Identifier":** identifiant signifiant de la notice de profil d'archivage.

  * Si Vitam est maître dans la création de cet identifiant, il est alors constitué du préfixe "PR-" suivi d'une suite de 6 chiffres. Par exemple : PR-001573. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine de la notice du profil d'archivage.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Name"**: indique le nom de la notice du profil d'archivage.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description"**: description du profil d'archivage.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"Status"**: statut du profil d'archivage.

  * Peut être ACTIVE ou INACTIVE
  * Si ce champ n'est pas défini lors de la création de l'enregistrement, alors il est par défaut INACTIVE.
  * Cardinalité : 1-1

**"Format": champ obligatoire** format attendu pour le fichier décrivant les règles du profil d'archivage.

  * Il s'agit d'une chaîne de caractères devant correspondre à l'énumération ProfileFormat.
  * Ses valeurs sont soit RNG, soit XSD.
  * Cardinalité : 1-1

**"CreationDate":** date de création de la notice du profil d'archivage.

  * La date est au format ISO 8601
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``Exemple : "CreationDate": "2017-04-10T11:30:33.798",``


**"LastUpdate":**  date de dernière mise à jour de la notice du profil d'archivage dans la collection Profile.

  * La date est au format ISO 8601
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``Exemple : "LastUpdate": "2017-04-10T11:30:33.798"``

**"ActivationDate":** date d'activation de la notice du profil d'archivage.

  * La date est au format ISO 8601
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``Exemple : "ActivationDate": "2017-04-10T11:30:33.798"``

**"DeactivationDate":** date de désactivation de la notice du profil d'archivage.

  * La date est au format ISO 8601
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``Exemple : "DeactivationDate": "2017-04-10T11:30:33.798"``

**"_v":**  version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Path":** champ contribué par Vitam lors d'un import de fichier XSD ou RNG.

  * Indique le chemin pour accéder au fichier du profil d'archivage.
  * Chaîne de caractères.
  * Le format de fichier doit correspondre à celui qui est décrit dans le champ Format.
  * Cardinalité : 0-1
