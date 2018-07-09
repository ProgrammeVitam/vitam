Collection LogbookOperation
###########################

Utilisation de la collection LogbookOperation
=============================================

La collection LogbookOperation comporte toutes les informations de traitement liées aux opérations effectuées dans la solution logicielle Vitam, chaque opération faisant l'objet d'un enregistrement distinct.

Ces opérations sont :

- Audit
- Export DIP
- Données de bases
- Entrée
- Mise à jour
- Sauvegarde des écritures
- Sécurisation
- Vérification

D'autres opérations types non implémentées sont à venir : élimination, préservation, reclassification...


Les valeurs correspondant à ces opérations dans les journaux sont détaillées dans l'annexe 6.3.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection LogbookOperation
=====================================================================================================

Extrait d'un JSON correspondant à une opération d'entrée terminée avec succès.

::

    {
       "_id": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "evId": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "evParentId": null,
       "evType": "PROCESS_SIP_UNITARY",
       "evDateTime": "2018-06-18T09:07:42.757",
       "evDetData": "{\n  \"EvDetailReq\" : \"SIP de test de recherche dans le titre et la description des units\",\n  \"EvDateTimeReq\" : \"2016-10-18T14:52:27\",\n  \"ArchivalAgreement\" : \"ArchivalAgreement0\",\n  \"ServiceLevel\" : null\n}",
       "evIdProc": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "evTypeProc": "INGEST",
       "outcome": "STARTED",
       "outDetail": "PROCESS_SIP_UNITARY.STARTED",
       "outMessg": "Début du processus d'entrée du SIP : aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "agId": "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",
       "agIdApp": "CT-000001",
       "agIdPers": null,
       "evIdAppSession": null,
       "evIdReq": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "agIdExt": "{\"originatingAgency\":\"FRAN_NP_009913\",\"TransferringAgency\":\"Identifier5\",\"ArchivalAgency\":\"Identifier4\"}",
       "rightsStatementIdentifier": "{\"ArchivalAgreement\":\"ArchivalAgreement0\"}",
       "obId": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
       "obIdReq": null,
       "obIdIn": "SIP de test de recherche dans le titre et la description des units",
       "events": [
           {
               "evId": "aedqaaaaachfbdnsab3bmalecitgejiaaaaq",
               "evParentId": null,
               "evType": "STP_SANITY_CHECK_SIP.STARTED",
               "evDateTime": "2018-06-18T09:07:42.757",
               "evDetData": null,
               "evIdProc": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "evTypeProc": "INGEST",
               "outcome": "OK",
               "outDetail": "STP_SANITY_CHECK_SIP.STARTED.OK",
               "outMessg": "Début du processus des contrôles préalables à l'entrée",
               "agId": "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",
               "agIdPers": null,
               "evIdReq": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "obId": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq"
           },
           {
               "evId": "aedqaaaaachfbdnsab3bmalecitge5iaaaaq",
               "evParentId": null,
               "evType": "STP_SANITY_CHECK_SIP",
               "evDateTime": "2018-06-18T09:07:42.879",
               "evDetData": null,
               "evIdProc": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "evTypeProc": "INGEST",
               "outcome": "OK",
               "outDetail": "STP_SANITY_CHECK_SIP.OK",
               "outMessg": "Succès du processus des contrôles préalables à l'entrée",
               "agId": "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",
               "agIdPers": null,
               "evIdReq": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "obId": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq"
           },
           {
               "evId": "aedqaaaaachfbdnsab3bmalecitge5iaaaba",
               "evParentId": "aedqaaaaachfbdnsab3bmalecitge5iaaaaq",
               "evType": "SANITY_CHECK_SIP",
               "evDateTime": "2018-06-18T09:07:42.879",
               "evDetData": null,
               "evIdProc": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "evTypeProc": "INGEST",
               "outcome": "OK",
               "outDetail": "SANITY_CHECK_SIP.OK",
               "outMessg": "Succès du contrôle sanitaire du SIP : aucun virus détecté",
               "agId": "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",
               "agIdPers": null,
               "evIdReq": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq",
               "obId": "aeeaaaaaachfbdnsab3bmalecitgbwqaaaaq"
           },
           {
           [...]
           }
       ],
       "_tenant": 0,
       "_v": 25,
       "_lastPersistedDate": "2018-06-18T09:08:46.344"
   }

