Collection LogbookOperation
###########################

Utilisation de la collection LogbookOperation
=============================================

La collection LogbookOperation comporte toutes les informations de traitement liées aux opérations effectuées dans la solution logicielle Vitam, chaque opération faisant l'objet d'un enregistrement distinct.

Ces opérations sont :

- Audit (implémentée dans la release en cours)
- Données de référence (implémentée dans la release en cours)
- Elimination (non implémentée dans la release en cours)
- Entrée (implémentée dans la release en cours)
- Mise à jour (implémentée dans la release en cours)
- Préservation (non implémentée dans la release en cours)
- Sécurisation (implémentée dans la release en cours)
- Vérification (implémentée dans la release en cours)

Les valeurs correspondant à ces opérations dans les journaux sont détaillées dans l'annexe 6.3.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection LogbookOperation
=====================================================================================================

Extrait d'un JSON correspondant à une opération d'entrée terminée avec succès.

::

 {
    "_id": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "evParentId": null,
    "evType": "PROCESS_SIP_UNITARY",
    "evDateTime": "2017-09-12T12:08:33.166",
    "evDetData": "{\n  \"EvDetailReq\" : \"Cartes postales (Grande Collecte)\",\n  \"EvDateTimeReq\" : \"2016-10-12T16:28:40\",\n  \"ArchivalAgreement\" : \"ArchivalAgreement0\",\n  \"ServiceLevel\" : null\n}",
    "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "evTypeProc": "INGEST",
    "outcome": "STARTED",
    "outDetail": "PROCESS_SIP_UNITARY.STARTED",
    "outMessg": "Début du processus d'entrée du SIP : aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
    "agIdApp": "CT-000001",
    "evIdAppSession": "MyApplicationId-ChangeIt",
    "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "agIdExt": "{\"originatingAgency\":\"Identifier0\",\"TransferringAgency\":\"ARCHIVES DEPARTEMENTALES DE LA VENDEE\",\"ArchivalAgency\":\"ARCHIVES DEPARTEMENTALES DE LA VENDEE\"}",
    "rightsStatementIdentifier": "{\"ArchivalAgreement\":\"ArchivalAgreement0\"}",
    "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "obIdReq": null,
    "obIdIn": "Cartes postales (Grande Collecte)",
    "events": [
        {
            "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evParentId": null,
            "evType": "STP_SANITY_CHECK_SIP.STARTED",
            "evDateTime": "2017-09-12T12:08:33.166",
            "evDetData": null,
            "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "STP_SANITY_CHECK_SIP.STARTED.OK",
            "outMessg": "Début du processus des contrôles préalables à l'entrée",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
            "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq"
        },
        {
            "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evParentId": null,
            "evType": "STP_SANITY_CHECK_SIP",
            "evDateTime": "2017-09-12T12:08:33.219",
            "evDetData": null,
            "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "STP_SANITY_CHECK_SIP.OK",
            "outMessg": "Début du processus des contrôles préalables à l'entrée",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
            "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq"
        },
        {
            "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evParentId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evType": "SANITY_CHECK_SIP",
            "evDateTime": "2017-09-12T12:08:33.219",
            "evDetData": null,
            "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "OK",
            "outDetail": "SANITY_CHECK_SIP.OK",
            "outMessg": "Succès du contrôle sanitaire",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
            "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq"
        },
        {
            [...]
        }
    ],
    "_tenant": 0,
    "_v": 1,
    "_lastPersistedDate": "2017-09-12T12:08:33.219"
  }

Détail des champs du JSON stocké dans la collection
===================================================

Chaque enregistrement de cette collection est composé d'une structure auto-imbriquée : la structure possède une première instanciation "incluante" et contient un tableau de n structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.

**"_id" (identifier):** Identifiant unique donné par le système lors de l'initialisation de l'opération

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * La règle classique est que sa valeur est égale à cele du champ evIdReq. Dans le cas d'une requête déclenchant plusieurs opérations, comme une mise à jour de règles de gestion par exemple, alors ce champ aura pour la première opération la même valeur que le champ evIdReq, puis celle du champ evIdProc pour les suivantes.
  * Cet identifiant constitue la clé primaire de l'opération dans la collection.
  * Cardinalité : 1-1
  * Ce champ existe uniquement pour la structure incluante.

**"evId" (event Identifier):** identifiant de l'événement

  * Il s'agit d'une chaîne de 36 caractères.
  * Champs obligatoire peuplé par la solution logicielle Vitam.
  * Il identifie l'opération de manière unique dans la collection.
  * Cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par paire (début/fin).
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses

**"evParentId" (event Parent Identifier):** identifiant de l'événement parent.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID.
    * Il identifie l'événement parent. Par exemple pour CHECK_SEDA, il s'agit de STP_INGEST_CONTROL_SIP.
    * Ce champ est toujours à null pour la structure incluante et les tâches principales
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"evType" (event Type):** code du type de l'opération

  * Issu de la définition du workflow en JSON (fichier default-workflow.json).
  * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se faisant via un fichier properties (vitam-logbook-message-fr.properties).
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evDateTime" (event DateTime):** date de lancement de l'opération

  * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
  * Elle est renseignée par le client LogBook.
    ``Exemple : "2016-08-17T08:26:04.227"``
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evDetData" (event Detail Data):** détails des données l'événement.

  * Donne plus de détails sur l'événement ou son résultat.
  * Par exemple, pour l'étape ATR_NOTIFICATION, ce champ détaille le nom de l'ArchiveTransferReply, son empreinte et l'algorithme utilisé pour calculer l'empreinte.

  * Sur la structure incluante d'une opération d'entrée, il contient un JSON composé des champs suivants :

    * evDetDataType : structure impactée. Chaîne de caractères. Doit correspondre à une valeur de l'énumération LogbookEvDetDataType
    * EvDetailReq : précisions sur la demande de transfert. Chaîne de caractères. Reprend le champ "Comment" du message ArchiveTransfer.
    * EvDateTimeReq : date de la demande de transfert inscrit dans le champ evDetData. Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes].
    * ArchivalAgreement : identifiant du contrat d'entrée utilisé. Reprend le champ "ArchivalAgreement" du message ArchiveTransfer
    * ArchiveProfile : identifiant du profil d'archivage utilisé. Reprend le champ "ArchiveProfile" du message ArchiveTransfer. Cardinalité 0-1.
    * ServiceLevel : niveau de service. Chaîne de caractères. Reprend le champ ServiceLevel du message ArchiveTransfer.
    * AcquisitionInformation : modalités d'entrée des archives. Chaîne de caractères. Reprend le champ AcquisitionInformation du message ArchiveTransfer
    * LegalStatus : statut des archives échangés. Chaîne de caractères. Reprend le champ LegalStatus du message ArchiveTransfer

  * Cardinalité pour les structures incluantes : 1-1
  * Cardinalité pour les structures incluses : 0-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evIdProc" (event Identifier Process):** identifiant du processus.

  * Il s'agit d'une chaîne de 36 caractères.
  * Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id". Dans le cas où une opération en déclenche d'autres, elles utilisent toutes le même evIdProc, qui permet alors de suivre une suite de processus.
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evTypeProc" (event Type Process):** type de processus.

  * Il s'agit d'une chaîne de caractères.
  * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe 6.3.
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"outcome":** Statut de l'événement.

  * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de la liste suivante :

    - STARTED (Début de l'événement)
    - OK (Succès de l'événement)
    - KO (Échec de l'événement)
    - WARNING (Succès de l'événement comportant toutefois des alertes)
    - FATAL (Erreur technique)

  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"outDetail" (outcome Detail):** code correspondant au résultat de l'événement.

  * Il s'agit d'une chaîne de caractères.
  * Il contient le code correspondant au résultat de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via un fichier properties (vitam-logbook-message-fr.properties)
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"outMessg" (outcome Detail Message):** détail du résultat de l'événement.

  * Il s'agit d'une chaîne de caractères.
  * C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement. Traduction du code présent dans outDetail, issue du fichier vitam-logbook-message-fr.properties.
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"agId" (agent Identifier):** identifiant de l'agent interne réalisant l'évènement.

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et l'identifiant du serveur, du site et de la plateforme. Ce champ est calculé par le journal à partir de ServerIdentifier et en s'appuyant sur des fichiers de configurations. ``Exemple : "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",``
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"agIdApp" (agent Identifier Application):** identifiant de l’application externe qui appelle la solution logicielle Vitam pour effectuer une opération. Cet identifiant est celui du contexte applicatif utilisé par l'application.

    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

**"agIdPers"** : identifiant personae, issu du certificat personnae.

    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"evIdAppSession" (event Identifier Application Session):** identifiant de la transaction qui a entraîné le lancement d'une opération dans la solution logicielle Vitam.

    * L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.
    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

**"evIdReq" (event Identifier Request):** identifiant de la requête déclenchant l’opération.

    * Il s'agit d'une chaîne de 36 caractères.
    * Cardinalité : 1-1
    * Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    * Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).
    * Il s'agit du X-Application-Id.
    * Ce champ existe pour les structures incluantes et incluses.

**"agIdExt" (agent Identifier External):** identifiant de l'agent externe mentionné dans le message ArchiveTransfer.

    * Il s'agit pour un ingest d'un JSON comprenant les champs suivants :

        * OriginatingAgency : identifiant du service producteur. Il s'agit d'une chaîne de caractères. Reprend le contenu du champ OriginatingAgencyIdentifier du message ArchiveTransfer.
        * TransferringAgency : identifiant du service de transfert. Il s'agit d'une chaîne de caractères. Reprend le contenu du champ TransferringAgencyIdentifier du message ArchiveTransfer.
        * ArchivalAgency : identifiant du service d'archivage. Il s'agit d'une chaîne de caractères. Reprend le contenu du champ ArchivalAgencyIdentifier du message ArchiveTransfer.
        * submissionAgency : identifiant du service versant. Il s'agit d'une chaîne de caractères. Reprend le contenu du champ SubmissionAgencyIdentifier du message ArchiveTransfer. Ne contient aucune valeur actuellement

    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

**"rightsStatementIdentifier":** identifiant des données référentielles en vertu desquelles l'opération peut s'éxécuter

    * Pour une opération d'INGEST, il comprend les champs suivant en JSON :

	   * ArchivalAgreement: identifiant du contrat d'entrée utilisé pour réaliser l'entrée.

	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchivalAgreement du message ArchiveTransfer.

	   * Profil: identifiant du profil utilisé pour réaliser l'entrée.

	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchiveProfile du message ArchiveTransfer.

    * Pour une opération d'UPDATE, il comprend les champs suivant en JSON :

    	* AccessContract : identifiant du contrat d'accès utilisé pour réaliser une mise à jour.

    * Cardinalité : 1-1

**"obId" (object Identifier):** identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    * Identifiant peuplé par la solution logicielle Vitam.
    * Il s'agit d'une chaîne de 36 caractères.
    * Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc).
    * Dans le cas d’une opération d'audit, il s’agit par exemple du nom d’un lot d’archives prédéfini.
    * Dans le cas d’une opération de mise à jour, il s’agit du GUID de l'unité archivistique mise à jour.
    * Dans le cas d'une opération de Masterdata, il s'agit de l'identifiant de l'opération.
    * Cardinalité structure incluante : 1-1
    * Cardinalité structure incluse : 0-1
    * Ce champ existe pour les structures incluantes et incluses.

**"obIdReq" (object Identifier Request):** identifiant de la requête caractérisant un lot d’objets auquel s’applique l’opération.

    * Identifiant peuplé par la solution logiciele Vitam.
    * Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.
    * Cardinalité : 1-1
    * Actuellement, la valeur est toujours 'null'.
    * Ce champ existe pour les structures incluantes et incluses.

**"obIdIn" (Object Identifier Income):** identifiant externe du lot d’objets auquel s’applique l’opération.

    * Chaîne de caractères intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se rapporte l'événement.
    * Reprend le contenu du champ MessageIdentifier du message ArchiveTransfer.
    * Cardinalité structure incluante : 1-1
    * Cardinalité structure incluse : 0-1
    * Ce champ existe pour les structures incluantes et incluses.

**"events":** tableau de structure.

    * Pour la structure incluante, le tableau contient n structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1
    * S'agissant d'un tableau, les structures incluses ont pour cardinalité 1-n.
    * Ce champ existe uniquement pour la structure incluante.

**"_tenant":** identifiant du tenant.

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

Champ présents dans les events
==============================

Les events sont au minimum composés des champs suivants:

      * evId
      * evParentId
      * evType
      * evDateTime
      * evDetData
      * evIdProc
      * evTypeProc
      * outcome
      * outDetail
      * outMessg
      * agId
      * AgIdPers
      * evIdReq
      * obId

D'autres champs peuvent apparaître dans certains events lorsqu'ils mettent à jour le master.

Détail des champs du JSON stocké en base spécifiques à une opération de sécurisation des journaux d'opération et de cycle de vie
================================================================================================================================

Ceci ne concerne aujourd'hui que les sécurisations des journaux d'opération et la sécurisation des journaux de cycle de vie.

Exemple de données stockées par l'opération de sécurisation des journaux d'opération :

::

    "evDetData":
      "{\"LogType\":\"OPERATION\",
      \"StartDate\":\"2018-07-16T06:55:02.577\",
      \"EndDate\":\"2018-07-16T07:55:02.436\",
      \"Hash\":\"Fdd5gi8oU9/nuuvudyShlVA2GqGff2ld2fxzzweNNIGwqlWAMlea/vXJmh3pGbM8B5Hj626iICAMRJxKriNEuw==\",
      \"TimeStampToken\":\"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\",
      \"PreviousLogbookTraceabilityDate\":\"2018-07-16T05:55:02.109\",
      \"MinusOneMonthLogbookTraceabilityDate\":\"2018-07-16T03:55:02.235\",
      \"MinusOneYearLogbookTraceabilityDate\":\"2018-07-16T03:55:02.235\",
      \"NumberOfElements\":3,
      \"FileName\":\"0_LogbookOperation_20180716_075502.zip\",
      \"Size\":41492,
      \"SecurisationVersion\":\"V1\",
      \"DigestAlgorithm\":\"SHA512\",
      \"MaxEntriesReached\":false}",

Dans le cas de l'événement final d'une opération de sécurisation du journal des opérations, le champ **"evDetData"** est composé des champs suivants :

**"LogType":** type de logbook sécurisé.

      * Collection faisant l'objet de l'opération de sécurisation

      ``Exemple : "operation"``

      * La valeur de ce champ est soit OPERATION, LIFECYCLE ou STORAGE, respectivement pour le journal des opérations, les journaux de cycles de vie ou le journal des écritures
      * Cardinalité : 1-1

**"StartDate":** date de début de la période de couverture de l'opération de sécurisation.

      * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la première sécurisation)

      ``Exemple : "2016-08-17T08:26:04.227"``

      * Cardinalité : 1-1

**"EndDate":** date de fin de la période de couverture de l'opération de sécurisation.

      * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée par la précédente sécurisation)

      ``Exemple : "2016-08-17T08:26:04.227"``

      * Cardinalité : 1-1

