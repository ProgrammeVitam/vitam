Objectif du document
====================

Ce document a pour but de décrire le modèle de données utilisé dans MongoDB. Il explicite chaque champ, précise la relation avec les sources (manifeste SEDA ou référentiel Pronom) et la structuration JSON.


Il décrit aussi parfois une utilisation particulière qui est faite de ces champs. Auquel cas la date de cet usage est mentionnée.

Collection LogbookOperation
===========================

Structure générale :
--------------------

Chaque entrée de cette collection est composée d'une structure auto-imbriquée : la structure possède une première instanciation "incluante", et contient un tableau de N structures identiques, dont seules les valeurs changent.

La structure est décrite ci-dessous. Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.


"_id" : Identifiant
    Le même identifiant est utilisé dans chaque bloc dans la structure incluante. Cet identifiant correspond à la copie de evIdProc et constitue la clé primaire.
    *Pour une structure incluse, ce champ n'existe pas.*

"evId" (event Identifier) : identifiant de l'événement
     Géré par le moteur processing et par l’ingest, il identifie l'entrée / le versement de manière unique dans la base ; cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par pair (début/fin), mais pas le même partout afin d'identifier un eventType.

"evType" (event Type) : nom de la tâche
    Géré par le moteur processing et par l’ingest, issu de la définition du workflow en json (fichier default-workflow.json).
    ``Exemple : "Check Version", "Check Objects Number"``

"evDateTime" (event DateTime) : date de l'événement
  Positionné par le client LogBook, sauf dans le cas de délégation.
  Date au format AAAA-MM-JJJ+"T"+hh:mm:ss:[3digits de millisecondes]
  ``Exemple : "2016-08-17T08:26:04.227"``

"evDetData" (event Detail Data) : détails des données de l'évenement.
  *Utilisation au 5/09/2016 : Positionné uniquement par le module access lors de la modification d'un unit. Lors de l'update d'un unit, un texte représentant le différentiel des champs modifiés est positionné dans ce champ.*

"evIdProc" (event Identifier Process) : identifiant du processus.
   Géré par le moteur processing. Toutes les mêmes entrées du journal des opérations partagent la même valeur.

   *Utilisation au 17/08/2016  : pour une opération d'entrée, tous les evIdProc et tous les _id sont les mêmes.*

"evTypeProc" (event Type Process) : type de processus.
  Nom du processus qui effectue l'action, parmi une liste de processus possible fixée.

    *Utilisation au 17/08/2016 et pour les opérations d'entrées : Pour la structure incluante, la valeur est "INGEST". Pour les structures incluses, la valeur est "INGEST" ou "OK" (ne devrait être que INGEST ou toute autre valeur définie dans l'enum LogbookTypeProcess.*

"outcome" : Statut de la tâche ou de l'étape.
   Parmi une liste de processus possible fixée : Started, Ok, Ko, Warning.

  *Utilisation au 17/08/2016 : les valeurs suivantes sont des possibilités de statust : STARTED, OK, WARNING, KO, FATAL*

"outDetail" (outcome Detail) : code correspondant à l'erreur

    *Utilisation au 17/08/2016 : la valeur est toujours à 'null'. Il semble que les codes soient à définir dans une story encore non planifiée (story #590 intégrée à l’IT08). La forme serait constituée d'un code d'erreur HTTP et d'un sous code d'erreur Vitam (exemple : 404_123456)*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
  C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
  Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier).
  ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

"agIdApp" (agent Identifier Application) : identifiant ou nom de l’application externe qui appelle Vitam pour effectuer une opération

   *Utilisation au 17/08/2016 : la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Pour une structure incluse, ce champ n'existe pas.*

"agIdAppSession" (agent Identifier Application Session) : identifiant donnée par l’application utilisatrice externe qui appelle Vitam à la session utilisée pour lancer l’opération
  L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe. *Pour une structure incluse, ce champ n'existe pas.*

    *Utilisation au 17/08/2016 : la valeur est toujours 'null'*

"evIdReq" (event Identifier Request) : identifiant de la requête déclenchant l’opération
  Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur..l’exterieur Dans le cas d'Ingest, il devrait s'agir du numéro de l'opération (EvIdProc).

"agIdSubm" (agent Identifier Submission) : identifiant du service versant.
  Il s'agit du SubmissionAgency dans le SEDA. *Pour une structure incluse, ce champ n'existe pas.*

