Collection AccessionRegisterSummary
###################################

Utilisation de la collection
============================

Cette collection contient une vue macroscopique des fonds pris en charge dans la solution logicielle Vitam. Chaque service producteur possède un et un seul document le concernant dans cette collection. Ce document est **calculé** à partir des données enregistrées dans la collection AccessionRegisterDetail pour ce service producteur.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": "aefaaaaaaaed4nrpaas4uak7cxykxiaaaaaq",
    "OriginatingAgency": "Vitam",
    "TotalObjects": {
        "ingested": 292,
        "deleted": 0,
        "remained": 292
    },
    "TotalObjectGroups": {
        "ingested": 138,
        "deleted": 0,
        "remained": 138
    },
    "TotalUnits": {
        "ingested": 201,
        "deleted": 0,
        "remained": 201
    },
    "ObjectSize": {
        "ingested": 35401855,
        "deleted": 0,
        "remained": 35401855
    },
      "CreationDate": "2017-04-12T17:01:11.764",
      "_v": 1,
      "_tenant": 0
  }

Détail des champs
=================

**"_id":** identifiant unique du fonds.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"OriginatingAgency":** identifiant d'un service producteur.

  * la valeur de ce champ est une chaîne de caractères.
  * Ce champ est la clef primaire pour un enregistrement dans le registre des fonds. Il permet l'agrégation de tous les documents de la collection AccessionRegisterDetail auquel pour ce service producteur. Cette valeur correspond nécessairement au champ Identifier de la collection Agencies.
  * Cardinalité : 1-1

**"TotalObjects":** Contient la répartition du nombre d'objets du service producteur par état

    - "ingested": nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"TotalObjectGroups":** Contient la répartition du nombre de groupes d'objets du service producteur par état

    - "ingested": nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre de groupes d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"TotalUnits":** Contient la répartition du nombre d'unités archivistiques du service producteur par état

    - "ingested": nombre total d'unités archivistiques prises en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"ObjectSize":** Contient la répartition du volume total des fichiers du service producteur par état

    - "ingested": volume total en octet des fichiers pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": volume total en octet des fichiers supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": volume actualisé en octet des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"CreationDate":**  Date du dernier calcul de ce document dans la collection.

  * La date est au format ISO 8601

  ``"CreationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
  * Cardinalité : 1-1

**"_tenant":** correspondant à l'identifiant du tenant.

  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