**"PreviousLogbookTraceabilityDate":** date de la précédente opération de sécurisation de ce type de journal.

      * Il s'agit  de la date de début de la précédente opération de sécurisation du même type au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation précédente)

      ``Exemple : "2016-08-17T08:26:04.227"``

      * Cardinalité : 1-1

**"MinusOneMonthLogbookTraceabilityDate":** date de l'opération de sécurisation passée d'un mois.

      * Il s'agit  de la date de début de la précédente opération de sécurisation du même type réalisée un mois avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]

      ``Exemple : "2016-08-17T08:26:04.227"``

      * Cardinalité : 1-1

**"MinusOneYeaLogbookTraceabilityDate":** date de l'opération de sécurisation passée d'un an.

      * Il s'agit de la date de début de la précédente opération de sécurisation du même type réalisée un an avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]

     ``Exemple : "2016-08-17T08:26:04.227"``

      * Cardinalité : 1-1

**"Hash":** empreinte racine.

      * Il s'agit d'une chaîne de caractères.
      * Empreinte de la racine de l'arbre de Merkle.
      * Cardinalité : 1-1

**"TimeStampToken":** tampon d’horodatage.

      * Il s'agit d'une chaîne de caractères.
      * Tampon d’horodatage sûr du journal sécurisé.
      * Cardinalité : 1-1

**"NumberOfElement":** nombre d'éléments.

      * Il s'agit d'un entier.
      * Nombre d'opérations sécurisées.
      * Cardinalité : 1-1

**"Size":** taille du fichier.

      * Il s'agit d'un entier.
      * Taille du fichier sécurisé (en octets).
      * Cardinalité : 1-1

**"SecurisationVersion":** version de l'algorithme de sécurisation.

      * Il s'agit d'une chaîne de caractères.
      * La version est une valeur fixe (v1, v2...)
      * Cardinalité : 1-1

**"FileName":** identifiant du fichier.

      * Il s'agit d'une chaîne de caractères.
      * Nom du fichier sécurisé sur les offres de stockage au format {tenant}_LogbookOperation_{AAAAMMJJ_HHMMSS}.zip.

      ``Exemple : "0_LogbookOperation_20170127_141136.zip"``

      * Cardinalité : 1-1

**"DigestAlgorithm":** algorithme de hachage.

      * Il s'agit d'une chaîne de caractères.
      * Il s'agit du nom de l'algorithme de hachage utilisé pour réaliser le tampon d'horodatage.
      * Cardinalité : 1-1
