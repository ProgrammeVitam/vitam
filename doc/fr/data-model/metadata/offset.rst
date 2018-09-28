Collection Offset
#################

Utilisation de la collection
============================

Cette collection permet de persister les offsets des dernières données reconstruites des offres de stockage lors de la reconstruction au fil de l'eau pour les collections :

  * Unit
  * ObjectGroup

Il y a une valeur d'offset par couple tenant/collection.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": ObjectId("507f191e810c19729de860ea"),
    "offset": 1357,
    "collection": "UNIT",
    "_tenant": 1
  }

Détail des champs
=================

**"_id":** identifiant unique mongo.

  * Il s'agit d'un champ de type mongo : ObjectId(<hexadecimal>).
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"offset":** la valeur de l'offset.

  * Il s'agit d'un entier encodé 64 bits.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"collection":** collection impactée.

  * les valeurs possibles sont *UNIT* et *OBJECTGROUP*.

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
