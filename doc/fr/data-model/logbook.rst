Base Logbooks
#############

Collections contenues dans la base
===================================

Il s'agit des collections relatives aux journaux d'opérations et de cycles de vie des archives et des objets numériques.

Collection LogbookOperation
===========================

Utilisation de la collection LogbookOperation
---------------------------------------------

La collection LogbookOperation comporte toutes les informations de traitement liées aux opérations effectuées dans la solution logicielle Vitam, chaque opération faisant l'objet d'un enregistrement distinct.

Ces opérations sont :

- Entrée (implémenté dans la release en cours)
- Mise à jour (implémenté dans la release en cours)
- Données de référence (implémenté dans la release en cours)
- Audit (développé post-bêta)
- Elimination (développé post-bêta)
- Préservation (développé post-bêta)
- Vérification (implémenté dans la release en cours)
- Sécurisation (implémenté dans la release en cours)

Exemple de JSON stocké en base
------------------------------

Extrait d'un JSON correspondant à une opération d'entrée terminée avec succès.

::

  {
    "_id": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "evType": "PROCESS_SIP_UNITARY",
    "evDateTime": "2017-04-06T23:12:09.233",
    "evDetData": "{\"evDetDataType\":\"MASTER\",\"EvDetailReq\":\"Jeu de test avec arborescence complexe\",\"EvDateTimeReq\":\"2016-11-22T13:50:57\",\"ArchivalAgreement\":\"ArchivalAgreement0\",\"AgIfTrans\":\"Identifier5\"}",
    "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": null,
    "outMessg": "Début du processus d'entrée du SIP : aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
    "agIdApp": null,
    "agIdAppSession": null,
    "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "agIdSubm": null,
    "agIdOrig": null,
    "obId": null,
    "obIdReq": null,
    "obIdIn": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
    "events": [
        {
            "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "evType": "STP_SANITY_CHECK_SIP",
            "evDateTime": "2017-04-06T23:12:09.234",
            "evDetData": null,
            "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du processus des contrôles préalables à l'entrée",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
            "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            "evId": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "evType": "SANITY_CHECK_SIP",
            "evDateTime": "2017-04-06T23:12:09.980",
            "evDetData": null,
            "evIdProc": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": null,
            "outMessg": "Début du contrôle sanitaire",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1045329142,\"SiteId\":1,\"GlobalPlatformId\":240022774}",
            "evIdReq": "aedqaaaaache45hwaantmak3iwfbl6qaaaaq",
            "obId": null,
            "obIdReq": null,
            "obIdIn": null
        },
        {
            
            [...]
        }
    ],
    "_tenant": 0
  }

Détail des champs du JSON stocké en base
-----------------------------------------

Chaque entrée de cette collection est composée d'une structure auto-imbriquée : la structure possède une première instanciation "incluante", et contient un tableau de N structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.


"_id" : Identifiant unique donné par le système lors de l'initialisation de l'opération
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire de l'opération dans la collection.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
     Il identifie l'entrée / le versement de manière unique dans la base.
     Cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par pair (début/fin).

     *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    Issu de la définition du workflow en json (fichier default-workflow.json).
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement.
    Par exemple, pour l'étape ATR_NOTIFICATION, ce champ détaille le nom de l'ATR, son empreinte et l'algorithme utilisé pour calculer l'empreinte.
    Sur la structure incluante du journal d'opérations d'entrée, il contient un JSON composé des champs suivants :
    * evDetDataType : Indique la structure impactée. Doit correspondre à une valeur de l'énumération LogbookEvDetDataType
    * EvDetailReq : chaîne de caractères reprenant le champ "comment" du bordereau
    * EvDateTimeReq : Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes. Date de l'événement inscrit dans le champs evDetData.
    * ArchivalAgreement : chaîne de caractères reprenant le nom du contrat utilisé pour réaliser l'entrée,  indiqué dans le champs  ArchivalAgreement du bordereau
    * AgIfTrans : chaîne de caractères contenant le nom de l'agence ayant réalisé le transfert du SIP
    * ServiceLevel : chaîne de caractères reprenant le champ "ServiceLevel" du bordereau
    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent interne réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"agIdApp" (agent Identifier Application) : identifiant de l’application externe qui appelle Vitam pour effectuer l'opération

    *Actuellement, la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe uniquement pour la structure incluante.*

