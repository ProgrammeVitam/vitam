Rapport d'audit
####################

Le rapport d'audit est un fichier JSON généré par la solution logicielle Vitam lorsqu'une opération d'audit se termine. Cette section décrit la manière dont ce rapport est structuré.

Exemple de JSON
=======================

.. code-block:: json

  {
     "tenant": 2,
     "auditOperationId": "aeeaaaaaakgtg6rzaahd4ak6od5brxaaaaaq",
     "auditType": "tenant",
     "objectId": "2",
     "DateTime": "2017-09-11T12:46:32.164",
     "Status": "KO",
     "outMessage": "Echec de l'audit",
     "LastEvent": "AUDIT_FILE_EXISTING",
     "source": [
        {
           "_tenant": "2",
           "agIdOrig": "FRAN_NP_009913",
           "evIdProc": "aedqaaaaakeuctkoabjgkak6lowhh6yaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "RATP",
           "evIdProc": "aedqaaaaakhu4m3aaaz2aak6loy4jxqaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "RATP",
           "evIdProc": "aedqaaaaakhu4m3aaaz2aak6lo2shsiaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "OBJ_KO",
           "evIdProc": "aedqaaaaakhfetkwabvlcak6lso7c7aaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "PROD_AUDIT_KO_2OBJ_1GO",
           "evIdProc": "aedqaaaaakhfetkwabvlcak6lsvp75aaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "PROD_OBJ_PHYSIQUE",
           "evIdProc": "aedqaaaaakfuavsrab2diak6mdzyw6aaaaaq"
        },
        {
           "_tenant": "2",
           "agIdOrig": "SP_SANS_OBJ",
           "evIdProc": "aedqaaaaakfuavsrab2diak6mdz7rraaaaaq"
        }
     ],
     "auditKO": [
        {
           "IdOp": "aeeaaaaaakgtg6rzaahd4ak6od5brxaaaaaq",
           "IdGOT": "aebaaaaaaifemvtkabtmsak6lso7pdiaaaaq",
           "IdObj": "aeaaaaaaaafemvtkabtmsak6lso7pcyaaaaq",
           "Usage": "BinaryMaster_1",
           "OriginatingAgency": "OBJ_KO",
           "OutDetail": "LFC.AUDIT_FILE_EXISTING.KO"
        },
        {
           "IdOp": "aeeaaaaaakgtg6rzaahd4ak6od5brxaaaaaq",
           "IdGOT": "aebaaaaaaifemvtkabtmsak6lsvqfkiaaaba",
           "IdObj": "aeaaaaaaaafemvtkabtmsak6lsvqflqaaaaq",
           "Usage": "TextContent_1",
           "OriginatingAgency": "PROD_AUDIT_KO_2OBJ_1GO",
           "OutDetail": "LFC.AUDIT_FILE_EXISTING.KO"
        },
        {
           "IdOp": "aeeaaaaaakgtg6rzaahd4ak6od5brxaaaaaq",
           "IdGOT": "aebaaaaaaifemvtkabtmsak6lsvqfjiaaaaq",
           "IdObj": "aeaaaaaaaafemvtkabtmsak6lsvqfjaaaaaq",
           "Usage": "BinaryMaster_1",
           "OriginatingAgency": "PROD_AUDIT_KO_2OBJ_1GO",
           "OutDetail": "LFC.AUDIT_FILE_EXISTING.KO"
        }
     ],
     "auditWarning": [
        "SP_SANS_OBJ"
     ]
   }



Partie "Master"
=================================================

La partie "master", c'est à dire le bloc à la racine du rapport (sans indentation) est composé des champs suivants :

- "tenant": le tenant sur lequel l'opération d'audit a été lancée
- "auditOperationId": l'identifiant de l'opération d'audit
- "auditType": l'élément sur lequel l'audit a été lancé. Celui ci peut être par "tenant", ou par "originatingagency"
- "objectId": l'identifiant de l'élément (tenant ou service producteur)
- "DateTime": la date du rapport
- "Status": la statut final du rapport, OK (l'audit n'a pas détecté d'anomalie), Warning (l'audit a détecté quelque chose de singulier qui n'a pas été considéré comme une anomalie), KO (l'audit a détecté une anomalie)
- "outMessage": le message final de l'audit, repris du journal des opérations
- "LastEvent": la clé correspondant au type d'audit. Par exemple pour l'audit de l'existence des fichiers il s'agit de "AUDIT_FILE_EXISTING"

Mais aussi :
- "source": la liste des opérations auditées
- "auditKO": la liste des anomalies détectées qui ont provoqué le KO de l'audit
- "auditWarning": la liste des éléments singuliers détectés qui ont provoqué un warning de l'audit

Liste des opérations auditées ("source")
=================================================

La liste des opérations auditées est une liste d'identifiant d'opérations d'ingest. Il s'agit des opérations à l'origine de la création des groupes d'objets qui ont été audités. Chaque groupe n'a par nature qu'une et une seule opération à l'origine de sa création. En partant de ces opérations, il est donc possible de retrouver l'ensemble des groupes d'objets qui ont été audités.

Au travers ces identifiants d'opérations, cette liste recense exhaustivement les groupes d'objets audités et ne présume en rien le succès ou l'échec de l'audit par rapport à ceux-ci.

Cette partie est construite autour des champs suivants :

- "_tenant": identifiant du tenant sur lequel l'opération s'est déroulée
- "agIdOrig": identifiant du service producteur relatif à cette opération
- "evIdProc": identifiant de l'opération étant à l'origine de la création du groupe d'objet audité

Liste des anomalies détectées générant un KO ("auditKO")
=================================================================================

Cette liste détaille l'ensemble des objets qui ont rencontré un KO lors de l'auditKO. Chaque objet possède son bloc, ayant les champs suivants :

- "IdOp": identifiant de l'opération étant à l'origine de la création du groupe d'objet auquel appartient l'objet KO audité
- "IdGOT": identifiant du groupe d'objet audité, possédant l'objet KO
- "IdObj": identifiant de l'objet KO
- "Usage": usage de l'objet KO dans son groupe d'objet
- "OriginatingAgency": service producteur de référence de l'objet
- "OutDetail": clé correspondant à l'audit qui a déclenché le KO, reprise du journal des opérations. Par exemple pour un audit de l'existence des fichiers, la clé est "LFC.AUDIT_FILE_EXISTING.KO"

Liste des éléments singuliers générant un avertissement ("auditWarning")
=================================================================================

Cette liste décrit les identifiant des services producteurs ayant généré un avertissement. Dans le cas de l'audit de l'existence des fichiers, une alerte correspond au fait qu'un service producteur n'a aucun objet à auditer. Cette liste est donc l'ensemble des services producteurs concernés par l'audit mais dont il n'existe aucun objet à auditer.