Détail des champs du JSON stocké dans la collection
===================================================

Chaque enregistrement de cette collection est composé d'une structure auto-imbriquée : la structure possède une première instanciation "incluante" et contient un tableau de n structures identiques, dont seules les valeurs contenues dans les champs changent.

La structure est décrite ci-dessous.
Pour certains champs, on indiquera s’il s'agit de la structure incluante ou d'une structure incluse dans celle-ci.

**"_id" (identifier):** Identifiant unique donné par le système lors de l'initialisation de l'opération

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * La valeur de ce champ peut être ré-utilisé dans les champ evIdProc et evIdReq pour pouvoir suivre une succession d'opération déclenchée par une première opération (comme la mise à jour du référentiel des règles de gestion pouvant déclencher une mise à jour des unités archivistiques).
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
    * Il identifie l'événement parent. Par exemple pour le traitement CHECK_SEDA, il s'agit de l'identifiant de l'étape STP_INGEST_CONTROL_SIP.
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

    * EvDetailReq : précisions sur la demande de transfert. Chaîne de caractères. Reprend le champ "Comment" du message ArchiveTransfer.
    * EvDateTimeReq : date de la demande de transfert inscrit dans le champ evDetData. Date au format ISO8601 AAAA-MM-JJ+"T"+hh:mm:ss:[3digits de millisecondes].
    * ArchivalAgreement : identifiant du contrat d'entrée utilisé. Reprend le champ "ArchivalAgreement" du message ArchiveTransfer
    * ServiceLevel : niveau de service. Chaîne de caractères. Reprend le champ ServiceLevel du message ArchiveTransfer.
    * AcquisitionInformation : modalités d'entrée des archives. Chaîne de caractères. Reprend le champ AcquisitionInformation du message ArchiveTransfer. Cardinalité 0-1.
    * LegalStatus : statut des archives échangés. Chaîne de caractères. Reprend le champ LegalStatus du message ArchiveTransfer. Cardinalité 0-1.

  * Cardinalité pour les structures incluantes : 1-1
  * Cardinalité pour les structures incluses : 0-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evIdProc" (event Identifier Process):** identifiant du processus.

  * Il s'agit d'une chaîne de 36 caractères.
  * Toutes les mêmes entrées du journal des opérations partagent la même valeur, qui est celle du champ "_id"
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"evTypeProc" (event Type Process):** type de processus.

  * Il s'agit d'une chaîne de caractères.
  * Nom du processus, parmi une liste de processus possibles fixée. Cette liste est disponible en annexe 6.3.
  * Cardinalité : 1-1
  * Ce champ existe pour les structures incluantes et incluses.

**"outcome":** statut de l'événement.

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

    * Il s'agit de plusieurs chaînes de caractères indiquant le nom, le rôle et l'identifiant du serveur, du site et de la plateforme. Ce champ est calculé par le journal à partir de ServerIdentifier. ``Exemple : "{\"Name\":\"vitam-env-itrec-external-01.vitam-env\",\"Role\":\"ingest-external\",\"ServerId\":1045466546,\"SiteId\":1,\"GlobalPlatformId\":240160178}",``
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"agIdApp" (agent Identifier Application):** identifiant de l’application externe qui appelle la solution logicielle Vitam pour effectuer une opération. Cet identifiant est celui du contexte applicatif utilisé par l'application.

    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

**"agIdPers"**

**"evIdAppSession" (event Identifier Application Session):** identifiant de la transaction qui a entraîné le lancement d'une opération dans la solution logicielle Vitam.

    * L’application externe est responsable de la gestion de cet identifiant. Il correspond à un identifiant pour une session donnée côté application externe.
    * Il s'agit d'une chaîne de caractères.
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

**"evIdReq" (event Identifier Request):** identifiant de la requête déclenchant l’opération.

    * Il s'agit d'une chaîne de 36 caractères.
    * Une requestId est créée pour chaque nouvelle requête http venant de l’extérieur.
    * Dans le cas du processus d'entrée, il devrait s'agir du numéro de l'opération (EvIdProc).
    * Il s'agit du X-Application-Id.
    * Cardinalité : 1-1
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

	   * ArchivalAgreement: identifiant du contrat d'entrée utilisé pour réaliser l'entrée. Cardinalité 1-1.

	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchivalAgreement du message ArchiveTransfer.

	   * Profil: identifiant du profil utilisé pour réaliser l'entrée.

	    Il s'agit d'une chaîne de caractères.
	    Reprend le contenu du champ ArchiveProfile du message ArchiveTransfer. Cardinalité 0-1.

    * Pour une opération d'UPDATE, il comprend les champs suivant en JSON :

    	* AccessContract : identifiant du contrat d'accès utilisé pour réaliser une mise à jour.

    * Cardinalité : 1-1

