# Présentation

>DSL : langage spécifique de domaine – langage ad-hoc

Le DSL de requête de VITAM permet d'effectuer de nombreuses opérations de recherche, lecture et modification sur les informations stockées dans les diverses collections de VITAM.

Le DSL Schema permet de spécifier la structure syntaxique d'une objet JSON. Il est du même ordre de les [JSON Schema](http://www.jsonschema.org) – ou pour XML, les XML Schema (XSD) – mais il est conçu pour être le plus simple possible par rapport aux besoins de spécification de VITAM. Il est notamment conçu pour produire des messages d'erreurs facilement compréhensible par les utilisateurs des API VITAM exploitant le DSL.

Le langage de DSL Schema est aussi conçu pour être facilement extensible.

Afin de mieux saisir son fonctionnement, voici quelques exemple

## Exemple 1

Message JSON à reconnaître :
```json
{
  "login": "marcel.proust",
  "password": "sw@nn",
  "admin": false
}
```

DSL Schema permettant de valider le message :
```javascript
{
  // Définition du type de base LOGIN
  "LOGIN": {
    
    // On cherche à reconnaître un objet JSON
    "format": "object",

    "elements": { 
      
      // Liste des éléments que l'objet doit comporter
      "login": "string", // "login" est de type string (notation compacte) 
      "password": {      
        
        // "password" est aussi de type string (notation standard)
        "format": "ref",
        "type": "string",

        // et le champs est optionnel
        "optional": true
      },

      "admin": {
        "format": "ref",
        "type": "boolean",

        // Message présenté dans le message d'erreur si l'attribut manque
        // ou est utilisé avec autre chose d'un booléen
        "hint": "Indique si l'utilisateur est un administrateur ou non" 
      }
    }
  }
}
```

# Mise en œuvre

La vérification en Java se fait en deux temps :

* Chargement du modèle, qui peut se faire dans un singleton, un fois pour toute :
  ```java
  // Si on veut supporter les commentaires, on le traite au niveau de l'ObjectMapper.
  JsonFactory f = new JsonFactory();
  f.enable(JsonParser.Feature.ALLOW_COMMENTS);
  ObjectMapper objectMapper = new ObjectMapper(f);

  // Chargement du modèle proprement dit.
  Schema schema = Schema.withMapper(objectMapper) // init du SchemaBuilder
                        // Chargement d'un fichier DSL Schema
                        .loadTypes(dslSchemaStream1)
                        // On peut charger plus d'un fichier
                        .loadTypes(dslSchemaStream2)
                        // Vérification 
                        .build();

  ```

* Validation d'un message JSON :
  ```java
  JsonNode json = ... // message JSON à validé, sortant généralement un ObjectMapper.
  try {
    // "LOGIN" est le nom du type auquel doit se conformer le contenu de `json`.
    Validator.validate(schema, "LOGIN", json);
  } catch (ValidationException e) {
    ... // Quelque-chose de pertinent présentant e.getMessage() à l'utilisateur du DSL.
  }
  ```

# Référence du DSL Schema

Un fichier DSL Schema est un fichier JSON :
 * La racine du fichier définit des _types_ :
	```javascript
	{
	  "TYPE1": { ... },
	  "TYPE2": { ... },
	  ...
	}
	```
	À l'exécution, le schéma et ses types sont représentés par la classe `fr.gouv.vitam.common.dsl.schema.meta.Schema`.

 * Chaque type est représenté par un sous-bloc de JSON. Plusieur _formats_ sont possible pour ces types : ce peut être un type simple (`string`, `integer`...) ou un type plus complexe (`enum`, `object`...).   
   L'attribut le plus important d'un type et `format`. C'est lui qui détermine le reste des attributs du type.
   À l'exécution, le type et ses formats sont représentés par la classe `fr.gouv.vitam.common.dsl.schema.meta.Format`. La méthode qui gouverne le comportement d'un format est `validate()`. En cas de doute sur la sémantique d'un format, c'est là qu'il faut regarder (_trust the code Luke_)

## Formats
Le détails d'un type dépend de son format. Indépendamment du format, le type est défini par différents attributs :
 * `format` : nom du format (cf. Liste des formats, ci-après).
 * `optional` (par défaut : `false`) : Indique si le champ est optionnel ou obligatoire lorsqu'il est cité dans un `object`.
 * `hint` : texte présenté dans les messages d'erreurs lorsque le valideur de DSL refuse une donnée, afin d'expliquer à l'utilisateur technique ce que la donnée attendue est sensée représenter. Ex :
	 * `"hint": "Fields to retrieve in the result"`
 * `min` (par défaut : `0`) : pour valeurs JSON à valider de type liste ou objet, indique le nombre minimal de valeurs qu'elles doivent avoir pour être valides. Ex :
	 * Pour min = 2
		 * valides : `[1, "deux"]`, `{ "un": 1, "deux": 2, "trois": 3}` ;
		 * invalide : `[1]`, `{ "deux: 2 }`.
 * `max` (par défaut : `Integer.MAX_VALUE`) : pour valeurs JSON à valider de type liste ou objet, indique le nombre maximal de valeurs qu'elles doivent avoir pour être valides. Ex :
	 * Pour max = 2
		 * valides : `[1, "deux"]`, `{ "deux: 2 }` ;
		 * invalide : `[1, "deux", 3]`, `{ "un": 1, "deux": 2, "trois": 3}`.
 

