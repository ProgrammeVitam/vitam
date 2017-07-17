Cahier des API externes et requêtes DSL
#######################################

Synthèse des tests
==================

Tests par opérateurs du DSL et de leurs faisabilité d'implémentation

+------------------------+---------------------------+---------------------------+
| Opérateur              | Code test                 | Implémentable             |
+========================+===========================+===========================+
| $path                  | #ACC4                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $and                   | #BOOL1, #BOOL2            | OK                        |
+------------------------+---------------------------+---------------------------+
| $not                   | #BOOL1                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $or                    | #BOOL2                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $gt                    | #COMP1                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $lte                   | #COMP2                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $gte                   | #COMP3                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $eq                    | #COMP4                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $gte                   | #COMP5                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $lte                   | #COMP5                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $range                 | #COMP6                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $exists                | #EX1                      | OK                        |
+------------------------+---------------------------+---------------------------+
| $missing               | #EX2                      | OK                        |
+------------------------+---------------------------+---------------------------+
| $isNull                | #EX3                      | OK                        |
+------------------------+---------------------------+---------------------------+
| $in                    | #TAB1                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $nin                   | #TAB2                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $size                  | #TAB3                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $wildcard              | #TEXT1, #TEXT2            | OK                        |
+------------------------+---------------------------+---------------------------+
| $term                  | #TEXT3                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $match                 | #TEXT4, #TEXT5            | OK                        |
+------------------------+---------------------------+---------------------------+
| $matchPhrase           | #TEXT6                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $matchPhrasePrefix     | #TEXT6                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $regex                 | #TEXT7                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $search                | #TEXT8                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $flt                   | #TEXT9                    | DEPRECIE                  |
+------------------------+---------------------------+---------------------------+
| $mlt                   | #TEXT10                   | OK                        |
+------------------------+---------------------------+---------------------------+
| $geometry              | #GEO1                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $box                   | #GEO1                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $polygon               | #GEO1                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $center                | #GEO1                     | OK                        |
+------------------------+---------------------------+---------------------------+
| $depth                 | #DEPTH1                   | OK                        |
+------------------------+---------------------------+---------------------------+
| $exactdepth            | #DEPTH2                   | OK                        |
+------------------------+---------------------------+---------------------------+
| $source                | #COLL1                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $set                   | #PUTS1                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $unset                 | #PUTS2                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $min                   | #PUTS3                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $max                   | #PUTS3                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $inc                   | #PUTS4                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $rename                | #PUTS5                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $push                  | #PUTS6                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $pull                  | #PUTS6                    | OK                        |
+------------------------+---------------------------+---------------------------+
| $add                   | #PUTS7                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $pop                   | #PUTS7                    | BUG                       |
+------------------------+---------------------------+---------------------------+
| $roots                 | #ROOTS1                   | OK                        |
+------------------------+---------------------------+---------------------------+
| $limit                 | #FILTER1                  | BUG                       |
+------------------------+---------------------------+---------------------------+
| $offset                | #FILTER2                  | BUG                       |
+------------------------+---------------------------+---------------------------+
| $orderby               | #FILTER3                  | OK                        |
+------------------------+---------------------------+---------------------------+
| $projection            | #FILTER4                  | OK                        |
+------------------------+---------------------------+---------------------------+



Scénarios de tests par catégories
=================================

**1. Accès direct**

*Cas de test #ACC4*

Opérateur : $path

On va mettre à jour un archive Unit id1 qui est enfant de l’AU id0, on ajoute une nouvelle propriété  : propriétaire est MrT

.. code-block:: json

    {
      "$roots": [ "id0" ],
        "$query": [
          { "$path": "id1" }
        ],
        "$filter": {  },
        "$action": [{ "$add": { "propriétaire": "MrT" } }]
    }

Mais cette requête ne peut pas être utilisé maintenant, parce-que l'API REST ("/units" update) n'est pas encore implémenté.

**2. Booléens**

Opérateur : $and, $or, $not

*Cas de test #BOOL1 :*

- Opérateur $and, $not
- Chercher une unit dont le titre contient “Ecole” et la description ne contient PAS “privé”

*Cas de test #BOOL2 :*

- Opérateur $and, $or
- Chercher une unit dont le titre contient (“Ecole” ET contient “publique”) OU (contient “Enseignement” ET contient “primaire”)

**3. Comparaison**

Opérateur : $eq, $ne, $lt, $lte, $gt, $gte

*Cas de test #COMP1 :*

