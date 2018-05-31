Workflow d'import d'un référentiel des formats
##############################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un référentiel des formats

Processus d'import d'un référentiel de formats (vision métier)
==============================================================

Le processus d'import du référentiel des formats vise à contrôler que les informations sont formalisées de la bonne manière dans le fichier soumis à la solution logicielle Vitam et que chaque format contient bien le nombre d'informations minimales attendues. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Import d'un référentiel de formats STP_REFERENTIAL_FORMAT_IMPORT (ReferentialFormatFileImpl)
--------------------------------------------------------------------------------------------

* Vérification du fichier de référentiel des formats

  + **Type** : bloquant

  + **Règle** : le fichier doit être au format xml et respecter le formalisme du référentiel PRONOM publié par the National Archives (UK)

  + **Statuts** :

    - OK : les informations correspondant à chaque format sont décrites comme dans le référentiel PRONOM (STP_REFERENTIAL_FORMAT_IMPORT.OK=Succès du processus d'import du référentiel des formats)

    - KO : la condition ci-dessus n'est pas respectée (STP_REFERENTIAL_FORMAT_IMPORT.KO=Échec du processus d'import du référentiel des formats)

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_REFERENTIAL_FORMAT_IMPORT.FATAL=Erreur fatale lors du processus d'import du référentiel des formats)