"agIdAppSession" (agent Identifier Application Session) : identifiant donnée par l’application utilisatrice externe
    qui appelle Vitam à la session utilisée pour lancer l’opération
    L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.

    *Actuellement, la valeur est toujours 'null' mais sera renseignée une fois le mécanisme d'authentification mis en place. Ce champ existe pour les structures incluantes et incluses*

"evIdReq" (event Identifier Request) : identifiant de la requête déclenchant l’opération
    Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).

    *Ce champ existe pour les structures incluantes et incluses*. Il s'agit du X-Application-Id.

"agIdSubm" (agent Identifier Submission) : identifiant du service versant.
    Il s'agit du <SubmissionAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"agIdOrig" (agent Identifier Originating) : identifiant du service producteur.
    Il s'agit du <OriginatingAgencyIdentifier> dans le SEDA. Mis en place avant le développement du registre des fonds.

    *Ce champ existe uniquement pour la structure incluante.*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
     Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc). Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini

     *Ce champ existe pour les structures incluantes et incluses*

"obIdReq" (object Identifier Request) : Identifiant de la requête caractérisant un lot d’objets auquel s’applique l’opération.
      Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.

      *Actuellement, la valeur est toujours 'null'. Ce champ existe pour les structures incluantes et incluses*

"obIdIn" (ObjectIdentifierIncome) : Identifiant externe du lot d’objets auquel s’applique l’opération.
      Chaîne de caractère intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se reporte l'événement.
      Il s'agit le plus souvent soit du nom du SIP lui-même, soit du <MessageIdentifier> présent dans le manifeste.

      *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
      Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

      *Ce champ existe uniquement pour la structure incluante.*

"_tenant": identifiant du tenant
      *Ce champ existe uniquement pour la structure incluante.*

Détail des champs du JSON stocké en base spécifiques à une opération de Sécurisation
------------------------------------------------------------------------------------

Exemple de données stockées :