- Opérateur : $gt
- Chercher une unité d’archive dont la date extrême de fin (EndDate) est strictement supérieure au 1er janvier 2015

*Cas de test #COMP2 :*

- Opérateur  : $lte
- Chercher une unité d’archive dont la date est strictement inférieure au 1er janvier 2015

*Cas de test #COMP3 :*

- Opérateur :  $gte
- Chercher une unité d’archive dont la date est supérieure ou égale au 1er janvier 2015

*Cas de test #COMP4 :*

- Opérateur : $eq
- Chercher une unité d’archive dont la date est le 1er janvier 2015

*Cas de test #COMP5 :*

- Opérateur : $gte, $lte
- Chercher une unité d’archive dont la date est entre le 1er janvier et le 1er mars 2015

*Cas de test #COMP6*

- Opérateur : $range
- Chercher une unité d’archive dont la date de transaction est située entre le 1er janvier 2016 et le 31 décembre 2017
- Etat : l’opérateur ne fonctionne pas dans le DSL


**4. Existence**

Opérateur : $exists, $missing, $isNull

*Cas de test #EX1 :*

- Opérateur $exists
- vérifier que le champ description existe pour une unit donnée

*Cas de test #EX2 :*

- Opérateur $missing
- Retourner toutes les units qui n’ont pas de champ description

*Cas de test #EX3 :*

- Opérateur $isNull
- Retourner toutes les unités d’archive dont la balise empreinte (MessageDigest) existe mais est vide

**5. Tableau**

Opérateur : $in, $nin

*Cas de test :*

- *#TAB1* : chercher les producteur qui a versé plus de 5 et moins de 10 objets ($in)
- *#TAB2* : chercher les producteur qui a versé moins de 5 ou plus de 10 objets ($nin)

*Cas de test #TAB3*

- Opérateur $size
- Compter le nombre de parents pour une unité d’archive donnée


**6. Textuel**

Opérateur :  $wildcard

*Cas de test : Opérateur $wildcard*

- #TEXT1 : rechercher toutes les units dont le titre commence par “Eco”
- #TEXT2 : rechercher toutes les units dont le titre contient “rivé”

*Cas de test $TEXT3*

Opérateur : $term
Etat : actuellement buggé dans l’implémentation du DSL


Opérateur : $match, $matchPhrase, $matchPhrasePrefix

*Cas de test pour $match*  :

- #TEXT4 : rechercher toutes les units dont le titre commence par “Eco” et ne trouver aucune réponse
- #TEXT5 : rechercher toutes les units dont le titre contient “Eco privée” et trouver l’unit dont le titre est ‘Ecole privée’

*Cas de test : #TEXT6*

- Opérateur : $matchPhrase, $matchPhrasePrefix
- Etat : actuellement buggé dans l’implémentation du DSL


*Cas de test : #TEXT7*

- Opérateur : $regex
- Rechercher toutes les units dont le titre commence par “document” et contient “version x.y”, où x et y sont des nombres entier

*Cas de test : #TEXT8 :*

- Opérateur : $search
- Rechercher toutes les units dont le titre contient “privé”

*Cas de test : #TEXT9 :*

- Opérateur : $flt,
- Etat : déprécié dans elasticsearch

*Cas de test : #TEXT10 :*

- Opérateur : $mlt
- Rechercher toutes les units dont le titre contient un mot ressemblant à “privé”




**7. Géomatique**

*Cas de test #GEO1*

- Opérateur : $geometry, $box, $polygon, $center
- Rechercher par coordonnées, pas de réel cas d’usage actuellement dans Vitam



**8. Argument additionnel : profondeur**

Opérateur : $depth, $exactdepth

*Cas de test : #DEPTH1*

- Opérateur $depth
- Chercher le nombre d’unité d’archive qui sont des pères d’une unité donnée dans l’arborescence

*Cas de test : #DEPTH2*

- Opérateur $exactdepth
- Chercher toutes les unités d’archive dont le titre contient “Ecole” à partir d’une unit donnée dans l’arborescence  et uniquement dans les fils directs (depth = 1)

**9. Argument additionnel : collection**

*Cas de test : #COLL1*

- Opérateur : $source
- Chercher tous les groupes d’objets dont le titre de leurs unités d’archive contient ‘document’

**10. Actions PUTS**

Opérateur : $set, unset

*Cas de test : #PUTS1*

- Opérateur $set
- Changer le titre d’une unité d’archive de “Ecole privée” à “Ecole publique”

