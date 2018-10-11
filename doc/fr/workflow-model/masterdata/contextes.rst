Workflow d'administration d'un référentiel des contextes applicatifs 
#####################################################################

Introduction
============

Cette section décrit le processus (workflow) d'import et de mise à jour des contextes dans le référentiel des contextes. Cette opération n'est réalisable que sur le tenant administration.

Processus d'import  et mise à jour d'un référentiel des contextes applicatifs
=============================================================================

Le processus d'import d'un référentiel des contextes applicatifs permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel des contextes STP_IMPORT_CONTEXT (ContextServiceImpl.java)
----------------------------------------------------------------------------------

Vérification de la présence des informations minimales, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.



+ **Règle** :   vérification et enregistrement du référentiel des contextes :

    * Le champ "Name" doit être peuplé avec une chaîne de caractères unique
    * Le champ "Status" doit être à "ACTIVE" ou "INACTIVE"
    * Le champ "EnableControl" doit être à "true" ou "false"
    * Le champ "Permissions" doit être peuplé avec un tableau contenant des fichiers JSON
    * Le champ "SecurityProfile" doit être peuplé avec une chaîne de caractères
    * Le champ "Identifier" doit être unique

+ **Type** : bloquant


+ **Statuts** :


    - OK : Les règles ci-dessus sont respectées (STP_IMPORT_CONTEXT.OK = Succès du processus d'import du contexte)

    - KO : 

	- Cas 1 : une des règles ci-dessus n'est pas respectée (STP_IMPORT_CONTEXT.KO = Échec du processus d'import du contexte)
	- Cas 2 : l'identifiant est déjà utilisé (STP_IMPORT_CONTEXT.IDENTIFIER_DUPLICATION.KO = Echec de l'import : l'identifiant est déjà utilisé) 
	- Cas 3 : un des champs obligatoires n'est pas renseigné (STP_IMPORT_CONTEXT.EMPTY_REQUIRED_FIELD.KO = Echec de l'import : au moins un des champs obligatoires n'est pas renseigné) 
	- Cas 4 : le profil de sécurité mentionné est inconnu du système (STP_IMPORT_CONTEXT.SECURITY_PROFILE_NOT_FOUND.KO = Echec de l'import : profil de sécurité non trouvé) 
	- Cas 5 : au moins un objet déclare une valeur inconnue (STP_IMPORT_CONTEXT.UNKNOWN_VALUE.KO = Echec de l'import : au moins un objet déclare une valeur inconnue ) 

    - FATAL : une erreur technique est survenue lors de l'import du contexte (STP_IMPORT_CONTEXT.FATAL=Erreur technique lors du processus d'import du contexte)

    

Mise à jour d'un contexte applicatif STP_UPDATE_CONTEXT (ContextServiceImpl.java)
---------------------------------------------------------------------------------

+ **Règle** : la modification d'un contexte applicatif doit suivre les mêmes règles que celles décrites pour la création. 

+ **Type** : bloquant

+ **Statuts** :

    - OK : Les règles ci-dessus sont respectées (STP_UPDATE_CONTEXT.OK = Succès du processus de mise à jour du contexte)

    - KO : une des règles ci-dessus n'est pas respectée (STP_UPDATE_CONTEXT.KO = Échec du processus mise à jour du contexte)

    - FATAL : une erreur technique est survenue lors de l'import du contexte (STP_UPDATE_CONTEXT.FATAL = Erreur technique lors du processus de mise à jour du contexte)



Sauvegarde du JSON STP_BACKUP_CONTEXT (ContextServiceImpl.java)
---------------------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou en modification.

  + **Règle** : enregistrement d'une copie de la base de données des contextes applicatifs sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de données nouvellement importée est enregistrée (STP_BACKUP_CONTEXT.OK = Succès du processus de sauvegarde des contextes)

      - KO : pas de cas KO

      - FATAL : une erreur technique est survenue lors de la copie de la base de données nouvellement importée (STP_BACKUP_CONTEXT.FATAL = Erreur technique lors du processus de sauvegarde des contextes)