::

  "evDetData":
  "{
  \"LogType\": \"operation\",
  \"StartDate\": \"2017-02-27T00:00:00.000\",
  \"EndDate\": \"2017-02-27T14:11:36.168\",
  \"PreviousLogbookTraceabilityDate\": \"2017-02-26T00:00:00.000\",
  \"MinusOneMonthLogbookTraceabilityDate\": \"2017-01-28T00:00:00.000\",
  \"MinusOneYearLogbookTraceabilityDate\": \"2016-02-28T00:00:00.000\",
  \"Hash\": \"cmKHRqv1HHB+Fd0JErOpztcdcV3BGlgcA0VAYxFjxjdEJO0+lOhhxNeK43mbrmgra6phNSuKBfVIXOE5i48
  77Q==\",
  \"TimeStampToken\": \"MIIEezAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEYAYJKoZIhvcNAQcCoIIEUTCCBE0CAQMxDzANBglghkg
  BZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARAiTJZ9fQyplZfbRHe7j34JFw1iQlJMmwEn5\/oa9hha3oeJ7b7A+I0MOiz8n3lhajK5GWDMptybTI\/qyydRxRwqAIBARgPMjAxNzAxMjcxNDExMzdaMYIDsjCCA64CAQEwYzBdMQswCQYDVQQGEwJGUjEMMAoGA1U
  ECBMDaWRmMQ4wDAYDVQQHEwVwYXJpczEPMA0GA1UEChMGVml0YW0uMR8wHQYDVQQDFBZDQV9zZXJ2ZXJfaW50ZXJtZWRpYXRlAgIAsDANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzAxMjcxNDExMzdaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQMa0fzRWvY0qJjOO4lO5aSfN3iW9xWwhSv24QSExqpp081WszJ0NIEP4gFOzAQIrE35Bz\/jgACNxVS8XXRda7\/AwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQAkVA\/7GPyjlbJC2NJJK+1ZY6k2vvEQls\/YcVrP9SV81nRL7fmrSw0mmia0Dj+kuu+qAun5hB6X9pzy4lbATsfEwDQYJKoZIhvcNAQENBQAEggIAgMAyr
  R6uTJYHxKqofV+HnPV+9fiykPb4DwNTWYKGEBOlu44yVfzep1P2GofDVBBguYQZHF0zCQ0vjktfGuVflh4GtiHsbhqKm6TMqeH+pdRv0MQvEYA3VK0ydA+\/36xb+tbOy8RBqUe3uXGpaafuqcrmlx0EYK4ey4I4sinvZKoB9c9kNCujlvpLxwPnL8teDe6\/jE4sWqvCHCSxorjXCXDN6aJTGvbFHepqa987eHRckDS5pdTiZ1a7V1IRjsX+bubA+ZYhWM5sA9L202msa8s\/zF5Nn+mmcApzpjiAkHu5u8QGuIe17jgHV0o73Zkv3Oranskz3Q3F3xXdNT8wblevU4mWFGQkW5wWhyyTf
  EKE97+z7+HTa5P4eLCEZkAgevkZPMo21PyEvNBUeXM3QIzfOKExX+wYpuL9k2\/5kg3ZmX3dMT1jxhZAr75puxp5pxOryuR+j0JFmeA8JI8a+XYsYZm75lV4uzSYl4QytMwNaSyxDwC4PBm
  Z9IGbPwRP8ttC8LSjeB+zwQug063kT0ZKmkCHzbZvVWHJlr3Iaew2UXjOabrWNIEijg6b6DBtze7sC9T8LXGHOlcAFFsW0kYfHb7MziVv22CCuUw4JyI5882I\/huPztjJqn+4bwzmAuWc8X\/OiyAbe2Iag23oaVJ36UU3QxzDLPhCg0TvNZg=\",
  \"NumberOfElement\": 366,
  \"Size\": 2554545,
  \"FileName\": \"0_LogbookOperation_20170127_141136.zip\"
  }"

Dans le cas d'un évènement final d'une opération de sécurisation du LogbookOperation, le champ **"evDetData"** est composé des champs suivants :

"LogType": type de logbook sécurisé.
      Type de la collection logbook sécurisée (LogbookOperation)
      ``Exemple : "operation"``

"StartDate": date de début.
      Date de début de la période de couverture de l'opération de sécurisation au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée par la précédente sécurisation)
      ``Exemple : "2016-08-17T08:26:04.227"``

"EndDate": date de fin.
      Date de fin de la période de couverture de l'opération de sécurisation  au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée)
      ``Exemple : "2016-08-17T08:26:04.227"``

"PreviousLogbookTraceabilityDate": date de la précédente sécurisation.
      Date de début de la précédente sécurisation du même type au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation précédente)
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneMonthLogbookTraceabilityDate": date de la sécurisation passée d'un mois.
      Date de début de la sécurisation un mois avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation passée d'un mois : logbook start 1 mois avant - logbookDate.mois(-1).suivant().sartDate)
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneMonthLogbookTraceabilityDate": date de la sécurisation passée d'un an.
     Date de début de la sécurisation un an avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation passée d'un an : logbook start 1 an avant - logbookDate.an(-1).suivant().sartDate)
     ``Exemple : "2016-08-17T08:26:04.227"``

"Hash": Empreinte racine.
      Empreinte de la racine de l'arbre de Merkle.

"TimeStampToken": Tampon d’horodatage.
      Tampon d’horodatage sûr du journal sécurisé.

"NumberOfElement": Nombre d'élèments.
      Nombre d'opérations sécurisées.

"Size": Taille du fichier.
      Taille du fichier sécurisé (en bytes).

"FileName": Identifiant du fichier.
      Nom du fichier sécurisé dans le stockage au format {tenant}_LogbookOperation_{AAAAMMJJ_HHMMSS}.zip.
      ``Exemple : "0_LogbookOperation_20170127_141136.zip"``


Collection LogbookLifeCycleUnit
===============================

Utilisation de la collection LogbookLifeCycleUnit
-------------------------------------------------

