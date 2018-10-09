Collection AccessContract
#########################

Utilisation de la collection AccessContract
===========================================

La collection AccessContract permet de référencer et de décrire unitairement les contrats d'accès.

Exemple d'un fichier d'import de contrat d'accès
================================================

Les contrats d'accès sont importés dans la solution logicielle Vitam sous la forme d'un fichier JSON.

::

    [

      {
          "Name": "ContratTNR",
          "Identifier": "AC-000034",
          "Description": "Contrat permettant de faire des opérations pour tous les services producteurs et sur tous les usages",
          "Status": "ACTIVE",
          "CreationDate": "2016-12-10T00:00:00.000",
          "LastUpdate": "2017-11-07T07:57:10.581",
          "ActivationDate": "2016-12-10T00:00:00.000",
          "DeactivationDate": "2016-12-10T00:00:00.000",
          "DataObjectVersion": [
              "PhysicalMaster",
              "BinaryMaster",
              "Dissemination",
              "Thumbnail",
              "TextContent"
          ],
          "WritingPermission": true,
          "EveryOriginatingAgency": true,
          "EveryDataObjectVersion": false,
      }
    ]

Les champs à renseigner obligatoirement à la création d'un contrat sont :

* Name
* Description

Un fichier d'import peut décrire plusieurs contrats.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection AccesContract
==================================================================================================

::

    {
    "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
    "Name": "SIA archives nationales",
    "Identifier": "AC-000009",
    "Description": "Contrat d'accès - SIA archives nationales",
    "Status": "ACTIVE",
    "CreationDate": "2017-04-10T11:30:33.798",
    "LastUpdate": "2017-04-10T11:30:33.798",
    "ActivationDate": "2017-04-10T11:30:33.798",
    "DeactivationDate": null,
    "DataObjectVersion": ["PhysicalMaster", "BinaryMaster", "Dissemination", "Thumbnail", "TextContent"],
    "OriginatingAgencies":["FRA-56","FRA-47"],
    "RootUnits": [
        "aeaqaaaaaahxunbaabg3yak6urend2yaaaaq",
        "aeaqaaaaaahxunbaabg3yak6urendoqaaaaq"
    ],
    "WritingPermission": true,
    "WritingRestrictedDesc": false,
    "EveryOriginatingAgency": false,
    "EveryDataObjectVersion": true,
    "AccessLog": "INACTIVE",
    "_tenant": 0,
    "_v": 0,
    "RootUnits": [
        "aeaqaaaaaahxunbaabg3yak6urend2yaaaaq",
        "aeaqaaaaaahxunbaabg3yak6urendoqaaaaq"
    ]
     "ExcludedRootUnits": ["aeaqaaaaaagbcaacaaceoalde3yowuaaaaoq"]
    }

Détail des champs
=================

**"_id":** identifiant unique du contrat par tenant par contrat.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.

  * Il s'agit de l'identifiant du tenant.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** Nom du contrat d'accès.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier" :** identifiant signifiant donné au contrat.

  * Il est constitué du préfixe "AC-" suivi d'une suite de 6 chiffres s'il est peuplé par la solution logicielle Vitam. Par exemple : AC-001223. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du contrat et est unique sur le tenant.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** Description du contrat d'accès.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"Status":** statut du contrat.

  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"CreationDate":** date de création du contrat.

  * La date est au format ISO 8601 et prend la forme suivante :

  ``"CreationDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"LastUpdate":** date de dernière mise à jour du contrat dans la collection AccesContrat.

  * La date est au format ISO 8601 et prend la forme suivante :

  ``"LastUpdate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du contrat.

  * La date est au format ISO 8601 et prend la forme suivante :

  ``"ActivationDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"DeactivationDate":** date de désactivation du contrat.

  * La date est au format ISO 8601 et prend la forme suivante :

  ``"DeactivationDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"DataObjectVersion":** types d'usages des groupes d'objets auxquels le détenteur du contrat a accès.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Peut être vide
  * Cardinalité : 0-1

**"OriginatingAgencies":** services producteurs dont le détenteur du contrat peut consulter les archives.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Peut être vide
  * Cardinalité : 0-1

**"WritingPermission":** droit d'écriture.

  * Il s'agit d'un booléen. Si la valeur est à true, le détenteur du contrat peut effectuer des mises à jour.
  * Cardinalité : 1-1

**"WritingRestrictedDesc":** droit de modification des métadonnées descriptives seulement.

  * Il s'agit d'un booléen. Si la valeur est à true, le détenteur du contrat peut effectuer des mises à jour seulement sur les métadonnées descriptives.
    Si la valeur est à false, le détenteur du contrat peut effectuer des mises à jour sur les métadonnées descriptives, ainsi que sur les métadonnées de gestion.
  * Cardinalité : 0-1

**"EveryOriginatingAgency":** droit de consultation sur tous les services producteurs.

  * Il s'agit d'un booléen.
  * Si la valeur est à true, alors le détenteur du contrat peut accéder aux archives de tous les services producteurs.
  * Cardinalité : 1-1

**"EveryDataObjectVersion":** droit de consultation sur tous les usages.

  * Il s'agit d'un booléen.
  * Si la valeur est à true, alors le détenteur du contrat peut accéder à tous les types d'usages.
  * Cardinalité : 1-1

**"AccessLog":** enregistrement des accès

  * Peut être ACTIVE ou INACTIVE
  * Si la valeur est à ACTIVE, alors les téléchargements des objets sont enregistrés dans un fichier de log
  * Cardinalité : 1-1

**"_tenant":** identifiant du tenant.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1

**"_v":**  version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"RootUnits":** Liste des noeuds de consultation auxquels le détenteur du contrat a accès. Si aucun noeud n'est spécifié, alors l'utilisateur a accès à tous les noeuds.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Peut être vide
  * Cardinalité : 0-1

**"ExcludedRootUnits":** Liste des noeuds de consultation à partir desquels le détenteur du contrat n'a pas accès. Si aucun noeud n'est spécifié, alors l'utilisateur a accès à tous les noeuds.

  * Il s'agit d'un tableau de chaînes de caractères.
  * Peut être vide
  * Cardinalité : 0-1
