*L'API d'Accès* propose les points d'entrées et les méthodes pour atteindre, requêter et récupérer les informations depuis les **Units** et les **Objects**.

Cette API est globalement reproduite dans tous les autres points d'accès lorsque l'accès à des _Units_ et _Objects_ est nécessaire. Les fonctionnalités offertes peuvent par contre varier (droit en modification, effacement, ... selon le contexte).

# API Access externe

les API externe dans le projet Vitam supportent le POST-X-HTTP-OVERRIDE=GET par contre au niveau des API interne ils supportent seulement le GET . 

# Units

**Units** est le point d'entrée pour toutes les descriptions d'archives. Celles-ci contiennent les métadonnées de description et les métadonnées archivistiques (métadonnées de gestion).

Une _Unit_ peut être soit un simple dossier (comme dans un plan de classement), soit une description d'un item. Les _Units_ portent l'arborescence d'un plan de classement. Ce plan de classement peut être multiple :
  - Plusieurs racines (_Unit_ de plus haut niveau)
  - Plusieurs parents (un dossier peut être rattaché à plusieurs dossiers)
  - Il s'agit d'une représentation dite de "_graphe dirigé sans cycle_" (DAG en Anglais pour "_Directed Acyclic Graph_").

Pour le model SEDA, il est équivalent à l'**ArchiveUnit**, notamment pour les parties **Content** et **Management**. Pour l'Isad(G) / EAD, il est équivalent à **Description Unit**.

A priori, un seul _groupe_ d'_objets_ d'archives (**Objects**) est attaché à une _Unit_. Cela signifie que si une _Unit_ devait avoir plus d'un _groupe_ d'_objets_ d'archive attaché, vous devez spécifier des sous-Units à l'_Unit_ principale, chaque sous-Unit n'ayant qu'un _groupe_ d'_objets_ d'archives attaché. Un même _groupe_ d'_objets_ d'archives peut par contre être attaché à de multiples _Units_.

Aucun effacement n'est autorisé, ni aucune mise à jour complète (seules les mises à jours partielles sont autorisées via la commande PUT).

### Structuration des métadonnées

La structuration d'un Unit est la suivante :
```json
  {
    "#id": "UnitId",
    "#tenantId": "tenantId",
    "#type": "DocumentTypeId",
    "#sector": "FilièreId",
    //Métadonnées du content
    "DescriptionLevel": "Fonds",
    "Title": "titre",
    "Description": "description du Unit",
    "#Management": {
      "StorageRule": {},
      "AppraisalRule": {},
      "AccessRule": {},
      "DisseminationRule": {},
      "ReuseRule": {},
      "ClassificationRule": {},
      "NeedAuthorization": false // Accès nécessitant une autorisation explicite
    },
    "#parents": [ "unitParentId", "unitParentId"],
    "#object": "objectId",
    "#childNb": 1 // Nombre de Unit fils
  }
```

# Objects

**Objects** est le point d'entrée pour toutes les archives binaires mais également les non binaires (comme une référence à des objet d'archives physiques ou externes au système). Elles contiennent les métadonnées techniques. Il est constitué de plusieurs usages et versions d'usages du même contenu. C'est dans ce sens qu'il est aussi appelé un **Groupe d'objets**.

Un _Groupe_ d'_Objects_ peut être constitué de plusieurs versions (sous-objets) pour différencier des usages comme version de conservation, version de diffusion...

Il peut exister plusieurs enregistrements pour une même version d'usage (pour l'original numérique notamment), au fur et à mesure du cycle de vie du _Groupe_ d'_Objects_.

Pour le model SEDA, il est équivalent à un **DataObjectGroup**. Pour l'EAD, il est équivalent à un **Digital Archive Object Group or Set**.

Chaque _Groupe_ d'_Objets_ doit être attaché à au moins un parent _Unit_.

Seul l'accès est autorisé (GET/HEAD).

Pour le model SEDA, chaque usage/version est équivalent à un **DataObject** (binaire avec **BinaryDataObject** ou physique avec **PhysicalDataObject**). Pour l'EAD, il est équivalent à un **Digital Archive Object**.

