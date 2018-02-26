Collection VitamSequence
########################

Utilisation de la collection
============================

Cette collection permet de générer des identifiants signifiants pour les enregistrements des collections suivantes :

  * IngestContract
  * AccesContract
  * Context
  * Profile
  * FileRules
  * SecurityProfile
  * Agencies
  
Ces identifiants sont composés d'un préfixe de deux lettres, d'un tiret et d'une suite de six chiffres. Par exemple : IC-027593. Il sont reportés dans les champs Identifier des collections concernées. 

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": "aeaaaaaaaahkwxukabqteak4q5mtmdyaaaaq",
    "Name": "AC",
    "Counter": 44,
    "_tenant": 1,
    "_v": 0
  }

Détail des champs
=================

**"_id":** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":**. Il s'agit du préfixe utilisé pour générer un identifiant signifiant. 

  * La valeur contenue dans ce champ doit correspondre à la table de concordance du service VitamCounterService.java. La liste des valeurs possibles est détaillée en annexe.
  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Counter":** numéro incrémental. 

  * Il s'agit du dernier numéro utilisé pour générer un identifiant signifiant.
  * Il s'agit d'un entier.
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"_tenant":** information sur le tenant. 

  * Il s'agit de l'identifiant du tenant utilisant l'enregistrement
  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1