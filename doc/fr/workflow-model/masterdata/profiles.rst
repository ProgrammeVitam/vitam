Workflow d'admininstration d'un référentiel des profils
#######################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer et de mettre à jour un profil d'archivage.

Processus d'import et mise à jour d'un profil (vision métier)
==============================================================

Le processus d'import d'un profil d'archivage permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import des métadonnées d'une notice de profil d'archivage STP_IMPORT_PROFILE_JSON (ProfileServiceImpl.java)
------------------------------------------------------------------------------------------------------------


+ **Règle** : le profil d'archivage répond aux exigences suivantes :

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

    + Les données suivantes sont obligatoirement remplies :

      * Le champ "Name" est peuplé d'une chaîne de caractères
      * Le champs "Identifier" est peuplé d'une chaîne de caractère si le référentiel des profils d'archivage est configuré en mode esclave sur le tenant séléctionné
      * Le champ "Format" doit être renseigné avec la valeur RNG ou XSD

    + Les données suivantes optionnelles si elles sont remplies le sont en respectant les règles énoncées pour chacune :

      * Le champ "Description" est peuplé avec une chaîne de caractères
      * Le champ "Status" est peuplé avec la valeur ACTIVE ou la valeur INACTIVE

+ **Type** : bloquant

+ **Statuts** :

    - OK : les règles ci-dessus sont respectées (STP_IMPORT_PROFILE_JSON.OK=Succès du processus d'import du profil d'archivage)

    - KO :

	- Cas 1 : une des règles ci-dessus n'a pas été respectée (STP_IMPORT_PROFILE_JSON.KO = Échec du processus d'import du profil d'archivage)
	- Cas 2 : l'identifiant est déjà utilisé (STP_IMPORT_PROFILE_JSON.IDENTIFIER_DUPLICATION.KO = Échec de l'import du profil d'archivage : l'identifiant est déjà utilisé)
	- Cas 3 : au moins un des champs obligatoires n'est pas renseigné ( STP_IMPORT_PROFILE_JSON.EMPTY_REQUIRED_FIELD.KO = Échec de l'importdu profil d'archivage : au moins un des champs obligatoires n'est pas renseigné)   
	- Cas 4 : le profil d'archivage est inconnu (STP_IMPORT_PROFILE_JSON.PROFILE_NOT_FOUND.KO = Échec de l'import du profil d'archivage : profil d'archivage non trouvé)
   
 - WARNING : Avertissement lors du processus d'import du profil d'archivage ( STP_IMPORT_PROFILE_JSON.WARNING = Avertissement lors du processus d'import du profil)  

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_IMPORT_PROFILE_JSON.FATAL = Erreur technique lors du processus d'import du profil d'archivage)



Import du profil d'archivage STP_IMPORT_PROFILE_FILE (ProfileServiceImpl.java)
------------------------------------------------------------------------------


* Vérification de la concordance entre le fichier importé dans un profil et le format décrit dans la métadonnée "Format"

 

  + **Règle** : le format du fichier doit être celui décrit dans le profil

  + **Type** : bloquant

  + **Statuts** :

    - OK : le fichier importé est au même format que celui décrit dans le champ "Format" (STP_IMPORT_PROFILE_FILE.OK=Succès du processus d'import du profil d'archivage (fichier xsd ou rng)

    - KO : le fichier importé n'est pas au même format que celui décrit dans le champ "Format" (STP_IMPORT_PROFILE_FILE.KO=Échec du processus d'import du profil d'archivage (fichier xsd ou rng))

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_IMPORT_PROFILE_FILE.FATAL=Erreur technique lors du processus d'import du profil d'archivage (fichier xsd ou rng))

Processus de mise à jour d'un profil d'archivage STP_UPDATE_PROFILE_JSON (ProfileServiceImpl.java)
--------------------------------------------------------------------------------------------------


  + **Règle** : La modification d'un profil d'archivage doit suivre les mêmes règles que celles décrites pour la création. L'association d'un fichier de profil avec les métadonnées d'un profil provoque également une opération de mise à jour du profil d'archivage.

  + **Type** : bloquant

  + **Statuts** :

    - OK : le fichier importé est au même format que celui décrit dans le champ "Format" (STP_UPDATE_PROFILE_JSON.OK = Succès du processus du processus de mise à jour du profil d'archivage (fichier xsd ou rng)

    - KO : 

	- Cas 1 : le fichier importé n'est pas au même format que celui décrit dans le champ "Format" (STP_UPDATE_PROFILE_JSON.KO = Échec du processus de mise à jour du profil d'archivage (fichier xsd ou rng)

	- Cas 2 : la mise à jour du profil d'archivage n'a pas été effectuée (STP_UPDATE_PROFILE_JSON.PROFILE_NOT_FOUND.KO = Échec du processus de mise à jour du profil d'archivage : profil non trouvé)
	- Cas 3 : la mise à jour du profil d'archivage n'a pas été effectuée (STP_UPDATE_PROFILE_JSON.NOT_IN_ENUM.KO = Échec du processus de mise à jour du profil d'archivage : une valeur ne correspond pas aux valeurs attendues)
	- cas 4 : la mise à jour du profil d'archivage n'a pas été effectuée (STP_UPDATE_PROFILE_JSON.IDENTIFIER_DUPLICATION.KO = Échec du processus de mise à jour du profil d'archivage : l'identifiant est déjà utilisé


    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_UPDATE_PROFILE_JSON.FATAL = Erreur technique lors du processus de mise à jour  du profil d'archivage (fichier xsd ou rng))



Sauvegarde du JSON BACKUP_PROFILE (ProfileServiceImpl.java)
-----------------------------------------------------------

Cette tâche est appellée que ce soit en import initial ou lors de la modification des métadonnées de profils

  + **Règle** : enregistrement d'une copie de la base de données des métadonnées de profils sur le stockage

  + **Type** : bloquant

  + **Statuts** :

      - OK : une copie de la base de données nouvellement importée est enregistrée (BACKUP_PROFILE.OK = Succès du processus de sauvegarde des profils)

      - KO : pas de cas KO

      - FATAL : une erreur technique est survenue lors de la copie de la base de données nouvellement importée (BACKUP_PROFILE.FATAL = Erreur technique lors du processus de sauvegarde des profils)
