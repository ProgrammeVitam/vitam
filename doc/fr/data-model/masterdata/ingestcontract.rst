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
* Identifier (selon la configuration du tenant : Identifier n'est obligatoire que si l'identifiant du contrat d'entrée n'est pas généré par la solution logicielle Vitam)

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
      "MasterMandatory":true,
      "EveryDataObjectVersion":false,
      "DataObjectVersion":"PhysicalMaster",
      "ArchiveProfiles": [
          "ArchiveProfile8"
      ],
      "CheckParentLink": "ACTIVE",
      "LinkParentId":
        "aeaqaaaaaagbcaacaax56ak35rpo6zqaaaaq",
      "FormatUnidentifiedAuthorized":true,
      "EveryFormatType":false,
      "FormatType":["fmt/17","fmt/12"],
      "_tenant": 0,
      "_v": 0    }

Détail des champs de la collection IngestContract
=================================================

**"_id":** identifiant unique du contrat.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"Name":** nom du contrat d'entrée.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Identifier":** identifiant signifiant donné au contrat.

  * Il est constitué du préfixe "IC-" suivi d'une suite de 6 chiffres dans le cas ou la solution logicielle Vitam peuple l'identifiant. Par exemple : IC-007485. Si le référentiel est en position esclave, cet identifiant peut être géré par l'application à l'origine du contrat.
  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 1-1

**"Description":** description du contrat d'entrée.

  * Il s'agit d'une chaîne de caractères.
  * Cardinalité : 0-1

**"Status":** statut du contrat.

  * Il s'agit d'une chaîne de caractères.
  * Peut être ACTIVE ou INACTIVE
  * Cardinalité : 1-1

**"CreationDate":** date de création du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

``Exemple : "CreationDate": "2017-04-10T11:30:33.798"``


**"LastUpdate":** date de dernière mise à jour du contrat dans la collection IngestContract.

  * La date est au format ISO 8601
  * Cardinalité : 1-1

``Exemple : "LastUpdate": "2017-04-10T11:30:33.798"``


**"ActivationDate":** date d'activation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 0-1

``Exemple : "ActivationDate": "2017-04-10T11:30:33.798"``


**DeactivationDate:** date de désactivation du contrat.

  * La date est au format ISO 8601
  * Cardinalité : 0-1

``Exemple : "DeactivationDate": "2017-04-10T11:30:33.798"``

**MasterMandatory:** option qui rend obligatoire la présence d'un objet dont l'usage est de type Master (Physical ou Binary)

  * True ou false
  * Dans le fichier JSON du contrat à importer, ce champ peut être absent. Dans ce cas, il sera enregistré avec la valeur true en base de données lors de l'import.
  * Cardinalité : 1-1

**EveryDataObjectVersion:** option qui permet de préciser que tous les types d'usages sont autorisés lors d'un versement d'un SIP procédant à des rattachement d'objets à des groupes d'objets techniques déjà existant.

  * Liste des valeurs autorisées : true, false
  * Si le champ est à false, alors le champ DataObjectVersion sera utilisé. S'il est à true, "DataObjectVersion" sera ignoré.
  * Dans le fichier JSON du contrat à importer, ce champ peut être absent. Dans ce cas, il sera enregistré avec la valeur "INACTIVE" en base de données lors de l'import.
  * Cardinalité : 1-1

**DataObjectVersion:** liste les types d'usages autorisés lors des versement de SIP procédant à des rattachements d'objets à des groupes d'objets techniques déjà existant. Les usages des objets rattachés n'étant pas dans cette liste provoqueront une entrée en KO des SIP.

  * Liste des valeurs autorisées : Dissemination, TextContent, PhysicalMaster, BinaryMaster, Thumbnail
  * Peut être vide. Si la variable EveryDataObjectVersion est à true, ce champ sera ignoré.
  * Cardinalité : 0-1

**"CheckParentLink":** option permettant d'activer un contrôle sur les noeuds de rattachements. Le noeud déclaré dans un SIP utilisant un contrat ayant cette variable à true doit impérativement être un fils du noeud déclaré dans ce paramètre.

  * Il s'agit d'une chaîne de caractères.
  * Liste des valeurs autorisées : "ACTIVE", "INACTIVE"
  * Dans le fichier JSON du contrat à importer, ce champ peut être absent. Dans ce cas, il sera enregistré avec la valeur false en base de données lors de l'import.
  * Cardinalité : 1-1

**"LinkParentId":** point de rattachement automatique des SIP en application de ce contrat correspondant à l'identifiant d’une unité archivistique d'un plan de classement ou d'un arbre de positionnement.

  * Il s'agit d'une chaîne de 36 caractères correspondant à un GUID et issue du champ _id d'un enregistrement de la collection Unit.
  * Cardinalité : 0-1

**L'unité archivistique concernée doit être de type FILING_UNIT ou HOLDING afin que l'opération aboutisse**


**FormatUnidentifiedAuthorized:** option autorisant ou non le versement d'objets n'étant pas identifiés par la solution logicielle Vitam

  * Liste des valeurs autorisées : true, false
  * Dans le fichier JSON du contrat à importer, ce champ peut être absent. Dans ce cas, il sera enregistré avec la valeur false en base de données lors de l'import.
  * Cardinalité : 1-1

**EveryFormatType:** option autorisant ou non le versement d'objets sans restriction de formats.

    * Liste des valeurs autorisées : true, false
    * Si ce champ est à false, alors le champ "FormatType" sera utilisé. Si il est à true, "FormatType" sera ignoré.
    * Dans le fichier JSON du contrat à importer, ce champ peut être absent. Dans ce cas, il sera enregistré avec la valeur false en base de données lors de l'import.
    * Cardinalité : 1-1

**FormatType:** liste de PUID de format autorisés lors du versement d'objet. Les objets n'étant pas dans cette liste de format provoqueront une entrée KO de leurs SIP

  * Liste des valeurs autorisées : true, false
  * Si la variable EveryFormatType est à true, ce champ sera ignoré
  * Cardinalité : 0-1

**"_tenant":** identifiant du tenant.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1

**"_v":** version de l'enregistrement décrit.

  * Il s'agit d'un entier.
  * Champ peuplé par la solution logicielle Vitam.
  * Cardinalité : 1-1
  * 0 correspond à l'enregistrement d'origine. Si le numéro est supérieur à 0, alors il s'agit du numéro de version de l'enregistrement.