*Cas de test : #PUTS2*

- Opérateur $unset
- Retirer la description d’une unité d’archive

*Cas de test : #PUTS3*

- Opérateur : $min, $max
- Mettre à jour la date de transaction d’une unité d’archive, en prenant comme valeur extrême le 1er janvier 1950 au plus tôt et le 1er janvier 2000 au plus tard dans le cas où la modification souhaiterait modifier la date en dehors de ces bornes (par exemple en essayant de mettre “1er janvier 2056”)


*Cas de test : $PUTS4*

- Opérateur $inc
- Ajouter 10 ans à une règle de gestion dont la durée est de 5 ans. La durée finale doit être de 15 ans.

*Cas de test #PUTS5*

- Opérateur $rename
- Changer le champ “Recipient” d’une unité d’archive (destinataire en copie) en “Addressee” (destinataire principal), tout en conservant la valeur du champ, en renommant la balise elle même directement

*Cas de test #PUTS6*

- Opérateur : $push, $pull
- Eliminer tous les parents d’une unités d’archives au delà du 5ème niveau de profondeur ascendant
- Etat : actuellement buggé dans l’implémentation du DSL

*Cas de test #PUTS7*

- Opérateur : $add, $pop
- Ajouter/éliminer un parent d’une unité d’archive
- Etat : actuellement buggé dans l’implémentation du DSL


**11/ Racine**

*Cas de test : #ROOTS1*

- Opérateur $roots
- Chercher les archives dont le titre contient “rectorat” à partir d’un certain niveau de l’arborescence, et avec une profondeur de 2 en descendant (vers les filles)

**12/ Filtre**

*Cas de test : #FILTER1*

- Opérateur $limit
- Rechercher les unités d’archive dont le titre contient “Ecole”, mais ne retourner que les 10 premiers résultats
- Etat : actuellement buggé dans l’implémentation du DSL

*Cas de test : #FILTER2*

- Opérateur $offset
- On filtre le résultat des logbook à partir de 200e résultat
- Etat : actuellement buggé dans l’implémentation du DSL

*Cas de test : #FILTER3*

- Opérateur $orderby
- Chercher toutes les opérations, triées par ordre chronologique inverse (de la plus ancienne à la plus récente)

*Cas de test : #FILTER4*

- Opérateur $projection
- Chercher les archive units dont la description est “privé” et ne vouloir en résultat uniquement les valeurs des titres et des dates de transaction


Scénarios de tests non implémentés, par modules
===============================================

**1. Logbook module opération (LGMO) : Afficher les opérations en warning des SIP versés dans les 10 dernières minutes**

Code : LGMO1

``API : {{accessServiceUrl}}/access-external/v1/operations``

Pré-requis :

Soit T l’instant présent. Verser à :

- T+0mon, SIP_WARNING_FORMAT.zip générant un warning
- T+5 min, SIP_bordereau_avec_objet_OK.zip dont le résultat est OK
- T+15 min, WARNING_SIP_sans_objet.zip générant un warning
- Exécuter la requête à T+20

Pour la TransactedDate, la valeur sera ajoutée dynamiquement. Pour cet exemple, on prend une valeur préétablie.


Requête :

.. code-block:: json

    {
      "$query": {
        "$and": [
          {
            "$eq": {"evTypeProc": "INGEST"}
          },
          {
            "$eq": {"outcome": "WARNING"}
          },
          {
            "$gte": {"TransactedDate": "2017-01-04T23:00:00.000Z"}
          }
        ]
      },
      "$filter": {
        "$orderby": {"evDateTime": -1}
      },
      "$projection": {}
    }


Critères d'acceptance:


La requête doit retourner l’ID de l’opération du versement
$result.#id = id du SIP WARNING_SIP_sans_objet.zip

**2. Registre des fonds : lister tous les producteurs qui ont versé plus de 5 objets**

Code : RGSTR1

``API:  {{accessServiceUrl}}/access-external/v1/accession-register``


*Pré-requis :*

Note : SIP avec producteurs ayant versé plus de 5 et moins de 5 objets

- Verser OK_SIP_RGSTR1_PRODUCTEUR_6OBJ.zip contenant le producteur FRAN_NP_001 versant 6 objets

- Verser OK_SIP_RGSTR1_PRODUCTEUR_1OBJ contenant le producteur FRAN_NP_002 versant 1 objet.zip

*Requête :*

