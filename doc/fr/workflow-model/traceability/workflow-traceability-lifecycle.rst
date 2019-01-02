Workflow de création de journal des cycles de vie sécurisé des groupes d'objets
###############################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant la sécurisation des journaux du cycle de vie des groupes d'objets mis en place dans la solution logicielle Vitam.
Le workflow mis en place dans la solution logicielle Vitam est défini dans le fichier "DefaultObjectGroupLifecycleTraceability.json".
Ce fichier est disponible dans : sources/processing/processing-management/src/main/resources/workflows.

 .. note:: Le traitement permettant la sécurisation des journaux du cycle de vie procède par des tranches de lots de 100K. La solution Vitam à la fin de ce premier lot enclenche un autre traitement de 100K et ce jusqu'à avoir traités l'ensemble des groupes d'objets.


Processus de sécurisation des journaux des cycles de vie des groupes d'objets (vision métier)
==============================================================================================

Le processus de sécurisation des journaux des cycles de vie consiste en la création d'un fichier .zip contenant l'ensemble des journaux du cycle de vie à sécuriser, ainsi que le tampon d'horodatage.

Ce fichier zip est ensuite enregistré sur les offres de stockage, en fonction de la stratégie de stockage.


Sécurisation des journaux du cycle de vie des groupes d'objets LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY (LogbookLFCAdministration.java)
====================================================================================================================================

+ **Règle** : sécurisation des journaux des cycles de vie des groupes d'objets

+ **Type** : bloquant

+ **Statuts** :

    - OK : les journaux du cycle de vie ont été sécurisés (LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY.OK = Succès de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - WARNING : il n'y pas de nouveaux journaux à sécuriser depuis la dernière sécurisation (LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY.WARNING = Avertissement lors de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la sécurisation du journal des opérations (LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY.FATAL = Erreur technique lors de la sécurisation des journaux du cycle de vie des groupes d'objets)

Processus de la sécurisation des journaux du cycle de vie des groupes d'objets STP_OG_LFC_TRACEABILITY
=======================================================================================================

Vérification des processus concurrents CHECK_CONCURRENT_WORKFLOW_LOCK
---------------------------------------------------------------------

  + **Règle** : le but est de vérifier s'il n'y a pas d'autres processus de traçabilité des journaux de cycle de vie des groupes d'objets concurrents.

  + **Type** : bloquant

  + **Statuts** :

    - OK : le contrôle de processus de traçabilité des journaux de cycle de vies des groupes d'objets concurrents s'est terminé avec succès (CHECK_CONCURRENT_WORKFLOW_LOCK.OK = Succès de la vérification des processus concurrents)

    - KO : des processus concurrents de traçabilité des groupes d'objets sont en cours d'exécution (CHECK_CONCURRENT_WORKFLOW_LOCK.KO = Échec lors de la vérification des processus concurrents)

    - FATAL : une erreur technique est survenue lors de la vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK.FATAL = Erreur technique lors de la vérification des processus concurrents)


