Base Logbook
############

Collections contenues dans la base
===================================

La base Logbook contient les collections relatives aux journaux d'opérations et de cycles de vie des unités archivistiques et des objets numériques de la solution logicielle Vitam.

L'ensemble des champs sont peuplés automatiquement par Vitam.

Collection LogbookOperation
===========================

Utilisation de la collection LogbookOperation
---------------------------------------------

La collection LogbookOperation comporte toutes les informations de traitement liées aux opérations effectuées dans la solution logicielle Vitam, chaque opération faisant l'objet d'un enregistrement distinct.

Ces opérations sont :

- Entrée (implémentée dans la release en cours)
- Mise à jour (implémentée dans la release en cours)
- Données de référence (implémentée dans la release en cours)
- Audit (implémentée dans la release en cours)
- Elimination (non implémentée dans la release en cours)
- Préservation (non implémentée dans la release en cours)
- Vérification (implémentée dans la release en cours)
- Sécurisation (implémentée dans la release en cours)
  
Les valeurs correspondant à ces opérations dans les journaux sont détaillées dans l'annexe 5.3.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

Extrait d'un JSON correspondant à une opération d'entrée terminée avec succès.

::
           
 {
    "_id": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
    "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
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
            "evType": "STP_SANITY_CHECK_SIP",
            "evDateTime": "2017-09-12T12:08:33.166",
            "evDetData": null,
            "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "STP_SANITY_CHECK_SIP.STARTED",
            "outMessg": "Début du processus des contrôles préalables à l'entrée",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
            "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq"
        },
        {
            "evId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evType": "SANITY_CHECK_SIP",
            "evDateTime": "2017-09-12T12:08:33.219",
            "evDetData": null,
            "evIdProc": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "evTypeProc": "INGEST",
            "outcome": "STARTED",
            "outDetail": "SANITY_CHECK_SIP.STARTED",
            "outMessg": "Début du contrôle sanitaire",
            "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"ingest-external\",\"ServerId\":1211004455,\"SiteId\":1,\"GlobalPlatformId\":137262631}",
            "evIdReq": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq",
            "obId": "aedqaaaaacec45rhabfy2ak6ox625ciaaaaq"
        },
        {
            [...]
        }
    ],
    "_tenant": 0
  }

Détail des champs du JSON stocké dans la collection
----------------------------------------------------

Chaque entrée de cette collection est composée d'une structure auto-imbriquée : la structure possède une première instanciation "incluante", et contient un tableau de N structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.

**"_id" (identifier):** Identifiant unique donné par le système lors de l'initialisation de l'opération
    
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Cet identifiant constitue la clé primaire de l'opération dans la collection.
  * Cardinalité : 1-1
  * Ce champ existe uniquement pour la structure incluante.*

**"evId" (event Identifier): Champs obligatoire peuplé par Vitam** identifiant de l'événement 

  * Il s'agit d'une chaîne de 36 caractères.
  * Il identifie l'opération de manière unique dans la collection.
  * Cet identifiant doit être l'identifiant d'un événement dans le cadre de l'opération (evIdProc) et doit donc être différent par paire (début/fin).
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses*

**"evType" (event Type):** nom de l'événement

  * Issu de la définition du workflow en JSON (fichier default-workflow.json).
  * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se faisant via un fichier properties (vitam-logbook-message-fr.properties).
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses*

**"evDateTime" (event DateTime):** date de l'événement
    
  * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
  * Positionnée par le client LogBook.
    ``Exemple : "2016-08-17T08:26:04.227"``
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses*

