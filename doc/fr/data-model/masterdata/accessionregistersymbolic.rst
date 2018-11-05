Collection AccessionRegisterSymbolic
####################################

Utilisation de la collection
============================

Cette collection contient une vue macroscopique des fonds relatifs aux services producteurs symboliques. Ces documents sont calculés périodiquement à partir des méta-données renseignées dans les unités archivistiques et les groupes d'objets.

Chaque document représente un instantané (snapshot) du stock symbolique pour un producteur, conservé pour l'historisation des fonds de ce dernier. Un nouveau document est donc créé à chaque fois que le registre des fonds symboliques est calculé.

::

  {
      "_id": "aefaaaaaaae2tauiaak6ualgbn5dp5aaaaaq",
      "CreationDate": "2018-09-24T14:07:31.053",
      "_tenant": 0,
      "OriginatingAgency": "RATP",
      "ArchiveUnit": 1,
      "ObjectGroup": 1,
      "BinaryObject": 1,
      "BinaryObjectSize": 6,
      "_v": 0
  }



Détail des champs
=================

**"_id":** identifiant unique du fonds.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"CreationDate":**  Date de calcul de ce document.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

``Exemple : "CreationDate": "2017-04-10T11:30:33.798"``

**"OriginatingAgency":** identifiant du service producteur symbolique.

  * La valeur de ce champ est une chaîne de caractères.
  * Cardinalité : 1-1

**"ArchiveUnit":** Nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"ObjectGroup":** Nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 0-1

**"BinaryObject":** nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 0-1

**"BinaryObjectSize":** Volume actualisé en octet des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 0-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement. Un document dans le registre des fonds symbolique n'est pas censé être modifié et donc avoir une version supérieure à 0
  * Cardinalité : 1-1

**"_tenant":** correspondant à l'identifiant du tenant.

  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
