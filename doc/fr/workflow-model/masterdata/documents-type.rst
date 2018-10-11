Workflow d'administration du référentiel des profils d'unités archivistiques
#############################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer et de mettre à jour un profil d'unité archivistique (documents type).

Processus d'import et mise à jour d'un profil d'unité archivistique (document type) (vision métier)
===================================================================================================

Le processus d'import d'un profil d'unité archivistique  (document type) permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import des métadonnées d'un profil d'unité archivistique (document type) IMPORT_ARCHIVEUNITPROFILE (ArchiveUnitProfileServiceImpl.java)
----------------------------------------------------------------------------------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.


  + **Règle** : le profil d'unité archivistique (document type) répond aux exigences suivantes :

    + Les données suivantes sont obligatoirement remplies :

      * Le champ "Name" est peuplé d'une chaîne de caractères
      * Le champ "Identifier" est peuplé d'une chaîne de caractères si le référentiel des profils d'unité archivistique est configuré en mode esclave sur le tenant séléctionné


    + Les données suivantes optionnelles si elles sont remplies le sont en respectant les règles énoncées pour chacune :

      * Le champ "Description" est peuplé avec une chaîne de caractères
      * Le champ "Status" est peuplé avec la valeur ACTIVE ou la valeur INACTIVE
      * Le champ "CreationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA
      * Le champ "ActivationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA
      * Le champ "DeactivationDate" est peuplé avec une valeur correspondant à une date au format : JJ/MM/AA

  + **Type** : bloquant

  + **Statuts** :

    - OK : les règles ci-dessus sont respectées (IMPORT_ARCHIVEUNITPROFILE.OK = Succès du processus d'import du profil d'unité archivistique (document type)

    - KO : 

	- Cas 1 : une des règles ci-dessus n'a pas été respectée (IMPORT_ARCHIVEUNITPROFILE.KO = Échec du processus d'import du profil d'unité archivistique (document type)
	- Cas 2 : l'identifiant est déjà utilisé (IMPORT_ARCHIVEUNITPROFILE.IDENTIFIER_DUPLICATION.KO = Echec de l'import : l'identifiant est déjà utilisé) 
	- Cas 3 : au moins un des champs obligatoires n'est pas renseigné (IMPORT_ARCHIVEUNITPROFILE.EMPTY_REQUIRED_FIELD.KO = Echec de l'import : au moins un des champs obligatoires n'est pas renseigné)
	- Cas 4 : Schéma JSON invalide (IMPORT_ARCHIVEUNITPROFILE.INVALID_JSON_SCHEMA.KO=Echec de l'import : schéma JSON non valide)
	

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'unité archivistique  (document type) (IMPORT_ARCHIVEUNITPROFILE.FATAL = Erreur technique lors du processus d'import du profil d'unité archivistique (document type)



Mise à jour d'un profil d'unité archivistique (document type) UPDATE_ARCHIVEUNITPROFILE (ArchiveUnitProfileManager.java)
--------------------------------------------------------------------------------------------------------------------------

+ **Règle** : le profil d'unité archivistique (document type) répond aux exigences suivantes :

+ **Type** : bloquant

+ **Statuts** :

    - OK : les règles ci-dessus sont respectées (UPDATE_ARCHIVEUNITPROFILE.OK = Succès du processus de mise à jour du profil d'unité archivistique (document type)

    - KO : une des règles ci-dessus n'a pas été respectée (UPDATE_ARCHIVEUNITPROFILE.KO = Échec du processus d'import du profil d'unité archivistique (document type)

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'unité archivistique (document type) (UPDATE_ARCHIVEUNITPROFILE.FATAL = Erreur technique lors du processus de mise à jour du profil d'unité archivistique (document type)


Sauvegarde du JSON STP_BACKUP_ARCHIVEUNITPROFILE (ArchiveUnitProfileManager.java)
----------------------------------------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou lors de la modification des métadonnées de profil d'unité archivistique (document type). 

+ **Règle** : enregistrement d'une copie de la base de données des métadonnées sur le stockage

+ **Type** : bloquant

+ **Statuts** :

      - OK : une copie de la base de données nouvellement importée est enregistrée (BACKUP_ARCHIVEUNITPROFILE.OK = Succès du processus de sauvegarde des profils d'unité archivistique (document type)

      - KO : échec du processus de sauvegarde du profil d'unité archivistique (document type) (BACKUP_ARCHIVEUNITPROFILE.KO = Echec du processus de sauvegarde des profil d'unité archivistique (document type)



