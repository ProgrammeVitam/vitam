Cette partie va essayer de montrer quelques exemples d'usages du DSL dans différentes conditions.

# Cas du SIP 1069_OK_RULES_COMPLEXE_COMPLETE.zip

- Je cherche l'AU dont le titre est Botzaris (Title = Botzaris)

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

- Je cherche les AU qui ne seront pas communicables au 01/01/2018 (= les AU qui ont une AccesRule avec une EndDate postérieure au 01/01/2018)

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

- Je cherche les AU qui ont une AppraisalRule avec sort final = Destroy

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

# Cas du SIP Mercier.zip

- Je cherche dans le dossier Sénat (Title = Sénat), les discours prononcés lors de la relative au défenseur des droits (Title = défenseur). 
**Recherche mono-requête**

Chercher l'AU avec pour titre "Sénat"

```json
{
  "$roots": [],
  "$query": [
    {
      "$and": [
        { "$match": { "Title": "Sénat" } },
        { "$eq": { "DescriptionLevel": "File" } }
      ]
    }
  ],
  "$filter": { },
  "$projection":  { "$fields": { "#id": 1 } }
}
```

Puis, à partir de l'AU trouvée (#id=GUID1), rechercher les sous AU avec Title = défenseur

```json
{
  "$roots": [ "GUID1" ],
  "$query": [
    {
      "$match": { "Title": "défenseur" },
      "$depth": 1
    }
  ],
  "$filter": { },
  "$projection":  { }
  }
}
```

- Je cherche dans le dossier Sénat (Title = Sénat), les discours prononcés lors de la relative au défenseur des droits (Title = défenseur). **Recherche multi-requêtes - Expérimental**

```json
{
  "$roots": [],
  "$query": [
    {
      "$and": [
        { "$match": { "Title": "Sénat" } },
        { "$eq": { "DescriptionLevel": "File" } }
      ]
    },
    {
      "$match": { "Title": "défenseur" },
      "$depth": 1
    }
  ],
  "$filter": {
    "$orderby": { "TransactedDate": 1 }
  },
  "$projection": { }
  }
}
```