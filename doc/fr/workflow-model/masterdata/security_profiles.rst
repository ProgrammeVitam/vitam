Workflow d'administration d'un référentiel des profils de sécurité
###################################################################

Introduction
============

Cette section décrit le processus (workflow) de création d'un profil de sécurité

Processus d'import  et mise à jour d'un profil de sécurité
==========================================================

Le processus d'import d'un profil de sécurité permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un profil de sécurité (STP_IMPORT_SECURITY_PROFILE)
-----------------------------------------------------------------

  + **Type** : bloquant

  + **Règle** : vérification et enregistrement du profil de sécurité. les données suivantes sont obligatoirement remplies :

    * Le champ "Name" doit être peuplé avec une chaîne de caractères unique
    * Le champ "Identifier" doit être unique
    * Le champ "FullAccess" doit être à "true" ou "false"

  + **Statuts** :

    - OK : les règles ci-dessus sont respectées (STP_IMPORT_SECURITY_PROFILE.OK=Succès du processus d'import du profil de sécurité)

    - KO : une des règles ci-dessus n'est pas respectée (STP_IMPORT_SECURITY_PROFILE.KO=Échec du processus d'import du profil de sécurité)

    - FATAL : une erreur technique est survenue lors de l'import du profil de sécurité (STP_IMPORT_SECURITY_PROFILE.FATAL=Erreur fatale lors du processus d'import du profil de sécurité)

Mise à jour d'un profil de sécurité (STP_UPDATE_SECURITY_PROFILE)
---------------------------------------------------------------------

La modification d'un profil de sécurité doit suivre les mêmes règles que celles décrites pour la création. La clé de l'événement est "STP_UPDATE_SECURITY_PROFILE", entraînant des statuts STP_UPDATE_SECURITY_PROFILE.OK, STP_UPDATE_SECURITY_PROFILE.KO et STP_UPDATE_SECURITY_PROFILE.FATAL sur les mêmes contrôles que l'import.

Sauvegarde du JSON (STP_BACKUP_SECURITY_PROFILE)
------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou en modification.

  + **Règle** : enregistrement d'une copie de la base de données des profils de sécurité sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (STP_BACKUP_SECURITY_PROFILE.OK = Succès du processus de sauvegarde des profils de sécurité)

      - KO : pas de cas KO

      - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_BACKUP_SECURITY_PROFILE.FATAL = Erreur fatale lors du processus de sauvegarde des profils de sécurité)
