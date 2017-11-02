Workflow d'administration d'un référentiel des contextes
#########################################################

Introduction
============

Cette section décrit le processus (workflow) d'import des contextes dans le référentiel des contextes

Processus d'import  et mise à jour d'un référentiel des contextes
======================================================================

Le processus d'import d'un référentiel des contextes permet à la fois de vérifier qu'il contient les informations minimales obligatoire, de vérifier la cohérence de l'ensemble des information, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel des contextes (STP_IMPORT_CONTEXT)
----------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** :   vérification et enregistrement du référentiel des contextes :

    * Le champ "Name" doit être peuplé avec une chaîne de caractères unique
    * Le champ "Status" doit être à "true" ou "false"
    * Le champ "EnableControl" doit être à "true" ou "false"
    * Le champ "Permissions" doit être peuplé avec un tableau contenant des JSON
    * Le champ "SecurityProfile" et "doit être peuplé avec une chaîne de caractères
    * Le champ "Identifier" doit être unique

  + **Statuts** :

    - OK : Les règles ci-dessus sont respectées (STP_IMPORT_CONTEXT.OK) :

    - KO : une des règles ci-dessus n'est pas respectée (STP_IMPORT_CONTEXT.KO)

    - FATAL : une erreur technique est survenue lors de l'import du contexte (STP_IMPORT_CONTEXT.FATAL)
