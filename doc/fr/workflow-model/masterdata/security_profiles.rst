Workflow d'administration d'un référentiel des profils de sécurité
###################################################################

Introduction
============

Cette section décrit le processus (workflow) de création d'un profil de sécurité

Processus d'import  et mise à jour d'un profil de sécurité
==========================================================

Le processus d'import d'un profil de sécurité permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un profil de sécurité (STP_IMPORT_SECURITY_PROFILE)
-----------------------------------------------------------------

  + **Type** : bloquant

  + **Règle** : vérification et enregistrement du profil de sécurité. les données suivantes sont obligatoirement remplies :

    * Le champ "Name" doit être peuplé avec une chaîne de caractères unique
    * Le champ "Identifier" doit être unique
    * Le champ "FullAccess" doit être à "true" ou "false"


  + **Statuts** :

    - OK : les règles ci-dessus sont respectées :

    - KO : une des règles ci-dessus n'est pas respectée

    - FATAL : une erreur technique est survenue lors de l'import du profil de sécurité (STP_IMPORT_SECURITY_PROFILE.FATAL=Erreur fatale lors de l''import du profil de sécurité)

Mise à jour d'un profil de sécurité (STP_UPDATE_SECURITY_PROFILE)
---------------------------------------------------------------------

La mise à jour d'un profil de sécurité reprend les mêmes règles que l'import du profil de sécurité.
