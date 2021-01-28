*L'API d'Accès* propose les points d'entrées et les méthodes pour atteindre, requêter et récupérer les informations depuis les **Units** et les **Objects**.

Cette API est globalement reproduite dans tous les autres points d'accès lorsque l'accès à des _Units_ et _Objects_ est nécessaire. Les fonctionnalités offertes peuvent par contre varier (droit en modification, effacement, ...) selon le contexte.

## API Access externe

Dans le projet Vitam, Les API externes supportent le POST-X-HTTP-OVERRIDE=GET. Les API internes ne supportent que le GET.

## Units api

**Units** est le point d'entrée pour toutes les descriptions d'archives. Celles-ci contiennent les métadonnées de description et les métadonnées archivistiques (métadonnées de gestion).

Une _Unit_ peut être de 3 types :
- "ingest" (unité archivistique classique),
- "holding" (unité archivistique d'arbre de positionnement)
- "filing" (unité archivistique de plan de classement).
Les métadonnées de ces unités peuvent tout aussi bien décrire un dossier qu'un groupe d'objet lié. Les _Units_ peuvent être organisées en arborescence et peuvent posséder :
  - Plusieurs racines dans l'arborescence (_Unit_ de plus haut niveau)
  - Plusieurs parents directs (un dossier peut être rattaché à plusieurs dossiers)

 Il s'agit d'une représentation dite de "_graphe dirigé sans cycle_" (DAG en Anglais pour "_Directed Acyclic Graph_").

Pour le model SEDA, il est équivalent à l'**ArchiveUnit**, notamment pour les parties **Content** et **Management**. Pour l'Isad(G) / EAD, il est équivalent à **Description Unit**.

Au plus, un seul _groupe_ d'_objets_ d'archives (**Objects**) est attaché à une _Unit_. Cela signifie que si une _Unit_ doit avoir plus d'un _groupe_ d'_objets_ d'archive attaché, vous devez spécifier des sous-Units à l'_Unit_ principale, chaque sous-Unit n'ayant qu'un _groupe_ d'_objets_ d'archives attaché. Un même _groupe_ d'_objets_ d'archives peut par contre être attaché à de multiples _Units_.

Aucun effacement n'est autorisé, ni aucune mise à jour complète (seules les mises à jours partielles sont autorisées via la commande POST via un update de masse).
Les mises à jour des _Unit_ doivent être réalisées via des mises à jour en masse, l'API d'update unitaire étant dépréciée.

### Structuration des métadonnées

La structuration d'un Unit est la suivante :
```json
  {
    "#id": "UnitId",
    "#tenant": "tenantId",
    "#version": 1,
    "#unitType" : "HOLDING_UNIT (arbre), FILING_UNIT (Plan) ou INGEST (ArchiveUnit standard)"
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
      "HoldRule": {},
      "NeedAuthorization": false // Accès nécessitant une autorisation explicite
    },
    "#unitups": [ "unitParentId", "unitParentId"], // liste des parents immédiats
    "#min": 1,
    "#max": 1,
    "#object": "objectId",
    "#nbunits": 1, // Nombre de Unit fils
    "#operations" : [ "id", "id" ], // liste des opérations auxquelles cette AU a participées
    "#allunitsups": [ "unitParentId", "unitParentId"], // liste de tous les parents jusqu'au sommet
    "#storage": { "#nbc": 2  } // information de stockage,
    "#originating_agency": "originationAgencyId",
    "#originating_agencies": [ "originationAgencyId" ],
    "#operations" : [ "operationId" ] // liste des opérations auxquelles cette AU a participées
```

## Objects api

**/units/{idu}/objects** est le point d'entrée pour :

 * tous les groupes objets, qui contiennent les métadonnées techniques (nom des objets, format, type MIME...)
 * tous les objets binaires (des fichiers) non binaires (des références à des objets d'archives physiques ou externes au système).

Un _Groupe_ d'_Objects_ peut posséder de 1 à N objets. Chaque objet a un usage et une version dans son groupe d'objet. Ces distinctions permettent de différencier des usages comme version de conservation, version de diffusion...

Il peut exister plusieurs versions de l'objet pour un même usage (pour l'original numérique notamment), au fur et à mesure du cycle de vie du _Groupe_ d'_Objects_. Par exemple l'objet d'usage+version "BinaryMaster_1" est la version 1 de l'usage BinaryMaster, le "BinaryMaster_2" est la version 2 de l'usage BinaryMaster. Dans le modèle de données de la solution logicielle Vitam, cet usage correspond au champ "qualifier".

Pour le model SEDA, il est équivalent à un **DataObjectGroup**. Pour l'EAD, il est équivalent à un **Digital Archive Object Group or Set**.

Chaque _Groupe_ d'_Objets_ doit être attaché à au moins un parent _Unit_.

Seul l'accès est autorisé (GET/POST en override de GET).

Chaque _Objet_ n'est lié qu'à un seul _Groupe d'objets_. Chaque _Objet_ a un qualifier (spécifiable via *#qualifiers* dans le DSL pour l'accès métadonnées et *X-Qualifier* en Header pour l'accès aux objets binaires) :

- **PhysicalMaster** : il s'agit de l'original papier ou physique s'il existe
- **BinaryMaster** : il s'agit de l'original numérique s'il existe
- **Dissemination** : il s'agit d'une version adaptée à la diffusion via le réseau et l'usage dans des navigateurs
- **Thumbnail** : il s'agit d'une version adaptée à la diffusion dans une taille et une qualité correspondant à une vignette (utile lors de l'affichage d'une liste)
- **TextContent** : il s'agit d'une version ne contenant que le texte du document, sans la mise en forme (son usage est en prévision du futur, par exemple pour des opérations d'analyses sémantiques)

Ces qualifiers peuvent être utilisés dans une requête GET en conjonction avec le Header *Accept: application/octet-stream* pour accéder en lecture à un usage particulier.

### Structuration des métadonnées

**A noter: la structuration va changer dans la prochaine release**

La structuration d'un Object est la suivante :
```json
  {
    "#id": "ObjectId",
    "#tenant": "tenantId",
    //Métadonnées de l'Object
    "FileInfo": {
      "Filename": "filename",
      "LastModified": "date",
      "GPS": {}
    },
    "#qualifiers": [{
      "qualifier": "PhysicalMaster",
      "#nbc": 1,
      "versions": [
        {
          "#id": "abcdef",
          "DataObjectGroupId": "abcdef",
          "DataObjectVersion": "PhysicalMaster_1",
          "PhysicalDimensions": {},
          "PhysicalId": {},
          "xxx": "" // autres informations
        }
      ]},
      {
      "qualifier": "BinaryMaster",  // Version numérique
      "#nbc": 1, // nombre de versions
      "versions": [
        {
          "#id": "abcdef",
          "#storage" : {// informations sur le stockage de l'objet}
          "DataObjectGroupId": "abcdef",
          "DataObjectVersion": "BinaryMaster_1",
          "MessageDigest": "algorithme digest", // empreinte et algorithme d'empreinte de l'objet
          "Algorithm": "SHA-512",
          "FileInfo": {// informations du fichier},
          "Size": 10, // taille de l'objet
          "Uri": "Content/uri", // uri de l'objet
          "FormatIdentification": {
            "FormatLitteral": "format Literral",
            "MimeType": "mimetype",
            "FormatId": "pronomId"
          },
          "OtherMetadata": {
            // autres métadonnées non classées
          }
        }
      ]
      },
      {
        "qualifier": "Dissemination",  // Version de diffusion
        "#nbc": 1, // nombre de versions
        "versions": [
          {//idem à BinaryMaster}
        ]
      },
      {
        "qualifier": "TextContent",  // Contenu brut
        "#nbc": 1, // nombre de versions
        "versions": [
          {//idem à BinaryMaster}
        ]
      },
      {
        "qualifier": "Thumbnail",  // Vignette
        "#nbc": 1, // nombre de versions
        "versions": [
          {//idem à BinaryMaster}
        ]
      }
    ],
    "#unitups": [ "unitParentId", "unitParentId"],
    "#nbobjects": 1, // Nombre de versions d'objets contenus pour tous les usages
    "#storage": { "#nbc": 2  }, // information de stockage,
    "#originating_agency": "originationAgencyId",
    "#originating_agencies": [ "originationAgencyId" ],
    "#operations" : [ "operationId" ] // liste des opérations auxquelles ce groupe d'objets a participées
  }
```
**Note :** A l'avenir, à l'intérieur d'une version d'usage, et pour chaque version (pour les **BinaryMaster** notamment), un contexte sera ajouté à la structure de l'Object afin de pouvoir y introduire des données de contexte (version du référentiel Pronom par exemple...).

## Dipexport api
**/dipexport** (en v1 ou en v2) sont les points d'entrée permettant l'export sous forme de DIP (paquet d'information diffusé ou Dissemination Information Package en anglais) d'une sélection d'unités archivistiques.

**Important** : Deux actions sont disponibles. La première permet de lancer un processus de génération d'un DIP. La deuxième permet de télécharger le fichier généré par le processus précédent, une fois terminé.
