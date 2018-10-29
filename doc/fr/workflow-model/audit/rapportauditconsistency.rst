Rapport d'audit de cohérence
############################

Le rapport d'audit de cohérence est un fichier JSON généré par la solution logicielle Vitam lorsqu'une opération d'audit se termine. En cas de succès (EVIDENCE_AUDIT.OK) ou en warning (EVIDENCE_AUDIT.WARNING) le rapport est vide. il n'y pas d'information spécifié. Dans les cas de KO le rapport retourne les informations suivantes.

Exemple de JSON : rapport d'audit de cohérence d'une unité archivistique
========================================================================

.. code::

    {
        "identifier": "aeaqaaaaaahjsaaiaabe6alggr7fv7aaaaba",
        "status": "KO",
        "message": "Traceability audit KO  Database check failure Errors are :  [ \" Metadata hash 'RHd+dg8mUQJ8kxURi+ENYmCHsb1n+IaN0VMHP061SdTghWw3t8CslyAfXXF80J70iI4xUt7apRSrF08JL8iClg==' mismatch secured lfc hash 'DlFXvtZV+mreWR/8I1B3Hq5CenDxHE37gRQZEpl+wNhPrVX9YAIbm4++kPNf6RrteWR8clxA9RbH8xj8ATq+HQ==' \" ]",
        "objectType": "UNIT",
        "securedHash": "e042f692e6a1b0af13e034db33265785a0825717843a30fec0c3fd864d294db9d58052e5ac45eafa9c165ecda07e11b017a325befca08858a9c20d534b0363b0",
        "offersHashes": {
            "offer-fs-1.service.int.consul": "e042f692e6a1b0af13e034db33265785a0825717843a30fec0c3fd864d294db9d58052e5ac45eafa9c165ecda07e11b017a325befca08858a9c20d534b0363b0",
            "offer-fs-2.service.int.consul": "e042f692e6a1b0af13e034db33265785a0825717843a30fec0c3fd864d294db9d58052e5ac45eafa9c165ecda07e11b017a325befca08858a9c20d534b0363b0"
    }
    }


Détails du rapport
===================

Chaque section du rapport correspond au résultat de l'audit de cohérence pour chaque objet ou groupe d'objets ou unité archivistique audité. On y trouve les informations suivantes : 


- "Identifier": Identifiant de l'objet ou groupe d'objets ou unité archivistique audité. 
- "status": "KO": le statut de l'opération (dans le cadre de cette release le statut sera systèmatiquement KO).
- "message": messsage qui signale une incohérence entre les signatures des fichiers sécurisés, des offres de stockage et de la base de données.
- "ObjectType": type de l'objet audité : objet ou groupe d'objets ou unité archivistique. 
- "securedHash": hash du journal sécurisé de l'unité archivistique, objet ou groupe d'objets.
- "offersHashes": signatures de l'élément audité de type unit ou GOT dans les offres de stockage.

