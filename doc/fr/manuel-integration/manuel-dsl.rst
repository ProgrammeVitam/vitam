DSL Vitam
#########

Le DSL (Domain Specific Language) est de la solution logicielle Vitam est le langage dédié à l'interrogation de la base de données et du moteur d'indexation et offrant un niveau d'abstraction afin de pouvoir exprimer un grand nombre de possibilités de requêtes. Sa structure est composée de deux parties :

- Request : contient la structure du Body contenant la requête au format JSON. Le DSL permet d'exprimer un grand nombre de possibilités de requêtes. Dans le cadre des collections Metadata (*Units* et *Objects*), cette requête peut être organisée comme un ensemble de sous requêtes.
- Response : contient la structure du Body contenant le résultat au format JSON. Il contient les différentes informations demandées.


Request
=======

Une requête est composée de plusieurs parties, et en particulier le Body qui contient un JSON exprimant la requête.

Elle peut être complétée par quelques valeurs dans le *Header* :

- **X-Application-Id** : pour conserver la session (valeur non signifiante) dans les journaux et logs du SAE associés à l'opération demandée
- **X-Tenant-Id** : pour chaque requête, le tenant sur lequel doit être exécutée la requête
- **X-Qualifier** et **X-Version** : pour une requête GET sur un **Object** pour récupérer un usage et une version particulière
- **X-Http-Method-Override** : pour permettre aux clients HTTP ne supportant pas tous les modes de la RFC 7231 (GET/PUT/DELETE avec Body)


BuilderRequest
--------------

Il existe des Helpers en Java pour construire les requêtes au bon format (hors headers) dans le package **common-database-public.**

- **Request**

  - Multiple : fr.gouv.vitam.common.database.builder.request.multiple avec MultipleDelete, MultipleInsert, MultipleSelect et MultipleUpdate (pour Units et Objects)
  - Single : fr.gouv.vitam.common.database.builder.request.single avec Select, Insert, Delete, Update (pour toutes les autres collections, en fonction des droits et des possibilités)

- **Query** qui sont des arguments d'une Request

  - fr.gouv.vitam.common.database.builder.query avec BooleanQuery, CompareQuery, ... et surtout le **QueryHelper** et le **VitamFieldsHelper** pour les champs protégés (commençant par un '#')
  - dans le cas de update fr.gouv.vitam.common.database.builder.action avec AddAction, IncAction, ... et surtout le **UpdateActionHelper**


Collection Units
----------------

**Points particuliers sur les end points**

- **/units** : il s'agit ici de requêter un ensemble d'archives (Units) sur leurs métadonnées. Bien que non encore supportée, il sera possible de réaliser des UPDATE massifs sur cet end-point.

- **$root** peut être vide ou renseigné : il sera contrôlé via les contrats d'accès associés à l'application.

  - S'il est vide, il prendra les valeurs renseignées par les contrats.
  - S'il contient des identifiants, il sera vérifié que ces identifiants sont bien soient ceux des contrats, soient fils de ceux spécifiés dans ces contrats.

  - Le résultat doit être une liste de Units (vide ou pas)

- **/units/id** : il s'agit ici de requêter depuis un Unit donné. Le résultat peut être multiple selon les query spécifiées (et notamment le *$depth*).

  - **$roots** est implicite à la valeur de id (si une valeur est spécifiée, elle sera ignorée)

    - Cet Id sera toujours contrôlé par rapport aux contrats d'accès associés à l'application.

  - Le résultat doit être une liste de Units (vide ou pas)

- **/units/id/object** : il s'agit ici d'accéder, s'il existe, à l'objet (ObjectGroup) associé à cet Unit.

  - Les query peuvent remonter les métadonnées (Header **Accept: application/json**)
  - Les query peuvent remonter un des objets binaires (Headers **Accept: application/octet-stream** et **X-Qualifier** et **X-Version**)
    - Le résultat doit être une liste de Objects (pour application/json) ou d'un seul objet binaire (pour application/octet-stream)


Collection Objects
------------------

**Points particuliers sur les end points**
**Cette collection est DEPRECATED et va disparaître car elle est contraire aux règles d'accès aux objets à partir d'une ArchiveUnit (/units/id/object).**