#### Listes des formats
### `ref` — Références
Classe : `fr.gouv.vitam.common.dsl.schema.meta.ReferenceFormat`

Les types avec le format `ref` font référence à d'autres types existants :
 * soit des types définit à la racine d'un schéma ;
 * soit des types primitifs (définit dans la méthode `validatePrimitive`).   
   Liste des types primitifs :
   * **guid** : GUID valide au sens de VITAM ; ex :               
     `"aeaqaaaaaagdmvr3abnwoak7fzjq75qaaaca"`
   * **integer** :`int` Java, positif, nul ou négatif ; ex :  
      `-432`, `0`, `42`
   * **posinteger** : `int` Java, positif, nul ; ex :  
     `0`, `42`
   * **string** : chaîne de caractères ; ex :  
     `"vitam"`
   * **anyvalue** : toute valeur qui n'est ni un tableau JSON, ni un objet JSON ; ex :  
     `42`, `"texte"`, `true` 
   * **anyarray** : n'importe quel tableau, quel que soit son contenu ; ex :  
     `[42, "texte", { "key": []}]`
   * **any** : n'importe quelle valeur, tableau ou objet sans restriction ; ex :  
     `42`, `"texte"`, `true`, `["vitam"]`, `{ "key": "value" }`

Remarque : n'hésitez pas à augmenter la liste des types primitifs supportés en fonction des besoins (exemple : support des booléens).

#### Attributs
 * `type` : nom du type référence (cf. ci-dessus).
 * `min` : usage interdit.
 * `max` : usage interdit.

Exemple d'utilisation : _définition d'un entier optionnel représentant une profondeur de recherche_
```javascript
{  
  "format": "ref",  
  "type": "integer",  
  "optional": true,  
  "hint": "Profondeur de recherche"  
}
```

### `enum` — Énumérations
Classe : `fr.gouv.vitam.common.dsl.schema.meta.EnumFormat`

