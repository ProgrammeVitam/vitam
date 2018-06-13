Collection LogbookLifeCycleObjectGroup
######################################

Utilisation de la collection LogbookLifeCycleObjectGroup
========================================================

Le journal du cycle de vie du groupe d'objets (ObjectGroup) trace tous les événements qui impactent le groupe d'objets (et les objets associés) dès sa prise en charge dans le système. Il doit être conservé aussi longtemps que les objets sont gérés dans le système.

- dès la réception des objets, on trace les opérations effectuées sur les groupes d'objets et objets qui sont dans le SIP.
- les journaux du cycle de vie sont "committés" une fois le stockage des objets effectué et l'indexation des métadonnées effectuée, avant l'envoi d'une notification au service versant.

Chaque groupe d'objets possède une et une seule entrée dans la collection LogbookLifeCycleObjectGroup.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
    "evId": "aedqaaaaacaam7mxaap44akyf7hurgaaaabq",
    "evParentId": null,
    "evType": "CHECK_CONSISTENCY",
    "evDateTime": "2016-11-04T14:47:43.512",
    "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": "STARTED",
    "outMessg": "Début de la vérification de la cohérence entre objets/groupes d’objets et ArchiveUnit.",
    "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
    "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
    "evDetData": null,
    "events": [
        {
            "evId": "aedqaaaaacaam7mxaap44akyf7hurgaaaabq",
            "evParentId": null,
            "evType": "CHECK_CONSISTENCY",
            "evDateTime": "2016-11-04T14:47:43.515",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet/groupe dobjet référencé par un ArchiveUnit.",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":773928267,\"SiteId\":1,\"GlobalPlatformId\":237057355}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
	    "_lastPersistedDate": "2016-11-04T14:47:45.132",
        },
        {
            "evId": "\"aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba\"",
            "evParentId": null,
            "evType": "CHECK_DIGEST",
            "evDateTime": "2016-11-04T14:47:45.132",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STARTED",
            "outMessg": "Début de la vérification de lempreinte.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": "{\"MessageDigest\":\"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc125905636
            5aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\",\"Algorithm\": \"SHA512\", \"SystemMessageDigest\": \"SHA-512\", \"SystemAlgorithm\": \"0f1de441a7d44a277e265eb741e748ea18c96a59c8c0385f938b9768a42e375716dfa3b20cc125905636
            5aa0d3541f6128389ad60c8effbdc63b94df9a2e02bb\"} ",
	    "_lastPersistedDate": "2016-11-04T14:47:45.132",
        },
        {
            
            [...]
            
        }
    ],
    "_tenant": 0,
    "_v": 0,
    "_lastPersistedDate": "2016-11-04T14:47:45.132"
    }


Détail des champs du JSON stocké en base
========================================

**"_id":** Identifiant donné par le système lors de l'initialisation du journal du cycle de vie.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. Il reprend la valeur du champ _id du groupe d'objets enregistré dans la collection ObjectGroup.
    * Cet identifiant constitue la clé primaire du journal du cycle de vie du groupe d'objets.
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
    * La valeur du champ est toujours "null" pour la structure incluante et les tâches principales
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.
    
**"evType"** (event Type): nom de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties).
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evDateTime" (event DateTime):** date de l'événement.

    * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]

    ``Exemple : "2016-08-17T08:26:04.227"``.

    * Ce champ est positionné par le client LogBook.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evIdProc" (event Identifier Process):** identifiant du processus. 

    * Il s'agit d'une chaîne de 36 caractères.
    * Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id" de l'opération enregistrée dans la collection LogbookOperation.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"evTypeProc" (event Type Process):** type de processus.

    * Il s'agit d'une chaîne de caractères.
    * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses

**"outcome":** statut de l'événement.

    * Il s'agit d'une chaîne de caractères devant correspondre une valeur de la liste suivante :

    	- STARTED (Début de l'événement)
    	- OK (Succès de l'événement)
    	- KO (Échec de l'événement)
    	- WARNING (Succès de l'événement comportant des alertes)
    	- FATAL (Erreur technique)

    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"outDetail" (outcome Detail):** code correspondant à l'erreur.

    * Il s'agit d'une chaîne de caractères.
    * Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"outMessg" (outcome Detail Message):** détail du résultat de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * C'est un message intelligible destiné à être lu par un être humain en tant que détail du résultat de l'événement.
    * Traduction du code présent dans outDetail, issue du fichier vitam-logbook-message-fr.properties du code présent dans outDetail.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"agId" (agent Identifier):** identifiant de l'agent réalisant l'évènement.

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.

    ``Exemple : {\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1514166061,\"SiteId\":1,\"GlobalPlatformId\":171988781}``

    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses.

**"obId" (object Identifier):** identifiant de la solution logicielle Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    * Si l'évènement touche tout le groupe d'objets, alors le champ contiendra l'identifiant de ce groupe d'objets. S'il ne touche qu'un seul objet du groupe d'objets, alors il ne contiendra que celui de l'objet en question
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"evDetData" (event Detail Data):** détails des données de l'événement.

    * Donne plus de détails sur l'événement.
    * Par exemple, pour l'événement LFC.CHECK_DIGEST, lorsque l'empreinte d'un objet inscrite dans le bordereau n'est pas calculée en SHA512, ce champ précise l'empreinte d'origine et celle réalisée ensuite par la solution logicielle Vitam. Dans la structure incluse correspondant à cet événement, il contient un JSON composé des champs suivants :

    	- MessageDigest : empreinte de l'objet dans le bordereau. Chaîne de caractères, reprenant le champ "MessageDigest" du message ArchiveTransfer.
    	- Algorithm : algorithme de hachage utilisé dans le bordereau. Chaîne de caractères, reprenant l'attribut de champ "MessageDigest" du message ArchiveTransfer.
    	- SystemMessageDigest : empreinte de l'objet réalisée par la solution logicielle Vitam. Chaîne de caractères.
    	- SystemAlgorithm : algorithme de hachage utilisé par la solution logicielle Vitam. Chaîne de caractères.

    * En outre, pour l'événement LFC.OBJ_STORAGE, on utilise ce champ pour tracer les informations sur l'objet (fichier binaire) sauvegardé. Il contient un JSON composé comme suit :

    	- FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé sur les offres de stockage.
    	- Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
    	- MessageDigest : Empreinte de l'objet. Il s'agit d'une chaîne de caractères contenant l'empreinte de l'objet.
    	- Offers : Offres de stockage. Il s'agit des offres de stockage utilisées pour la sauvegarde de l'objet.

    * Pour l'événement LFC.OG_METADATA_STORAGE, on utilise ce champ pour tracer les informations sur le fichier (métadonnées) sauvegardé. Il contient un JSON composé comme suit :

    	- FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé sur les offres de stockage.
    	- Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
    	- MessageDigest : Empreinte du fichier. Il s'agit d'une chaîne de caractères contenant l'empreinte du fichier.
    	- Offers : Offres de stockage. Il s'agit des offres de stockage utilisées pour la sauvegarde du fichier.
    
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses

**"events":** tableau de structure.
    
    * Pour la structure incluante, le tableau contient n structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1 
    * S'agissant d'un tableau, les structures incluses ont pour cardinalité 1-n.
    * Ce champ existe uniquement pour la structure incluante.

**"_tenant":** identifiant du tenant.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 

**"_v":** version de l'enregistrement décrit.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.

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
    * LastPersistedDate
