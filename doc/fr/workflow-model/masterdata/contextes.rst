Workflow d'import d'un référentiel des contextes
################################################

Introduction
============

Cette section décrit l'import d'un contexte

Processus d'import  et mise à jour d'un référentiel de services agents
======================================================================

Le processus d'import d'un contexte permet à la fois de vérifier qu'il contient les information minimales obligatoire, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisées au cours de ce processus sont éxécutés dans une seule étape.

Import d'un référentiel de services agents (STP_IMPORT_AGENCIES)
----------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Règle** :   vérification et enregistrement du contexte

  + **Statuts** :

    - OK : Les données suivantes sont obligatoirement remplies :

        * Le champ Name doit être peuplé avec une chaîne de caractères unique
        * Le champ Status doit être à true ou false
        * Le champ EnableControl doit être à true ou false
        * Le champ Permissions doit être peuplé avec un tableau conteanant des JSON
        * Le champ doit être peuplé avec une chaîne de caractères
        * Le champ Identifier doit être unique
    
    - KO : une des règles ci-dessus n'est pas respectée

    - FATAL : une erreur technique est survenue lors de l'import du contexte