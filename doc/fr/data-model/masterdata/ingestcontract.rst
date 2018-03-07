Collection IngestContract
#########################

Utilisation de la collection
============================

La collection IngestContract permet de référencer et décrire unitairement les contrats d'entrée.

Exemple d'un fichier d'import de contrat
========================================

Les contrats d'entrée sont importés dans la solution logicielle Vitam sous la forme d'un fichier JSON.

::

    [
        {
            "Name":"Contrat Archives Départementales",
            "Description":"Test entrée - Contrat Archives Départementales",
            "Status" : "ACTIVE",
        },
        {
            "Name": "SIA archives nationales",
            "Description": "Contrat d'accès - SIA archives nationales",
            "Status" : "INACTIVE",
            "ArchiveProfiles": [
              "ArchiveProfile8"
            ],
            "LinkParentId" : "aeaqaaaaaagbcaacaax56ak35rpo6zqaaaaq"
        }
    ]

Les champs à renseigner obligatoirement à l'import d'un contrat sont :

* Name
* Description

Un fichier d'import peut décrire plusieurs contrats.

Exemple de JSON stocké en base comprenant l'exhaustivité des champs de la collection IngestContract
===================================================================================================

::

    {
      "_id": "aefqaaaaaahbl62nabkzgak3k6qtf3aaaaaq",
      "Name": "SIA archives nationales",
      "Identifier": "IC-000012",
      "Description": "Contrat d'accès - SIA archives nationales",
      "Status": "INACTIVE",
      "CreationDate": "2017-04-10T11:30:33.798",
      "LastUpdate": "2017-04-10T11:30:33.798",
      "ActivationDate": "2017-04-10T11:30:33.798",
      "DeactivationDate": null,
      "ArchiveProfiles": [
          "ArchiveProfile8"
      ],
      "LinkParentId":
        "aeaqaaaaaagbcaacaax56ak35rpo6zqaaaaq",
      "_tenant": 0,
      "_v": 0    }

Détail des champs de la collection IngestContract
=================================================

**"_id":** identifiant unique du contrat.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** Nom du contrat d'entrée.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":** Identifiant signifiant donné au contrat.
  
  * Il est constitué du préfixe "IC-" suivi d'une suite de 6 chiffres dans le cas ou la solution logicielle Vitam peuple l'identifiant. Par exemple : IC-007485. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du contrat.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du contrat d'entrée.
  
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Status":** statut du contrat.

  * Il s'agit d'une chaîne de caractères.
  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"CreationDate":** date de création du contrat.

  * La date est au format ISO 8601

  ``Exemple : "CreationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1

**"LastUpdate":** date de dernière mise à jour du contrat dans la collection IngestContract.

  * La date est au format ISO 8601

  ``Exemple : "LastUpdate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 1-1

**"ActivationDate":** date d'activation du contrat.

  * La date est au format ISO 8601

  ``Exemple : "ActivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1

**"DeactivationDate":** date de désactivation du contrat.

  * La date est au format ISO 8601

  ``Exemple : "DeactivationDate": "2017-04-10T11:30:33.798"``

  * Cardinalité : 0-1

**"ArchiveProfiles":** liste des profils d'archivage pouvant être utilisés par le contrat d'entrée.
  
  * Tableau de chaînes de caractères correspondant à la valeur du champ Identifier de la collection Profile.
  * Peut être vide
  * Cardinalité : 0-1

**"LinkParentId":** point de rattachement automatique des SIP en application de ce contrat correspondant à l'identifiant d’une unité archivistique d'un plan de classement ou d'un arbre de positionnement.
  
  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID et issue du champ _id d'un enregistrement de la collection Unit.
  * Cardinalité : 0-1

**L'unité archivistique concernée doit être de type FILING_UNIT ou HOLDING afin que l'opération aboutisse**

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1 

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.