- **/objects** : il s'agit ici de requêter un ensemble d'objets sur leurs métadonnées uniquement.

  - **$root** peut être vide ou renseigné : il sera contrôlé via les contrats d'accès associés à l'application et ne concerne que des Id de Units.

    - S'il est vide, il prendra les valeurs renseignées par les contrats (Id de Units parentes).
    - S'il contient des identifiants, il sera vérifié que ces identifiants sont bien soient ceux des contrats, soient fils de ceux spécifiés dans ces contrats (toujours des Id de Units).

  - Le résultat doit être une liste de Objects (vide ou pas)
  - Cette fonction est surtout utile pour des données statistiques sur les objets dans leur ensemble.



Structure générale d'une requête DSL
====================================

Une requête DSL se décompose en 3 parties principales :

- **$query** : la requête, composée de critères de sélection
- **$roots** (optionnel) : les racines à partir desquels la recherche est lancée, uniquement pour les collections units et objects, dont les données sont organisées en mode arborescent
- **$filter** (optionnel) : le tri / la limite en nombre de résultats retournés
- **$projections** (optionnel): un sous ensemble de champs devant être retournés

::

  {
	"$query": {},
	"$filter": {},
	"$projection": {}
  }

Pour comparaison avec le langage SQL ::

   SELECT field1, field2 FROM table WHERE field3 < value LIMIT n SORT field1 ASC

- *SELECT field1, field2* : la Projection
- *FROM table* : la Collection
- *WHERE field3 < value* : la partie query
- *LIMIT n SORT field1 ASC* : les filtres


Différences de requêtage entre le DSL sur les collections Metadata et les autres
================================================================================

Tableau de query et mono-query
------------------------------

Les collections de Metadata, que sont les collections units et objects, se requêtent en utilisant un tableau de $query. Les autres collections de Mongo s'utilisent avec une seule $query.

Par exemple, pour chercher un titre contenant "Alpha" dans une unités archivistique, on utilise **$query[{expression1}]** comme suit :

::

  {
    "$query": [
      {
        "$match": {
          "Title": "Alpha"
        }
      }
    ],
    "$filter": {},
    "$projection": {}
  }

Alors que pour chercher la même chose, mais pour un contrat d'accès dans la collection accesscontract, on utilise :

::

  {
    "$query": {
      "$match": {
        "Name": "ContratTNR"
      }
    },
    "$filter": {},
    "$projection": {}
  }


Parcourir les arborescences
---------------------------

Il est possible sur les collections Metadata d'optimiser les recherches en se servant d'opérateur de parcours de graphe. Il y a en a deux, optionnels, dans le DSL : **roots** et **depth** :

- **$roots[id1, id2, ...]** : il s'agit de la ou des racines (Units) à partir desquelles la requête est exprimée (toutes les recherches sur les Units et Objects sont en mode arborescente). Il correspond d'une certaine façon à "*FROM x*" dans le langage SQL étendu au cas des arborescences. Ceci ne concerne pas les autres collections qui ne manipulent pas des objets arborescents. En l'absence du paramètre, le DSL prendra en $roots par défaut toutes les racines de l'arborescence présente dans le système.
- **$depth:valeur** : spécifie la profondeur maximale dans l'arborescence et à partir des $roots dans laquelle la recherche doit s'exécuter. La *valeur* de $depth doit être un entier positif ou nul. Par défaut, $depth = 1.
  - *$depth = 0* : cherche uniquement sur les unités précisées dans le $roots
  - *$depth =n avec n >0* : cherche sur les unités enfants jusqu'à *n* niveau de profondeur et ne cherche pas dans les unités de $roots elles-mêmes.

Il est recommandé d'associer systématiquement $depth et $roots, et de ne pas utiliser l'un sans l'autre.

Voici quelques exemples :

1/ Rechercher les unités ayant "Alpha" dans leurs titres, et étant une enfant d'une des unités ayant pour _id GUID1 ou GUID2, peu importe leurs niveaux de profondeurs

::

  {
    "$roots": [
      "GUID1",
      "GUID2"
    ],
    "$query": [
      {
        "$match": {
          "Title": "Alpha"
        }
      }
    ],
    "$filter": {},
    "$projection": {}
  }