Les types `enum` listent les valeurs admissible pour une entrée JSON (en général de type chaîne de caractère, l'usage de booléens et de nombres sont possibles).
#### Attributs
 * `values` : liste des valeurs admissibles.
 * `min` : usage interdit.
 * `max` : usage interdit.

Exemple d'utilisation : _définition pseudo-booléen pouvant prendre aussi bien les valeurs **true** et **false** que **0** et **1**, ou encore **"oui"** et **"non"**_
```javascript
{  
  "format": "enum",  
  "values": [  
    0, 1,
    true, false,
    "oui", "non"  
  ]  
}
```

### `object` — Objets JSON
Classe : `fr.gouv.vitam.common.dsl.schema.meta.ObjectFormat`

Les types `object` listes les attributs qu'un objet JSON peut avoir. Les attributs pouvant être optionnels, il peut être intéressant d'utiliser les attributs `min` et `max` pour limiter leur usage.
#### Attributs
 * `elements` : définition des attributs de l'objet JSON.
	 * en clé : le nom de l'attribut ;
	 * en valeur : le type de l'attribut ou une référence vers celui-ci.  
	   En cas de référence, l'attribut est nécessairement _obligatoire_ et ne peut avoir de _hint_. Si l'un des deux est nécessaire, il faut utiliser un _ref_.

Exemple d'utilisation : _définition d'un objet JSON avec trois attributs possibles :_ alpha _— un enum optionnel ;_ bravo _— une référence simple vers une string obligatoire ;_ charlie _— une référence vers une string optionnelle ; seuls deux attributs peuvent être utilisés concurremment (max=2)._
```javascript
{  
  "format": "object",  
  "elements": {
    "alpha": {
      "format": "enum",
      "values": [ true, false ],
      "optional": true,
	  "hint": "Le champs bravo_enum active la fonction ..."  
    },
    "bravo": "string",
    "charlie": {
	  "format": "ref",
      "optional": true 
    } 
  },
  "max": 2 
}
```

### `array` – Tableau
Classe : `fr.gouv.vitam.common.dsl.schema.meta.ArrayFormat`

Les types `array` rendent admissibles des tableaux JSON et indiquent le type des éléments de tableau. Il peut être intéressant d'utiliser les attributs `min` et `max` pour indiquer le nombre d'éléments que doit avoir le tableau.
#### Attributs
 * `itemtype` : le type des valeurs contenues dans le tableau, ou une référence vers celui-ci.  
	   En cas de référence, l'attribut est nécessairement _obligatoire_ et ne peut avoir de _hint_. Si l'un des deux est nécessaire, il faut utiliser un _ref_.

Exemple d'utilisation : _tableau d'ELEMENT, définis par ailleurs. Il faut au moins 3 éléments_

```javascript
{
  "format": "array",
  "itemtype": {
    "format": "ref",
    "itemtype": "ELEMENT",
    "hint": "Élement à mettre dans le tableau"
  },
  "min": 3,
  "hint": "Liste ayant au moins 3 d'éléments"
}
```

### `union` – Union de définition de type
Classe : `fr.gouv.vitam.common.dsl.schema.meta.UnionFormat`

Les types `union` permettent de composer plusieurs types.

#### Attributs
 * `types`  : liste de types que l'objet JSON en cours de validation doit vérifier.

Exemple d'utilisation : _élément de type QUERY (généralement un `object`) auquel on rajoute un attribut optionnel_ zulu _de type entier._
```javascript
{  
  "format": "union",  
  "types": [  
    "QUERY",  
    {  
      "format": "object",  
      "elements": {  
        "zulu": {  
          "format": "ref",  
          "type": "integer",  
          "optional": true,  
        }  
      }  
    }  
  ]  
}
```

### `anykey` – Clé libre
Classe : `fr.gouv.vitam.common.dsl.schema.meta.AnyKeyFormat`

Les type `anykey` permettent de valider des objets JSON dont les noms d'attributs sont libres, mais dont les valeurs doivent respecter un format particulier. Il peut être intéressant d’utiliser les attributs `min` et `max` pour indiquer le nombre d’attributs que doit avoir l'objet.

#### Attributs
Même syntaxe que pour `array`.

*  `itemtype`  : le type des valeurs contenues dans le tableau, ou une référence vers celui-ci.  
    En cas de référence, l’attribut est nécessairement  _obligatoire_  et ne peut avoir de  _hint_. Si l’un des deux est nécessaire, il faut utiliser un  _ref_.
 
Exemple d’utilisation :  _objets dont chaque attribut doit être un ELEMENT, définis par ailleurs. Il faut au moins 3 attributs._
```javascript
{
  "format": "anykey",
  "itemtype": {
    "format": "ref",
    "itemtype": "ELEMENT",
    "hint": "Élement défini pour chaque attribut"
  },
  "min": 3,
  "hint": "Objet ayant au moins 3 attributs de type ELEMENT"
}
```   

Valeur admissible :
```javascript
{
  "alpha": { ... ELEMENT n° 1 ... },
  "bravo": { ... ELEMENT n° 2 ... },
  "charlie": { ... ELEMENT n° 3 ... }
}
```
    
### `keychoice` – Choix en fonction d'une clé d'objet
Classe : `fr.gouv.vitam.common.dsl.schema.meta.KeyChoiceFormat`

Similaire à un `object` dont tous les champs seraient optionnels, avec min=1 et max=1. L'avantage est d'avoir un message d'erreur plus compréhensible en cas d'erreur.

#### Attributs
Même syntaxe pour pour `object`.

 * `elements`  : définition des attributs de l’objet JSON.
    *  en clé : le nom de l’attribut ;
    *  en valeur : le type de l’attribut ou une référence vers celui-ci.  
       En cas de référence, l’attribut est nécessairement  _obligatoire_  et ne peut avoir de  _hint_. Si l’un des deux est nécessaire, il faut utiliser un  _ref_.

### `typechoice` – CHoix en fonction du type JSON
Classe : `fr.gouv.vitam.common.dsl.schema.meta.TypeChoiceFormat`

Les types `typechoice` permettent de choisir le type de validation en fonction du type JSON (_object_, _string_, _array_...).

#### Attributs
 * `choices` : objet JSON dont :
	 * en clé : le types JSON de l'objet à valider : `object`, `array`, `string`, `number`, `boolean`.
	 * en valeur : le type de l’attribut ou une référence vers celui-ci.

Exemple d'utilisation : _En cas de tableau, on attends une liste de ROOT_QUERY, en cas d'objet on attends un ROOT_QUERY simple_.
```javascript
{  
  "format": "typechoice",  
  "choices": {  
    "array": {  
      "format": "array",  
      "itemtype": "ROOT_QUERY"  
    },  
    "object": "ROOT_QUERY"  
  },  
  "hint": "Query or list of query"  
}
```

### `typechoicearray` – CHoix en fonction du type JSON dans un tableau
Classe : `fr.gouv.vitam.common.dsl.schema.meta.TypeChoiceArrayFormat`

Généralisation de `typechoice` aux éléments d'un tableau. Le choix du type se fait en fonction du type JSON du premier élément du tableau.

_UNSUPPORTED_