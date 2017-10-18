Workflow d'import d'un référentiel des formats
##############################################

Introduction
============

Cette section décrit le processus d'import de référentiel des formats

Processus d'import d'un référentiel de formats (vision métier)
==============================================================

Le processus d'import de référentiel des formats vise à controler que les informations sont formalisée de la bonne manière dans le fichier soumis à la solution logicielle Vitam, et que chaque format contient bien le nombre d'informations minimales attendues

Import d'un référentiel de formats (STP_REFERENTIAL_FORMAT_IMPORT)
------------------------------------------------------------------

* Vérification du fichier de référentiel des formats

  + **Règle** : le fichier doit être au format xml et respecter le formalisme du référentiel PRONOM des archives britanniques

  + **Statuts** :

    - OK : les informations correspondant à chaque format sont décrites comme dans le référentiel PRONOM

    - KO : la condition ci-dessus n'est pas respectée

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_REFERENTIAL_FORMAT_IMPORT.FATAL=Erreur de l''import du référentiel de format)