"agIdOrig" (agent Identifier Originating) : identifiant du service producteur.
  Il s'agit du OriginatingAgency dans le SEDA. *Pour une structure incluse, ce champ n'existe pas.*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
   Dans le cas d’une opération ‘Ingest’, il s’agit du GUID de l’entrée (evIdProc). Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini

    *Utilisation au 17/08/2016 : la valeur est toujours 'null'*

"obIdReq" (object Identifier Request) : requête caractérisant un lot d’objets auquel s’applique l’opération.
  Ne concerne que les lots d’objets dynamique, c’est-à-direc’est à dire obtenu par la présente requête. Ne concerne pas les lots ayant un identifiant défini.

  *Utilisation au 17/08/2016 : la valeur est toujours 'null'*

"obIdIn" (ObjectIdentifierIncome) :   identifiant externe du lot d’objets auquel s’applique l’opération.
  Chaîne de caractère intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se reporte l'événement. Il s'agit le plus souvent soit du nom du SIP lui-mêmelui même, soit du MessageIdentifier présent dans le manifeste. Il pourrait aussi être renseigné au démarrage avec le nom du fichier du SIP reçu, si ce nom existe.

"events": tableau de structure
  Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date) ; *Pour une structure incluse, le champ n'existe pas.*

"_tenant": identifiant du tenant
  .



Collection LogbookLifeCycleUnit & LogbookLifeCycleObjectGroup
=============================================================

Chaque Unit et groupe d'objet possède une et une seule entrée dans sa collection respective (LogbookLifeCycleUnit et LogbookLifeCycleObjectGroup).

"_id" : Identifiant attribué dans le code.
  Le même identifiant est utilisé pour chaque _id, que ce soit dans la structure incluante ou les incluses. Cet identifiant est la copie de obId (Object Identifier) qui constitue la clé primaire.

"evId" (event Identifier) : identifiant de l'événement.
   Il s’agit du GUID, sauf dans le cas du storage des groupes d’objets, auquel cas il s’agit de l’identifiant XML du BinaryDataObject dans le manifeste. Ceci devrait être TOUJOURS l'identifiant GUID de l'événement (comme dans Opération).

"evType" (event Type) : nom de la tâche.
  Géré par le moteur processing issu de la définition du workflow en json (fichier default-workflow.json).
  ``Exemple : "Check Version", "Check Objects Number"``

"evDateTime" (event DateTime) : date de l'événement.
  Date au format AAAA-MM-JJJ+"T"+hh:mm:ss:[3digits de millisecondes]
  ``Exemple : "2016-08-17T08:26:04.227"``

"evIdProc" (event Identifier Process) : identifiant du processus.
  Géré par le moteur processing. Toutes les mêmes entréesentrée du journal des opérations partagent la même valeur. Toutes les entrées d'un cycle de vie se rapportant à la même opération utilisent le même identifiant d'opération dans ce champ. Ce champ sert de jointure entre la table Opération et les tables Cycles de vie.

    *Utilisation au 17/08/2016 : pour une opération d'entrée, tous les evIdProc et toust les _id sont les mêmes.*

"evTypeProc" (event Type Process) : type de processus.
   Nom du processus qui effectue l'action, parmi une liste de processus possible fixée.

"outcome" : statut de la tâche ou de l'étape.
  Parmi une liste de processus possible fixée

    *Utilisation au 17/08/2016 : les valeurs suivantes sont des possibilités de statust : STARTED, OK, WARNING, KO, FATAL*

"outDetail" (outcome Detail)  : code correspondant à l'erreur

    *Utilisation au 17/08/2016 : on peut y trouver des statuts comme "STARTED". La forme serait constituée d'un code d'erreur HTTP et d'un sous code d'erreur Vitam (exemple : 404_123456)*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
  C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement pour lui permettre de comprendre le résultat de l’opération effectuée sur l’objet.

