Collection Context
##################

Utilisation de la collection
============================

La collection Context permet de référencer et décrire unitairement les contextes applicatifs.

Exemple d'un fichier d'import de contexte applicatif
====================================================

Les contextes applicatifs sont importés dans la solution logicielle Vitam sous la forme d’un fichier JSON.

::

  {
      "Name": "My_Context_5",
      "Status": "ACTIVE",
      "SecurityProfile": "admin-security-profile",
      "Permissions": [
        {
          "tenant": 1,
          "AccessContracts": [
            "AccessContracts_1",
            "AccessContracts_2"
          ],
          "IngestContracts": [
            "IngestContracts_1",
            "IngestContracts_2"
          ]
        },
        {
          "tenant": 0,
          "AccessContracts": [
            "AccessContracts_5",
            "AccessContracts_6"
          ],
          "IngestContracts": [
            "IngestContracts_9",
            "IngestContracts_10"
          ]
        }
      ]
    }

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection Context
============================================================================================

::

  {
      "_id": "aegqaaaaaaevq6lcaamxsak7psqdcmqaaaaq",
      "Name": "admin-context",
      "Status": "ACTIVE",
      "EnableControl": false,
      "Identifier": "CT-000001",
      "SecurityProfile": "admin-security-profile",
      "Permissions": [
          {
              "tenant": 0,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 1,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 2,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 3,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 4,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 5,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 6,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 7,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 8,
              "AccessContracts": [],
              "IngestContracts": []
          },
          {
              "tenant": 9,
              "AccessContracts": [],
              "IngestContracts": []
          }
      ],
      "CreationDate": "2017-11-02T12:06:34.034",
      "LastUpdate": "2017-11-02T12:06:34.036",
      "_v": 0
  }

Il est possible de mettre plusieurs contextes applicatifs dans un même fichier, sur le même modèle que les contrats d'entrée ou d'accès par exemple. On pourra noter que le contexte est multi-tenant et définit chaque tenant de manière indépendante. Il doit être enregistré dans le tenant d'administration.

Les champs à renseigner obligatoirement à la création d'un contexte applicatif sont :

* Name
* Permissions. La valeur de Permissions peut cependant être vide : "Permissions : []"

Détail des champs
=================

**"_id":** identifiant unique du contexte applicatif.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** nom du contexte applicatif.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Status":** statut du contexte applicatif.

  * Il s'agit d'une chaîne de caractères.
  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"Identifier":** identifiant signifiant donné au contexte applicatif.

  * Il est constitué du préfixe "CT-" suivi d'une suite de 6 chiffres. Par exemple : CT-001573.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"SecurityProfile":** nom du profil de sécurité utilisé par le contexte applicatif.

  * Ce nom doit correspondre à celui d'un profil de sécurité enregistré dans la collection SecurityProfile.
  * Il s'agit d'une chaîne de caractères
  * Cardinalité : 1-1

**"Permissions":** début du bloc appliquant les permissions à chaque tenant.

  * C'est un mot clé qui n'a pas de valeur associée.
  * Il s'agit d'un tableau.
  * Peut être vide.
  * Cardinalité : 1-1

**"tenant":** tenant sur lequel sont appliquées les permissions

  * Il s'agit d'un entier.
  * Cardinalité : 1-1

**"AccessContracts":** tableau d'identifiants de contrats d'accès appliqués sur le tenant.

  * Il s'agit d'un tableau de chaînes de caractères
  * Peut être vide
  * Cardinalité : 0-1

**"IngestContracts":** tableau d'identifiants de contrats d'entrées appliqués sur le tenant.

  * Il s'agit d'un tableau de chaînes de caractères
  * Peut être vide
  * Cardinalité : 0-1

**"CreationDate":** "CreationDate": date de création du contexte applicatif.

  * Il s'agit d'une date au format ISO 8601

  ``"CreationDate": "2017-04-10T11:30:33.798",``

  * Cardinalité : 1-1

**"LastUpdate":** date de dernière modification du contexte applicatif.

  * Il s'agit d'une date au format ISO 8601

  ``"LastUpdate": "2017-04-10T11:30:33.798",``

  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du contexte applicatif.

  * La date est au format ISO 8601

  ``Exemple : "ActivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1

**"DeactivationDate":** date de désactivation du contexte applicatif.

  * La date est au format ISO 8601

  ``Exemple : "DeactivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1

**"_v":**  version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"EnableControl":** activation des contrôles sur les tenants.

  * Il peut avoir pour valeur "true" ou "false" et a la valeur par défaut : "false".
  * Il s'agit d'un booléen
  * "true" : le contrôle est actif
  * "false" : le contrôle est inactif
  * Cardinalité : 1-1
