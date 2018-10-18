Workflow d'analyse de l'élimination des unités archivistiques
##############################################################

Introduction
============

Cette section décrit les processus en lien avec les phases d'analyses des unités archivistiques potentiellement éliminables et l'action d'élimination en masse d'unités archivistiques. 

Ces deux étapes ne sont pas liées, l'action d'élimination peut être exécutée directement depuis le panier. La phase d'analyse peut servir à déterminer une liste d'unités archivistiques potentiellement éliminables.


Processus de préparation de l'analyse de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_PREPARATION)
========================================================================================================================

+ **Règle** : Processus de préparation de l'analyse de l'élimination des unités archivistiques

* **Type** : bloquant

* **Statuts** :

  + OK : la préparation de l'analyse de l'unité archivistique a bien été effectuée (ELIMINATION_ANALYSIS_PREPARATION.OK = Succès de la préparation de l'analyse de l'élimination des unités archivistiques)

  + KO : la préparation de l'analyse de l'unité archivistique n'a pas été effectuée (ELIMINATION_ANALYSIS_PREPARATION.KO = Échec de la préparation de l'analyse de l'élimination des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la préparation de l'analyse de l'unité archivistique (ELIMINATION_ANALYSIS_PREPARATION.FATAL=Erreur technique lors de la préparation de l'analyse de l'élimination des unités archivistiques)



Vérification des seuils de l'élimination définitive des unités archivistiques ELIMINATION_ANALYSIS_CHECK_DISTRIBUTION_THRESHOLD
----------------------------------------------------------------------------------------------------------------------------------