"evDetData" (eventDetailData) : Information complémentaire concernant l’événement.
  C’est un message intelligible destiné à être lu par un être humain.

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
   Il s'agit de plusieurs chaîneschaine de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal.

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
  Dans le cas d’une opération ‘Ingest’, il s’agit du GUID de l’entrée. Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini

    *Utilisation au 17/08/2016 : la valeur est toujours 'null'.  Il s'agit de l'identifiant unique du Unit (cycle de vie des ArchiveUnits) ou du Groupe d'Objet (cycle de vie des Groupes d'objets), et non du GUID de l'entrée.*

    **Proposition : ajouter un champ pour nommer dans le cas des groupes d'objets l'objet sur lequel cet enregistrement porte (exemple : check d'empreinte d'un groupe d'objet de 2 objets, il faut pouvoir tracer les 2). Chaque ligne peut alors partager le evId.**

"events": tableau de structure.
  Pour la structure incluante, le tableau contient N structures incluses ; pour une structure incluse, le tableau est vide.

"_tenant": identifiant du tenant.
  .

Collection Unit
===============

La structure de la collection Unit est composée de : la transposition JSON de toutes les balises XML contenues dans la balise <DescriptiveMetadata> du bordereau SEDA, c'est-à-dire toutes les balises se rapportant aux archivesArchiveUnit. Cette transposition se fait comme suit :

"_id" : l'identifiant de cette unit, attribué par le code.
  - L'intégralité de la balise <content> du bordereau pour cette unit.

- Lorsque des balises sont imbriquées, on procède comme suit :
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

     <content>
      <Keyword>
         <KeywordContent>KeywordContent0</KeywordContent>
      </Keyword>
     </content>

     Devient :
     {
        "Keyword": {
            "KeywordContent": "KeywordContent0"
        },
     }

  - Dans le champ "DataObjectReference", on y indique le _og. ``Exemple :
    "DataObjectReference": {
        "_og": "aeaaaaaaaaaam7mxabgneakwum2qewyaaaaq"
    }, ``  Je ne comprends pas cet élément : Est-ce l'OG ou l'object ? Pourquoi DataObjectReference ? Est-ce un champ intermédiaire ?

"_mgt" :
  possède les balises reprises du bloc <Management> du bordereau pour cette unit

"_og": liste des identifiants des groupes d'objets référencés dans cette unit
  ce n'est pas une liste mais un seul identifiant, celui de l'ObjectGroup (sans s) référencé par cet Unit ?
"_up": [] est un tableau qui recense les _id des units parentes (parents immédiats)
  .
"_dom": il s'agit de l'identifiant du tenant
  (à changer en _tenant pour être homogène)
* "_max": profondeur maximum de l'unit par rapport à une racine "depthmax"
  Calculé, cette profondeur est le maximum des profondeurs, quelles ques soient les racines concernées et les chemins possibles
* "_min": profondeur minimum de l'unit par rapport à une racine, "depthmin"
  calculé , symétriquement le minimum des profondeurs, quelles ques soient les racines concernées et les chemins possibles ;
* "_nbc": nombre d'enfants immédiats de l'unit
  .
Manque en description :
:::::::::::::::::::::::

 _uds: { GUID1 : depth1, GUID2 : depth2, ... }, // parentalité, non indexé et pas dans ES
 _ud: [ GUID1, GUID2, ... }, // parentalité, indexé
 ArchiveUnitProfile : profile, // devrait être plus tard = _type
 _type: documentType, // sera utilisé plus tard avec les notions de DocumentType, Filière
 _dom: TenantId, // à renommer en _tenantId


Collection ObjectGroup
======================

"_id" : identifiant du groupe d'objet
  .
"_tenantId" : identifiant du tenant
  .
"_type" : repris du nom de la balise présente dans le <Metadata> du <DataObjectPackage> du manifeste qui concerne le BinaryMaster.
  Attention, il s'agit d'une reprise de la balise et non pas des valeurs à l'intérieur. Les valeurs possibles pour ce champ sont : Audio, Document, Text, Image et Video. Des extensions seront possibles (Database, Plan3D, ...)

::

Exemple :

 <DataObjectPackage>
  <Metadata>
   <Audio>MaValeur</Audio>
  </Metadata>
  ...
 </DataObjectPackage>

 Deviendra :

 _type : Audio

 "FileInfo" : reprend le bloc FileInfo du BinaryMaster ; l'objet de cette copie est de pouvoir conserver les informations initiales du premier BinaryMaster (version de création), au cas où cette version serait détruite (selon les règles de conservation), et car ces informations ne sauraient être maintenues de manière garantie dans les futures versions.

 "_qualifiers" : est une structure qui va décrire les objets inclus dans ce groupe d'objet. Il est composé comme suit  ::

 {
   [Nom du DataObjectVersion (BinaryMaster, Dissemination...) ]: {
            "nb": nombre d'objets de cet usage
            "versions" : [] tableau des objets par version (une version = une entrée dans le tableau). Ces informations sont toutes issues du manifest
                {
                    "_id": identifiant de 'objet
                    "DataObjectGroupId" : identifiant du groupe d'objet : ne sert à rien puisque c'est le _id global
                    "DataObjectVersion" : usage de l'objet : ne sert à rien, car c'est le nom de la balise englobante
                    "MessageDigest": empreinte de l'objet (mais manque l'algorithme et au format VITAM – SHA-512 - )
                    "Size": taille de l'objet (en octets)
                    "FormatIdentification": {
                        "FormatLitteral" : nom du format
                        "MimeType" : type Mime
                        "FormatId" : PUID du format
                    },
                    "FileInfo": {
                        "Filename" : nom de l'objet
                        "LastModified" : date de dernière modification de l'objet au format YYY-MM-DD + 'T' + hh:mm:ss.millisecondes "+" timezone hh:mm. Exemple : "2016-08-19T16:36:07.942+02:00"
                    }
                }
            ]
        }
    }
 La liste n'est pas exhaustive : le modèle est capable de reprendre l'intégralité des champs du manifeste.
    "_up": [] : tableau d'identifiant des units parentes
    ],
    "_nb": nombre d'objets dans ce groupe d'objet
    "_dom": identifiant du tenant, au même titre que _tenantid (devrait être _tenant ou _tenantId selon la normalisation à appliquer)
    "_nbc": ce champ n'est pas utilisé (choix à faire entre _nb ou _nbc, car ce sont les mêmes)
    Utilisation au 17/08/2016 : la valeur est toujours '0'
 }