2/ Pour effectuer cette recherche uniquement sur les enfants directs des unités de $roots
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    {
      "$roots": [
        "GUID1",
        "GUID2"
      ],
      "$query": [
        {
          "$match": {
            "Title": "Alpha"
          },
          "$depth": 1
        }
      ],
      "$filter": {},
      "$projection": {}
    }


3/ Rechercher dans tous les enfants directs de toutes les racines du système

::

  {
  	"$query": [{
  		"$match": {
  			"Title": "Alpha"
  		},
  		"$depth": 1
  	}],

  	"$filter": {},
  	"$projection": {}
  }

4/ Rechercher uniquement dans toutes les racines du système

::

  {
  	"$query": [{
  		"$match": {
  			"Title": "Alpha"
  		},
  		"$depth": 0
  	}],
    	"$filter": {},
  	"$projection": {}
  }


Opérateur $roots (collection units et objects)
==============================================

- **$roots** (optionnel), pour les collections objets et units uniquement (organisation arborescente des données). Voir la section "Parcourir les arborescences"

Opérateur $query
================

- $query peut contenir plusieurs Query, qui seront exécutées successivement (tableau de Query).
- Une query correspond à la formulation "*WHERE xxx*" dans le langage SQL, c'est à dire les critères de sélection.
- La succession est exécutée avec la signification suivante :

- Depuis $roots, chercher les Units/Objects tel que Query[1], conduisant à obtenir une liste d'identifiants[1]
- Cette liste d'identifiants[1] devient le nouveau $roots, chercher les Units/Objects tel que Query[2], conduisant à obtenir une liste d'identifiants[2]
- Et ainsi de suite, la liste d'identifiants[n] de la dernière Query[n] est la liste de résultat définitive sur laquelle l'opération effective sera réalisée (SELECT, UPDATE, INSERT, DELETE) selon ce que l'API supporte (GET, PUT, POST, DELETE).
- Chaque query peut spécifier une profondeur où appliquer la recherche (voir opérateur $depth)
- Le principe est résumé dans le graphe d'états suivant :

.. image:: images/multi-query-schema.png

Une query peut être exprimée avec des opérateurs :

+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Catégorie          | Opérateurs                                              | Arguments                                  | Utilisable sur champs analysés ? | Utilisable sur champs non analysés ? | Commentaire                                                                  |
+====================+=========================================================+============================================+==================================+======================================+==============================================================================+
| Booléens           | $and, $or, $not                                         | opérateurs                                 | NA                               | NA                                   | Combinaison logique d'opérateurs                                             |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Comparaison        | $eq, $lt, $lte, $gt, $gte                               | Champ et valeur                            | Non                              | **Oui**                              | Comparaison de la valeur d'un champ et la valeur passée en argument          |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
|                    | $range                                                  | Champ, $lt, $lte, $gt, $gte et valeurs     | Non                              | **Oui**                              | Comparaison de la valeur d'un champ avec l'intervalle passé en argument      |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Existence          | $exists                                                 | Champ                                      | **Oui**                          | **Oui**                              |                                                                              |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Tableau            | $in                                                     | Champ et valeurs                           | Non                              | **Oui**                              | Présence de valeurs dans un tableau                                          |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Textuel            | $wildcard                                               | Champ, mot clef                            | Non                              | **Oui**                              | Comparaison de champs mots-clefs à valeur exacte                             |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
|                    | $match, $match_all, $match_phrase, $match_phrase_prefix | Champ, phrase, $max_expansions (optionnel) | **Oui**                          | Non                                  | Recherche plein texte soit sur des mots, des phrases ou un préfixe de phrase |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
|                    | $regex                                                  | Champ, Expression régulière                | Non                              | **Oui**                              | Recherche via une expression régulière                                       |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
|                    | $search                                                 | Champ, string avec opérateur du $search    | **Oui**                          | Non                                  | Recherche du type moteur de recherche                                        |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+
| Parcours de graphe | $depth                                                  | entier positif ou nul                      | NA                               | NA                                   | Recherche jusqu'à un niveau de profondeur                                    |
+--------------------+---------------------------------------------------------+--------------------------------------------+----------------------------------+--------------------------------------+------------------------------------------------------------------------------+



