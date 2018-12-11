Collection PreservationScenario
###############################

Utilisation de la collection PreservationScenario
=================================================

Cette collection référence et décrit les scénarios de préservations utilisés pour lancer des opérations de préservations.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection Agencies
=============================================================================================

::

  {
      "_id": "aefqaaaabahn6dttabew6alha45dfgqaaaaq",
      "Identifier": "PSC-000023",
      "Name": "Normalisation d'entrée",
      "Description": "Ce scénario permet de faire une validation des format et de créer une version de diffusion en PDF. Il est en général appliqué au contenu d'une entrée pour donner un retour de la qualité du versement et préparer une consultation fréquente.",
      "CreationDate": "2018-11-16T15:55:30.721",
      "LastUpdate": "2018-11-20T15:34:21.542",
      "ActionList": ["ANALYSE", "GENERATE"],
      "GriffinByFormat": [{
              "FormatList": ["fmt/136", "fmt/137", "fmt/138", "fmt/139", "fmt/290", "fmt/294", "fmt/292", "fmt/296", "fmt/291", "fmt/295", "fmt/293", , "fmt/297"],
              "GriffinIdentifier": "GRI-0000023",
              "TimeOut": 20,
              "MaxSize": 10000000,
              "ActionDetail": [{
                      "Action": "ANALYSE",
                      "Values": {
                          "Args": ["-strict"]
                      }
                  }, {
                      "Action": "GENERATE",
                      "Values": {
                          "Extension": "pdf",
                          "Args": ["-f", "pdf", "-e", "SelectedPdfVersion=1"]
                      }
                  }
              ]
          }, {
              "FormatList": ["fmt/41", "fmt/42", "x-fmt/398", "x-fmt/390", "x-fmt/391", "fmt/645",
                  "fmt/43", "fmt/44", "fmt/112", "fmt/11", "fmt/12", "fmt/13", "fmt/935", "fmt/152",
                  "fmt/399", "fmt/388", "fmt/387", "fmt/155", "fmt/353", "fmt/154", "fmt/153",
                  "fmt/156", "x-fmt/392", "x-fmt/178", "fmt/408", "fmt/568", "fmt/567", "fmt/566"],
              "GriffinIdentifier": "GRI-0000012",
              "TimeOut": 10,
              "MaxSize": 10000000,
              "ActionDetail": [{
                      "Action": "ANALYSE",
                      "Values": null
                  }, {
                      "Action": "GENERATE",
                      "Values": {
                          "Extension": "pdf",
                          "Args": ["-quality", "90"]
                      }
                  }
              ]
          }
      ],
      "GriffinDefault": {
          "GriffinIdentifier": "GRI-0000005",
          "TimeOut": 10,
          "MaxSize": 10000000,
          "ActionDetail": [{
                  "Action": "ANALYSE",
                  "Values": {
                      "Args": ["-strict"]
                  }
              }
          ]
      },
      " _tenant": 3,
      " _v": 2
  }
  ```

Détail des champs
=================

**"_id":** identifiant unique faisant à un scénario de préservation.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** nom du scénario de préservation.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":**  identifiant signifiant donné au scénario de préservation.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du scénario de préservation.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"CreationDate":** date de création du scénario.

  * La date est au format ISO 8601.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``"CreationDate": "2017-04-10T11:30:33.798"``

**"LastUpdate":** date de dernière mise à jour du scénario dans la collection PreservationScenario.

  * La date est au format ISO 8601.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

``"LastUpdate": "2017-04-10T11:30:33.798"``

**"ExecutableVersion":** version du griffon.

  * Version du griffon utilisé. Un même exécutable (ExecutableName) peut être associé à plusieurs versions.
  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"ActionList":** liste des actions prévues par le scénario.

  * Il s'agit d'une tableau de chaîne de caractères faisant partie d'une énumérations.
  * L'énumération est : ANALYSE, GENERATE, IDENTIFY, EXTRACT_MD_AU et EXTRACT_MD.
  * Cardinalité : 1-1

**"GriffinByFormat":** description des actions à effectuer.

  * Description des actions à mener pour une liste de formats.
  * Il s'agit d'un tableau d'objets.
  * Cardinalité : 0-1
  * Cet objet est composé des champs suivants :

    * **FormatList :** tableau de PUID de formats : ces PUID font références aux différents formats du référentiel des formats.
    * **GriffinIdentifier :** identifiant du griffon qui est appelé sur les objets concernés par un format de FormatList. Cette identifiant doit être un identifiant valide de la collection Griffin.
    * **TimeOut :** temps en minutes au bout duquel Vitam, en l'absence de réponse du griffon, arrêtera l'action de préservation.
    * **MaxSize :** taille maximale en octet des objets pouvant être traités par ce scénaro de préservation.
    * **ActionDetail :** tableau d'objet permettant de décrire les commandes techniques associées à chaque action de préservation. Les objets sont composés des champs suivants :

      * **Action :** action de préservation. Ce champ doit avoir une chaîne de caractère faisant partie des valeurs du champ ActionList.
      * **Values :** pour les actions ANALYSE, GENERATE et les EXTRACT, ce champ est null. Pour l'action GENERATE, c'est un objet possédant deux champs : **Extension** est une chaîne de caractère servant à mettre une extension aux fichiers générés (ex : .pdf). **Args** : est une liste d'argument utilisés lors de la commande système qui exécute le griffon.

**"GriffinDefault":** description de l'action par défaut

  * Description de l'actions à mener si aucun format ne correspond à ceux attendus dans les objets de GriffinByFormat.
  * Il s'agit d'un d'objets reprenant la structure de ceux de GriffinByFormat.
  * Cardinalité : 1-1
  * Si il n'y a pas d'action par défaut à mener, ce champ peut être 'null'.

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant utilisant le griffon.
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
