Collection AccessionRegisterDetail
##################################

Utilisation de la collection AccessionRegisterDetail
====================================================

Cette collection a pour vocation de référencer l'ensemble des informations sur les opérations d'entrée réalisées pour un service producteur. A ce jour, il y a autant d'enregistrements que d'opérations d'entrées effectuées pour ce service producteur, mais des évolutions sont d'ores et déjà prévues. Cette collection reprend les élements du bordereau de transfert.

Exemple de la description dans le XML d'entrée
==============================================

Les seuls élements issus du message ArchiveTransfer utilisés ici sont ceux correspondant à la déclaration des identifiants du service producteur et du service versant. Ils sont placés dans le bloc <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": "aehaaaaaaaecjmacabcnualfddjzvpyaaaaq",
    "OriginatingAgency": "FRAN_NP_051314",
    "SubmissionAgency": "FRAN_NP_005761",
    "ArchivalAgreement": "ArchivalAgreement0",
    "EndDate": "2018-08-08T09:17:25.567",
    "StartDate": "2018-08-08T09:17:25.567",
    "LastUpdate": "2018-08-08T09:17:25.567",
    "Status": "STORED_AND_COMPLETED",
    "TotalObjectGroups": {
        "ingested": 2,
        "deleted": 0,
        "remained": 2
    },
    "TotalUnits": {
        "ingested": 3,
        "deleted": 0,
        "remained": 3
    },
    "TotalObjects": {
        "ingested": 2,
        "deleted": 0,
        "remained": 2
    },
        "ObjectSize": {
        "ingested": 12,
        "deleted": 0,
        "remained": 12
    },
    "Opc": "aeeaaaaaacecjmacabdbaalfddjuhciaaaaq",
    "Opi": "aeeaaaaaacecjmacabdbaalfddjuhciaaaaq",
    "Events": [
        {
            "Opc": "aeeaaaaaacecjmacabdbaalfddjuhciaaaaq",
            "OpType": "INGEST",
            "Gots": 2,
            "Units": 3,
            "Objects": 2,
            "ObjSize": 12,
            "CreationDate": "2018-08-08T11:17:25.582"
        }
    ],
    "OperationIds": [
        "aeeaaaaaacecjmacabdbaalfddjuhciaaaaq"
    ],
    "_v": 0,
    "_tenant": 0
  }

Détail des champs
=================

**"_id":** identifiant unique.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"OriginatingAgency":** identifiant du service producteur.

  * Il est issu du le bloc <OriginatinAgencyIdentifier>
  * Correspond au champ Identifier de la collection Agencies.

Par exemple :

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

On récupère la valeur FRAN_NP_051314

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"Opc":** identifiant d'une opération courante

    * Il s'agit soit de l'identifiant de l'opération ayant versé les archives recensées dans ce détail du registre des fonds, soit d'une opération ayant modifié le fonds d'une opération d'ingest opi donnée. Exemple: l'opération de l'élimination
    * Cardinalité 1-1

**"Opi":** : identifiant de l'opération d'entrée ayant versé les archives recensées dans ce détail du registre des fonds

    * Dans le cas de SIP faisant des rattachements (par exemple une nouvelle AU à une AU existante), il s'agira toujours de l'identifiant de l'opération de l'entrée en cours (celle générant ces documents Mongo)
    * Cardinalité 1-1

**"OpType":** : Le type de l'opération (INGEST, ELIMINATION, TRANSFER, ...)

    * Il s'agit du type d'opération qui a provoqué le changement du détail du registre des fonds
    * Cardinalité 1-1


**"SubmissionAgency":** contient l'identifiant du service versant.

    * Il est contenu entre les balises <SubmissionAgencyIdentifier>
    * Correspond au champ Identifier de la collection Agencies.

Par exemple

::

  <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>

On récupère la valeur FRAN_NP_005761.

  * Il s'agit d'une chaîne de caractère.
  * Cardinalité : 1-1

Ce champ est facultatif dans le bordereau. S'il' est absente ou vide, alors la valeur contenue dans le champ <OriginatingAgencyIdentifier> est reportée dans ce champ.