$and, $or, $not : combinaison logique d'opérateurs
--------------------------------------------------

Format :

- **$and : [ expression1, expression2, ... ]** où chaque expression est une commande et *toutes* les commandes doivent être vérifiées
- **$or : [ expression1, expression2, ... ]** où chaque expression est une commande et *au moins une* commande doit être vérifiée
- **$not : [ expression1, expression2, ... ]**  où chaque expression est une commande et *aucune* ne doit être vérifiée ($not[condition A, condition B] peut donc s'écrire sous une forme plus explicite de : $or[$not(condition A), $not(condition B)]).

- Exemple :
:::::::::::

  {
  	"$roots": [],
  	"$query": [{
  		"$or": [{
  				"$match": {
  					"Title": "Porte de Bagnolet"
  				}
  			},
  			{
  				"$and": [{
  						"$match": {
  							"Title": "porte"
  						}
  					},
  					{
  						"$not": [{
  							"$match": {
  								"Title": "Chapelle"
  							}
  						}]
  					}
  				]
  			}
  		],
  		"$depth": 20
  	}],
  	"$filter": {
  		"$offset": 0,
  		"$limit": 100
  	},
  	"$projection": {
  		"$fields": {
  			"Title": 1
  		}
  	}
  }



Sur la collection units, cette requête demande toutes les unités archivistiques dont le titre est "Porte de bagnolet" ou dont le titre contient "porte" mais pas "chapelle"

$eq, $ne, $lt, $lte, $gt, $gte : recherche par comparateurs d'égalité
---------------------------------------------------------------------

Comparaison de la valeur d'un champ et la valeur passée en argument

Format :
- { **"$eq" : { name : value }** } : où name est le nom du champ, et value est la valeur recherchée (Equals)
- { **"$ne" : { name : value }** } : où name est le nom du champ, et value est la valeur recherchée (Not Equals)
- { **"$lt" : { name : value }** } : où name est le nom du champ, et value est la valeur à comparer (Less Than)
- { **"$lte" : { name : value }**} : où name est le nom du champ, et value est la valeur à comparer (Less Than or Equal)
- { **"$gt" : { name : value }** } : où name est le nom du champ, et value est la valeur à comparer (Greater Than)
- { **"$gte" : { name : value }**} : où name est le nom du champ, et value est la valeur à comparer (Greater Than or Equal)

Exemples :
::::::::::

  { "$eq" : { "Identifier" : "CT-000001" } }
  { "$eq" : { "StartDate" : "2014-03-25" } }
  { "$ne" : { "PI" : 3.14 } }
  { "$ne" : { "Status" : true } }
  { "$lt" : { "Identifier" : "CT-000001" } }
  { "$lte" : { "StartDate" : "2014-03-25" } }
  { "$gt" : { "PI" : 3.14 } }
  { "$gte" : { "Count" : 3 } }



Comparaison de nombres réels
++++++++++++++++++++++++++++

Lors d'une comparaison d'un nombre réel, seule la partie entière est comparée :

- { "$eq" : { "#max" : "1.5" } } : trouvera tous les #max à "1"
- { "$gte" : { "#max" : "2.5" } } : trouvera tous les #max supérieur ou égaux à 2 (et non à 2.5)


Notes :
- Ces opérateurs ne doivent être utilisés que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.
- La comparaison doit se faire entre le même type. Le comportement dans le cas de types de données différents (par exemple comparer une date et un booléen) est non supporté.

$range : Recherche par intervalle de valeurs
--------------------------------------------

Il s'agit d'un raccourci pour les opérateurs $lt, $lte, $gt et $gte pour effectuer des recherches sur un intervalle ouvert, semi-ouvert ou fermé.

Format :
{ **$range** : { name : { minOperator : minValue, maxOperator : maxValue } } } : où name est le nom du champ, minOperator est l'opérateur de comparaison ($gt ou $gte),  minValue est la valeur de comparaison minimale, maxOperator est l'opérateur de comparaison ($lt ou $lte),  maxValue est la valeur de comparaison maximale.

Exemples :
::::::::::

  { "$range" : { "Identifier" : { "$gte" : "CT-000001", "$lte" : "CT-000009" } } }
  { "$range" : { "StartDate" : { "$gt" : "2014-03-25", "$lt" : "2014-04-25" } } }
  { "$range" : { "Count" : { "$gte" : 0, "$lt" : 10 } } }


Notes :
  - Cet opérateur ne doit être utilisé que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.
  - La comparaison doit se faire entre le même type. Le comportement dans le cas de types de données différents (par exemple comparer une date et un booléen) est non supporté.
  - Aucune vérification n'est effectuée quant aux valeurs passées dans les *$gt* et *$lt* (ou encore *$gte* et *$lte*), ce qui signifie qu'en cas de mauvais range (ex: "$gt" : "2014-04-25", "$lt" : "2014-04-24") la query sera considérée correcte, mais aucun résultat ne sera retourné.
  - Les dates dans VITAM étant la plupart du temps au format ISO, pour rechercher des Units ayant leur StartDate sur un jour donné, il convient donc d'utiliser une Query range.


$exists : test d'existence d'au moins une valeur non nulle dans un champ.
-------------------------------------------------------------------------

Format:

{ **"$exists" : name** } : où name est le nom du champ à vérifier.

Exemples :
::::::::::

  L'application de la requête suivante : { "$exists" : "Data" } sélectionne les documents suivants :
  { "Data": false }            : Booléen
  { "Data": "2017-01-01" }     : Date
  { "Data": "" }               : chaîne ou texte vide
  { "Data": "DATA" }           : chaîne ou texte non vide
  { "Data": [ "DATA" ] }       : Tableau contenant au moins une valeur non nulle
  { "Data": [ "DATA", null ] } : Tableau contenant au moins une valeur non nulle


**$exists** n'est pas prévu pour interroger des champs existant mais vides. L'application de la même requête ( { "$exists" : "Data" } ) ne sélectionne pas les documents suivants :
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

  { "Data": null }             : Champ null
  { "Data": [ ] }              : Tableau vide
  { "Data": [ null ] }         : Tableau vide
  { "PasDeChampData" }         : Champ inexistant


$in : recherche dans une liste de valeurs
-----------------------------------------

Format :
{ **$in** : { name : [ value1, value2, ... ] } } : où name est le nom du champ, valueN les valeurs recherchées. Il suffit d'une seule valeur présente dans le tableau pour qu'il soit sélectionné.

Exemples :
::::::::::

  { $in : { "Identifier" : [ "CT-000001", "CT-000002" ] } }
  { $in : { "StartDate" : [ "2014-03-25", "2014-03-26" ] } }
  { $in : { "HierarchyLevel" : [ 1, 2 ] } }


Notes :

- Cet opérateur ne doit être utilisé que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.
- La comparaison doit se faire entre le même type. Le comportement dans le cas de types de données différents (par exemple comparer une date et un booléen) est non supporté.

$wildcard : recherche via une expression générique
--------------------------------------------------

Recherche via une expression générique (wildcard).

Format :
{ **"$wildcard"** : { name : expression } } : où name est le nom du champ, expression l'expression recherchée.

Les wildcards autorisés dans *expression* sont :
  - « * » : qui correspond à toute séquence de caractères, vide inclus.
  - « ? » : qui correspond à un caractères unique

Exemples :
  - { "$wildcard" : { "Champ" : "vo*re" } } : Retourne les chaînes qui commencent pas anti par "vo" et se termine par "re" (Ex : vore, votre, voiture).
  - { "$wildcard" : { "Champ" : "vo?re" } } : Retourne les chaînes qui commencent pas anti par "vo" suivi d'un caractère et se terminant par "re". (Ex: voire, votre)

Recherche de toutes les unités archivistique dont le DescriptionLevel commence par "re" et se termine par "grp"
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

  {
  "$query": [
    {
      "$wildcard": {
        "DescriptionLevel": "Re*Grp"
      }
    }
  ],
  "$filter": {
    "$offset": 0,
    "$limit": 1000
  },
  "$projection": {}
  }

Notes :

- Les expressions génériques peuvent être très lentes et très coûteuses.
- Cet opérateur ne doit être utilisé que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.


$match, $match_all, $match_phrase, $match_phrase_prefix : recherche full-text
-----------------------------------------------------------------------------

Recherche plein texte utilisant la recherche approchante du moteur d'indexation.

Format :
  - **$match : { name : words, $max\_expansions : n }** : où *name* est le nom du champ et *words* les mots recherchés, avec un opérator OR entre chaque mots. *$match* cherche donc **au moins un** mot spécifié dans les *$words* Le paramètre optionnel $max_expansions : *n* indique une extension des mots recherchés ("seul" avec n=5 permet de trouver "seulement")
  - **$match\_all : { name : words, $max\_expansions : n }** où *name* est le nom du champ, *words* les mots recherchés, dans n'importe quel ordre, avec un opérateur AND entre chaque mots. *$match\_all* cherche **tous** les mots spécifiés dans les *$words*, dans n'importe quel ordre. Le paramètre optionnel $max_expansions : *n* indiquant une extension des mots recherchés ("seul" avec n=5 permet de trouver "seulement")
  - **$match\_phrase : { name : words, $max\_expansions : n }** où *name* est le nom du champ et *words* les mots recherchés, avec un opérator AND entre chaque mots. De plus *$match\_phrase\_prefix* cherche **tous** les mot spécifiés dans les *$words* en tenant également compte de **l'ordre des mots**

Chaque mot recherché dans $match, $match\_all et $match\_phrase est analysé individuellement, utilisant la recherche approximative du moteur d'indexation.

- **$match\_phrase\_prefix : { name : words, $max\_expansions : n }** où *name* est le nom du champ et *words* les mots recherchés, avec un opérator AND entre chaque mots. De plus *$match\_phrase\_prefix* cherche **tous** les mot spécifiés dans les *$words* en tenant également compte de **l'ordre des mots**. Cet opérateur effectue sur recherche exacte (et non approchante) sur chaque mots de *$words* à l'exception du dernier mot, qui bénéficie de la recherche approchante.

Note :

- Cet opérateur ne doit être utilisé que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.

Exemples :

Recherche dans un champ "Title": "Voyez ce koala fou qui mange des journaux et des photos dans un bungalow"

Pour $match :
:::::::::::::

  { "$match" : { "Title" : "koala fou" } } : OK
  { "$match" : { "Title" : "fou koala" } } : OK (pas d'ordre des mots)
  { "$match" : { "Title" : "fous koalas" } } : OK (la recherche approchante trouve les pluriels)
  { "$match" : { "Title" : "koala chocolat" } } : OK (correspondance partielle)
  { "$match" : { "Title" : "Dessert chocolat" } } : KO (aucun mot trouvé)


Pour $match_all :
:::::::::::::::::

  { "$match_all" : { "Title" : "koala fou" } } : OK
  { "$match_all" : { "Title" : "fou koala" } } : OK (pas d'ordre des mots)
  { "$match" : { "Title" : "fous koalas" } } : OK (la recherche approchante trouve les pluriels)
  { "$match_all" : { "Title" : "koala chocolat" } } : KO (Pas de correspondance partielle)
  { "$match_all" : { "Title" : "Dessert chocolat" } } : KO (Pas de correspondance)


Pour $match\_phrase :
:::::::::::::::::::::

  { "$match_phrase" : { "Title" : "koala fou" } } : OK
  { "$match" : { "Title" : "koalas fous" } } : OK (la recherche approchante trouve les pluriels)
  { "$match_phrase" : { "Title" : "fou koala" } } : KO (l'ordre des mots n'est pas respecté)
  { "$match_phrase" : { "Title" : "koala chocolat" } } : KO (Pas de correspondance partielle)


Pour $match\_phrase\_prefix :
:::::::::::::::::::::::::::::

  { "$match_phrase_prefix" : { "Title" : "koala fou" } } : OK (Correspondance complète)
  { "$match_phrase_prefix" : { "Title" : "koala f" } } : OK (Correspondance avec préfixe sur le dernier terme)
  { "$match" : { "Title" : "koalas fou" } } : KO (le premier mot utilise une recherche exacte)
  { "$match_phrase_prefix" : { "Title" : "fou koala" } } : KO (Ordre non respecté)
  { "$match_phrase_prefix" : { "Title" : "koala chocolat" } } : KO (Pas de correspondance partielle)


$regex : recherche via une expression régulière
-----------------------------------------------

La syntaxe utilisée est celle d'elasticsearch.

Format :
{ "$regex" : { name : regex } } : où *name* est le nom du champ et *regex* l'expression régulière recherchée.

Exemple :
{ "$regex" : { "Champ" : "ABCD[0-9]+" } } : Retourne les chaînes qui commence par ABCD suivis par au ou plusieurs chiffres.

Notes :

- Les expressions régulères peuvent être très lentes et très coûteuses.
- Cet opérateur ne doit être utilisé que pour les champs de type chaîne non analysée, dates, nombres et booléens. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.

$search : recherche approchante avec opérateurs
-----------------------------------------------

Permet des recherches approchantes avec des expression exactes, des opérateurs ET  / OU / NON, des expressions génériques (wildcards)...

Format :
{ **"$search" : { name : searchParameter }**  } : où *name* est le nom du champ, *searchParameter* est une expression de recherche.

L'expression *searchParameter* peut être formulée avec les opérateurs suivants :
  - « + » signifie AND
  - « | » signifie OR
  - « - » empêche le mot qui lui est accolé (tout sauf ce mot)
  - « " » permet d'exprimer un ensemble de mots en une phrase (l'ordre des mots est impératif dans la recherche)
  - « * » Avant la fin de la racine d'un mot signifie que l'on recherche tous les mots commençant par ce début de mot. Attention :
    - Si " * " est placé après la racine du mot, il est possible de ne pas avoir de résultat. Ne pas confondre le comportement de cet opérateur dans le $search avec celui utilisé dans le $wildcard.
    - L'utilisation du "*" empêche les recherches avec des accents. Ainsi un search de "éco*" ne trouvera pas "école", en revanche "eco" trouvera "école". Il est donc recommandé de passer les recherches sans accents en cas d'utilisation de '*'
  - « ( » et « ) » signifie une précédence dans les opérateurs (priorisation des recherches AND, OR)
  - « ~N » après un mot est proche du * mais en limitant le nombre de caractères dans la complétion (fuzziness)
  - « ~N » après une phrase (encadré par ") autorise des "trous" dans la phrase, N étant le nombre de mots maximums autorisés pour la complétion

Par défaut, *$search* effectue un OR entre chaque mot de l'expression de recherche. Chaque terme de recherche est analysé séparemment, aussi rechercher "une descript pointue" retournera des résultats comportant de mots assimilés à "descript" et à "pointue" (description, descriptions, descriptif...)

Exemples :

- { "$search" : { "Title" : alpha bravo charlie } } : cherche un titre contenant au moins un mot entre alpha, bravo et charlie
- { "$search" : { "Title" : alpha +bravo charlie } } : cherche un titre contenant le mot bravo obligatoirement
- { "$search" : { "Title" : +alpha -bravo } } : cherche un titre contenant le mot alpha **ou** ne contenant pas le mot bravo
- { "$search" : { "Title" : +alpha +-bravo } } : cherche un titre contenant le mot alpha **et ne contenant pas** le mot bravo
- { "$search" : { "Title" : +alpha -"\"bravo charlie\"" } } : cherche un titre contenant le mot alpha et ne contenant pas l'expression "bravo charlie"
- { "$search" : { "Title" : "\"alpha delta\"~1" } } : cherche un titre contenant l'expression "alpha delta", plus un mot dans cette expression. Ainsi "bravo alpha delta", "alpha bravo delta" sont trouvés mais pas "alpha bravo charlie delta". Il aurait fallu utiliser { "Title" : "\"alpha delta\"~2" }  ou >2 pour cela.
- { "$search" : { "Title" : "alp\*2" } } : cherche un titre contenant le mot alpha **ou** ne contenant pas le mot bravo
- { "$search" : { "Title" : "+alpha +(bravo | charlie) } } : cherche un titre contenant le mot alpha et contenant soit le mot bravo et/ou le mot charlie

Note :

- Cet opérateur ne doit être utilisé que pour les champs de type chaîne analysée. Le comportement dans le cas d'un champ de type texte analysé ou null est non supporté.


Cas particulièr : recherches par #id
------------------------------------

Un cas particulier est traité dans ce paragraphe, il s'agit de la recherche par identifiant technique.
Sur les différentes collections, le champ #id est un champ obligatoire peuplé par Vitam. Il s'agit de l'identifiant unique du document (unité archivistique, groupe d'objets, différents référentiels...) représentée sous la forme d'une chaîne de 36 caractères correspondant à un GUID.
Il est possible de faire des requêtes sur ce champ, voici les opérateurs à privilégier dans ce cadre : 
- { **"$eq" : { "#id" : value }** } : où value est la valeur recherchée sous forme d'un GUID
- { **"$in" : { "#id" : [ value1, value2, ... ] }** } : où valueN sont les valeurs recherchées sous forme de GUID.

Il est aussi possible, mais moins recommandé, de faire : 
- { **"$ne" : { "#id" : value }** } : étant la recherche eq inversée
- { **"$nin" : { "#id" : [ value1, value2, ... ] }** } : étant la recherche in inversée


Opérateur $filter
=================

- **$filter**:

  - Il permet de spécifier des filtres additionnels pour *GET* :

    - **$limit**: le nombre maximum d'items retournés (limité à 1000 par défaut)

    - **$offset**: la position de démarrage dans la liste retournée (positionné à 0 par défaut)

    - **$orderby: { fieldname: 1, fieldname: -1 }** : permet de définir un tri ascendant ou descendant

      - **IMPORTANT** : pour un champ analysé (plein texte), le tri n'est pas lexicographique mais basé sur le score de correspondance
        - si le nom du champ est **#score**, cela permet de trier volontairement par la pertinence avant l'apparition d'une requête plein texte (par défaut, toute recherche contenant du plein texte trie sur la pertinence lors de l'apparition de la clause).

    - **$projection: { fieldname: 0, fieldname: 1 }** uniquement pour *GET* (SELECT)

      - Cet opérateur permet de ne récupérer qu'un sous ensemble de champs, déclarés dans la requête. Il correspond au "*SELECT \**" dans le langage SQL.
      - Une valeur à 1 réclame le champ.
      - Si rien n'est spécifié, cela correspond à tous les champs (équivalent à "SELECT \*")




Response
========

Une réponse est composée de plusieurs parties :

- **$hits**:

 - **limit**: le nombre maximum d'items retournés
 - **offset**: la position de démarrage dans la liste retournée
 - **total**: le nombre total des résultats correspondants à la recherche
 - **size**: le nombre réel d'items retournés (limité par $limit et par le nombre réel de résultat possible)
 - **time_out**: Vrai si la requête a durée trop longtemps et donc avec un résultat potentiellement partiel

- **$context**: rappelle la requête exprimée
- **$results**: contient le résultat de la requête sous forme d'une liste d'items


Des champs sont protégés dans les requêtes :

- Il est interdit d'exprimer un champ qui préfixé par un *'_'* dans la base de données (_id, _tenant...)
- La plupart de ces champs protégés sont interdits à la modification. Ils ne sont utilisables que dans la partie *$projection* ou *$query* mais pas dans la partie *$data*

La réponse dispose également de champs dans le *Header* :

- **X-Request-Id** : pour chaque requête, un unique identifiant est fourni en réponse
- **X-Tenant-Id** : pour chaque requête, le tenant sur lequel a été exécutée l'opération demandée
- **X-Application-Id** : pour conserver la session (valeur non signifiante) dans les journaux et logs associés à l'opération demandée
- **X-Qualifier** et **X-Version** : pour une requête GET sur un **Object** pour indiquer un usage et une version particulière


Exemples
--------

Réponse pour Units
++++++++++++++++++

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
++++++++++++++++++++

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
- **description** : explication de l'erreur

Exemple
+++++++
::

  {
    "httpCode": 412,
    "code": "020100",
    "context": "External Access",
    "state": "Input / Output",
    "message": "Access external client error in selectUnits method.",
    "description": "Invalid Parse Operation"
  }