Le journal du cycle de vie d'une unité archivistique (ArchiveUnit) trace tous les événements qui impactent celle-ci dès sa prise en charge dans le système. Il doit être conservé aussi longtemps qu'elle est gérée par le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les ArchiveUnit qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des métadonnées OK, avant notification au service versant

Chaque unité archivistique possède une et une seule entrée dans sa collection LogbookLifeCycleUnit.

Exemple de JSON stocké en base
------------------------------

Extrait d'un JSON correspondant à un journal de cycle de vie d'une unité archivistique.

::

  {
    "_id": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
    "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
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
            "evType": "LFC.CHECK_MANIFEST",
            "evDateTime": "2017-04-10T12:39:37.953",
            "evIdProc": "aedqaaaaaghe45hwabliwak3k7qg7kaaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "LFC.CHECK_MANIFEST.STARTED",
            "outMessg": "Début de la vérification de la cohérence du bordereau",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
            "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
            "evDetData": null,
            "_tenant": 1
        },
        {
            "evId": "aedqaaaaaghbl62nabqkwak3k7qg5tiaaabq",
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
            "_tenant": 1
        },{

        [...]
        
        }
    ],
    "_tenant": 1
  }

Extrait d'un exemple avec une mise à jour de métadonnées

::

 {
   "_id": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
   "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
   "evType": "LFC.LFC_CREATION",
   "evDateTime": "2017-04-10T12:52:50.173",
   "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
   "evTypeProc": "INGEST",
   "outcome": "STARTED",
   "outDetail": "LFC.LFC_CREATION.STARTED",
   "outMessg": "!LFC.LFC_CREATION.STARTED!",
   "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
   "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
   "evDetData": null,
   "events": [
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
           "evType": "LFC.CHECK_MANIFEST",
           "evDateTime": "2017-04-10T12:52:50.205",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "STARTED",
           "outDetail": "LFC.CHECK_MANIFEST.STARTED",
           "outMessg": "Début de la vérification de la cohérence du bordereau",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           "evId": "aedqaaaaachbl62nabqkwak3k7wik7iaaaba",
           "evType": "LFC.CHECK_MANIFEST.LFC_CREATION",
           "evDateTime": "2017-04-10T12:52:50.205",
           "evIdProc": "aedqaaaaache45hwabliwak3k7wim4qaaaaq",
           "evTypeProc": "INGEST",
           "outcome": "OK",
           "outDetail": "LFC.CHECK_MANIFEST.LFC_CREATION.OK",
           "outMessg": "Succès de la création du journal du cycle de vie",
           "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
           "obId": "aeaqaaaaaahbl62nabqkwak3k7wik7iaaaaq",
           "evDetData": null,
           "_tenant": 0
       },
       {
           
           [...]

       }
   ],
   "_tenant": 0
}

Détail des champs du JSON stocké en base
-----------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal du cycle de vie.
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal du cycle de vie de l'unité archivistique.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possible fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement. Par exemple, l'historisation lors d'une modification de métadonnés se fait dans ce champ.

    *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
    Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

    *Ce champ existe uniquement pour la structure incluante*

"_tenant": identifiant du tenant
    *Ce champ existe pour les structures incluantes et incluses*



Détail des champs du JSON stocké en base spécifiques à une mise à jour
-----------------------------------------------------------------------

Exemple de données stockées :

::

   "evDetData": "{\"diff\":\"-  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS)\\n+  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS) 222\\n-  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq \\n+  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq, aecaaaaaacaam7mxabjssak2dzsjniyaaaaq \"}"


Dans le cas d'une mise à jour de métadonnées d'une unité archivistique (ArchiveUnit), le champ **"evDetData"** de l'évènement final est composé des champs suivants :

"diff": historisation des modifications de métadonnées.
    Son contenu doit respecter la forme suivante : les anciennes valeurs sont précédées d'un "-" (``-champ1: valeur1``) et les nouvelles valeurs sont précédées d'un "+" (``+champ1: valeur2``)

    ``Exemple :
    -Titre: Discours du Roi \n+Titre: Discours du Roi Louis XVI \n-Description: Etat Généraux du 5 mai 1789 \n+Description: Etat Généraux du 5 mai 1789 au Château de Versailles``


