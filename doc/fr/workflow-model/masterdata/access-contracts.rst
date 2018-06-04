Workflow d'administration d'un référentiel des contrats d'accès
################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un contrat d'accès.

Processus d'import  et mise à jour d'un contrat d'accès (vision métier)
========================================================================

Le processus d'import d'un contrat d'accès permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations et de lui affecter des éléments peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un contrat d'accès (STP_IMPORT_ACCESS_CONTRACT)
----------------------------------------------------------

* Vérification de la présence des informations minimales obligatoires, de la cohérence des informations et affecter des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** : vérification et enregistrement du contrat

  + Les données suivantes sont obligatoirement remplies :

    * Le champ "Name" est peuplé d'une chaîne de caractères
    * Le champ "Identifier" est peuplé d'une chaîne de caractères si le référentiel des contrats d'accès est configuré en mode esclave sur le tenant sélectionné

  + Les données suivantes optionnelles, si elles sont remplies, le sont en respectant les règles énoncées pour chacune :

    * Le champ "Description" est peuplé avec une chaîne de caractères
    * Le champ "Status" est peuplé avec la valeur ACTIVE ou INACTIVE
    * Le champ "DataObjectVersion" est soit vide, soit peuplé avec un tableau d'une ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre à un des usages définis dans les groupe d'objets techniques  pris en charge dans la solution logicielle Vitam.
    * Le champ "OriginatingAgencies" est soit vide soit peuplé avec un tableau d'une ou plusieurs chaînes de caractères. Chacune de ces chaînes de caractères doit correspondre au champ "Identifier" d'un service agent contenu dans le référentiel des services agents.
    * Le champ "WritingPermission" doit être à "true" ou "false"
    * Le champ "EveryOriginatingAgency" doit être à "true" ou "false"
    * Le champ "EveryDataObjectVersion" doit être à "true" ou "false"
    * Le champ "RootUnit" est soit vide, soit peuplé avec un tableau d'une ou plusieurs chaînes de caractère. Chacune des chaînes de caractère doit correspondre au GUID d'une unité archivistique prise en charge dans la solution logicielle Vitam.


  + **Type** : bloquant

  + **Statuts** :

    - OK : le contrat répond aux exigences des règles (STP_IMPORT_ACCESS_CONTRACT.OK=Succès du processus d'import du contrat d'accès)

    - KO : une des règles ci-dessus n'a pas été respectée (STP_IMPORT_ACCESS_CONTRACT.KO=Échec du processus d'import du contrat d'accès)

    - FATAL : une erreur fatale est survenue lors de la vérification de l'import du contrat d'accès (STP_IMPORT_ACCESS_CONTRACT.FATAL=Erreur fatale lors du processus d'import du contrat d'accès)

    - WARNING: Avertissement lors du processus d'import du contrat d'accès ( STP_IMPORT_ACCESS_CONTRACT.WARNING=Avertissement lors du processus d'import du contrat d'accès )

    - DUPLICATION: l'identifiant du contrat est déjà utilisé ( STP_IMPORT_ACCESS_CONTRACT.IDENTIFIER_DUPLICATION.KO=Échec de l'import : l'identifiant est déjà utilisé )

    - EMPTY REQUIRED FIELD : au moins un des champs obligatoires n'est pas renseigné ( STP_IMPORT_ACCESS_CONTRACT.EMPTY_REQUIRED_FIELD.KO=Échec de l'import : au moins un des champs obligatoires n'est pas renseigné ) 

    - AGENCY NOT FOUND : Service producteur inconnu ( STP_IMPORT_ACCESS_CONTRACT.AGENCY_NOT_FOUND.KO=Échec de l'import : au moins un service producteur est inconnu ) 

    - VALIDATION ERROR : Erreur de validation du contrat ( STP_IMPORT_ACCESS_CONTRACT.VALIDATION_ERROR.KO=Échec de l'import : erreur de validation du contrat ) 



Mise à jour d'un contrat d'accès STP_UPDATE_ACCESS_CONTRACT (AdminExternalClientRest.java)
------------------------------------------------------------------------------------------

La modification d'un contrat d'accès doit suivre les mêmes règles que celles décrites pour la création.

    - OK : le contrat répond aux exigences des règles (STP_UPDATE_ACCESS_CONTRACT.OK=Succès du processus de mise à jour du contrat d'accès)

    - KO : une des règles ci-dessus n'a pas été respectée (STP_UPDATE_ACCESS_CONTRACT.KO=Échec du processus de mise à jour du contrat d'accès)

    - FATAL : une erreur fatale est survenue lors de la vérification de l'import du contrat d'accès (STP_UPDATE_ACCESS_CONTRACT.FATAL=Erreur fatale lors du processus de mise à jour du contrat d''accès)



Sauvegarde du JSON STP_BACKUP_INGEST_CONTRACT (IngestContractImpl.java)
-----------------------------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou en modification.

  + **Règle** : enregistrement d'une copie de la base de données des contrats d'accès sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (STP_BACKUP_ACCESS_CONTRACT.OK = Succès du processus de sauvegarde des contrats d'accès)

      - KO : pas de cas KO

      - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_BACKUP_ACCESS_CONTRACT.FATAL = Erreur fatale lors du processus de sauvegarde des contrats d'accès)