.. code-block:: json

     {
       "$query": {
         "$and": [
           {
             "$exists": "OriginatingAgency"
           },
           {
             "$gt": {
               "TotalObjectGroups.Total": 5
             }
           }
         ]
       },
       "$filter": {
         "$orderby": {
           "OriginatingAgency": 1
         }
       },
       "$projection": {}
     }

*Critères d’acceptance :*

La réponse doit renvoyer uniquement le bon producteur :
“OriginatingAgencyIdentifier” : “FRAN_NP_001”

Pour pouvoir exécuter plusieurs fois ce même cas de test sans devoir purger le registre des fonds (ce qui nuirait à l’utilisation normale de la plateforme), il sera intéressant de diversifier automatiquement le nom des producteurs de ces deux SIP afin de les rendre unique à chaque fois que le test est lancé.

Dans le cas contraire FRAN_NP_002 aura versé un objet la première que les tests sont lancés,
2 objets la 2eme fois que le test est lancé sans purge de la base,
6 objets la 6ème fois, ce qui aura pour conséquence que FRAN_NP_002 aura lui aussi versé plus de 6 objets au total et se retrouvera dans les résultats, ce qui n’est pas le comportement désiré pour garantir une bonne qualité du jeu de test.


Scénarios de tests implémentés, par modules
===========================================

**1. Search (SRC)**

**1.1. Chercher les unités d’archives dont les dates extrêmes sont contenues entre 1914-1918 (inclus) et qui contiennent des objets**

Code : SRC1

``API :   {{accessServiceUrl}}/access-external/v1/units``

*Pré-requis :*

Verser le SIP SRC1.zip. Ce SIP contient :

	- 1 unité d’archive dont le titre est « Correspondance » et dont les dates extrêmes sont 1916-1920 et qui contiennent des objets
	- 1 unité d’archive « Compte rendu » dont les dates sont 1910-1916 et qui contiennent des objets
	- 1 unité d’archive « Liste des armements »dont les dates extrêmes sont 1917-1918 et qui contiennent des objets
	- 1 unité d’archive “Vidéos d’époque” dont les dates extrêmes sont 1915-1916 et qui n’a pas d’objet

Les dates extrêmes sont toujours à date du 1er janvier de l’année

*Requête :*

.. code-block:: json

    {
      "$roots": [],
      "$query": [
        {
          "$and": [
            {
              "$gte": {
                "StartDate": "1914-01-01T23:00:00.000Z"
              }
            },
            {
              "$lte": {
                "EndDate": "1918-12-31T22:59:59.000Z"
              }
            }
          ],
          "$depth": 20
        }
      ],
      "$filter": {
        "$orderby": {
          "TransactedDate": 1
        }
      },
      "$projection": {
        "$fields": {
          "TransactedDate": 1,
          "#id": 1,
          "Title": 1,
          "#object": 1,
          "DescriptionLevel" : 1,
          "EndDate": 1,
          "StartDate": 1
        }
      }
    }



*Critères d'acceptance:*

La requête doit retourner uniquement l’unité d’archive répondant aux critères demandés, c’est à dire::

	$result.#id = identifiant de l’opération ayant versée le SIP
	$result.Title = ‘Liste des armements’
	$result.DescriptionLevel = ‘Item’
	$result.StartDate = ‘1917-01-01’
	$result.EndDate = ‘1918-01-01’


**1.2. Chercher les unités dont le titre contient “Rectorat” et dont la description contient “public” ou “privé”**

Code : SRC2


``API : {{accessServiceUrl}}/access-external/v1/units``

*Pré-requis :*

Verser le sip WARNING_SIP_SRC2_TITLE_DESC_UNITS_SANS_OBJ.zip. Ce sip contient :

- 1 unité d’archive dont le titre est “Rectorat de Noisiel”, et dont la description est “Dossier relatif au secteur public”
- 1 unité d’archive dont le titre est “Rectorat de Reims”, et dont la description contient “Dossier relatif au secteur privé”
- 1 unité d’archive dont le titre est “Rectorat de Poitier”, et dont la description est vide
- 1 unité d’archive dont le titre est “Rectorat de Toulouse” et dont la description est “Bilan de l’entretien annuel”
- 1 unité d’archive dont le titre est “Rectorat de Nantes”, et dont la description contient “Comparatif domaine public et privé”


*Requête :*

