Collection AccessionRegisterSummary
###################################

Utilisation de la collection
============================

Cette collection contient une vue macroscopique des fonds pris en charge dans la solution logicielle Vitam. Elle est constitué à partir des élements du bordereau de transfert.

Exemple de la description dans le bordereau de transfert
========================================================

Les seuls élements issus du  message ArchiveTransfer, utilisés ici sont ceux correspondants à la déclaration des identifiants du service producteur et du service versant. Ils sont placés dans le bloc <ManagementMetadata>

::

  <ManagementMetadata>
           <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>
           <SubmissionAgencyIdentifier>FRAN_NP_005761</SubmissionAgencyIdentifier>
  </ManagementMetadata>

Exemple de JSON stocké en base comprenant l'exhaustivité des champs
===================================================================

::

  {
    "_id": "aefaaaaaaaed4nrpaas4uak7cxykxiaaaaaq",
    "OriginatingAgency": "Vitam",
    "TotalObjects": {
        "ingested": 292,
        "deleted": 0,
        "remained": 292,
        "attached": 12,
        "detached": 0,
        "symbolicRemained": 12
    },
    "TotalObjectGroups": {
        "ingested": 138,
        "deleted": 0,
        "remained": 138,
        "attached": 14,
        "detached": 0,
        "symbolicRemained": 14
    },
    "TotalUnits": {
        "ingested": 201,
        "deleted": 0,
        "remained": 201,
        "attached": 37,
        "detached": 0,
        "symbolicRemained": 37
    },
    "ObjectSize": {
        "ingested": 35401855,
        "deleted": 0,
        "remained": 35401855,
        "attached": 917440,
        "detached": 0,
        "symbolicRemained": 917440
    },
      "CreationDate": "2017-04-12T17:01:11.764",
      "_v": 1,
      "_tenant": 0
  }

Détail des champs
=================

**"_id":** identifiant unique du fonds.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"OriginatingAgency":** identifiant d'un service producteur.

  * la valeur de ce champ est une chaîne de caractères.  
  * Ce champ est la clef primaire pour un enregistrement dans le registre des fonds et sert de concaténation pour toutes les entrées effectuées sur ce producteur d'archives. Récupère la valeur contenue dans le bloc <OriginatinAgencyIdentifier> du message ArchiveTransfer. Cette valeur doit également correspondre au champ Identifier de la collection Agencies.
  * Cardinalité : 1-1 

Par exemple pour

::

  <OriginatingAgencyIdentifier>FRAN_NP_051314</OriginatingAgencyIdentifier>

On récupère la valeur FRAN_NP_051314.

**"TotalObjects":** Contient la répartition du nombre d'objets du service producteur par état

    - "ingested": nombre total d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.
    - "attached": nombre total d'objets attachés symboliquement de ce service producteur. La valeur contenue dans le champ est un entier.
    - "detached": nombre d'objets détachés symboliquement de ce service producteur. La valeur contenue dans ce champ est un entier.
    - "symbolicRemained": nombre actualisé d'objets attachés symboliquement de ce service producteur et conservés dans la solution logicielle Vitam. La valeur contenue dans ce champ est un entier.
            
  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 

**"TotalObjectGroups":** Contient la répartition du nombre de groupes d'objets du service producteur par état

    - "ingested": nombre total de groupes d'objets pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre de groupes d'objets supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé de groupes d'objets conservés dans le système. La valeur contenue dans ce champ est un entier.
    - "attached": nombre de groupes d'objets attachés symboliquement de ce service producteur. La valeur contenue dans le champ est un entier.
    - "detached": nombre de groupes d'objets détachés symboliquement de ce service producteur. La valeur contenue dans ce champ est un entier.
    - "symbolicRemained": nombre actualisé de groupes d'objets rattachés symboliquement de ce service producteur et conservés dans la solution logicielle Vitam. La valeur contenue dans ce champ est un entier.
      
  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 

**"TotalUnits":** Contient la répartition du nombre d'unités archivistiques du service producteur par état

    - "ingested": nombre total d'unités archivistiques prises en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": nombre d'unités archivistiques supprimées ou sorties du système. La valeur contenue dans ce champ est un entier.
    - "remained": nombre actualisé d'unités archivistiques conservées. La valeur contenue dans ce champ est un entier.
    - "attached": nombre total d'unités archivistiques attachées symboliquement de ce service producteur. La valeur contenue dans le champ est un entier.
    - "detached": nombre d'unités archivistiques détachées symboliquement de ce service producteur. La valeur contenue dans ce champ est un entier.
    - "symbolicRemained": Nombre actualisé d'unités archivistiques attachés symboliquement de ce service producteur. La valeur contenue dans ce champ est un entier.
            
  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 
  
**"ObjectSize":** Contient la répartition du volume total des fichiers du service producteur par état

    - "ingested": volume total en octet des fichiers pris en charge dans le système pour ce service producteur. La valeur contenue dans le champ est un entier.
    - "deleted": volume total en octet des fichiers supprimés ou sortis du système. La valeur contenue dans ce champ est un entier.
    - "remained": volume actualisé en octet des fichiers conservés dans le système. La valeur contenue dans ce champ est un entier.
    - "attached": volume total en octet des fichiers attachés symboliquement de ce service producteur. La valeur contenue dans le champ est un entier.
    - "detached": volume total en octet des fichiers détachés symboliquement de ce service producteur. La valeur contenue dans ce champ est un entier.
    - "symbolicRemained": volume actualisé en octet des fichiers rattachés symboliquement de ce service producteur et conservés dans la solution logicielle Vitam. La valeur contenue dans ce champ est un entier.
            
  * Il s'agit d'un JSON
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 
    
**"CreationDate":**  Date d'inscription du service producteur concerné dans le registre des fonds. 

  * La date est au format ISO 8601

  ``"CreationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1
    
**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
  
**"_tenant":** correspondant à l'identifiant du tenant.
  
  * Il s'agit d'une chaîne de caractères.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 