**"obId" (object Identifier):** identifiant du lot d’objets auquel s’applique l’opération (lot correspondant à une liste).

    * Identifiant peuplé par la solution logicielle Vitam.
    * Il s'agit d'une chaîne de 36 caractères.
    * Dans le cas d’une opération d'entrée, il s’agit du GUID de l’entrée (evIdProc).
    * Dans le cas d’une opération d'audit, il s’agit par exemple du nom d’un lot d’archives prédéfini.
    * Dans le cas d’une opération de mise à jour, il s’agit du GUID de l'unité archivistique mise à jour.
    * Dans le cas d'une opération de données de base, il s'agit de l'identifiant de l'opération.
    * Cardinalité pour les structures incluantes : 1-1
    * Cardinalité pour les structures incluses : 0-1
    * Ce champ existe pour les structures incluantes et incluses.

**"obIdReq" (object Identifier Request):** identifiant de la requête caractérisant un lot d’objets auquel s’applique l’opération.

    * Identifiant peuplé par la solution logiciele Vitam.
    * Ne concerne que les lots d’objets dynamiques, c’est-à-dire obtenus par la présente requête. Ne concerne pas les lots ayant un identifiant défini.
    * Actuellement, la valeur est toujours 'null'.
    * Cardinalité : 1-1
    * Ce champ existe pour les structures incluantes et incluses.

**"obIdIn" (Object Identifier Income):** identifiant externe du lot d’objets auquel s’applique l’opération, utilisé pour les opérations d'entrées.

    * Chaîne de caractères intelligible pour un humain qui permet de comprendre à quel SIP ou quel lot d'archives se rapporte l'événement.
    * La structure incluante reprend le contenu du champ Comment, les structures incluses le contenu du MessageIdentifier du message ArchiveTransfer.
    * Cardinalité pour les structures incluantes : 1-1
    * Cardinalité pour les structures incluses : 0-1
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

    * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
    * Il s'agit d'un entier.
    * Cardinalité : 1-1
    * Ce champ existe uniquement pour la structure incluante.

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
      * evIdReq
      * obId

D'autres champs peuvent apparaître dans certains events lorsqu'ils mettent à jour le master.

Détail des champs du JSON stocké en base spécifiques à une opération de sécurisation des journaux d'opération et de cycle de vie
================================================================================================================================

Ceci ne concerne aujourd'hui que les sécurisations des journaux d'opération et la sécurisation des journaux de cycle de vie.

Exemple de données stockées par l'opération de sécurisation des journaux d'opération :