+ **Règle** : Vérification des seuils de l'élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

  + OK : la préparation de l'analyse de l'unité archivistique a bien été effectuée (ELIMINATION_ANALYSIS_CHECK_DISTRIBUTION_THRESHOLD.OK = Succès de vérification des seuils de l'élimination définitive des unités archivistiques)

  + KO : la préparation de l'analyse de l'unité archivistique n'a pas été effectuée (ELIMINATION_ANALYSIS_CHECK_DISTRIBUTION_THRESHOLD.KO = Échec de vérification des seuils de l'élimination définitive des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la préparation de l'analyse de l'unité archivistique (ELIMINATION_ANALYSIS_CHECK_DISTRIBUTION_THRESHOLD.FATAL = Erreur technique lors de la vérification des seuils de l'élimination définitive des unités archivistiques)




Préparation de l'analyse de l'élimination des unités archivistiques ELIMINATION_ANALYSIS_PREPARATION
-----------------------------------------------------------------------------------------------------

+ **Règle** : Préparation de l'analyse de l'élimination des unités archivistiques

* **Type** : bloquant

* **Statuts** :

  + OK : la préparation de l'analyse de l'unité archivistique a bien été effectuée (ELIMINATION_ANALYSIS_PREPARATION.OK = Succès de la préparation de l'analyse de l'élimination des unités archivistiques)

  + KO : la préparation de l'analyse de l'unité archivistique n'a pas été effectuée (ELIMINATION_ANALYSIS_PREPARATION.KO = Échec de la préparation de l'analyse de l'élimination des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la préparation de l'analyse de l'unité archivistique (ELIMINATION_ANALYSIS_PREPARATION.FATAL = Erreur technique lors de la préparation de l'analyse de l'élimination des unités archivistiques)




Processus d'indexation de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_UNIT_INDEXATION)
=============================================================================================================

+ **Règle** : indexation des unités archivistiques qui répondent aux critères d'éliminabilité cités plus haut 

* **Type** : bloquant

* **Statuts** :

  + OK : l'indexation des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_UNIT_INDEXATION.OK = Succès de l'indexation de l'élimination des unités archivistiques)

  + KO : l'indexation des unités archivistiques n'a pas été effectuée (ELIMINATION_ANALYSIS_UNIT_INDEXATION.KO = Échec lors de l'indexation de l'élimination des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de l'indexation des unités archivistiques (ELIMINATION_ANALYSIS_UNIT_INDEXATION.FATAL = Erreur technique lors de l'indexation de l'élimination des unités archivistiques)



Processus de finalisation de l'analyse de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_FINALIZATION)
===========================================================================================================================


+ **Règle** : finalisation de l'analyse de l'élimination des unités archivistiques

* **Type** : bloquant

* **Statuts** :

  + OK : la finalisation de l'analyse des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_FINALIZATION.OK = Succès de la finalisation de l'analyse de l'élimination des unités archivistiques)

  + KO : la finalisation de l'analyse des unités archivistiques n'a pas été effectuée (ELIMINATION_ANALYSIS_FINALIZATION.KO=Échec lors de la finalisation de l'analyse de l'élimination des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de l'analyse de l'élimination des unités archivistiques (ELIMINATION_ANALYSIS_FINALIZATION.FATAL=Erreur technique lors de la finalisation de l'analyse de l'élimination des unités archivistiques)



Analyse des archives éliminables et indexation en base de données (ELIMINATION_ANALYSIS)
=========================================================================================


+ **Règle** : Le système va effectuer pour chaque unité archivistique une vérification des règles de gestion et des règles d'héritage dont elle dépend

* **Type** : bloquant

* **Statuts** :

  + OK : l'analyse de l'élimination des unités archivistiques des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS.OK = Succès de l'analyse de l'élimination des unités archivistiques)

  + KO : la finalisation de l'analyse des unités archivistiques n'a pas été effectuée (ELIMINATION_ANALYSIS.KO = Échec de l'analyse de l'élimination des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de l'analyse de l'élimination des unités archivistiques (ELIMINATION_ANALYSIS.FATAL = Erreur technique lors de l'analyse de l'élimination des unités archivistiques)


Structure de workflow d'analyse de l'élimination des unités archivistiques
============================================================================

.. image:: images/workflow_elimination_analysis.png
    :align: center


Workflow d'élimination définitive des unités archivistiques
##############################################################



Analyse des éliminables et action d'élimination (ELIMINATION_ACTION)
====================================================================

Le processus d'élimination comprend deux phases, une première d'analyse consistant à s'assurer que l'élimination des unités archivistiques ne produit pas de cas d'orphelinage. La seconde comprenant la phase d'action proprement dite. 


Processus de préparation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_PREPARATION)
====================================================================================================================

+ **Règle** : processus de préparation de l'élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de préparation de l'élimination définitive des unités archivistiques a bien été effectuée (STP_ELIMINATION_ACTION_PREPARATION.OK = Succès du début du processus de préparation de l'élimination définitive des unités archivistiques)

+ KO : Le processus de préparation de l'élimination définitive des unités archivistiques n'a pas été effectuée (STP_ELIMINATION_ACTION_PREPARATION.KO = Echec lors du début du processus de préparation de l'élimination définitive des unités archivistiques)

+ WARNING : Le processus de préparation de l'élimination définitive des unités archivistiques est en warning (STP_ELIMINATION_ACTION_PREPARATION.WARNING = Avertissement lors du début du processus de l'étape de préparation de l'élimination définitive des unités archivistiques)  

+ FATAL : Une erreur technique est survenue lors du début du processus de préparation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_PREPARATION.FATAL = Erreur technique lors du début du processus de préparation de l'élimination définitive des unités archivistiques)



Vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK)
-----------------------------------------------------------------------

+ **Règle** : la vérification des processus concurrents

* **Type** : bloquant

* **Statuts** :

+ OK : La vérification des processus concurrents a bien été effectuée (CHECK_CONCURRENT_WORKFLOW_LOCK.OK = Succès lors de la vérification des processus concurrents)

+ KO : La vérification des processus concurrents n'a pas été effectuée  (CHECK_CONCURRENT_WORKFLOW_LOCK.KO = Echec lors de la vérification des processus concurrents)

+ WARNING : La vérification des processus concurrents est en warning (CHECK_CONCURRENT_WORKFLOW_LOCK.WARNING = Avertissement lors de la vérification des processus concurrents)

+ FATAL : Une erreur technique est survenue lors de la vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK.FATAL = Erreur technique lors de la vérification des processus concurrents)



Vérification des seuils de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD)
-------------------------------------------------------------------------------------------------------------------------------


+ **Règle** : la vérification des seuils de traitement des unités archivistiques

* **Type** :  bloquant

* **Statuts** :

+ OK : La vérification des seuils de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.OK = Succès de vérification des seuils de l'élimination définitive des unités archivistiques)

+ KO : La vérification des seuils de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.KO = Echec de vérification des seuils de l'élimination définitive des unités archivistiques)

+ WARNING : La vérification des seuils de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.WARNING = Avertissement de vérification des seuils de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la vérification des seuils de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.FATAL = Erreur technique de vérification des seuils de l'élimination définitive des unités archivistiques



Préparation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_UNIT_PREPARATION)
---------------------------------------------------------------------------------------------------------



+ **Règle** : préparation de l'élimination définitive des unités archivistiques 
 
* **Type** : bloquant

* **Statuts** :

+ OK : La préparation de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_UNIT_PREPARATION.OK = Succès lors de la préparation de l'élimination définitive des unités archivistiques)

+ KO : La préparation de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_UNIT_PREPARATION.KO = Echec lors de la préparation de l'élimination définitive des unités archivistiques)

+ WARNING : La préparation de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_UNIT_PREPARATION.WARNING = Avertissement lors de la préparation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la préparation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_UNIT_PREPARATION.FATAL= Erreur technique lors de la préparation de l'élimination définitive des unités archivistiques)



Processus d'élimination définitive des unités archivistiques éliminables (STP_ELIMINATION_ACTION_DELETE_UNIT)
==============================================================================================================


+ **Règle** : processus d'élimination définitive des unités archivistiques éliminables 
 
* **Type** : bloquant

* **Statuts** :

+ OK : Le processus d'élimination définitive des unités archivistiques éliminables a bien été effectuée (STP_ELIMINATION_ACTION_DELETE_UNIT.OK = Succès du processus d'élimination définitive des unités archivistiques éliminables)

