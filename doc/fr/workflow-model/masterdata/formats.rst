Workflow d'import d'un référentiel des formats
##############################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un référentiel des formats

Processus d'import d'un référentiel de formats (vision métier)
==============================================================

Le processus d'import du référentiel des formats contrôle que les informations sont formalisées de la bonne manière dans le fichier soumis à la solution logicielle Vitam et que chaque format contient bien le nombre d'informations minimales attendues. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel de formats REFERENTIAL_FORMAT_IMPORT (ReferentialFormatFileImpl)
--------------------------------------------------------------------------------------------

* Vérification du fichier de référentiel des formats


  + **Règle** : le fichier doit être au format xml et respecter le formalisme du référentiel PRONOM publié par the National Archives (UK)

  + **Type** : bloquant

  + **Statuts** :

    - OK : les informations correspondant à chaque format sont décrites comme dans le référentiel PRONOM (STP_REFERENTIAL_FORMAT_IMPORT.OK = Succès du processus d'import du référentiel des formats)

    - KO : la condition ci-dessus n'est pas respectée (STP_REFERENTIAL_FORMAT_IMPORT.KO = Échec du processus d'import du référentiel des formats)

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_REFERENTIAL_FORMAT_IMPORT.FATAL = Erreur technique lors du processus d'import du référentiel des formats)


Processus de sauvegarde du référentiel des formats BACKUP_REFERENTIAL_FORMAT 
------------------------------------------------------------------------------


  + **Règle** : sauvegarde du référentiel des formats 
  
  + **Type** : bloquant

  + **Statuts** :

    - OK : la sauvegarde du référentiel des formats a bien été effectuée (STP_BACKUP_REFERENTIAL_FORMAT.OK = Succès du processus de sauvegarde du référentiel des formats)

    - KO : la sauvegarde du référentiel des formats n'a pas été effectuée (STP_BACKUP_REFERENTIAL_FORMAT.KO = Échec du processus de sauvegarde du référentiel des formats)

    - FATAL : une erreur technique est survenue lors de la sauvegarde du référentiel des formats (STP_BACKUP_REFERENTIAL_FORMAT.FATAL = Erreur technique lors du processus de sauvegarde du référentiel des formats)