**"evDetData" (event Detail Data):** détails de l'événement.

    * Donne plus de détail sur l'événement ou son résultat.
    * Par exemple, pour l'étape ATR_NOTIFICATION, ce champ détaille le nom de l'ArchiveTransferReply, son empreinte et l'algorithme utilisé pour calculer l'empreinte.
    
    * Sur la structure incluante du journal d'opérations d'entrée, il contient un JSON composé des champs suivants :

        * evDetDataType : structure impactée. Chaîne de caractères. Doit correspondre à une valeur de l'énumération LogbookEvDetDataType
        * EvDetailReq : précisions sur la demande de transfert. Chaîne de caractères. Reprend le champ "Comment" du message ArchiveTransfer. 
        * EvDateTimeReq : date de la demande de transfert inscrit dans le champs evDetData. Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes].
        * ServiceLevel : niveau de service. Chaîne de caractères. Reprend le champ ServiceLevel du message ArchiveTransfer
    
    * Cardinalité pour les structures incluantes : 1-1 
    * Cardinalité pour les structures incluses : 0-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"evIdProc" (event Identifier Process):** identifiant du processus. 

    * Il s'agit d'une chaîne de 36 caractères.
    * Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id"
    * Cardinalité : 1-1
    *Ce champ existe pour les structures incluantes et incluses*

