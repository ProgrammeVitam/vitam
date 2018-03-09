Workflow d'administration d'un référentiel des contextes
#########################################################

Introduction
============

Cette section décrit le processus (workflow) d'import des contextes dans le référentiel des contextes

Processus d'import  et mise à jour d'un référentiel des contextes applicatifs
=============================================================================

Le processus d'import d'un référentiel des contextes applicatifs permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel des contextes (STP_IMPORT_CONTEXT)
----------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** :   vérification et enregistrement du référentiel des contextes :

    * Le champ "Name" doit être peuplé avec une chaîne de caractères unique
    * Le champ "Status" doit être à "true" ou "false"
    * Le champ "EnableControl" doit être à "true" ou "false"
    * Le champ "Permissions" doit être peuplé avec un tableau contenant des fichiers JSON
    * Le champ "SecurityProfile" et "doit être peuplé avec une chaîne de caractères
    * Le champ "Identifier" doit être unique

  + **Statuts** :

    - OK : Les règles ci-dessus sont respectées (STP_IMPORT_CONTEXT.OK = Succès du processus d'import du contexte)

    - KO : une des règles ci-dessus n'est pas respectée (STP_IMPORT_CONTEXT.KO=Échec du processus d'import du contexte)

    - FATAL : une erreur technique est survenue lors de l'import du contexte (STP_IMPORT_CONTEXT.FATAL=Erreur fatale lors du processus d'import du contexte)

Mise à jour d'un contexte applicatif (STP_UPDATE_CONTEXT)
---------------------------------------------------------------------

La modification d'un profil de sécurité doit suivre les mêmes règles que celles décrites pour la création. La clé de l'événement est "STP_UPDATE_SECURITY_PROFILE", entraînant des statuts STP_UPDATE_CONTEXT.OK, STP_UPDATE_CONTEXT.KO et STP_UPDATE_CONTEXT.FATAL sur les mêmes contrôles que l'import.

Sauvegarde du JSON (STP_BACKUP_CONTEXT)
------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou en modification.

  + **Règle** : enregistrement d'une copie de la base de données des contextes applicatifs sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (STP_BACKUP_CONTEXT.OK = Succès du processus de sauvegarde des contextes)

      - KO : pas de cas KO

      - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_BACKUP_CONTEXT.FATAL = Erreur fatale lors du processus de sauvegarde des contextes)