Collection LogbookLifeCycleObjectGroup
======================================

Utilisation de la collection LogbookLifeCycleObjectGroup
---------------------------------------------------------

Le journal du cycle de vie du groupe d'objets (ObjectGroup) trace tous les événements qui impactent le groupe d'objets (et les objets associés) dès sa prise en charge dans le système et doit être conservé aussi longtemps que les objets sont gérés dans le système.

- dès la réception de l'entrée, on trace les opérations effectuées sur les groupes d'objets et objets qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets OK et l'indexation des MD OK, avant notification au service versant

Chaque groupe d'objets possède une et une seule entrée dans sa collection LogbookLifeCycleObjectGroup.

Exemple de JSON stocké en base
-------------------------------

::

  {
    "_id": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
    "evId": "aedqaaaaacaam7mxaap44akyf7hurgaaaabq",
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
            "evType": "CHECK_CONSISTENCY",
            "evDateTime": "2016-11-04T14:47:43.515",
            "evIdProc": "aedqaaaaacaam7mxaau56akyf7hr45qaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "OK",
            "outMessg": "Objet/groupe dobjet référencé par un ArchiveUnit.",
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
            "_tenant": 0
        },
        {
            "evId": "\"aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba\"",
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
            "_tenant": 0
        },
        {
            
            [...]
            
        }
    ],
    "_tenant": 0
    }


Détail des champs du JSON stocké en base
-----------------------------------------

"_id" : Identifiant unique donné par le système lors de l'initialisation du journal du cycle de vie.
    Il est constitué d'une chaîne de 36 caractères.
    Cet identifiant constitue la clé primaire du journal du cycle de vie du groupe d'objet.

    *Ce champ existe uniquement pour la structure incluante.*

"evId" (event Identifier) : identifiant de l'événement constitué d'une chaîne de 36 caractères.
    Il s'agit du GUID de l'évènement. Il identifie l'évènement de manière unique dans la base.

    *Ce champ existe pour les structures incluantes et incluses*

"evType" (event Type) : nom de la tâche,
    La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"evDateTime" (event DateTime) : date de l'événement
    Positionné par le client LogBook.
    Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``

    *Ce champ existe pour les structures incluantes et incluses*

"evIdProc" (event Identifier Process) : identifiant du processus. Il s'agit d'une chaîne de 36 caractères.
    Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id"

    *Ce champ existe pour les structures incluantes et incluses*

"evTypeProc" (event Type Process) : type de processus.
    Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.

    *Ce champ existe pour les structures incluantes et incluses*

"outcome" : Statut de l'évènement.
    Parmi une liste de valeurs fixée :

    - STARTED (début de l'évènement)
    - OK (Succès de l'évènement)
    - KO (Echec de l'évènement)
    - WARNING (Succès de l'évènement comportant des alertes)
    - FATAL (Erreur technique)

    *Ce champ existe pour les structures incluantes et incluses*

"outDetail" (outcome Detail) : code correspondant à l'erreur
    *Ce champ existe pour les structures incluantes et incluses*
    Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)

    *Ce champ existe pour les structures incluantes et incluses*

"outMessg" (outcomeDetailMessage) : détail de l'événement.
    C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    La liste des valeurs possibles pour ce champ se trouve en annexe. Il est directement lié au code présent dans outDetail.

    *Ce champ existe pour les structures incluantes et incluses*

"agId" (agent Identifier) : identifiant de l'agent réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    *Ce champ existe pour les structures incluantes et incluses*

"obId" (object Identifier) : identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    *Ce champ existe pour les structures incluantes et incluses*

"evDetData" (event Detail Data) : détails des données de l'évènement.
    Donne plus de détail sur l'évènement.

    *Ce champ existe pour les structures incluantes et incluses*

"events": tableau de structure
    Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)

    *Ce champ existe uniquement pour la structure incluante.*

"_tenant": identifiant du tenant
    *Ce champ existe pour les structures incluantes et incluses*