.. code-block:: json

    {
       "$roots": [],
       "$query": [
         {
           "$or": [
             {
               "$and": [
                 {
                   "$match": {
                     "Title": "Rectorat"
                   }
                 },
                 {
                   "$match": {
                     "Description": "public"
                   }
                 }
               ]
             },
             {
               "$and": [
                 {
                   "$match": {
                     "Title": "Rectorat"
                   }
                 },
                 {
                   "$match": {
                     "Description": "privé"
                   }
                 }
               ]
             }
           ],
           "$depth": 20
         }
       ],
       "$filter": {
         "$orderby": {
           "TransactedDate": 1
         }
       },
       "$projection": {
         "$fields": {
           "TransactedDate": 1,
           "#id": 1,
           "Title": 1,
           "#object": 1
         }
       }
    }


*Critère d’acceptance :*

La requête doit retourner uniquement les unités d’archives suivante :
“Title” : “Rectorat de Noisiel”
“Title” : “Rectorat de Reims”
“Title” : “Rectorat de Nantes”

Ainsi que:

_Id = identifiant de l’opération ayant versée le SIP


**2. Référentiel des règles de gestion (REFRG) : afficher les règles de type AppraisalRule ET dont l'intitulé est "Dossier individuel d’agent civil"**

Code : REFRG2

``API : {{accessServiceUrl}}/admin-external/v1/rules``

*Pré-requis :*

Importer le fichier jeu_donnees_OK_regles_CSV.csv contenant 3  règles dont les intitulés sont “Dossier individuel d’agent civil”

- APP-00001 : Dossier individuel d’agent civil, est une AppraisalRule (DUA)
- STO-00001 : Dossier individuel d’agent civil, est une StorageRule (DUC)
- DIS-00004 : Dossier individuel d’agent civil, est une DisseminatonRule (Règle de communicabilité)

Requête :

.. code-block:: json

     {
      "$query": {
           "$and": [
             {
               "$eq": {
                 "RuleValue": "Dossier individuel d’agent civil"
               }
             },
             {
               "$eq": {
                 "RuleType": "AppraisalRule"
               }
             }
           ]
         },
      "$filter": {},
      "$projection": {}
     }


*Critères d’acceptance :*

La requête doit retourner le résultat qui contient  :

- “RuleId” = “APP-00001”
- Intitulé = Dossier individuel d’agent civil
- Catégorie = AppraisalRule


Si la règle n’existe pas, il va retourner la réponse avec statut 500 (Il doit être 404. Le code est à corriger)


**3. Référentiel des formats (REFRMT) : afficher tous les formats relatifs aux PNG**

Code : REFRMT1

``API : {{accessServiceUrl}}/admin-external/v1/formats``

*Pré-requis :*

Importer le fichier PRONOM Droid Signature Files Version 88. Ce fichier contient 4 formats relatifs à l’extension .png

*Requête :*

.. code-block:: json

     {
     "$query": {
           "$and": [
             {
               "$eq": {
                 "Extension": "png"
               }
             }
           ]
         },
      "$filter": {},
      "$projection": {}
     }


*Critères d’acceptance :*

Affichage de 4 résultats dont les PUID sont fmt/11, fmt/12, fmt13, fmt/935

formatNumber = 4

Content = [...]

.. figure:: images/Png_results.png
	:align: center
	:height: 22 cm

	Capture d’écran du résultat sur le site des archives nationales anglaises pour la recherche PNG

**4. Mise à jour : modifier le titre et la description d’une unité d'archive**

Code : UPDATE1

``API: {{accessServiceUrl}}/access-external/v1/units/{{unit_id}}``

*Pré-requis :*

Verser WARNING_SIP_SRC2_TITLE_DESC_UNITS_SANS_OBJ.zip contenant une unité d’archive dont le titre est “Rectorat de Noisiel” et la description “Dossier relatif au secteur public”.

On souhaite changer le titre pour “Rectorat de Seine-Et-Marne” et la description pour “Dossier relatifs aux secteurs publics et privés”.
*Requête :*

.. code-block:: json

      {
       "$query": [],
       "$filter": {},
       "$action": [
         {
           "$set": {
             "Title": "Rectorat de Seine-Et-Marne",
             "Description": "Dossier relatifs aux secteurs publics et privés"
           }
         }
       ]
      }

*Critères d’acceptance :*

En recherchant cette unité d’archive par son identifiant, on doit retrouver comme valeurs :

- “Id” : identifiant de l’unité d’archive

- “Title” : "Rectorat de Seine-Et-Marne"

- “Description” :  "Dossier relatifs aux secteurs publics et privés"