+ KO : Le processus d'élimination définitive des unités archivistiques éliminables n'a pas été effectuée (STP_ELIMINATION_ACTION_DELETE_UNIT.KO = Echec du processus d'élimination définitive des unités archivistiques éliminables)

+ WARNING : Le processus d'élimination définitive des unités archivistiques éliminables est en warning (STP_ELIMINATION_ACTION_DELETE_UNIT.WARNING = Avertissement lors du processus d'élimination définitive des unités archivistiques éliminables)

+ FATAL : Une erreur technique est survenue lors du processus d'élimination définitive des unités archivistiques éliminables (STP_ELIMINATION_ACTION_DELETE_UNIT.FATAL= Erreur technique lors du processus d'élimination définitive des unités archivistiques éliminables

Établissement de la liste des objets  OBJECTS_LIST_EMPTY
---------------------------------------------------------

+ **Règle** : établissement de la liste des objets
 
* **Type** : bloquant

* **Statuts** :

+ OK : l'établissement de la liste des objets a bien été effectuée (OBJECTS_LIST_EMPTY.OK = Succès de l'établissement de la liste des objets)

+ KO : l'établissement de la liste des objets n'a pas été effectuée (OBJECTS_LIST_EMPTY.KO = Échec de l'établissement de la liste des objets)

+ WARNING : Le processus d'établissement de la liste des objets est en warning (OBJECTS_LIST_EMPTY.WARNING = Avertissement lors de l'établissement de la liste des objets : il n'y a pas d'objet pour cette étape)

+ FATAL : Une erreur technique est survenue lors de l'établissement de la liste des objets (OBJECTS_LIST_EMPTY.FATAL = Erreur technique lors de l'établissement de la liste des objets)



Processus de préparation de l'élimination définitive des groupes d'objets techniques (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION)
=======================================================================================================================================


+ **Règle** : processus de préparation de l'élimination définitive des groupes d'objets techniques

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de préparation de l'élimination définitive des groupes d'objets techniques a bien été effectuée (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.OK = Succès du processus de préparation de l'élimination définitive des groupes d'objets techniques)

+ KO : Le processus de préparation de l'élimination définitive des groupes d'objets techniques n'a pas été effectuée (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.KO = Echec du processus de préparation de l'élimination définitive des groupes d'objets techniques)

+ WARNING : Le processus de préparation de l'élimination définitive des groupes d'objets techniques est en warning (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.WARNING = Avertissement lors du processus de préparation de l'élimination définitive des groupes d'objets techniques)

+ FATAL : Une erreur technique est survenue lors du processus de préparation de l'élimination définitive des groupes d'objets techniques (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.FATAL= Erreur technique lors du processus de préparation de l'élimination définitive des groupes d'objets techniques)



