Voici d'autres exemples d'utilisation du DSL


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