**"evTypeProc" (event Type Process):** type de processus.

    * Il s'agit d'une chaîne de caractères.
    * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe 5.3.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"outcome":** Statut de l'événement.

    * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de la liste suivante :

    - STARTED (début de l'événement)
    - OK (Succès de l'événement)
    - KO (Echec de l'événement)
    - WARNING (Succès de l'événement comportant des alertes)
    - FATAL (Erreur technique)

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outDetail" (outcome Detail):** code correspondant au résultat de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * Il contient le code correspondant au résultat de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via un fichier properties (vitam-logbook-message-fr.properties)

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outMessg" (outcomeDetailMessage):** détail de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    Traduction du code présent dans outDetail issue du fichier vitam-logbook-message-fr.properties.

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"agId" (agent Identifier):** identifiant de l'agent interne réalisant l'action.
    Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"agIdApp" (agent Identifier Application):** identifiant de l’application externe qui appelle la solution logicielle Vitam pour effectuer l'opération. Cet identifiant est celui du contexte utilisé par l'application.

    * Il s'agit d'une chaîne de caractères. 
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"evIdAppSession" (agent Identifier Application Session):** identifiant de la transaction qui a entraîne le lancement d'une opération dans Vitam.

    * L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.
    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"evIdReq" (event Identifier Request):** identifiant de la requête déclenchant l’opération.

    * Il s'agit d'une chaîne de 36 caractères.
    * Cardinalité : 1-1 
    * Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    * Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).
    * Il s'agit du X-Application-Id
    * Ce champ existe pour les structures incluantes et incluses*. .

**"agIdExt" (agent Identifier External):** identifiants des agents externes mentionnés dans le message ArchiveTransfer.

    * Il s'agit pour un ingest d'un json comprennant les champs suivants :

	* "originatingAgency": identifiant du service producteur.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ OriginatingAgencyIdentifier du message ArchiveTransfer.
	* "transferringAgency": identifiant du service de transfert.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ TransferringAgencyIdentifier du message ArchiveTransfer.
	* "ArchivalAgency": identifiant du service d'archivage.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchivalAgencyIdentifier du message ArchiveTransfer.	    
	* "submissionAgency": identifiant du service versant.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ SubmissionAgencyIdentifier du message ArchiveTransfer.
	    Non rempli pour l'instant.

    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"rightsStatementIdentifier":** identifiants des données référentielles en vertue desquelle l'opération peut s'éxécuter.

    * Il s'agit pour un ingest d'un json comprennant les champs suivants :

	* ArchivalAgreement: contrat d'entrée utilisé pour réaliser l'ingest.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchivalAgreement du message ArchiveTransfer.	    
	* Profil: profil utilisé pour réaliser l'ingest.
	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchiveProfil du message ArchiveTransfer.	 

**"obId" (object Identifier):** identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    * Il s'agit d'une chaîne de 36 caractères.
    * Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc). 
    * Dans le cas d’une opération ‘Audit’, il s’agit par exemple du nom d’un lot d’archives prédéfini.
    * Dans le cas d’une opération d'update, il s’agit du GUID de l'objet mis à jour.
    * Cardinalité structure incluante : 1-1 
    * Cardinalité structure incluse : 0-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"obIdReq" (object Identifier Request):** Identifiant Vitam de la requête caractérisant un lot d’objets auquel s’applique l’opération.
    Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.

    *Actuellement, la valeur est toujours 'null'. Ce champ existe pour les structures incluantes et incluses*

**"obIdIn" (ObjectIdentifierIncome):** Identifiant externe du lot d’objets auquel s’applique l’opération.

    * Chaîne de caractères intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se raporte l'événement.
    * Reprend le contenu du champ MessageIdentifier du message ArchiveTransfer.
    * Cardinalité structure incluante : 1-1 
    * Cardinalité structure incluse : 0-1 
    * Ce champ existe pour les structures incluantes et incluses*
    * Non utilisé à ce jour*

**"events":** tableau de structure.

    * Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"_tenant":** identifiant du tenant.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
      *Ce champ existe uniquement pour la structure incluante.*

Champ présents dans les events
------------------------------

Les events sont au minimum composés des champs suivants:

      * evId 
      * evType
      * evDateTime
      * evDetData
      * evIdProc
      * evTypeProc
      * outcome
      * outDetail
      * outMessg
      * agId
      * evIdReq
      * obId

D'autres champs peuvent apparaitent dans certains events lorsqu'ils mettent à jour le master.

Détail des champs du JSON stocké en base spécifiques à une opération de sécurisation
------------------------------------------------------------------------------------

Exemple de données stockées :

::

	"evDetData":
	{
	\"LogType\":\"OPERATION\",
	\"StartDate\":\"2017-06-29T09:22:23.227\",
	\"EndDate\":\"2017-06-29T09:39:08.690\",
	\"Hash\":\"HYnFf07gFkar3lO+U2FQ9qkhi9eUMFN5hcH7oU7vrAAL3FAlMm8aJP7+VxkVWhLzmmFolwUEcq6fbS7Km2is5g==\",
	\"TimeStampToken\":\"MIIEljAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEewYJKoZIhvcNAQcCoIIEbDCCBGgCAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARAvp71IU4GqUJ/rIVKZ74J09qdSDeHw24HHsjw0tAnHjD6ZfUJHjDp8yQSdB6Lf2a6ORPF5JCgsh86CctQ9h93mwIBARgPMjAxNzA2MjkwOTM5MDdaMYIDzTCCA8kCAQEwfjB4MQswCQYDVQQGEwJmcjEMMAoGA1UECAwDaWRmMQ4wDAYDVQQHDAVwYXJpczEOMAwGA1UECgwFdml0YW0xFDASBgNVBAsMC2F1dGhvcml0aWVzMSUwIwYDVQQDDBxjYV9pbnRlcm1lZGlhdGVfdGltZXN0YW1waW5nAgIAtzANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzA2MjkwOTM5MDdaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQN8TGGTXtmpAztB16UGznFwW2xZMKRuX3zMnF9bTZFybM9tCGJtJd/IdBglcs69fsH05yuXOEYuwPhN1yQijSGEwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQGlkJQTJOiVJrGpFe2GsjJ0Ekug0n9Opel3//wOcpCmpqIET8w2yUcP1yqQJXYc87YeY1/OWhZiWFqbWXVV9HS4wDQYJKoZIhvcNAQENBQAEggIAV/rdnxIAyhvoGDprIahKAK3TPcriTggh1+gtDjEiD7kGB0KtXwAmPn2gb/2YtOmvIU7/a5KBFlfBR+foIRrc6z52cEdalhSpyHpYgpFuF7SjMFO6Mfso1dwjI9KpZTv6OI6Kplbg6zwK939GpDbPgKaMrXw0EDafk184RQz6NNFFYG8JuQxhlba1SYkEMg0+oOkcz814H1ET7zUbt2yq75zdffduUDB81dxjsvpKbx/LbBEOUswGgnfnYGOlo1XbQaI2sM2+YiXHGD/qnl/uAteBayFeaHKXel+gkp8D1ykBFOrE46n6fCI5i0OhKHcPAxvxTg8p03M38PrZIwnqSUI1rxfJhk9Hu0JVcQi1EYLBMmyL4IbhXNFz2ZmSHgC6/BGTMZmuEksrA4vJr1WEFMUocEFQnL9pOJ+iI8U0SusJEDYvjde+yvfnxC8ZOGXOsaP9aUsuITOMT/wFdrH4RFe8q8Wjxzu5p4lSvJI9P+soSfBbLyzGUjmF2lAi/HdyzjunhmRr/kxHK8P9Bo2CSz77xgN566k2r44ER/lyHFvHme5ITq25CRhJf39kfbPh1Jjku3ulwiquykhnjXX7YGx5bDRNv+z29l4tq+AkZqq8O+0XY5fLGgauptsKhpj+CsfYs0uNJCywZtIOHzdL0NBeF7AF97nwTV841ZN/rKg=\",
	\"NumberOfElement\":379,
	\"FileName\":\"0_LogbookOperation_20170629_093907.zip\",
	\"Size\":3975227,
	\"DigestAlgorithm\":\"SHA512\"}"

Dans le cas de l'événement final d'une opération de sécurisation du LogbookOperation, le champ **"evDetData"** est composé des champs suivants :

"LogType": type de logbook sécurisé.
      Collection faisant l'objet de l'opération de sécurisation (LogbookOperation)
      ``Exemple : "operation"``

"StartDate": Date de début de la période de couverture de l'opération de sécurisation.
      Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée par la précédente sécurisation)
      ``Exemple : "2016-08-17T08:26:04.227"``

"EndDate": date de fin de la période de couverture de l'opération de sécurisation.
      Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de la dernière opération sécurisée par la précédente sécurisation)
      ``Exemple : "2016-08-17T08:26:04.227"``

"PreviousLogbookTraceabilityDate": date de la précédente opération de sécurisation.
      Il s'agit  de la date de début de la précédente opération de sécurisation du même type au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] (correspond à la date de début de la sécurisation précédente)
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneMonthLogbookTraceabilityDate": date de l'opération de sécurisation passée d'un mois.
      Il s'agit  de la date de début de la précédente opération de sécurisation du même type réalisée un mois avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] 
      ``Exemple : "2016-08-17T08:26:04.227"``

"MinusOneYeargbookTraceabilityDate": date de l'opération de sécurisation passée d'un an.
     Il s'agit  de la date de début de la précédente opération de sécurisation du même type réalisée un an avant au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes] 
     ``Exemple : "2016-08-17T08:26:04.227"``

"Hash": Empreinte racine.
      Il s'agit d'une chaîne de caractères.
      Empreinte de la racine de l'arbre de Merkle.

"TimeStampToken": Tampon d’horodatage.
      Il s'agit d'une chaîne de caractères.
      Tampon d’horodatage sûr du journal sécurisé.

"NumberOfElement": Nombre d'éléments.
      Il s'agit d'un entier.
      Nombre d'opérations sécurisées.

"Size": Taille du fichier.
      Il s'agit d'un entier.
      Taille du fichier sécurisé (en bytes).

"FileName": Identifiant du fichier.
      Il s'agit d'une chaîne de caractères.
      Nom du fichier sécurisé dans le stockage au format {tenant}_LogbookOperation_{AAAAMMJJ_HHMMSS}.zip.
      ``Exemple : "0_LogbookOperation_20170127_141136.zip"``

"DigestAlgorithm": algorithme de hachage.
      Il s'agit d'une chaîne de caractères.
      Il s'agit du nom de l'algorithme de hachage utilisé pour réaliser le tampon d'horodatage.

Collection LogbookLifeCycleUnit
===============================

Utilisation de la collection LogbookLifeCycleUnit
-------------------------------------------------

Le journal du cycle de vie d'une unité archivistique (ArchiveUnit) trace tous les événements qui impactent celle-ci dès sa prise en charge dans le système. Il doit être conservé aussi longtemps que l'ArchiveUnit est gérée par le système.

- dès la réception d'une ArchiveUnit, on trace les opérations effectuées sur elles
- les journaux du cycle de vie sont "committés" une fois le stockage des objets effectué sans échec et l'indexation des métadonnées effectuée sans échec, avant notification au service versant

Chaque unité archivistique possède une et une seule entrée dans la collection LogbookLifeCycleUnit.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
-------------------------------------------------------------------

Extrait d'un JSON correspondant à un journal de cycle de vie d'une unité archivistique.

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
            "outcome": "STARTED",
            "outDetail": "LFC.CHECK_MANIFEST.STARTED",
            "outMessg": "Début de la vérification de la cohérence du bordereau",
            "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"worker\",\"ServerId\":1041627981,\"SiteId\":1,\"GlobalPlatformId\":236321613}",
            "obId": "aeaqaaaaaehbl62nabqkwak3k7qg5tiaaaaq",
            "evDetData": null,
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
        },{

        [...]
        
        }
    ],
    "_tenant": 1,
    "_v": 0
  }

