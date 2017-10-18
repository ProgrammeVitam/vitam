Workflow d'import d'un référentiel des profils
##############################################

Introduction
============

Cette section décrit le processus de création d'un profil.

Processus d'import  et mise à jour d'un profil (vision métier)
==============================================================

Le processus d'import d'un profil permet à la fois de vérifier qu'il contient les information minimales obligatoire, de vérifier la cohérence de l'ensemble des informations, et de lui affecter des élements peuplés automatiquement.

Tous les élements réalisées au cours de ce processus sont éxécutés dans une seule étape.

Import des métadonnées d'un profil (STP_IMPORT_PROFILE_JSON)
------------------------------------------------------------

* Vérification de la présence des informations minimales, de la cohérence des informations et affectation des données aux champs peuplés par la solution logicielle Vitam.

  + **Règle** : vérification et enregistrement du profil

  + **Statuts** :

    - OK : Le profil répond aux exigences suivantes :

      + Les données suivantes sont obligatoirement remplies :

        * Le champ Name est peuplé d'une chaîne de caractères
        * Le champs Identifier est peuplé d'une chaîne de caractère si le référentiel des contrats d'entrée est configuré en mode esclave sur le tenant séléctionné
        * Le champ Format doit être renseigné avec la valeur RNG ou XSD

      + Les données suivantes optionnelles si elles sont remplies le sont en respectants les règles énnonées pour chacune :
  
        * Le champ Description est peuplé avec une chaîne de caractères
        * Le champ Status est peuplé soit de la valeur ACTIVE ou INACTIVE

    - KO : Une des règles ci-dessus n'a pas été respecté

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil (STP_IMPORT_PROFILE_JSON.FATAL=Erreur fatale lors de l'import du profil d'archivage)
  
Update d'un contrat d'entrée (STP_UPDATE_INGEST_CONTRACT)
----------------------------------------------------------

La modification d'un profil doit suivre les mêmes règles que celles décrites pour la création.

Import d'une description de profil d'archivage (STP_IMPORT_PROFILE_FILE)
------------------------------------------------------------------------

* Vérification de la concordance entre le fichier importé dans un profil et le format décrit dans la métadonnée Format
  
  + **Règle** : le format du fichier doit être celui décrit dans le profil

  + **Statuts** :
    
    - OK : le fichier importé est au même format que celui décrit dans le champ Format
      
    - KO : le fichier importé n'est pas au même format que celui décrit dans le champ Format

    - FATAL : une erreur technique est survenue lors de la vérification de l'import du profil d'archivage (STP_IMPORT_PROFILE_JSON.FATAL=Erreur fatale lors de l'import du profil d'archivage)