Préparation de l'élimination définitive des groupes d'objets techniques (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION)
---------------------------------------------------------------------------------------------------------------------

+ **Règle** : processus de préparation de l'élimination définitive des groupes d'objets techniques

* **Type** : bloquant

* **Statuts** :

+ OK : La préparation de l'élimination définitive des groupes d'objets techniques a bien été effectuée (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.OK = Succès de la préparation de l'élimination définitive des groupes d'objets techniques)

+ KO : La préparation de l'élimination définitive des groupes d'objets techniques n'a pas été effectuée (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.KO = Echec de la préparation de l'élimination définitive des groupes d'objets techniques)

+ WARNING : La préparation de l'élimination définitive des groupes d'objets techniques est en warning (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.WARNING = Avertissement lors de la préparation de l'élimination définitive des groupes d'objets techniques)

+ FATAL : Une erreur technique est survenue lors de la préparation de l'élimination définitive des groupes d'objets techniques (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.FATAL = Erreur technique lors de la préparation de l'élimination définitive des groupes d'objets techniques



Processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP)
=======================================================================================================================================================================


+ **Règle** : processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées a bien été effectuée (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.OK = Succès du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ KO : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées n'a pas été effectuée (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.KO = Echec du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ WARNING : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées est en warning (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.WARNING = Avertissement lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ FATAL : Une erreur technique est survenue lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.FATAL = Erreur technique lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)


Établissement de la liste des objets OBJECTS_LIST_EMPTY
---------------------------------------------------------

+ **Règle** : établissement de la liste des objets
 
* **Type** : bloquant

* **Statuts** :

+ OK : l'établissement de la liste des objets a bien été effectuée (OBJECTS_LIST_EMPTY.OK = Succès de l'établissement de la liste des objets)

+ KO : l'établissement de la liste des objets n'a pas été effectuée (OBJECTS_LIST_EMPTY.KO = Échec de l'établissement de la liste des objets)

+ WARNING : Le processus d'établissement de la liste des objets est en warning (OBJECTS_LIST_EMPTY.WARNING = Avertissement lors de l'établissement de la liste des objets : il n'y a pas d'objet pour cette étape)

+ FATAL : Une erreur technique est survenue lors de l'établissement de la liste des objets (OBJECTS_LIST_EMPTY.FATAL = Erreur technique lors de l'établissement de la liste des objets)


Processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP)
===================================================================================================================================================================


+ **Règle** : processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées a bien été effectuée (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.OK = Succès du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)

+ KO : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées n'a pas été effectuée (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.KO = Echec du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)

+ WARNING : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées est en warning (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.WARNING = Avertissement lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées) 

+ FATAL : Une erreur technique est survenue lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.FATAL = Erreur technique lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)


Établissement de la liste des objets  OBJECTS_LIST_EMPTY
---------------------------------------------------------

+ **Règle** : établissement de la liste des objets
 
* **Type** : bloquant

* **Statuts** :

+ OK : l'établissement de la liste des objets a bien été effectuée (OBJECTS_LIST_EMPTY.OK = Succès de l'établissement de la liste des objets)

+ KO : l'établissement de la liste des objets n'a pas été effectuée (OBJECTS_LIST_EMPTY.KO = Échec de l'établissement de la liste des objets)

+ WARNING : Le processus d'établissement de la liste des objets est en warning (OBJECTS_LIST_EMPTY.WARNING = Avertissement lors de l'établissement de la liste des objets : il n'y a pas d'objet pour cette étape)

+ FATAL : Une erreur technique est survenue lors de l'établissement de la liste des objets (OBJECTS_LIST_EMPTY.FATAL = Erreur technique lors de l'établissement de la liste des objets)



Processus de mise à jour du registre des fonds suite à l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_ACCESSION_REGISTER_PREPARATION)
===================================================================================================================================================================


+ **Règle** : mise à jour du registre des fonds suite à l'élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_ACCESSION_REGISTER_PREPARATION.OK = Succès de la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques)

+ KO :la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_ACCESSION_REGISTER_PREPARATION.KO = Echec de la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques)

