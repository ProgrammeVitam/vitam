Collection LogbookLifeCycleUnit
###############################

Utilisation de la collection LogbookLifeCycleUnit
=================================================

Le journal du cycle de vie d'une unité archivistique (ArchiveUnit) trace tous les événements qui impactent celle-ci dès sa prise en charge dans le système. Il doit être conservé aussi longtemps que l'unité d'archives est gérée par le système.

- dès la réception d'une unité d'archives, l'ensemble des opérations qui lui sont appliquées est tracé.
- les journaux du cycle de vie sont "committés" une fois le stockage des objets et l'indexation des métadonnées effectués sans échec, avant l'envoi d'une notification au service versant

Chaque unité archivistique possède une et une seule entrée dans la collection LogbookLifeCycleUnit.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection LogbookLifeCycleUnit
=========================================================================================================

Extrait d'un JSON correspondant à un journal de cycle du vie d'une unité archivistique.

::

  {
    "_id": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
    "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
    "evParentId": null,
    "evType": "LFC.LFC_CREATION",
    "evDateTime": "2017-04-10T12:39:37.933",
    "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": "LFC.LFC_CREATION.STARTED",
    "outMessg": "!LFC.LFC_CREATION.STARTED!",
    "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
    "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
    "evDetData": null,
    "events": [
        {
            "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
            "evParentId": null,
            "evType": "LFC.CHECK_MANIFEST",
            "evDateTime": "2017-04-10T12:39:37.953",
            "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "LFC.CHECK_MANIFEST.OK",
            "outMessg": "Succès de la vérification de la cohérence du bordereau",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
            "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
            "evDetData": null,
            "_lastPersistedDate": "2017-04-10T12:39:37.953"
        },
        {
            "evId": "aedqaaaaaghbl62n5g8ftak3k7qg5tiaaabq",
            "evParentId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
            "evType": "LFC.CHECK_MANIFEST.LFC_CREATION",
            "evDateTime": "2017-04-10T12:39:37.953",
            "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "LFC.CHECK_MANIFEST.LFC_CREATION.OK",
            "outMessg": "Succès de la création du journal du cycle de vie",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
            "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
            "evDetData": null,
	    "_lastPersistedDate": "2017-04-10T12:39:37.953"
        },{

        [...]
        
        }
    ],
    "_tenant": 1,
    "_v": 0,
    "_lastPersistedDate": "2017-04-10T12:39:37.953"
  }

Détail des champs du JSON stocké en base
========================================

**"_id":** Identifiant donné par le système lors de l'initialisation du journal du cycle de vie.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID.
    * Cet identifiant constitue la clé primaire du journal du cycle de vie de l'unité archivistique. Il reprend la valeur du champ _id d'une unité archivistique enregistré dans la collection Unit.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.

**"evId" (event Identifier):** identifiant de l'événement.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. 
    * Il identifie l'événement de manière unique dans la base.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evParentId" (event Parent Identifier):** identifiant de l'événement parent.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. 
    * Il identifie l'événement parent. Par exemple pour LFC.CHECK_MANIFEST.LFC_CREATION, ce champs fera référence au GUID de l'évènement LFC.CHECK_MANIFEST.
    * La valeur est toujours null pour la structure incluante et les tâches principales
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evType" (event Type):** code du type d'événement.

    * Il s'agit d'une chaîne de caractères.
    * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se fait via un fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evDateTime" (event DateTime):** date de l'événement.

    * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]

    ``Exemple : "2016-08-17T08:26:04.227"``

    * Ce champ est positionné par le client LogBook.
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"evIdProc" (event Identifier Process):** identifiant du processus. 

    * Il s'agit d'une chaîne de 36 caractères.
    * Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id" d'une opération enregistrée dans la collection LogbookOperation
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evTypeProc" (event Type Process):** type de processus.

    * Il s'agit d'une chaîne de caractères.
    * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"outcome":** statut de l'événement.

    * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de la liste suivante :

        - STARTED (Début de l'événement)
        - OK (Succès de l'événement)
        - KO (Echec de l'événement)
        - WARNING (Succès de l'événement comportant des alertes)
        - FATAL (Erreur technique)

    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"outDetail" (outcome Detail):** code correspondant à l'erreur.

    * Il s'agit d'une chaîne de caractères.
    * Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se fait via le fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"outMessg" (outcome Detail Message):** détail du résultat de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    * Traduction du code présent dans outDetail issue du fichier vitam-logbook-message-fr.properties.
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"agId" (agent Identifier):** identifiant de l'agent réalisant l'évènement.

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.

    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"obId" (object Identifier):** identifiant de la solution logicielle Vitam correspondant au GUID de l'unité archivistique sur laquelle s'applique l'opération.

    * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses

**"evDetData" (event Detail Data):** détails des données de l'événement.

    * Donne plus de détails sur l'événement. 
    * Par exemple, l'historisation de métadonnées lors d'une modification se fait dans ce champ. Dans la structure incluse correspondant à cet événement, il contient, par exemple, composé du champ suivant :

        - diff: contient la différence entre les métadonnées d'origine et les métadonnées modifiées. Chaîne de caractères.

    * En outre, lors de l'historisation de la sauvegarde de l'unité archivistique sur les offres de stockage, on utilise ce champ pour tracer les informations sur le fichier sauvegardé. Il contient, ainsi, un JSON composé comme suit :

        - FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé sur les offres de stockage.
        - Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
        - MessageDigest : Empreinte du fichier. Il s'agit d'une chaîne de caractères contenant l'empreinte du fichier.
        - Offers : Offres de stockage. Il s'agit des offres de stockage utilisées pour la sauvegarde du fichier.

    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"events":** tableau de structure.

    * Pour la structure incluante, le tableau contient n structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1 
    * S'agissant d'un tableau, les structures incluses ont pour cardinalité 1-n.
    * Ce champ existe uniquement pour la structure incluante.

**"_tenant":** identifiant du tenant

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.

**"_v":** version de l'enregistrement décrit 

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.
    * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.

**"_lastPersistedDate":** date technique de sauvegarde en base.

    * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    * Elle est renseignée par le serveur Logbook.
      ``Exemple : "2016-08-17T08:26:04.227"``
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

Champs présents dans les events
===============================

    * evId
    * evParentId
    * evType
    * evDateTime
    * evIdProc
    * evTypeProc
    * outcome
    * outDetail
    * outMessg
    * agId
    * obId
    * evDetData
    * lastPersistedDate

Détail des champs du JSON stocké en base spécifiques à une mise à jour
======================================================================

Exemple de données stockées :

::

   "evDetData": "{\"diff\":\"-  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS)\\n+  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS) 222\\n-  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq \\n+  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq, aecaaaaaacaam7mxabjssak2dzsjniyaaaaq \"}"


Dans le cas d'une mise à jour de métadonnées d'une unité archivistique (ArchiveUnit), le champ **"evDetData"** de l'événement final est composé du champ suivant :

**"diff":** historisation des modifications de métadonnées.

    * Son contenu doit respecter la forme suivante : les anciennes valeurs sont précédées d'un "-" (``-champ1: valeur1``) et les nouvelles valeurs sont précédées d'un "+" (``+champ1: valeur2``)

    ``Exemple :
    -Titre: Discours du Roi \n+Titre: Discours du Roi Louis XVI \n-Description: Etat Généraux du 5 mai 1789 \n+Description: Etat Généraux du 5 mai 1789 au Château de Versailles``

