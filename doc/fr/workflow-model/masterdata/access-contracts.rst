Workflow d'administration d'un référentiel des contrats d'accès
################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un contrat d'accès.

Processus d'import  et mise à jour d'un contrat d'accès (vision métier)
========================================================================

Le processus d'import d'un contrat d'accès permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un contrat d'accès (STP_IMPORT_ACCESS_CONTRACT)
----------------------------------------------------------

* Vérification de la présence des informations minimales obligatoires, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** : vérification et enregistrement du contrat

  + Les données suivantes sont obligatoirement remplies :

    * Le champ "Name" est peuplé d'une chaîne de caractères
    * Le champ "Identifier" est peuplé d'une chaîne de caractères si le référentiel des contrats d'accès est configuré en mode esclave sur le tenant séléctionné

  + Les données suivantes optionnelles, si elles sont remplies, le sont en respectant les règles énnoncées pour chacune :

    * Le champ "Description" est peuplé avec une chaîne de caractères
    * Le champ "Status" est peuplé soit de la valeur ACTIVE ou INACTIVE
    * Le champ "DataObjectVersion" est soit vide, soit peuplé avec un tableau d'une ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre à un des usages définis dans les groupe d'objets techniques  pris en charge dans la solution logicielle Vitam.
    * Le champ "OriginatingAgencies" est soit vide soit peuplé avec un tableau d'une ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre au champ "Identifier" d'un service agent contenu dans le référentiel des services agents.
    * Le champ "WritingPermission" doit être à "true" ou "false"
    * Le champ "EveryOriginatingAgency" doit être à "true" ou "false"
    * Le champ "EveryDataObjectVersion" doit être à "true" ou "false"
    * Le champ "RootUnit" est soit vide, soit peuplé avec un tableau d'une ou plusieurs chaînes de caractère. Chacune des chaînes de caractère doit correspondre au GUID d'une unité archivistique prise en charge dans la solution logicielle Vitam.


  + **Statuts** :

    - OK : le contrat répond aux exigences des règles (STP_IMPORT_ACCESS_CONTRACT.OK=Succès du processus d'import du contrat d'accès)

    - KO : une des règles ci-dessus n'a pas été respectée (STP_IMPORT_ACCESS_CONTRACT.KO=Échec du processus d'import du contrat d'accès)

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du contrat (STP_IMPORT_ACCESS_CONTRACT.FATAL=Erreur fatale lors du processus d'import du contrat d'accès)

Mise à jour d'un contrat d'accès (STP_UPDATE_ACCESS_CONTRACT)
---------------------------------------------------------------

La modification d'un contrat d'entrée doit suivre les mêmes règles que celles décrites pour la création.