+ WARNING :la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_ACCESSION_REGISTER_PREPARATION.WARNING = Avertissement lors de la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_ACCESSION_REGISTER_PREPARATION.FATAL = Erreur technique lors de la préparation du registre des fonds suite à l'élimination définitive des unités archivistiques)


Processus de mise à jour du registre des fonds suite à l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_ACCESSION_REGISTER_UPDATE)
=============================================================================================================================================================

+ **Règle** : mise à jour du registre des fonds suite à l'élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : La génération du rapport d'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.OK = Succès de la génération du rapport d'élimination définitive des unités archivistiques)

+ KO : La génération du rapport d'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.KO = Echec de la génération du rapport d'élimination définitive des unités archivistiques)

+ WARNING : La génération du rapport d'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_REPORT_GENERATION.WARNING = Avertissement lors de la génération du rapport d'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la génération du rapport d'élimination définitive des unités archivistiques (ELIMINATION_ACTION_REPORT_GENERATION.FATAL = Erreur technique lors de la génération du rapport d'élimination définitive des unités archivistiques)


Établissement de la liste des objets OBJECTS_LIST_EMPTY
---------------------------------------------------------

+ **Règle** : établissement de la liste des objets
 
* **Type** : bloquant

* **Statuts** :

+ OK : l'établissement de la liste des objets a bien été effectuée (OBJECTS_LIST_EMPTY.OK = Succès de l'établissement de la liste des objets)

+ KO : l'établissement de la liste des objets n'a pas été effectuée (OBJECTS_LIST_EMPTY.KO = Échec de l'établissement de la liste des objets)

+ WARNING : Le processus d'établissement de la liste des objets est en warning (OBJECTS_LIST_EMPTY.WARNING = Avertissement lors de l'établissement de la liste des objets : il n'y a pas d''objet pour cette étape)

+ FATAL : Une erreur technique est survenue lors de l'établissement de la liste des objets (OBJECTS_LIST_EMPTY.FATAL = Erreur technique lors de l''établissement de la liste des objets)


Processus de génération du rapport d'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_REPORT_GENERATION)
=================================================================================================================================


+ **Règle** : génération du rapport d'élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : La génération du rapport d'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.OK = Succès de la génération du rapport d'élimination définitive des unités archivistiques)

+ KO : La génération du rapport d'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.KO = Echec de la génération du rapport d'élimination définitive des unités archivistiques)

+ WARNING : La génération du rapport d'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_REPORT_GENERATION.WARNING = Avertissement lors de la génération du rapport d'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la génération du rapport d'élimination définitive des unités archivistiques (ELIMINATION_ACTION_REPORT_GENERATION.FATAL = Erreur technique lors de la génération du rapport d'élimination définitive des unités archivistiques)



Processus de finalisation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_FINALIZATION)
----------------------------------------------------------------------------------------------------------------------

+ **Règle** : finalisation de l'élimination définitive des unités archivistiques

* **Statuts** :

+ OK : Le processus de finalisation de l'élimination définitive des unités archivistiques a bien été effectuée (STP_ELIMINATION_ACTION_FINALIZATION.OK = Succès du processus de finalisation de l'élimination définitive des unités archivistiques) 

+ KO : Le processus de finalisation de l'élimination définitive des unités archivistiques n'a pas été effectuée (STP_ELIMINATION_ACTION_FINALIZATION.KO = Echec du processus de finalisation de l'élimination définitive des unités archivistiques)

+ WARNING : Le processus de finalisation de l'élimination définitive des unités archivistiques est en warning (STP_ELIMINATION_ACTION_FINALIZATION.WARNING = Avertissement lors du processus de finalisation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors du processus de finalisation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_FINALIZATION.FATAL = Erreur technique lors du processus de finalisation de l'élimination définitive des unités archivistiques)


Finalisation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_FINALIZATION)
----------------------------------------------------------------------------------------------------


+ **Règle** : élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : La finalisation de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_FINALIZATION.OK = Succès de la finalisation de l'élimination définitive des unités archivistiques)

+ KO : La finalisation de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_FINALIZATION.KO = Echec de la finalisation de l'élimination définitive des unités archivistiques)

+ WARNING : La finalisation de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_FINALIZATION.WARNING = Avertissement lors de la finalisation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la finalisation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_FINALIZATION.FATAL = Erreur technique lors de la finalisation de l'élimination définitive des unités archivistiques)


