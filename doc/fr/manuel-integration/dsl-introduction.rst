DSL Vitam
#########

Le DSL (Domain Specific Language) Vitam est composé de deux parties :

- Request: Contient la structure du Body contenant la requête au format Json. Le DSL permet d'exprimer un grand nombre de possibilités de requêtes. Dans le cadre des *Units* et *Objects*, cette requête peut être arborescente et multiples.
- Response: Contient la structure du Body contenant le résultat au format Json. Il contient différentes informations utiles ou demandées.

Request
=======

Une requête est composée de plusieurs parties, et en particulier le Body qui contient un Json exprimant la requête.

Elle peut être complétée par quelques valeurs dans le *Header* :

- **X-Application-Id** : (**UNSUPPORTED**) pour conserver la session (valeur non signifiante) dans les journaux et logs du SAE associés à l'opération demandée
- **X-Valid: true** : (**UNSUPPORTED**) pour une requête HEAD sur un **Object** pour vérifier la validité (check d'empreinte)
- **X-Tenant-Id** : pour chaque requête, le tenant sur lequel doit être exécutée la requête
- **X-Qualifier** et **X-Version** : pour une requête GET sur un **Object** pour récupérer un usage et une version particulière
- **X-Callback** (**UNSUPPORTED**) : pour les opérations de longue durée et donc asynchrones pour indiquer l'URL de Callback
- **X-Cursor: true** et **X-Cursor-Id** (**UNSUPPORTED**) : pour la gestion d'une requête en mode "curseur"
- **X-Http-Method-Override** : pour permettre aux clients HTTP ne supportant pas tous les modes de la RFC 7231 (GET/PUT/DELETE avec Body)

BuilderRequest
--------------

Il existe des Helpers en Java pour construire les requêtes au bon format (hors headers) dans le package **common-database-public.**

- **Request**

  - Multiple : fr.gouv.vitam.common.database.builder.request.multiple avec MultipleDelete, MultipleInsert, MultipleSelect et MultipleUpdate (pour Units et Objects)
  - Single : fr.gouv.vitam.common.database.builder.request.single avec Select, Insert, Delete, Update (pour toutes les autres collections, en fonction des droits et des possibilités)

- **Query** qui sont des arguments d'une Request

  - fr.gouv.vitam.common.database.builder.query avec BooleanQuery, CompareQuery, ... et surtout le **QueryHelper** et le **VitamFieldsHelper** pour les champs protégés (commençant par un '#')
  - dans le cas de update fr.gouv.vitam.common.database.builder.action avec AddAction, IncAction, ... et surtout le **UpdateActionHelper***

Collections Units et Objects uniquement
---------------------------------------


- **$roots**:

  - Il s'agit des racines (Units) à partir desquelles la requête est exprimée (toutes les recherches sur les Units et Objects sont en mode arborescente). Il correspond d'une certaine façon à "*FROM x*" dans le langage SQL étendu au cas des arborescences.
  - Autres collections: ce champ n'existe pas car il n'y a pas d'arborescence.

- **$query**:
  - $query peut contenir plusieurs Query, qui seront exécutées successivement (tableau de Query).
  - Une Query correspond à la formulation "*WHERE xxx*" dans le langage SQL, c'est à dire les critères de sélection.
  - La succession est exécutée avec la signification suivante :

    - Depuis $roots, chercher les Units/Objects tel que Query[1], conduisant à obtenir une liste d'identifiants[1]
    - Cette liste d'identifiants[1] devient le nouveau $roots, chercher les Units/Objects tel que Query[2], conduisant à obtenir une liste d'identifiants[2]
    - Et ainsi de suite, la liste d'identifiants[n] de la dernière Query[n] est la liste de résultat définitive sur laquelle l'opération effective sera réalisée (SELECT, UPDATE, INSERT, DELETE) selon ce que l'API supporte (GET, PUT, POST, DELETE).
    - Chaque query peut spécifier une profondeur où appliquer la recherche :

      - $depth = 0 : sur les items spécifiés (filtre sur les mêmes items, à savoir pour la première requête ceux de $roots, pour les suivantes, le résultat de la requête précédente, c'est à dire le nouveau $roots)
      - $depth < 0 : sur les items parents (hors les items spécifiés dans le $roots courant)
      - $depth > 0 : sur les items enfants (hors les items spécifiés dans le $roots courant)
      - par défaut, $depth vaut 1 (enfants immédiats dans le $roots courant)

    - Le principe est résumé dans le graphe d'états suivant :

.. image:: images/multi-query-schema.png

  - $source (**UNSUPPORTED**) permet de changer de collections entre deux query (unit ou object)

- **$filter**:

  - Il permet de spécifier des filtres additionnels :

    - Pour *GET* :

      - **$limit**: le nombre maximum d'items retournés (limité à 1000 par défaut, maximum à 100000)
      - **$per_page** (**UNSUPPORTED**) : le nombre maximum des premiers items retournés (limité à 100 par défaut, maximum à 100)
      - **$offset**: la position de démarrage dans la liste retournée (positionné à 0 par défaut, maximum à 100000)
      - **$orderby: { fieldname: 1, fieldname: -1 }** : permet de définir un tri ascendant ou descendant
      - **$hint: "nocache"** (**UNSUPPORTED**) permet de spécifier si l'on ne veut pas bénéficier du cache (cache actif par défaut)

    - Pour *POST*, *PUT* et *DELETE*

      - **$mult**: booléen où *true* signifie que l'opération peut concerner de multiples items (par défaut, positionné à *false*)

        - *POST*: les pères sélectionnés peuvent être multiples
        - *PUT*: la mise à jour peut concerner de multiples items simultanément
        - *DELETE*: l'effacement peut concerner plusieurs items

- **$projection: { fieldname: 0, fieldname: 1 }** uniquement pour *GET* (SELECT)

  - Il permet de définir la forme du résultat que l'on souhaite. Il correspond au "*SELECT \**" dans le langage SQL.
  - Une valeur à 1 réclame le champ.
  - Une valeur à 0 exclut le champ.
  - Si rien n'est spécifié, cela correspond à tous les champs (équivalent à "SELECT \*")

- **$data**: uniquement pour *POST* (INSERT)

  - Permet de définir le contenu à insérer dans la collection.
- **$action**: uniquement pour *PUT* (UPDATE)

  - Permet de définir le contenu à modifier dans la collection.

- Il n'y a pas d'argument complémentaire pour *DELETE* (DELETE) hormis la partie *$filter*
- **facetQuery** (**UNSUPPORTED**): uniquement pour *GET* et optionnel

  - Permet de définir des sous-requêtes (sous la forme d'agrégats) correspondant généralement à des facettes dans l'application Front-Office

Autres collections
------------------

- **$query**:

  - Il s'agit d'une **Query** unique.
  - Une Query correspond à la formulation "*WHERE xxx*" dans le langage SQL, c'est à dire les critères de sélection.

- **$filter**:

  - Il permet de spécifier des filtres additionnels :

    - Pour *GET* :

      - **$limit**: le nombre maximum d'items retournés (limité à 1000 par défaut, maximum à 100000)
      - **$per_page** (**UNSUPPORTED**): le nombre maximum des premiers items retournés (limité à 100 par défaut, maximum à 100)
      - **$offset**: la position de démarrage dans la liste retournée (positionné à 0 par défaut, maximum à 100000)
      - **$orderby: { fieldname: 1, fieldname: -1 }** : permet de définir un tri ascendant ou descendant
      - **$hint: "nocache"** (**UNSUPPORTED**) permet de spécifier si l'on ne veut pas bénéficier du cache (cache actif par défaut)

    - Pour *POST*, *PUT* et *DELETE*

      - **$mult** (**UNSUPPORTED**): booléen où *true* signifie que l'opération peut concerner de multiples items (par défaut, positionné à *false*)

        - *POST*: les pères sélectionnés peuvent être multiples
        - *PUT*: la mise à jour peut concerner de multiples items simultanément
        - *DELETE*: l'effacement peut concerner plusieurs items

- **$projection: { fieldname: 0, fieldname: 1 }** uniquement pour *GET*

  - Il permet de définir la forme du résultat que l'on souhaite. Il correspond au "*SELECT \**" dans le langage SQL.
  - Une valeur à 1 réclame le champ.
  - Une valeur à 0 exclut le champ.
  - Si rien n'est spécifié, cela correspond à tous les champs (équivalent à "SELECT \*")

- **$data**: uniquement pour *POST*

  - Permet de définir le contenu à insérer dans la collection.

- **$action**: uniquement pour *PUT*

  - Permet de définir le contenu à modifier dans la collection.

- **facetQuery** (**UNSUPPORTED**): uniquement pour *GET* et optionnel

  - Permet de définir des sous-requêtes (sous la forme d'agrégats) correspondant généralement à des facettes dans l'application Front-Office

Query
-----

Les commandes de la Query peuvent être :

Une query est exprimée avec des opérateurs (inspirés de MongoDB / Elastic)

+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Catégorie                  | Opérateurs                               | Arguments                                  | Commentaire                                                                  |
+============================+==========================================+============================================+==============================================================================+
| Accès direct               | $path                                    | identifiants                               | Accès direct à un noeud                                                      |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Booléens                   | $and, $or, $not                          | opérateurs                                 | Combinaison logique d'opérateurs                                             |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Comparaison                | $eq, $ne, $lt, $lte, $gt, $gte           | Champ et valeur                            | Comparaison de la valeur d'un champ et la valeur passée en argument          |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $range                                   | Champ, $lt, $lte, $gt, $gte et valeurs     | Comparaison de la valeur d'un champ avec l'intervalle passé en argument      |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Existence                  | $exists, $missing, $isNull               | Champ                                      | Existence d'un champ                                                         |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Tableau                    | $in, $nin                                | Champ et valeurs                           | Présence de valeurs dans un tableau                                          |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $size                                    | Champ et taille                            | Comparaison (égale) de la taille d'un tableau                                |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | [n] **UNSUPPORTED**                      | Position (n >= 0)                          | Élément d'un tableau                                                         |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Textuel                    | $term, $wildcard                         | Champ, mot clef                            | Comparaison de champs mots-clefs à valeur exacte                             |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $match, $matchPhrase, $matchPhrasePrefix | Champ, phrase, $max_expansions (optionnel) | Recherche plein texte soit sur des mots, des phrases ou un préfixe de phrase |
+                            +------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $regex                                   | Champ, Expression régulière                | Recherche via une expression régulière                                       |
+                            +------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $search                                  | Champ, valeur                              | Recherche du type moteur de recherche                                        |
+                            +------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
|                            | $flt, $mlt                               | Champ, valeur                              | Recherche « More Like This », soit par valeurs approchées                    |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| Géomatique                 | $geometry, $box, $polygon, $center       | Positions                                  | Définition d'une position géographique                                       |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+
| **UNSUPPORTED**            | $geoWithin, $geoIntersects, $near        | Une forme                                  | Recherche par rapport à une forme géométrique                                |
+----------------------------+------------------------------------------+--------------------------------------------+------------------------------------------------------------------------------+

Chaque Query dispose éventuellement d'arguments additionnels pour gérer l'arborescence :

+------------+---------------------+-----------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Catégorie  | Opérateur           | Arguments       | Commentaire                                                                                                                                                                                     |
+============+=====================+=================+=================================================================================================================================================================================================+
| Profondeur | $depth, $exactdepth | \+ ou - n       | Permet de spécifier si la query effectue une recherche vers les racines (-) ou vers les feuilles (+) et de quelle profondeur (n), avec une profondeur relative ($depth) ou exacte ($exactdepth) |
|            |                     |                 | - $depth = 0 signifie que l'on ne change pas de profondeur (mêmes objets concernés)                                                                                                             |
|            |                     |                 | - $depth > 0 indique une recherche vers les fils uniquement                                                                                                                                     |
|            |                     |                 | - $depth < 0 indique une recherche vers les pères uniquements (cf. schéma sur les multiples queries)                                                                                            |
+------------+---------------------+-----------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Collection | $source             | units / objects | Permet dans une succession de Query de changer de collection. Attention, la dernière Query doit respecter la collection associée à la requête                                                   |
+------------+---------------------+-----------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+


Actions
-------

Dans la commande PUT (Update) :

+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| Opérateur    | Arguments                       | Commentaire                                                                                                    |
+==============+=================================+================================================================================================================+
| $set         | nom de champ, valeur            | change la valeur du champ                                                                                      |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $unset       | liste de noms de champ          | enlève le champ                                                                                                |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $min, $max   | nom de champ, valeur            | change la valeur du champ à la valeur minimale/maximale si elle est supérieure/inférieure à la valeur précisée |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $inc         | nom de champ, valeur            | incrémente/décremente la valeur du champ selon la valeur indiquée                                              |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $rename      | nom de champ, nouveau nom       | change le nom du champ                                                                                         |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $push, $pull | nom de champ,  liste de valeurs | ajoute en fin ou retire les éléments de la liste du champ (qui est un tableau)                                 |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $add         | nom de champ,  liste de valeurs | ajoute les éléments de la liste du champ (qui est un "set" avec unicité des valeurs)                           |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+
| $pop         | nom de champ,  -1 ou 1          | retire le premier (-1) ou le dernier (1) de la liste du champ                                                  |
+--------------+---------------------------------+----------------------------------------------------------------------------------------------------------------+

FacetQuery **UNSUPPORTED**
--------------------------

Lors d'une commande GET (Select), les possibilités envisagées sont :

+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Opérateur pour les facet | Arguments                                       | Commentaire                                                                                                                                                                                        |
+==========================+=================================================+====================================================================================================================================================================================================+
| $cardinality             | nom de champ                                    | indique le nombre de valeurs différentes pour ce champ                                                                                                                                             |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $avg, $max, $min, $stats | nom de champ numérique                          | indique la valeur moyenne, maximale, minimale ou l'ensemble des statistiques du champ                                                                                                              |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $percentile              | nom de champ numérique, valeurs optionnelles    | indique les percentiles de répartition des valeurs du champ, éventuellement selon la répartition des valeurs indiquées                                                                             |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $date_histogram          | nom de champ, intervalle                        | indique la répartition selon les dates selon un intervalle définie sous la forme "nX"                                                                                                              |
|                          |                                                 | où n est un nombre et X une lettre parmi y (year), M (month), d(day), h(hour), m(minute), s(seconde) ou encore de la forme "year", "quarter", "month", "week", "day", "hour", "minute" ou "second" |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $date_range              | nom de champ,  format, ranges                   | indique la répartition selon les dates selon un intervalle défini "ranges" : [ { "to": "now-10M/M" }, { "from": "now-10M/M" } ] et "format" : "MM-yyyy"                                            |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $range                   | nom de champ,  intervalles                      | indique la répartition selon des valeurs numériques par la forme "ranges" : [ { "to": 50 }, { "from": 50, "to": 100 }, { "from": 100 } ]                                                           |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $terms                   | nom de champ                                    | indique la répartition selon des valeurs textuelles du champ                                                                                                                                       |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| $significant_terms       | nom de champ principal, nom de champ secondaire | indique la répartition selon des valeurs textuelles du champ principal et affiche pour chaque les termes significatifs pour le second champ                                                        |
+--------------------------+-------------------------------------------------+----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+


Exemples
--------

GET
***

- La query sélectionne les Units qui vont être retournées.
  - Le contenu est :

    - Pour **Units/Objects** :

      - **$roots**
      - **$query**
      - **$filter**
      - **$projection: { fieldname: 0, fieldname: 1 }**
      - **facetQuery**  optionnel (**UNSUPPORTED**)

    - Pour les autres collections :

      - **$query**
      - **$filter**
      - **$projection: { fieldname: 0, fieldname: 1 }**
      - **facetQuery**  optionnel (**UNSUPPORTED**)

Exemple::

    {
      "$roots": [ "id0" ],
      "$query": [
        { "$match": { "Title": "titre" }, "$depth": 4 }
      ],
      "$filter": { "$limit": 100 },
      "$projection": { "$fields": { "#id": 1, "Title": 1, "#type": 1, "#parents": 1, "#object": 1 } },
      "$facetQuery": { "$terms": "#object.#type" } //(**UNSUPPORTED**)
    }


POST
****

- La query sélectionne le ou les Units parents de celle qui va être créée.
  - Le contenu est :

    - Pour **Units/Objects** :

      - **$roots**
      - **$query**
      - **$filter**
      - **$data**

    - Pour les autres collections :

      - **$query**
      - **$filter**
      - **$data**

::

   {
    "$roots": [ "id0" ],
    "$query": [
      { "$match": { "Title": "titre" }, "$depth": 4 }
    ],
    "$filter": {  },
    "$data": { "Title": "mytitle", "description": "my description", "value": 1 }
   }

PUT
***

- La query sélectionne les Units sur lesquelles l'update va être réalisé.
  - Le contenu est :
    - Pour **Units/Objects** :
      - **$roots**
      - **$query**
      - **$filter**
      - **$action**
    - Pour les autres collections :
      - **$query**
      - **$filter**
      - **$action**

::

   {
    "$roots": [ "id0" ],
    "$query": [
      { "$eq": { "Title": "mytitle" }, "$depth": 5 }
    ],
    "$filter": {  },
    "$action": [{ "$inc": { "value": 10 } }]
   }


Response
========

Une réponse est composée de plusieurs parties :

- **$hits**:

  - **limit**: le nombre maximum d'items retournés (limité à 1000 par défaut)
  - **offset**: la position de démarrage dans la liste retournée (positionné à 0 par défaut)
  - **total**: le nombre total potentiel (estimation) des résultats possibles
  - **size**: le nombre réel d'items retournés
  - **time_out**: Vrai si la requête a durée trop longtemps et donc avec un résultat potentiellement partiel

- **$context**: rapelle la requête exprimée
- **$results**: contient le résultat de la requête sous forme d'une liste d'items
- **$facets**: contient le résultat de la partie $facetQuery.

Des champs sont protégés dans les requêtes :

- Il est interdit d'exprimer un champ qui démarre par un *'_'*
- La plupart de ces champs protégés sont interdits à la modification. Ils ne sont utilisables que dans la partie *$projection* ou *$query* mais pas dans la partie *$data*
- Communs Units et Objects

  - **#id** est l'identifiant de l'item
  - **#all** est l'équivalent de "SELECT \*"
  - **#unitups** est la liste des Units parents
  - **#tenant** est le tenant associé
  - **#operations** est la liste des opérations qui ont opéré sur cet élément
  - **#originating_agency** est l'OriginatingAgency su SIP d'origine
  - **#originating_agencies** est l'ensemble des OriginatingAgencies issues du SIP et des rattachements (héritage)

- Spécifiques pour les Units

  - **#unittype** est la typologie du Unit (Arbre HOLLDING_UNIT, Plan FILING_UNIT ou ArchiveUnit INGEST)
  - **#nbunits** est le nombre de fils immédiats à un Unit donné
  - **#object** est l'objet associé à un Unit (s'il existe)
  - **#type** est le type d'item (Document Type)
  - **#allunitups** est l'ensemble des Units parents (depuis les racines)
  - **#management** est la partie règles de gestion associées au Unit (ce champ est autorisée à être modifiée et donc dans *$data*)

- Spécifiques pour les Objects

  - **#type** est le type d'item (Type d'Objet : Document, Audio, Video, Image, Text, ...)
  - **#nbobjects** est le nombre d'objets binaires (usages/version) associé à cet objet
  - **#qualifiers** est la liste des qualifiers disponibles

    - Les "qualifiers" disponibles pour les objets :

      - **PhysicalMaster** pour original physique
      - **BinaryMaster** pour conservation
      - **Dissemination** pour la version de diffusion compatible avec un accès rapide et via navigateur
      - **Thumbnail** pour les vignettes pour les affichages en qualité très réduite et très rapide en "prévue"
      - **TextContent** pour la partie native texte (ASCII UTF8)

La réponse dispose également de champs dans le *Header* :

- **FullApiVersion** : (**UNSUPPORTED**) retourne le numéro précis de la version de l'API en cours d'exécution
- **X-Request-Id** : pour chaque requête, un unique identifiant est fourni en réponse
- **X-Tenant-Id** : pour chaque requête, le tenant sur lequel a été exécutée l'opération demandée
- **X-Application-Id** : (**UNSUPPORTED**) pour conserver la session (valeur non signifiante) dans les journaux et logs associés à l'opération demandée
- **X-Qualifier** et **X-Version** : pour une requête GET sur un **Object** pour indiquer un usage et une version particulière
- **X-Callback** (**UNSUPPORTED**): pour les opérations de longue durée et donc asynchrones pour indiquer l'URL de Callback
- (**UNSUPPORTED**) Si **X-Cursor: true** a été spécifié et si la réponse nécessite l'usage d'un curseur (nombre de réponses > *$per_page*), le SAE retourne **X-Cursor-Id** et **X-Cursor-Timeout** (date de fin de validité du curseur) : pour la gestion d'une requête en mode "curseur" par le client

Exemples
--------

Réponse pour Units
******************

::

   {
    "$hits": {
      "total": 3,
      "size": 3,
      "offset": 0,
      "limit": 100,
      "time_out": false
    },
    "$context": {
      "$roots": [ "id0" ],
      "$query": [
        { "$match": { "Title": "titre" }, "$depth": 4 }
      ],
      "$filter": { "$limit": 100 },
      "$projection": { "$fields": { "#id": 1, "Title": 1, "#type": 1, "#unitups": 1, "#object": 1 } },
      "$facetQuery": { "$terms": "#object.#type" }
    },
    "$results": [
      {
        "#id": "id1", "Title": "titre 1", "#type": "DemandeCongés",
        "#unitups": [ { "#id": "id4", "#type": "DossierCongés" } ],
        "#object": { "#id": "id101", "#type": "Document",
          "#qualifiers": { "BinaryMaster": 5, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 } }
      },
      {
        "#id": "id2", "Title": "titre 2", "#type": "DemandeCongés",
        "#unitups": [ { "#id": "id4", "#type": "DossierCongés" } ],
        "#object": { "#id": "id102", "#type": "Document",
          "#qualifiers": { "BinaryMaster": 5, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 } }
      },
      {
        "#id": "id3", "Title": "titre 3", "#type": "DemandeCongés",
        "#unitups": [ { "#id": "id4", "#type": "DossierCongés" } ],
        "#object": { "#id": "id103", "#type": "Image",
          "#qualifiers": { "BinaryMaster": 3, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 } }
      }
    ],
    "$facet": { // **UNSUPPORTED**
      "#object.#type": { "Document": 2, "Image": 1 }
    }
   }


Réponse pour Objects
********************

::

   {
    "$hits": {
      "total": 3,
      "size": 3,
      "offset": 0,
      "limit": 100,
      "time_out": false
    },
    "$context": {
      "$roots": [ "id0" ],
      "$query": [
        { "$match": { "Title": "titre" }, "$depth": 4, "$source": "units" },
        { "$eq": { "#type": "Document" }, "$source": "objects" }
      ],
      "$filter": { "$limit": 100 },
      "$projection": { "$fields": { "#id": 1, "#qualifiers": 1, "#type": 1, "#unitups": 1 } }
    },
    "$results": [
      {
        "#id": "id101", "#type": "Document",
        "#qualifiers": { "BinaryMaster": 5, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 },
        "#unitups": [ { "#id": "id1", "#type": "DemandeCongés" } ]
      },
      {
        "#id": "id102", "#type": "Document",
        "#qualifiers": { "BinaryMaster": 5, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 },
        "#unitups": [ { "#id": "id2", "#type": "DemandeCongés" } ]
      },
      {
        "#id": "id103", "#type": "Document",
        "#qualifiers": { "BinaryMaster": 3, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 },
        "#unitups": [ { "#id": "id3", "#type": "DemandeCongés" } ]
      }
    ]
   }


Réponse en cas d'erreurs
------------------------

En cas d'erreur, Vitam retourne un message d'erreur dont le format est :

- **httpCode** : code erreur Http
- **code** : code erreur Vitam
- **context** : contexte de l'erreur
- **state** : statut en format de message court sous forme de code
- **message** : statut en format de message court
- **description** : statut détaillé
- **errors** : le cas échéant des sous-erreurs associées avec le même format


Exemple de retour en erreur
***************************

::

   {
    "httpCode": 404,
    "code" : "codeVitam1",
    "context": "ingest",
    "state": "Item_Not_Found",
    "message": "Item is not found",
    "description": "Operation on item xxx cannot be done since item is not found in <<resourcePathName>>",
    "errors": [
      { "httpCode": 415,
        "code" : "codevitam2",
        "context": "ingest",
        "state": "Unsupported_Media_Type",
        "message": "Unsupported media type detected",
        "description": "File xxx has an unsupported media type yyy" },
      { "httpCode": 412,
        "code": "codevitam3",
        "context": "ingest",
        "state": "Precondition_Failed",
        "message": "Precondition in error",
        "description": "Operation on file xxx cannot continue since precondition is in error" }
    ]
   }


Cas particulier : HEAD pour test d'existence et validation (**UNSUPPORTED**)
----------------------------------------------------------------------------

La commande *HEAD* permet de savoir pour un item donné s'il existe (retour **204**) ou pas (retour **404**).

(**UNSUPPORTED**) Si dans le Header est ajoutée la commande **X-Valid: true**, la commande *HEAD* vérifie si l'item (Unit ou Object) existe et s'il est conforme (audit de l'item sur la base de son empreinte). S'il n'est pas conforme mais qu'il existe, le retour est **417** (Expectation Failed).
