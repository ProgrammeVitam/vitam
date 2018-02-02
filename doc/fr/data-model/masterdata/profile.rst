Collection Profile
##################

Utilisation de la collection profile
====================================

La collection Profile permet de référencer et décrire unitairement les profils d'archivage.

Exemple d'un fichier d'import de profils d'archivage
====================================================

Un fichier d'import peut décrire plusieurs profils d'archivage.

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

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection profile
============================================================================================

::

  {
    "_id": "aegaaaaaaehlfs7waax4iak4f52mzriaaaaq",
    "_tenant": 1,
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
    "Path": "1_profile_aegaaaaaaehlfs7waax4iak4f52mzriaaaaq_20170522_092333.xsd"
  }

Détail des champs
=================

**"_id":** identifiant unique du profil d'archivage.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"Identifier":** indique l'identifiant signifiant du profil SEDA.

  * Si Vitam est maître dans la création de cet identifiant, il est alors constitué du préfixe "PR-" suivi d'une suite de 6 chiffres. Par exemple : PR-001573. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du profil d'archivage.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Name": indique le nom du profil d'archivage.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Description": Description du profil d'archivage.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

"Status": statut du profil d'archivage.

  * Peut être ACTIVE ou INACTIVE
  * Si ce champ n'est pas défini lors de la création de l'enregistrement, alors il est par défaut INACTIVE.
  * Cardinalité : 1-1

**"Format": Champ obligatoire** Indiquant le format attendu pour le fichier décrivant les règles du profil d'archivage.
  
  * Il s'agit d'une chaîne de caractères devant correspondre à l'énumération ProfileFormat.
  * Peut être ACTIVE ou INACTIVE.
  * Cardinalité : 1-1
  
**"CreationDate":** date de création du contrat.

  * La date est au format ISO 8601

  ``"CreationDate": "2017-04-10T11:30:33.798",``

  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"LastUpdate":**  date de dernière mise à jour du contrat dans la collection AccesContrat.

  * La date est au format ISO 8601

  ``"LastUpdate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du contrat.

  * La date est au format ISO 8601

  ``"ActivationDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"DeactivationDate":** date de désactivation du contrat.

  * La date est au format ISO 8601

  ``"DeactivationDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_v":**  version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"Path": Champ contribué par Vitam lors d'un import de fichier XSC ou RNG** Indiquant le chemin pour accéder au fichier du profil d'archivage.

  * Chaîne de caractères.
  * Le type de fichier doit correspondre à ce qui est décrit dans le champ Format
  * Cardinalité : 0-1 