::

 Selon le document Graphe :
 ObjectGroup : {
  _id: GUID, // identifiant de l'ObjectGroup
  MD technique globale (FileInfo),
  _tenantId: TenantId, // Tenant (_dom à supprimer)
  _type: audio|video|document|text|image|..., // sera utilisé plus tard avec les notions de DocumentType
  _qualifiers : { // accès aux usages
    BinaryMaster : { // mais aussi Dissemination, Thumbnail, TextContent, …
      nb : N, // nb de versions
      versions : [ // tableau de versions
        {
          _id: GUIDObject, // identifiant de l'Object dans ce Groupe d'objets (non pérenne)
          MD techniques, // directement à la racine (FormatIdentification, FileInfo, Metadata, OtherMetadata)
          _version : rank, // rang de cette version
          CreatedDate : date, // date de création de cette version
          MessageDigest : { value : val, algo : algo },
          Size: size,
          Fmt: fmt,
          _copies : [ // tableau de copies
            { sid: identifiant de l'offre de stockage }, { sid }, ...
          ]
        }, { _version : N, ...}, ...
      ]
    }, Dissemination : {… }, …,
  },
  // Gestion du DAG
  _up : [ GUIDUnit1, GUIDUnit2, ... ], // parents (Units) immédiats
  _nbc : nb objects // Nombre d'Objets intégrés dans cet ObjectGroup (pas _nb)
 }

 Collection FileFormat
 {
    "_id" : identifiant du format
    "CreatedDate" : la date de création du fichier pronomdate de création du fichier de signature pronom utilisé pour alimenter l’enregistrement correspondant au format dans Vitam (balise DateCreated dans le fichier)
    "VersionPronom":  version du fichier de signature pronom utilisé pour alimenter l’enregistrement correspondant au format dans Vitam (balise Version)
    "MIMEType": [], tableau des types mimes de ce format (attribut MIMEType du FileFormat)
    "PUID": PUID du format (attribut PUID du FileFormat)
    "Version": version du format (attribut Version du FileFormat)
    "Name": nom complet du format (attribut Name du FileFormat)
    "Extension": [], tableau des extensions possibles pour ce format (tableau réunissant les balises <Extension>)
    "HasPriorityOverFileFormatID": [], tableau d'identifiants qui ont une priorité moindre que ce format (tableau réunissant les balises <HasPriorityOverFileFormatID>) mais au format PUID
 }


**Champs dont la création avait été demandée dans la story (bug créé)**

=============== ================================ =========================================
Comment          Commentaire                      String (forme string/string)
Alert            obsolescence du format           type booléen, valeur par défaut = false
Group            famille du format de fichier     String (forme string/string)
=============== ================================ =========================================