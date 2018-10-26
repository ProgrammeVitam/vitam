Collection Agencies
###################

Utilisation de la collection Agencies
=====================================

La collection Agencies permet de référencer et décrire unitairement les services agents.

Cette collection est alimentée par l'import d'un fichier CSV contenant l'ensemble des services agents. Celui doit être structuré comme ceci :

.. csv-table::
  :header: "Identifier","Name","Description"

  "Identifiant du service agent","Nom du service agent","Description du service agent"

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection Agencies
=============================================================================================

::

  {
      "_id": "aeaaaaaaaaevq6lcaamxsak7psyd2uyaaadq",
      "Identifier": "Identifier5",
      "Name": "Identifier5",
      "Description": "une description de service agent",
      "_tenant": 2,
      "_v": 1
  }

Détail des champs
=================

**"_id":** identifiant unique du service agent.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** nom du service agent.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du service agent.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"Identifier":**  identifiant signifiant donné au service agent.

  * Le contenu de ce champ est obligatoirement renseigné dans le fichier CSV permettant de créer le service agent. En aucun cas la solution logicielle Vitam ne peut être maître sur la création de cet identifiant comme cela peut être le cas pour d'autres données référentielles.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"_tenant":** information sur le tenant.

  * Il s'agit de l'identifiant du tenant utilisant l'enregistrement
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
