Workflow d'import d'un référentiel des documents types (Profil d'unité archivistique)
#####################################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un documents type.

Processus d'import et mise à jour d'un document type (Profil d'unité archivistique) (vision métier)
===================================================================================================

Le processus d'import d'un document type (profil d'unité archivistique) permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import des métadonnées d'un document type (profil d'unité archivistique) IMPORT_ARCHIVEUNITPROFILE (ArchiveUnitProfileServiceImpl.java) 
---------------------------------------------------------------------------------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

 
  + **Règle** : le document type répond aux exigences suivantes :

    + Les données suivantes sont obligatoirement remplies :

      * Le champ "Name" est peuplé d'une chaîne de caractères
      * Le champs "Identifier" est peuplé d'une chaîne de caractère si le référentiel des documents type est configuré en mode esclave sur le tenant séléctionné


    + Les données suivantes optionnelles si elles sont remplies le sont en respectant les règles énoncées pour chacune :

      * Le champ "Description" est peuplé avec une chaîne de caractères
      * Le champ "Status" est peuplé avec la valeur ACTIVE ou la valeur INACTIVE
      * Le champ "CreationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA
      * Le champ "ActivationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA
      * Le champ "DeactivationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA

  + **Type** : bloquant

  + **Statuts** :

    - OK : les règles ci-dessus sont respectées (IMPORT_ARCHIVEUNITPROFILE.OK=Succès du processus d'import du document type)

    - KO : une des règles ci-dessus n'a pas été respectée (IMPORT_ARCHIVEUNITPROFILE.KO=Échec du processus d'import du document type)

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du document type (IMPORT_ARCHIVEUNITPROFILE.FATAL=Erreur fatale lors du processus d'import du du document type)

    - STARTED : Début du processus d'import du document type ( IMPORT_ARCHIVEUNITPROFILE.STARTED=Début du processus d'import du document type ) 

    - WARNING : Avertissement lord du processus d'import du document type ( IMPORT_ARCHIVEUNITPROFILE.WARNING=Avertissement lors du processus d'import du document type )

    - IDENTIFIER DUPLICATION : L'identifiant est déjà utilisé ( IMPORT_ARCHIVEUNITPROFILE.IDENTIFIER_DUPLICATION.KO=Echec de l'import : l'identifiant est déjà utilisé ) 

    - EMPTY REQUIRED FIELD : Au moins un des champs obligatoires n'est pas renseigné ( IMPORT_ARCHIVEUNITPROFILE.EMPTY_REQUIRED_FIELD.KO=Echec de l'import : au moins un des champs obligatoires n'est pas renseigné ) 

    - INVALID JSON SCHEMA : Schéma JSON invalide ( IMPORT_ARCHIVEUNITPROFILE.INVALID_JSON_SCHEMA.KO=Echec de l'import : schéma JSON non valide) 


Mise à jour d'un document type (Profil d'unité archivistique) UPDATE_ARCHIVEUNITPROFILE (ArchiveUnitProfileManager.java)
------------------------------------------------------------------------------------------------------------------------


    - OK : les règles ci-dessus sont respectées (UPDATE_ARCHIVEUNITPROFILE.OK=Succès du processus de mise à jour du document type)
    - KO : une des règles ci-dessus n'a pas été respectée (UPDATE_ARCHIVEUNITPROFILE.KO=Échec du processus d'import du document type)
    - FATAL : une erreur technique est survenue lors de la vérification de l'import du document type (UPDATE_ARCHIVEUNITPROFILE.FATAL=Erreur fatale lors du processus de mise à jour du document type)


Sauvegarde du JSON BACKUP_ARCHIVEUNITPROFILE (ArchiveUnitProfileManager.java)
-----------------------------------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou lors de la modification des métadonnées de document type. 

  + **Règle** : enregistrement d'une copie de la base de données des métadonnées sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de donnée nouvellement importée est enregistrée (BACKUP_ARCHIVEUNITPROFILE.OK = Succès du processus de sauvegarde des document types)
      - KO : Echech du processus de sauvegarde du document type (BACKUP_ARCHIVEUNITPROFILE.KO = Echec du processus de sauvegarde des document types)