::

	"evDetData":
	{
	\"LogType\":\"OPERATION\",
	\"StartDate\":\"2017-06-29T09:22:23.227\",
	\"EndDate\":\"2017-06-29T09:39:08.690\",
	\"Hash\":\"HYnFf07gFkar3lO+U2FQ9qkhi9eUMFN5hcH7oU7vrAAL3FAlMm8aJP7+VxkVWhLzmmFolwUEcq6fbS7Km2is5g==\",
	\"TimeStampToken\":\"MIIEljAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEewYJKoZIhvcNAQcCoIIEbDCCBGgCAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARAvp71IU4GqUJ/rIVKZ74J09qdSDeHw24HHsjw0tAnHjD6ZfUJHjDp8yQSdB6Lf2a6ORPF5JCgsh86CctQ9h93mwIBARgPMjAxNzA2MjkwOTM5MDdaMYIDzTCCA8kCAQEwfjB4MQswCQYDVQQGEwJmcjEMMAoGA1UECAwDaWRmMQ4wDAYDVQQHDAVwYXJpczEOMAwGA1UECgwFdml0YW0xFDASBgNVBAsMC2F1dGhvcml0aWVzMSUwIwYDVQQDDBxjYV9pbnRlcm1lZGlhdGVfdGltZXN0YW1waW5nAgIAtzANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzA2MjkwOTM5MDdaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQN8TGGTXtmpAztB16UGznFwW2xZMKRuX3zMnF9bTZFybM9tCGJtJd/IdBglcs69fsH05yuXOEYuwPhN1yQijSGEwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQGlkJQTJOiVJrGpFe2GsjJ0Ekug0n9Opel3//wOcpCmpqIET8w2yUcP1yqQJXYc87YeY1/OWhZiWFqbWXVV9HS4wDQYJKoZIhvcNAQENBQAEggIAV/rdnxIAyhvoGDprIahKAK3TPcriTggh1+gtDjEiD7kGB0KtXwAmPn2gb/2YtOmvIU7/a5KBFlfBR+foIRrc6z52cEdalhSpyHpYgpFuF7SjMFO6Mfso1dwjI9KpZTv6OI6Kplbg6zwK939GpDbPgKaMrXw0EDafk184RQz6NNFFYG8JuQxhlba1SYkEMg0+oOkcz814H1ET7zUbt2yq75zdffduUDB81dxjsvpKbx/LbBEOUswGgnfnYGOlo1XbQaI2sM2+YiXHGD/qnl/uAteBayFeaHKXel+gkp8D1ykBFOrE46n6fCI5i0OhKHcPAxvxTg8p03M38PrZIwnqSUI1rxfJhk9Hu0JVcQi1EYLBMmyL4IbhXNFz2ZmSHgC6/BGTMZmuEksrA4vJr1WEFMUocEFQnL9pOJ+iI8U0SusJEDYvjde+yvfnxC8ZOGXOsaP9aUsuITOMT/wFdrH4RFe8q8Wjxzu5p4lSvJI9P+soSfBbLyzGUjmF2lAi/HdyzjunhmRr/kxHK8P9Bo2CSz77xgN566k2r44ER/lyHFvHme5ITq25CRhJf39kfbPh1Jjku3ulwiquykhnjXX7YGx5bDRNv+z29l4tq+AkZqq8O+0XY5fLGgauptsKhpj+CsfYs0uNJCywZtIOHzdL0NBeF7AF97nwTV841ZN/rKg=\",
	\"PreviousLogbookTraceabilityDate\":null,
	\"MinusOneMonthLogbookTraceabilityDate\":null,
	\"MinusOneYeargbookTraceabilityDate\":null,
	\"NumberOfElement\":379,
	\"FileName\":\"0_LogbookOperation_20170629_093907.zip\",
	\"Size\":3975227,
	\"DigestAlgorithm\":\"SHA512\"}"

Dans le cas de l'événement final d'une opération de sécurisation du LogbookOperation, le champ **"evDetData"** est composé des champs suivants :

**"LogType":** type de logbook sécurisé.

      * Collection faisant l'objet de l'opération de sécurisation

      ``Exemple : "operation"``

      * La valeur de ce champ est soit OPERATION soit LIFECYCLE.
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

**"Hash":** Empreinte racine.

      * Il s'agit d'une chaîne de caractères.
      * Empreinte de la racine de l'arbre de Merkle.
      * Cardinalité : 1-1

**"TimeStampToken":** Tampon d’horodatage.

      * Il s'agit d'une chaîne de caractères.
      * Tampon d’horodatage sûr du journal sécurisé.
      * Cardinalité : 1-1

**"NumberOfElement":** Nombre d'éléments.

      * Il s'agit d'un entier.
      * Nombre d'opérations sécurisées.
      * Cardinalité : 1-1

**"Size":** Taille du fichier.

      * Il s'agit d'un entier.
      * Taille du fichier sécurisé (en octets).
      * Cardinalité : 1-1

**"FileName":** Identifiant du fichier.

      * Il s'agit d'une chaîne de caractères.
      * Nom du fichier sécurisé sur les offres de stockage au format {tenant}_LogbookOperation_{AAAAMMJJ_HHMMSS}.zip.

      ``Exemple : "0_LogbookOperation_20170127_141136.zip"``

      * Cardinalité : 1-1

**"DigestAlgorithm":** algorithme de hachage.

      * Il s'agit d'une chaîne de caractères.
      * Il s'agit du nom de l'algorithme de hachage utilisé pour réaliser le tampon d'horodatage.
      * Cardinalité : 1-1
