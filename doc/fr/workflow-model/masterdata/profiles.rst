Workflow d'import d'un référentiel des profils
##############################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un profil d'archivage.

Processus d'import  et mise à jour d'un profil (vision métier)
==============================================================

Le processus d'import d'un profil d'archivage permet à la fois de vérifier qu'il contient les informations minimales obligatoires, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import des métadonnées d'un profil d'archivage (STP_IMPORT_PROFILE_JSON)
-------------------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

  + **Type** : bloquant

  + **Règle** : le profil d'archivage répond aux exigences suivantes :

    + Les données suivantes sont obligatoirement remplies :

      * Le champ "Name" est peuplé d'une chaîne de caractères
      * Le champs "Identifier" est peuplé d'une chaîne de caractère si le référentiel des profils d'archivage est configuré en mode esclave sur le tenant séléctionné
      * Le champ "Format" doit être renseigné avec la valeur RNG ou XSD

    + Les données suivantes optionnelles si elles sont remplies le sont en respectant les règles énoncées pour chacune :

      * Le champ "Description" est peuplé avec une chaîne de caractères
      * Le champ "Status" est peuplé soit de la valeur ACTIVE ou INACTIVE

  + **Statuts** :

    - OK : les règles ci-dessus sont respectées (STP_IMPORT_PROFILE_JSON.OK=Succès du processus d'import du profil d'archivage)

    - KO : une des règles ci-dessus n'a pas été respectée (STP_IMPORT_PROFILE_JSON.KO=Échec du processus d'import du profil d'archivage)

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_IMPORT_PROFILE_JSON.FATAL=Erreur fatale lors du processus d'import du profil d'archivage)

Mise à jour d'un profil d'archivage (STP_UPDATE_PROFILE_JSON)
----------------------------------------------------------------------

La modification d'un profil d'archivage doit suivre les mêmes règles que celles décrites pour la création.

Import du profil d'archivage (STP_IMPORT_PROFILE_FILE)
-------------------------------------------------------

* Vérification de la concordance entre le fichier importé dans un profil et le format décrit dans la métadonnée "Format"

  + **Type** : bloquant

  + **Règle** : le format du fichier doit être celui décrit dans le profil

  + **Statuts** :

    - OK : le fichier importé est au même format que celui décrit dans le champ "Format" (STP_IMPORT_PROFILE_FILE.OK=Succès du processus d'import du profil d'archivage (fichier xsd ou rng)

    - KO : le fichier importé n'est pas au même format que celui décrit dans le champ "Format" (STP_IMPORT_PROFILE_FILE.KO=Échec du processus d'import du profil d'archivage (fichier xsd ou rng))

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_IMPORT_PROFILE_FILE.FATAL=Erreur fatale lors du processus d'import du profil d'archivage (fichier xsd ou rng))