Détail des champs du JSON stocké en base
-----------------------------------------

**"_id":** Identifiant donné par le système lors de l'initialisation du journal du cycle de vie.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID.
    * Cet identifiant constitue la clé primaire du journal du cycle de vie de l'unité archivistique. Il reprend la valeur du champ _id de la collection Unit.
    * Cardinalité : 1-1 
    *Ce champ existe uniquement pour la structure incluante.*

**"evId" (event Identifier):** identifiant de l'événement.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. 
    * Il identifie l'événement de manière unique dans la base.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"evParentId" (event Parent Identifier):** identifiant de l'événement parent.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. 
    * Il identifie l'événement parent.
    * Ce champ est toujours à null pour la structure incluante et les tâches principales
    * Cardinalité : 1-1 

    *Ce champ existe pour les structures incluantes et incluses*

**"evType" (event Type):** nom de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se fait via un fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"evDateTime" (event DateTime):** date de l'événement.

    * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``
    * Ce champ est positionné par le client LogBook.
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"evIdProc" (event Identifier Process):** identifiant du processus. 

    * Il s'agit d'une chaîne de 36 caractères.
    * Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id"
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"evTypeProc" (event Type Process):** type de processus.

    * Il s'agit d'une chaîne de caractères.
    * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outcome":** statut de l'événement.

    * Il s'agit d'une chaîne de caractères devant correspondre à une valeur de la liste suivante :

    - STARTED (début de l'événement)
    - OK (Succès de l'événement)
    - KO (Echec de l'événement)
    - WARNING (Succès de l'événement comportant des alertes)
    - FATAL (Erreur technique)

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outDetail" (outcome Detail):** code correspondant à l'erreur.

    * Il s'agit d'une chaîne de caractères.
    * Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction se fait via le fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outMessg" (outcomeDetailMessage):** détail de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    Traduction du code présent dans outDetail issue du fichier vitam-logbook-message-fr.properties.
    * Cardinalité : 1-1
    *Ce champ existe pour les structures incluantes et incluses*

**"agId" (agent Identifier):** identifiant de l'agent réalisant l'action.

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"obId" (object Identifier):** identifiant Vitam de l'objet ou du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"evDetData" (event Detail Data):** détails des données de l'événement.

    * Cardinalité : 1-1 
    * Donne plus de détail sur l'événement. Par exemple, l'historisation de métadonnées lors d'une modification se fait dans ce champ. Dans la structure incluse correspondant à cet événement, il contient un JSON composé du champ suivant :

    - diff: contient la différence entre les métadonnées d'origine et les métadonnées modifiées. Chaîne de caractères.

En outre, lors de l'historisation de la sauvegarde de l'unité archivistique sur les offres de stockage, on utilise ce champ pour tracer les informations sur le fichier sauvegardé. Il contient, ainsi, un JSON composé comme suit :

    - FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé dans le stockage.
    - Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
    - MessageDigest : Empreinte du fichier. Il s'agit d'une chaîne de caractères contenant l'empreinte du fichier.
    - Offers : Offres de srockage. Il s'agit des offres de stockage utilisées pour la sauvegarde du fichier.

    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"events":** tableau de structure

    * Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1 
    *Ce champ existe uniquement pour la structure incluante*

**"_tenant":** identifiant du tenant

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    *Ce champ existe uniquement pour la structure incluante.*

**"_v":** version de l'objet décrit

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    *Ce champ existe uniquement pour la structure incluante.*

Détail des champs du JSON stocké en base spécifiques à une mise à jour
-----------------------------------------------------------------------

Exemple de données stockées :

::

   "evDetData": "{\"diff\":\"-  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS)\\n+  Title : Recommandation de 2012 du CCSDS for Space Data System Practices - Reference Model for an Open Archival Information System (OAIS) 222\\n-  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq \\n+  #operations : [ aedqaaaaacaam7mxabxecakz3jbfwpaaaaaq, aecaaaaaacaam7mxabjssak2dzsjniyaaaaq \"}"


Dans le cas d'une mise à jour de métadonnées d'une unité archivistique (ArchiveUnit), le champ **"evDetData"** de l'événement final est composé des champs suivants :

"diff": historisation des modifications de métadonnées.
    Son contenu doit respecter la forme suivante : les anciennes valeurs sont précédées d'un "-" (``-champ1: valeur1``) et les nouvelles valeurs sont précédées d'un "+" (``+champ1: valeur2``)

    ``Exemple :
    -Titre: Discours du Roi \n+Titre: Discours du Roi Louis XVI \n-Description: Etat Généraux du 5 mai 1789 \n+Description: Etat Généraux du 5 mai 1789 au Château de Versailles``


Collection LogbookLifeCycleObjectGroup
======================================

Utilisation de la collection LogbookLifeCycleObjectGroup
---------------------------------------------------------

Le journal du cycle de vie du groupe d'objets (ObjectGroup) trace tous les événements qui impactent le groupe d'objets (et les objets associés) dès sa prise en charge dans le système. Il doit être conservé aussi longtemps que les objets sont gérés dans le système.

- dès la réception des objets, on trace les opérations effectuées sur les groupes d'objets et objets qui sont dans le SIP
- les journaux du cycle de vie sont "committés" une fois le stockage des objets effectué sans échec et l'indexation des métadonnées effectuée sans échec, avant notification au service versant

Chaque groupe d'objets possède une et une seule entrée dans sa collection LogbookLifeCycleObjectGroup.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
--------------------------------------------------------------------

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
            "agId": "{\"Name\":\"vitam-iaas-worker-01\",\"Role\":\"worker\",\"PlatformId\":425367}",
            "obId": "aeaaaaaaaaaam7mxaap44akyf7hurgaaaaba",
            "evDetData": null,
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
        },
        {
            
            [...]
            
        }
    ],
    "_tenant": 0,
    "_v": 0
    }


Détail des champs du JSON stocké en base
-----------------------------------------

**"_id":** Identifiant donné par le système lors de l'initialisation du journal du cycle de vie.

    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. Il reprend la valeur du champ _id de la collection ObjectGroup.
    * Cet identifiant constitue la clé primaire du journal du cycle de vie du groupe d'objet.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"evId" (event Identifier):** identifiant de l'événement.
    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID.
    * Il identifie l'événement de manière unique dans la base.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

    *Ce champ existe pour les structures incluantes et incluses*

**"evParentId" (event Parent Identifier):** identifiant de l'événement parent.
    * Il est constitué d'une chaîne de 36 caractères correspondant à un GUID. 
    * Il identifie l'événement parent.
    * Ce champ est toujours à null pour la structure incluante et les tâches principales
    * Cardinalité : 1-1 

    *Ce champ existe pour les structures incluantes et incluses*
    
**"evType"** (event Type): nom de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code doit être stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties).
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"evDateTime" (event DateTime):** date de l'événement.
    * Il s'agit d'une date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes]
    ``Exemple : "2016-08-17T08:26:04.227"``.
    * Ce champ est positionné par le client LogBook.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"evIdProc" (event Identifier Process):** identifiant du processus. 
    * Il s'agit d'une chaîne de 36 caractères.
    * Toutes les mêmes entrées du journal du cycle de vie partagent la même valeur, qui est celle du champ "_id".
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"evTypeProc" (event Type Process):** type de processus.

    * Il s'agit d'une chaîne de caractères.
    * Nom du processus qui effectue l'action, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"outcome":** statut de l'événement.

    * Il s'agit d'une chaîne de caractères devant correspondre une valeur de la liste suivante :

    - STARTED (Début de l'événement)
    - OK (Succès de l'événement)
    - KO (Echec de l'événement)
    - WARNING (Succès de l'événement comportant des alertes)
    - FATAL (Erreur technique)

    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"outDetail" (outcome Detail):** code correspondant à l'erreur

    * Il s'agit d'une chaîne de caractères.
    * Il contient le code fin de l'événement, incluant le statut. La liste des valeurs possibles pour ce champ se trouve en annexe. Seul le code est stocké dans ce champ, la traduction doit se faire via le fichier properties (vitam-logbook-message-fr.properties)
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"outMessg" (outcomeDetailMessage):** détail de l'événement.

    * Il s'agit d'une chaîne de caractères.
    * C'est un message intelligible destiné à être lu par un être humain en tant que détail de l'événement.
    * Traduction du code présent dans outDetail issue du fichier vitam-logbook-message-fr.properties du code présent dans outDetail.
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"agId" (agent Identifier):** identifiant de l'agent réalisant l'action.

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et le PID de l'agent. Ce champ est calculé par le journal à partir de ServerIdentifier.
    * ``Exemple : {\"name\":\"ingest-internal_1\",\"role\":\"ingest-internal\",\"pid\":425367}``
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"obId" (object Identifier):** identifiant Vitam du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).
    * Cardinalité : 1-1 
    *Ce champ existe pour les structures incluantes et incluses*

**"evDetData" (event Detail Data):** détails des données de l'événement.

    * Donne plus de détails sur l'événement.
    * Par exemple, pour l'événement LFC.CHECK_DIGEST, lorsque l'empreinte d'un objet inscrite dans le bordereau n'est pas calculée en SHA512, ce champ précise l'empreinte d'origine et celle réalisée ensuite par la solution logicielle Vitam. Dans la structure incluse correspondant à cet événement, il contient un JSON composé des champs suivants :

    - MessageDigest : empreinte de l'objet dans le bordereau. Chaîne de caractères. Reprends le champ "MessageDigest" du message ArchiveTransfer.
    - Algorithm : algorithme de hachage utilisé dans le bordereau. Chaîne de caractères. Reprends l'attribut de champ "MessageDigest" du message ArchiveTransfer.
    - SystemMessageDigest : empreinte de l'objet réalisé par la solution logicielle Vitam. Chaîne de caractères.
    - SystemAlgorithm : algorithme de hachage utilisé par la solution logicielle Vitam. Chaîne de caractères.

En outre, pour l'événement LFC.OBJ_STORAGE, on utilise ce champ pour tracer les informations sur l'objet (fichier binaire) sauvegardé. Il contient un JSON composé comme suit :

    - FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé dans le stockage.
    - Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
    - MessageDigest : Empreinte de l'objet. Il s'agit d'une chaîne de caractères contenant l'empreinte de l'objet.
    - Offers : Offres de srockage. Il s'agit des offres de stockage utilisées pour la sauvegarde de l'objet.

Pour l'événement LFC.OG_METADATA_STORAGE, on utilise ce champ pour tracer les informations sur le fichier (métadonnée) sauvegardé. Il contient un JSON composé comme suit :

    - FileName : Identifiant du fichier. Il s'agit du nom du fichier sauvegardé dans le stockage.
    - Algorithm : Algorithme de hachage. Il s'agit du nom de l'algorithme de hachage.
    - MessageDigest : Empreinte du fichier. Il s'agit d'une chaîne de caractères contenant l'empreinte du fichier.
    - Offers : Offres de srockage. Il s'agit des offres de stockage utilisées pour la sauvegarde du fichier.
    - 
    * Cardinalité : 1-1 
    * Ce champ existe pour les structures incluantes et incluses*

**"events":** tableau de structure.
    
    * Pour la structure incluante, le tableau contient N structures incluses dans l'ordre des événements (date)
    * Cardinalité : 1-1 
    *Ce champ existe uniquement pour la structure incluante.*

**"_tenant":** identifiant du tenant.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*

**"_v":** version de l'objet décrit.

    * Il s'agit d'un entier.
    * Cardinalité : 1-1 
    * Ce champ existe uniquement pour la structure incluante.*