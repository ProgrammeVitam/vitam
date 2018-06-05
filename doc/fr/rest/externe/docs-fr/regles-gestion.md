Cette section fait un focus sur le cas des règles de gestion.
Traitées comme n'importe quelle données pour la plupart des requêtes, le fait que certains champs soient calculés (hérités) fait des règles de gestion une donnée plus complexe disposant d'une fonctionnalité supplémentaire.

On utilisera comme jeu de test le SIP **SIP 1069_OK_RULES_COMPLEXE_COMPLETE.zip**

# Structure des règles de gestion et héritage

La structure des règles de gestion est décrite plus amplement dans le document du modèle de données. Pour faire un rapide rappel, les règles de gestion sont décrites au travers de catégorie de règles (StorageRule, AccessRule...). Chacune de ces catégories sont composées d'un ou plusieurs variables :

  * Celles liées à la catégorie de la règle, comme le sort final pour les StorageRule ou AppraisalRule, des données propres aux règles de classification pour la ClassificationRule et des paramètres pour bloquer l'héritage comme PreventInheritance ou RefNonRuleId
  * Celles propres à une règle en particulier : l'identifiant de la règle, ses dates de début et parfois de fin d'application

- Pour retrouver cette structure, on va chercher l'AU dont le titre est Botzaris (Title = Botzaris)

```json
{
  "$roots": [],
  "$query": [
    { "$match": { "Title": "Botzaris" } }
  ],
  "$filter": {
    "$orderby": { "TransactedDate": 1 }
  },
  "$projection": { }
}
```

On voit dans la réponse cet extrait :

```json
"#management": {
      "AccessRule": {
        "Rules": [
          {
            "Rule": "ACC-00003",
            "StartDate": "2002-01-01",
            "EndDate": "2027-01-01"
          }
        ]
      }
    }
```

"Botzaris" a donc une règle d'accessibilité d'identifiant ACC-00003 qui s'étend du 1er janvier 2002 au 1er janvier 2027.

Les AU situées en dessous de Botzaris dans l'arborescence vont donc hériter de cette règle. On cherche dans les AU celles qui ont pour père Botzaris en utilisant l'ID de Botzaris qu'on aura récupéré et en le cherchant dans les #allunitups (c'est à dire les parents de l'AU) du système :

```json
{
  "$roots": [],
  "$query": [
    {
      "$eq": {
        "#allunitups": "aeaqaaaaaahexbfjaallkaldvongvhyaaaba"
      }
    }
  ],
  "$filter": {},
  "$projection": {}
}
```

Dans les résultats on récupère une AU dont le titre est : "Buttes-Chaumont". Cette AU ne possède pas de règle de gestion et c'est normal : seule les règles de gestions enregistrées en base sont retournées, il n'y a pas de calcul d'héritage.

Pour voir les règles réellement appliquées à l'AU au travers l'héritage, il faut utiliser l'opérateur $rules dans la $projection. En reprenant la même requête que ci dessus et en lui demandant l'héritage :

```json
{
  "$roots": [],
  "$query": [
    {
      "$eq": {
        "#allunitups": "aeaqaaaaaahexbfjaallkaldvongvhyaaaba"
      }
    }
  ],
  "$filter": {},
  "$projection": {
    "$fields": {
      "$rules": 1
    }
  }
}
```
On obtient un bloc "inheritedRule" (règles héritées) dans les réponses dont voici un extrait :

```json
  "inheritedRule": {
       "DisseminationRule": {
         "DIS-00001": {
           "aeaqaaaaaahexbfjaallkaldvongvhiaaaba": {
             "StartDate": "2000-01-01",
             "EndDate": "2025-01-01",
             "path": [
               [
                 "aeaqaaaaaahexbfjaallkaldvongvhiaaaba",
                 "aeaqaaaaaahexbfjaallkaldvongviaaaacq",
                 "aeaqaaaaaahexbfjaallkaldvongvhyaaaba",
                 "aeaqaaaaaahexbfjaallkaldvongvhyaaacq"
               ],
               [
                 "aeaqaaaaaahexbfjaallkaldvongvhiaaaba",
                 "aeaqaaaaaahexbfjaallkaldvongvhiaaacq",
                 "aeaqaaaaaahexbfjaallkaldvongvhyaaaba",
                 "aeaqaaaaaahexbfjaallkaldvongvhyaaacq"
               ]
             ]
           }
         }
       }}
```