**"ArchivalAgreement":**

  * Contient le contrat utilisé pour réaliser l'entrée.
  * Il est contenu entre les balises <ArchivalAgreement>
  * Correspond à la valeur contenue dans le champ Identifier de la collection IngestContract.

Par exemple pour

::

  <ArchivalAgreement>IC-000001</ArchivalAgreement>

On récupère la valeur IC-000001.

  * Il s'agit d'une chaîne de caractère.
  * Cardinalité : 1-1

**"AcquisitionInformation":**

  * Contient les modalités d'entrée des archives
  * Il est contenu entre les balises <AcquisitionInformation>
  * Cardinalité : 1-1

**"LegalStatus":**

  * Contient le statut des archives échangés
  * Il est contenu entre les balises <LegalStatus>
  * Cardinalité : 1-1

**"EndDate":** date de la dernière opération d'entrée pour l'enregistrement concerné.

  * La date est au format ISO 8601

  ``"EndDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"StartDate":** date de la première opération d'entrée pour l'enregistrement concerné.

  * La date est au format ISO 8601

  ``"StartDate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Status":** Indication sur l'état des archives concernées par l'enregistrement.

  * Il s'agit d'une chaîne de caractères
  * Champ peuplé par Vitam.
  * Cardinalité : 1-1

**"LastUpdate":** Date de la dernière mise à jour pour l'enregistrement concerné.

  * La date est au format ISO 8601

  ``"LastUpdate": "2017-04-10T11:30:33.798"``

  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"TotalObjectGroups":** Contient la répartition du nombre de groupes d'objets du fonds par état pour l'opération journalisée (ingested, deleted et remained) :
    - "ingested": nombre de groupes d'objets pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": nombre de groupes d'objets supprimés ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": nombre de groupes d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"TotalUnits":** Il contient la répartition du nombre d'unités archivistiques du fonds par état pour l'opération journalisée :
    - "ingested": nombre d'unités archivistiques prises en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'unités archivistiques supprimées ou sorties du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": nombre d'unités archivistiques conservées dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"TotalObjects":** Contient la répartition du nombre d'objets du fonds par état pour l'opération journalisée :
    - "ingested": nombre  d'objets priss en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'objets supprimés ou sorties du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": nombre d'objets conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"ObjectSize":** Contient la répartition du volume total des fichiers du fonds par état pour l'opération journalisée (ingested, deleted etremained) :
    - "ingested": volume en octet des fichiers pris en charge dans le cadre de l'enregistrement concerné. La valeur contenue dans le champ est un entier.
    - "deleted": volume en octet des fichiers supprimés ou sortis du système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.
    - "remained": volume en octet des fichiers conservés dans le système pour l'enregistrement concerné. La valeur contenue dans ce champ est un entier.

  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"OperationIds":** opérations d'entrée concernées

  * Il s'agit d'un tableau.
  * Ne peut être vide
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-n

**"Events":** les détails des registres des fonds ayant modifié un lot d'ingest existant.

  * Le premier événement contient les remained de l'opération d'ingest.
  * Les événements suivants concernent les opérations ayant modifié un lot d'ingest existant (Elimination, Transfer, ...)
  * Cardinalité : 1-n

**"Events.Opc":** l'id de l'opération courante.

  * Dans le cas d'un ingest, opc égale à l'id de l'opération d'ingest.
  * Cardinalité : 1-1

**"Events.OpType":** Le type de l'opération (INGEST, ELIMINATION, TRANSFER, ...)

  * Cardinalité : 1-1

**"Events.Gots":**  Nombre total de groupe d'objets impactés par l'opération de l'événement

  * Cardinalité : 1-1

**"Events.Units":**  Nombre total d'unités archivistiques impactées par l'opération de l'événement

  * Cardinalité : 1-1

**"Events.Objects":** Nombre total d'objets impactés par l'opération de l'événement

  * Cardinalité : 1-1

**"Events.ObjSize":** Le poids total de tous les objets impactés par l'opération de l'événement.

  * Dans le cas d'un ingest, opc égale à l'id de l'opération d'ingest.
  * Cardinalité : 1-1

**"Events.CreationDate":** La date de l'évenement.

  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
  * Cardinalité : 1-1

**"_tenant":** correspondant à l'identifiant du tenant.

  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
