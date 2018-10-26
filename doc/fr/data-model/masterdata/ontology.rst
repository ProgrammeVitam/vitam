Collection Ontology
###################

Utilisation de la collection
============================

La collection Ontology permet de référencer et décrire unitairement les champs définissant l'ontologie VITAM.


Exemple d'un fichier d'import d'ontology
=========================================

Les ontologies sont importées dans la solution logicielle Vitam sous la forme d'un fichier JSON.

.. code-block:: json

    [ {
        "Identifier" : "AcquiredDate",
        "SedaField" : "AcquiredDate",
        "ApiField" : "AcquiredDate",
        "Description" : "unit-es-mapping.json",
        "Type" : "DATE",
        "Origin" : "INTERNAL",
        "ShortName" : "AcquiredDate",
        "Collections" : [ "Unit" ]
      }, {
        "Identifier" : "BirthDate",
        "SedaField" : "BirthDate",
        "ApiField" : "BirthDate",
        "Description" : "unit-es-mapping.json",
        "Type" : "DATE",
        "Origin" : "INTERNAL",
        "ShortName" : "BirthDate",
        "Collections" : [ "Unit" ]
      },
      [...]
    ]


Les champs à renseigner obligatoirement pour chaque définition de champ dans l'ontologie :

* Identifier
* SedaField
* ApiField
* Description
* Type
* Origin
* ShortName
* Collections

Un fichier JSON décrit la totalité des champs de l'ontologie (interne et externe).


Détail des champs de la collection Ontology
===========================================

**"_id":** identifiant unique du champ de l'ontologie

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Identifier":** identifiant unique du champ de l'ontologie.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"SedaField":** identifiant dans la nomenclature SEDA du champ de l'ontologie

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"ApiField":** identifiant du champ de l'ontologie qui sera retourné via le DSL

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du champ de l'ontologie.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Type":** type du champ de l'ontologie.

  * Il s'agit d'une chaîne de caractères.
  * Peut avoir comme valeur : DATE, TEXT, KEYWORD, BOOLEAN, LONG, DOUBLE, ENUM, GEO_POINT.
  * Cardinalité : 1-1

**"Origin":** origine du champ de l'ontologie

  * Peut avoir comme valeur : INTERNAL ou EXTERNAL
  * Cardinalité : 1-1

**"ShortName":** identifiant technique pour traduction

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Collections":** collections concernées par le champ de l'ontologie

  * Il s'agit d'une liste de chaînes de caractères.
  * Cardinalité : 1-n

.. only:: builder_html

    Exemple d'un fichier d'import complet des ontologies
    =====================================================

    :download:`Fichier d'import des ontologies <ontologies.json>`.