Comprenant donc :

  * L'objet "inheritedRule"
  * Les catégories de règles, ici "DisseminationRule"
  * Les identifiant de règles dans chaque catégorie ("DIS-00001")
  * Les dates de débuts et fin d'applications
  * Les chemins d'héritage, c'est à dire tous les chemins possibles ayant pour conséquence que cette AU hérite de cette règle (dans l'exemple il y a deux chemins possibles). Le chemin étant une succession d'identifiant d'AU

**Attention : les requêtes utilisant $rules peuvent être très coûteuses pour le système. Il convient de limiter le périmètre de la requête au maximum pour éviter un recalcul massif inutilement**

# Les métadonnées de catégories

Dans le SIP utilisé pour ce jeu de test, on peut chercher l'AU de titre "Eglise de Pantin". On remarquera dans cette AU le retour concernant la métadonnée de sort final (FinalAction) et celles des règles de classification (ClassificationLevel et ClassificationOwner), dont voici un extrait :

```json
"StorageRule": {
      "Rules": [
        {
          "Rule": "STO-00001",
          "StartDate": "2000-01-01",
          "EndDate": "2001-01-01"
        }
      ],
      "FinalAction": "Copy"
    },
    "ClassificationRule": {
           "ClassificationLevel": "Secret Défense",
           "ClassificationOwner": "RATP",
           "Rules": [
             {
               "Rule": "CLASS-00001",
               "StartDate": "2000-01-01",
               "EndDate": "2010-01-01"
             }
           ]
         }
       },
```

Pour finir en cherchant l'AU de titre "1_Saint Denis Université" on peut voir la structure de PreventInheritance (qui indique que toutes les AU sous celle ci n'hériteront pas des règles AccessRule)

```json
"#management": {
      "AccessRule": {
        "Inheritance": {
          "PreventInheritance": true,
          "PreventRulesId": []
        }
      }
    },
```

# Chercher des AU en utilisant des règles de gestion

- On cherche les AU dans l'arborescence qui ne seront pas communicables au 01/01/2018 sans prendre en compte l'héritage (= les AU qui ont une AccessRule avec une EndDate postérieure au 01/01/2018 et pas d'utilisation du $rules).

```json
{
  "$roots": [],
  "$query": [
    { "$gt": { "#management.AccessRule.Rules.EndDate": "2018-01-01" } }
  ],
  "$filter": {
    "$orderby": { "TransactedDate": 1 }
  },
  "$projection": {
    "$fields": { "#id": 1, "Title": 1 }
  }
}
```

- On cherche les AU qui ont une AppraisalRule avec sort final = Destroy

```json
{
  "$roots": [],
  "$query": [
    { "$eq": { "#management.AppraisalRule.FinalAction": "Destroy" } }
  ],
  "$filter": {
    "$orderby": {
      "TransactedDate": 1
    },
    "$limit": 100
  },
  "$projection": {
    "$fields": {
    	"$rules" : 1, "Title" : 1
    }
  }
}
```

# Modifier des règles de gestion

En utilisant l'opérateur $set comme pour n'importe quelle variable, on peut modifier des règles de gestion. Pour cela, il est nécessaire de rédéclarer l'intégrité de la catégorie de règle dont un élément va être changé **à l'exception des règles de fin (EndDate) qui sont automatiquement calculées par Vitam**.

- On modifie une date de début de règle

Par exemple on veut changer la date de début (StartDate) d'une AccessRule de Botzaris (voir ci-dessus) pour lui donner une nouvelle date.

Pour rappel voici ce que l'API retourne au sujet des règles de Botzaris :

```json
"#management": {
      "AccessRule": {
        "Rules": [
          {
            "Rule": "ACC-00003",
            "StartDate": "2002-01-01",
            "EndDate": "2027-01-01"
          }
        ]
      }
    },
```

La requête de mise à jour sera alors :

```json
{
	"$action": [{
		"$set": {
			"#management.AccessRule.Rules": [{
				"Rule": "ACC-00003",
				"StartDate": "2020-01-01"
			}]
		}
	}]
}
```

La nouvelle date de fin (EndDate) qui correspond à la date de début + la durée de la règle sera enregistrée par Vitam.

Si Botzaris avait initialement deux AccessRule, par exemple "ACC-00003" et "ACC-00004", cette dernière ACC-0004 aurait été supprimée par la requête de mise à jour car la requête redéfinit l'intégralité des règles pour la catégorie, même si une seule de ces règles a changé.

- On ajoute une exclusion d'héritage

Toujours sur Botzaris, on va indiquer que cette AU ne **doit pas** hériter ni de la règle d'identifiant ACC-00004, ni de ACC-00005. On pourrait redéclarer toute la catégorie en ajoutant le blocage de l'héritage de ces deux règles en plus, ou plus simplement n'ajouter que le paramètre souhaité :

```json
{
  "$action": [
    {
      "$set": {
        "#management.AccessRule.Inheritance.PreventRulesId": [
              "ACC-00004",
              "ACC-00005"
            ]
      }
    }
  ]
}
```

Si l'on souhaite ajouter un nouvel identifiant de règle, on doit redéclarer l'ensemble du tableau :

```json
{
  "$action": [
    {
      "$set": {
        "#management.AccessRule.Inheritance.PreventRulesId": [
              "ACC-00004",
              "ACC-00005",
              "ACC-00010"
            ]
      }
    }
  ]
}
```
On utilise le même principe pour changer les champs liés aux catégories (comme FinalAction pour une Storage ou AppraisalRule ou un ClassificationOwner pour une ClassificationRule).

Par exemple pour changer le FinalAction (sort final) d'une catégorie StorageRule :

```json
{
  "$action": [
    {
      "$set": {
        "#management.StorageRule.FinalAction": "Copy"
      }
    }
  ]
}
```

Avec cette requête l'ensemble des autres éléments de la StorageRule (le tableau des règles ou les champs de blocage d'héritages) restent inchangés