Chaque _Objet_ n'est lié qu'à un seul _Groupe_. Chaque _Objet_ a un qualifier (spécifiable via *#qualifiers* dans le DSL et *X-Qualifier* en Header) :

- **PhysicalMaster** : il s'agit de l'original papier ou physique s'il existe
- **BinaryMaster** : il s'agit de l'original numérique s'il existe
- **Dissemination** : il s'agit d'une version adaptée à la diffusion via le réseau et l'usage dans des navigateurs
- **Thumbnail** : il s'agit d'une version adaptée à la diffusion dans une taille et une qualité correspondant à une vignette (utile lors de l'affichage d'une liste)
- **TextContent** : il s'agit d'une version ne contenant que le texte du document, sans la mise en forme (son usage est en prévision du futur, par exemple pour des opérations d'analyses sémantiques)
- **All** (**UNSUPPORTED**) : il s'agit en consultation d'un accès en cas de ZIP ou TAR à l'ensemble des usages et versions. Il est utilisable également en mode HEAD.

Ces qualifiers peuvent être utilisés dans une requête GET en conjonction avec le Header *Accept: application/octet-stream ou application/zip ou application/x-tar* pour accéder en lecture ou en check à un usage particulier.
Le qualifier **All** est ajouté pour permettre l'accès à l'ensemble des usages avec le Header *Accept: application/zip ou application/x-tar*.
Pour la commande HEAD, ces Qualifiers peuvent être spécifiés pour préciser sur quoi porte le test d'existence.

### Structuration des métadonnées

**A noter: la structuration va changer dans la prochaine release**

La structuration d'un Object est la suivante :
```json
  {
    "#id": "ObjectId",
    "#tenantId": "tenantId",
    "#type": "ObjectTypeId", // Audio, Video, Document, Text, Image, ...
    //Métadonnées de l'Object
    "FileInfo": {
      "Filename": "filename",
      "LastModified": "date",
      "GPS": {}
    },
    "#qualifiers": {
      "PhysicalMaster": { // Version papier
        "PhysicalId": "abcdef",
        "PhysicalDimensions": {},
        "xxx": "" // autres informations
      },
      "BinaryMaster": { // Version numérique
        "nb": 1, // nombre de versions
        "versions": [
          {

            "MessageDigest": "algorithme digest", // empreinte et algorithme d'empreinte de l'objet
            "IngestDate": "yyyy-MM-dd'T'HH:mm:ssZ", // date de l'ingest de cette version
            "Rank": 1, // quantième de la version
            "Size": 10, // taille de l'objet
            "FormatIdentification": {
              "FormatLitteral": "format Literral",
              "MimeType": "mimetype",
              "FormatId": "pronomId"
            },
            "Metadata": { // un parmi tous
              "Text": {}, "Document": {}, "Image": {}, "Audio": {}, "Video": {}
            },
            "OtherMetadata": {
              // autres métadonnées non classées
            }
          }
        ]
      },
      "Dissemination": {
        // idem à #master
      },
      "Thumbnail": {
        // idem à #master
      },
      "TextContent": {
        // idem à #master
      },
    }
    "#parents": [ "unitParentId", "unitParentId"],
    "#childNb": 1 // Nombre de versions d'objets contenus pour tous les usages
  }
```
**Note :** A l'avenir, à l'intérieur d'une version d'usage, et pour chaque version (pour les **BinaryMaster** notamment), un contexte sera ajouté à la structure de l'Object afin de pouvoir y introduire des données de contexte (version du référentiel Pronom par exemple...).

De plus, pour le moment, les recherches actuelles retournent uniquement le nombre de versions pour chaque usage (ex : "#qualifiers": { "BinaryMaster": 3, "Dissemination": 1, "Thumbnail": 1, "TextContent": 1 }), mais à l'avenir, il sera possible de retourner le détail de chaque usage (ou de tous les usages), comme indiqué dans l'exemple ci-dessus.