Elimination définitive des unités archivistiques (ELIMINATION_ACTION)
---------------------------------------------------------------------

+ **Règle** : élimination définitive des unités archivistiques

* **Type** : bloquant

* **Statuts** :

+ OK : L'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION.OK = Succès lors de l'élimination définitive des unités archivistiques) 

+ KO : L'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION.KO = Echec lors de l'élimination définitive des unités archivistiques) 

+ WARNING : L'élimination définitive des unités archivistiques est an warning (ELIMINATION_ACTION.WARNING = Avertissement lors de l'élimination définitive des unités archivistiques) 

+ FATAL : Une erreur technique est survenue lors de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION.FATAL = Erreur technique lors de l'élimination définitive des unités archivistiques) 

.. image:: images/workflow_elimination_action.png
    :align: center


Rapport élimination
###################

Le rapport d’élimination est un fichier JSON généré par la solution logicielle Vitam lorsqu’une opération d’élimination se termine. Cette section décrit la manière dont ce rapport est structuré.

Exemple de JSON : rapport d'élimination
=======================================

.. code-block:: json
  
  {
  "units": [
    {
      "id": "id_unit_1",
      "originatingAgency": "sp1",
      "opi": "opi1",
      "status": "DELETED",
      "objectGroupId": "id_got_1"
    },
    {
      "id": "id_unit_2",
      "originatingAgency": "sp2",
      "opi": "opi2",
      "status": "GLOBAL_STATUS_KEEP",
      "objectGroupId": "id_got_2"
    },
    {
      "id": "id_unit_3",
      "originatingAgency": "sp3",
      "opi": "opi3",
      "status": "NON_DESTROYABLE_HAS_CHILD_UNITS",
      "objectGroupId": "id_got_3"
    },
    {
      "id": "id_unit_4",
      "originatingAgency": "sp4",
      "opi": "opi4",
      "status": "GLOBAL_STATUS_KEEP",
      "objectGroupId": "id_got_2"
    },
      {
      "id": "id_unit_5",
      "originatingAgency": "sp5",
      "opi": "opi5",
      "status": "DELETED",
      "objectGroupId": "id_got_5"
    },
  ],
  "objectGroups": [
    {
      "id": "id_got_1",
      "originatingAgency": "sp1",
      "opi": "opi1",
      "objectIds": [
        "id_got_1_object_1",
        "id_got_1_object_2"
      ],
      "status": "DELETED"
    },
    {
      "id": "id_got_5",
      "originatingAgency": "sp5",
      "opi": "opi5",
      "status": "PARTIAL_DETACHMENT",
      "deletedParentUnitIds": [
        "id_unit_5"
      ]
    }
  ]
  }

"id" : "aeaqaaaabqhjsaaiabvjualghkg5n6aaaaca",
  "originatingAgency" : "RATP",
  "opi" : "aeeaaaaabshcalzeaami2alghkg45pyaaaaq",
  "objectGroupId" : "aebaaaaabqhjsaaiabvjualghkg5nvyaaaaq",
  "status" : "GLOBAL_STATUS_KEEP"


Détails du rapport
==================

Chaque section du rapport correspond aux résultats de l’élimination 
    "id": identifiant de l’objet ou groupe d’objets ou unité archivistique
    "originatingAgency" : service producteur
    "opi" : identifiant de l'opération d'élimination 
   
   Les statuts possibles pour les unités archivistiques :
   
      - GLOBAL_STATUS_KEEP : les unités archivistiques sont conservées.
           "objectGroupId": identifiant du groupe d'objet auxquel appartient l'unité archivistique éliminée
     
      - NOT_DESTROYABLE_HAS_CHILD_UNIT : les unités ne sont pas éliminables car elles disposent d'enfants et leur suppression entrainerait une incohérence dans le graph.

      - DELETED Les unités sans enfants ont pour statut d'élimination DELETED et sont supprimées 
           "objectIds": liste des objets éliminés  
  

   Les statuts possibles pour les GOT : 

      - DELETED : le Got a été éliminé

      - PARTIAL_DETACHEMENT : le GOT a été détaché des unités archivistiques concernées dans le cas d'un GOT partagé par deux unités archivistiques dont une seule est éliminée.  