Préparation de la liste des journaux du cycle de vie et des métadonnées des groupes d'objets PREPARE_OG_LFC_TRACEABILITY
-------------------------------------------------------------------------------------------------------------------------

  + **Règle** : récupération des journaux des cycles de vie à sécuriser et récupération des informations concernant les dernières opérations de sécurisation.

  + **Type** : bloquant

  + **Statuts** :

    - OK : les fichiers des journaux des cycles de vie ont été exportés (dans ObjectGroup) ainsi que les informations concernant les dernières opérations de sécurisation (PREPARE_OG_LFC_TRACEABILITY.OK = Succès de la préparation des journaux du cycle de vie et des métadonnées des groupes d'objets)

    - KO : les informations sur la dernière opération de sécurisation n'ont pas pu être obtenues / exportées, ou un problème a été rencontré avec un journal de cycle de vie (PREPARE_OG_LFC_TRACEABILITY.KO = Échec de la préparation des journaux du cycle de vie et des métadonnées des groupes d'objets)

    - FATAL : une erreur technique est survenue (PREPARE_LC_TRACEABILITY.FATAL= Erreur technique lors de la préparation des journaux du cycle de vie et des métadonnées des groupes d'objets)


Sécurisation des journaux du cycle de vie des groupes d'objets BUILD_OG_LFC_TRACEABILITY
-----------------------------------------------------------------------------------------

  + **Règle** : application de l'algorithme pour créer les fichiers sécurisés des journaux du cycle de vie des groupes d'objets, journal par journal, et génèration du fichier sécurisé.

  + **Type** : bloquant

  + **Statuts** :

    - OK : le fichier sécurisé pour le journal du cycle de vie en cours a été généré (BUILD_OG_LFC_TRACEABILITY.STARTED.OK = Succès de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - WARNING : il n'y a pas de nouveaux journaux à sécuriser (BUILD_OG_LFC_TRACEABILITY.WARNING = Avertissement lors de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - KO : le fichier pour le groupe d'objet n'a pas pu être trouvé (BUILD_OG_LFC_TRACEABILITY.KO = Échec de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - FATAL : une erreur technique est survenue lors de la génération des fichiers sécurisés (BUILD_OG_LFC_TRACEABILITY.FATAL = de la sécurisation des journaux du cycle de vie des groupes d'objets)


Finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets FINALIZE_OG_LFC_TRACEABILITY
---------------------------------------------------------------------------------------------------------------

  + **Règle** : récupération des différents fichiers générés puis calcul du tampon d'horodatage

  + **Type** : non applicable

  + **Statuts** :


    - OK : la finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets a bien été effectué (FINALIZE_OG_LFC_TRACEABILITY.OK = Succès de la finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - KO : la finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets n'a pas été effectué (FINALIZE_OG_LFC_TRACEABILITY.KO = Échec de la finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets)

    - FATAL : une erreur technique est survenue lors de la finalisation de la la sécurisation des journaux du cycle de vie des groupes d'objets (FINALIZE_OG_LFC_TRACEABILITY.FATAL = Erreur technique lors de la finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets)


Structure du workflow du processus de sécurisation des journaux des cycles de vie des groupes d'objets
=======================================================================================================

 .. figure:: images/workflow_lfc_og_traceability.png
    :align: center



Workflow de création de journal des cycles de vie sécurisé des unités archivistiques
####################################################################################

Introduction
============

Cette section décrit le processus (workflow) permettant la sécurisation des journaux du cycle de vie mis en place dans la solution logicielle Vitam des unités archivistiques.
Le workflow mis en place dans la solution logicielle Vitam est défini dans le fichier "DefaultUnitLifecycleTraceability.json".
Ce fichier est disponible dans : sources/processing/processing-management/src/main/resources/workflows.

.. note:: Le traitement permettant la sécurisation des journaux du cycle de vie procède par des tranches de lots de 100K. La solution Vitam à la fin de ce premier lot enclenche un autre traitement de 100K et ce jusqu'à avoir traités l'ensemble des unités archivistiques.


Processus de sécurisation des journaux des cycles de vie des unités archivistiques  (vision métier)
====================================================================================================

Le processus de sécurisation des journaux des cycles de vie consiste en la création d'un fichier .zip contenant l'ensemble des journaux du cycle de vie à sécuriser, ainsi que le tampon d'horodatage.

Ce fichier zip est ensuite enregistré sur les offres de stockage, en fonction de la stratégie de stockage.


Sécurisation des journaux du cycle de vie  des unités archivistiques LOGBOOK_UNIT_LFC_TRACEABILITY (LogbookLFCAdministration.java)
==================================================================================================================================

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

    - OK : les journaux du cycle de vie ont été sécurisés (LOGBOOK_UNIT_LFC_TRACEABILITY.OK = Succès de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - WARNING : il n'y pas de nouveaux journaux à sécuriser depuis la dernière sécurisation (LOGBOOK_UNIT_LFC_TRACEABILITY.WARNING = Avertissement lors de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la sécurisation du journal des opérations (LOGBOOK_UNIT_LFC_TRACEABILITY.FATAL = Erreur technique lors de la sécurisation des journaux du cycle de vie des unités archivistiques)

Processus de la sécurisation des journaux du cycle de vie des unités archivistiques STP_UNIT_LFC_TRACEABILITY
=============================================================================================================

Vérification des processus concurrents CHECK_CONCURRENT_WORKFLOW_LOCK
---------------------------------------------------------------------

  + **Règle** : le but est de vérifier s'il n'y a pas d'autres processus de traçabilité des journaux de cycle de vie des unités archivistiques concurrents.

  + **Type** : bloquant

  + **Statuts** :

    - OK : le contrôle de processus de traçabilité des journaux de cycle de vies des unités archivistiques concurrents s'est terminé avec succès (CHECK_CONCURRENT_WORKFLOW_LOCK.OK = Succès de la vérification des processus concurrents)

    - KO : des processus concurrents de traçabilité des unités archivistiques sont en cours d'exécution (CHECK_CONCURRENT_WORKFLOW_LOCK.KO = Échec lors de la vérification des processus concurrents)

    - FATAL : une erreur technique est survenue lors de la vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK.FATAL = Erreur technique lors de la vérification des processus concurrents)


Préparation de la liste des journaux du cycle de vie et des métadonnées des unités archivistiques PREPARE_UNIT_LFC_TRACEABILITY
--------------------------------------------------------------------------------------------------------------------------------

  + **Règle** : récupération des journaux des cycles de vie à sécuriser et récupération des informations concernant les dernières opérations de sécurisation.

  + **Type** : bloquant

  + **Statuts** :

    - OK : les fichiers des journaux des cycles de vie ont été exportés ainsi que les informations concernant les dernières opérations de sécurisation (PREPARE_UNIT_LFC_TRACEABILITY.OK = Succès de la préparation des journaux du cycle de vie et des métadonnées des unités archivistiques)

    - KO : les informations sur la dernière opération de sécurisation n'ont pas pu être obtenues / exportées, ou un problème a été rencontré avec un journal de cycle de vie (PREPARE_UNIT_LFC_TRACEABILITY.KO = Échec de la préparation des journaux du cycle de vie et des unités archivistiques)

    - FATAL : une erreur technique est survenue (PREPARE_UNIT_LFC_TRACEABILITY.FATAL = Erreur technique lors de la préparation des journaux du cycle de vie et des métadonnées des unités archivistiques)


Sécurisation des journaux du cycle de vie des groupes d'objets BUILD_UNIT_LFC_TRACEABILITY
-------------------------------------------------------------------------------------------

  + **Règle** : application de l'algorithme pour créer les fichiers sécurisés des journaux du cycle de vie des unités archivistiques, journal par journal, et génèration du fichier sécurisé.

  + **Type** : bloquant

  + **Statuts** :

    - OK : le fichier sécurisé pour le journal du cycle de vie en cours a été généré (BUILD_UNIT_LFC_TRACEABILITY.STARTED.OK = Succès de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - WARNING : il n'y a pas de nouveaux journaux à sécuriser (BUILD_UNIT_LFC_TRACEABILITY.WARNING = Avertissement lors de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - KO : le fichier pour le groupe d'objet n'a pas pu être trouvé (BUILD_UNIT_LFC_TRACEABILITY.KO = Échec de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - FATAL : une erreur technique est survenue lors de la génération des fichiers sécurisés (BUILD_UNIT_LFC_TRACEABILITY.FATAL = de la sécurisation des journaux du cycle de vie des unités archivistiques)


Finalisation de la sécurisation des journaux du cycle de vie des groupes d'objets FINALIZE_UNIT_LFC_TRACEABILITY
-----------------------------------------------------------------------------------------------------------------

  + **Règle** : récupération des différents fichiers générés puis calcul du tampon d'horodatage

  + **Type** : non applicable

  + **Statuts** :


    - OK : la finalisation de la sécurisation des journaux du cycle de vie des unités archivistiques a bien été effectué (FINALIZE_UNIT_LFC_TRACEABILITY.OK = Succès de la finalisation de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - KO : la finalisation de la sécurisation des journaux du cycle de vie des unités archivistiques n'a pas été effectué (FINALIZE_UNIT_LFC_TRACEABILITY.KO = Échec de la finalisation de la sécurisation des journaux du cycle de vie des unités archivistiques)

    - FATAL : une erreur technique est survenue lors de la finalisation de la la sécurisation des journaux du cycle de vie des unités archivistiques (FINALIZE_UNIT_LFC_TRACEABILITY.FATAL = Erreur technique lors de la finalisation de la sécurisation des journaux du cycle de vie des unités archivistiques)


Structure du workflow du processus de sécurisation des journaux des cycles de vie des unités archivistiques
============================================================================================================

  .. figure:: images/workflow_lfc_unit_traceability.png
    :align: center


    
