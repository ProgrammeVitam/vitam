workflow d'élimination
######################

Introduction
============

Cette section décrit les processus en lien avec les phases d'analyses des unités archivistiques potentiellement éliminables et l'action d'élimination en masse d'unités archivistiques. 

Ces deux étapes ne sont pas liées, l'action d'élimination peut être exécutée directement depuis le panier. La phase d'analyse peut servir à déterminer une liste d'unités archivistiques potentiellement éliminables.


Analyse de éliminables et indexation en base de données
=======================================================


Lors de cette étape, le système va effectuer pour chaque unité archivistique une vérification des règles de gestion et des règles d'héritage dont elle dépend.  


Les unités archivistiques éliminables apparaissent dans l'écran d'affichage des résultats d'élimination  :
 
 - Unités archivistiques ayant un statut "DESTROY" = les unités archivistiques qui ont une règle de gestion de type "Durée d'utilité administrative" arrivée à échéance et dont le sort final est "détruire"
 - Unités archivistiques ayant un statut "CONFLICTS" = 
				- KEEP_ACCESS_SP = un conflit existe entre services producteurs, le service producteur principal veut détruire l'archive, le secondaire la conserver.  
				- ACCESS_LINK_INCONSISTENCY = deux unités empruntent le même chemin, il n'est pas possible de couper le lien à un parent pour l'une est pas pour l'autre. 

Processus de préparation de l'analyse de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_ELIMINATION)
-----------------------------------------------------------------------------------------------------------------------


* **Type** : bloquant

* **Statuts** :

  + OK : la préparation de l'analyse de l'unité archivistique a bien été effectuée (ELIMINATION_ANALYSIS_PREPARATION.OK = la préparation de l'analyse de l'unité archivistique a bien été effectuée)

  + KO : la préparation de l'analyse de l'unité archivistique n'a pas été effectuée (ELIMINATION_ANALYSIS_PREPARATION.KO = Echec lors de la préparation de l'analyse de l'unité archivistique)

  + FATAL : une erreur technique est survenue lors de la préparation de l'analyse de l'unité archivistique (ELIMINATION_ANALYSIS_PREPARATION.FATAL = Erreur technique lors de la préparation de l'analyse )


Processus d'indexation de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_INDEXATION)
-------------------------------------------------------------------------------------------------------

Cette étape à pour but d'indexer les unités archivistiques qui répondent aux critères d'éliminabilité cités plus haut. 

* **Type** : bloquant

* **Statuts** :

  + OK : l'indexation des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_INDEXATION.OK = l'indexation des unités archivistiques a bien été effectuée)

  + KO : Erreur lors de l'indexation des unités archivistiques (ELIMINATION_ANALYSIS_INDEXATION.KO = erreur lors de l'indexation des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de l'indexation des unités archivistiques (ELIMINATION_ANALYSIS_INDEXATION.FATAL = erreur fatale lors de la préparation de l'analyse de l'unité archivistique)


Processus de finalisation de l'analyse de l'élimination des unités archivistiques (STP_ELIMINATION_ANALYSIS_FINALIZATION)
-------------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

  + OK : la finalisation de l'analyse des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_FINALIZATION.OK = la finalisation des unités archivistiques a bien été effectuée)

  + KO : Erreur lors de la finalisation de l'analyse des unités archivistiques (ELIMINATION_ANALYSIS_FINALIZATION.KO = erreur lors de la finalisation de l'analyse des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de l'analyse des unités archivistiques (ELIMINATION_ANALYSIS_FINALIZATION.FATAL = erreur fatale lors de l'analyse de l'unité archivistique)


Analyse des éliminables et action d'élimination (ELIMINATION_ACTION)
====================================================================

Le processus d'élimination comprend deux phases, une première d'analyse consistant à s'assurer que l'élimination des unités archivistiques ne produit pas de cas d'orphelinage. La seconde comprenant la phase d'action proprement dite. 


Processus de préparation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_PREPARATION)
-------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de préparation de l'élimination définitive des unités archivistiques a bien été effectuée (STP_ELIMINATION_ACTION_PREPARATION.OK = Succès lors du processus de préparation de l'élimination définitive des unités archivistiques)

+ KO : Le processus de préparation de l'élimination définitive des unités archivistiques n'a pas été effectuée (STP_ELIMINATION_ACTION_PREPARATION.KO = Echec lors du début du processus de préparation de l'élimination définitive des unités archivistiques)

+ WARNING : Le processus de préparation de l'élimination définitive des unités archivistiques est en warning (STP_ELIMINATION_ACTION_PREPARATION.WARNING = Avertissement lors du processus de l'étape de préparation de l'élimination définitive des unités archivistiques)  

+ FATAL : Une erreur technique est survenue lors du processus de préparation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_PREPARATION.FATAL = Erreur technique lors du processus de préparation de l'élimination définitive des unités archivistiques)



Vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK)
-----------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : La vérification des processus concurrents a bien été effectuée (CHECK_CONCURRENT_WORKFLOW_LOCK.OK = Succès lors de la vérification des processus concurrents)

+ KO : La vérification des processus concurrents  n'a pas été effectuée  (CHECK_CONCURRENT_WORKFLOW_LOCK.KO = Echec lors de la vérification des processus concurrents)

+ WARNING : La vérification des processus concurrents est en warning (CHECK_CONCURRENT_WORKFLOW_LOCK.WARNING = Avertissement lors de la vérification des processus concurrents)

+ FATAL : Une erreur technique est survenue lors de la vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK.FATAL = Erreur technique lors de la vérification des processus concurrents)



Vérification des seuils de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD)
-------------------------------------------------------------------------------------------------------------------------------

* **Type** :  bloquant

* **Statuts** :

+ OK : La vérification des  seuils de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.OK = Succès de vérification des seuils de l'élimination définitive des unités archivistiques)

+ KO : La vérification des  seuils de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.KO = Echec de vérification des seuils de l'élimination définitive des unités archivistiques)

+ WARNING : La vérification des  seuils de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.WARNING = Avertissement de vérification des seuils de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la vérification des  seuils de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_CHECK_DISTRIBUTION_THRESHOLD.FATAL = Erreur technique de vérification des seuils de l'élimination définitive des unités archivistiques



Préparation de l'élimination définitive des unités archivistiques  (ELIMINATION_ACTION_UNIT_PREPARATION)
--------------------------------------------------------------------------------------------------------
 
* **Type** : bloquant

* **Statuts** :

+ OK : La préparation de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_UNIT_PREPARATION.OK = Succès lors de la préparation de l'élimination définitive des unités archivistiques)

+ KO : La préparation de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_UNIT_PREPARATION.KO = Echec lors de la préparation de l'élimination définitive des unités archivistiques)

+ WARNING : La préparation de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_UNIT_PREPARATION.WARNING = Avertissement lors de la préparation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la préparation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_UNIT_PREPARATION.FATAL= Erreur technique lors de la préparation de l'élimination définitive des unités archivistiques)



Processus d'élimination définitive des unités archivistiques éliminables (STP_ELIMINATION_ACTION_DELETE_UNIT)
-------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus d'élimination définitive des unités archivistiques éliminables a bien été effectuée (STP_ELIMINATION_ACTION_DELETE_UNIT.OK = Succès du processus d'élimination définitive des unités archivistiques éliminables)

+ KO : Le processus d'élimination définitive des unités archivistiques éliminables n'a pas été effectuée (STP_ELIMINATION_ACTION_DELETE_UNIT.KO = Echec du processus d'élimination définitive des unités archivistiques éliminables)

+ WARNING : Le processus d'élimination définitive des unités archivistiques éliminables est en warning (STP_ELIMINATION_ACTION_DELETE_UNIT.WARNING = Avertissement lors du processus d'élimination définitive des unités archivistiques éliminables)

+ FATAL : Une erreur technique est survenue lors du processus d'élimination définitive des unités archivistiques éliminables (STP_ELIMINATION_ACTION_DELETE_UNIT.FATAL= Erreur technique lors du processus d'élimination définitive des unités archivistiques éliminables


Processus de préparation de l'élimination définitive des groupes d'objets techniques (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION)
--------------------------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de préparation de l'élimination définitive des groupes d'objets techniques a bien été effectuée (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.OK = Succès du processus de la préparation de l'élimination définitive des groupes d'objets techniques)

+ KO : Le processus de préparation de l'élimination définitive des groupes d'objets techniques n'a pas été effectuée (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.KO = Echec du processus de la préparation de l'élimination définitive des groupes d'objets techniques)

+ WARNING : Le processus de préparation de l'élimination définitive des groupes d'objets techniques est en warning (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.WARNING = Avertissement lors du processus de la préparation de l'élimination définitive des groupes d'objets techniques)

+ FATAL : Une erreur technique est survenue lors du processus de préparation de l'élimination définitive des groupes d'objets techniques (STP_ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.FATAL= Erreur technique lors du processus de la préparation de l'élimination définitive des groupes d'objets techniques)



Préparation de l'élimination définitive des groupes d'objets techniques (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION)
---------------------------------------------------------------------------------------------------------------------


* **Type** : bloquant

* **Statuts** :

+ OK : La préparation de l'élimination définitive des groupes d'objets techniques a bien été effectuée (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.OK = Succès de la préparation de l'élimination définitive des groupes d'objets techniques)

+ KO : La préparation de l'élimination définitive des groupes d'objets techniques n'a pas été effectuée (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.KO = Echec de la préparation de l'élimination définitive des groupes d'objets techniques)

+ WARNING : La préparation de l'élimination définitive des groupes d'objets techniques est en warning (ELIMINATION_ACTION_OBJECT_GROUP_PREPARATION.WARNING = Avertissement lors de la préparation de l'élimination définitive des groupes d'objets techniques)

+ FATAL : Une erreur technique est survenue lors de la préparation de l'élimination définitive des groupes d'objets techniques  Erreur technique lors de la préparation de l'élimination définitive des groupes d'objets techniques



Processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP)
----------------------------------------------------------------------------------------------------------------------------------------------------------------------


* **Type** : bloquant

* **Statuts** :

+ OK : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées a bien été effectuée (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.OK = Succès du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ KO : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées n'a pas été effectuée (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.KO = Echec du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ WARNING : Le processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées est en warning (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.WARNING = Avertissement lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)

+ FATAL : Une erreur technique est survenue lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DELETE_OBJECT_GROUP.FATAL = Erreur technique lors du processus d'élimination définitive des groupes d'objets techniques dont les unités archivistiques parentes sont éliminées)



Processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP)
------------------------------------------------------------------------------------------------------------------------------------------------------------------


* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées a bien été effectuée (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.OK = Succès du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)

+ KO : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées n'a pas été effectuée (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.KO = Echec du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)

+ WARNING : Le processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées est en warning (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.WARNING = Avertissement lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées) 

+ FATAL : Une erreur technique est survenue lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées (STP_ELIMINATION_ACTION_DETACH_OBJECT_GROUP.FATAL = Erreur technique lors du processus de détachement des groupes d'objets techniques dont certaines unités archivistiques parentes sont éliminées)



Processus de génération du rapport d'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_REPORT_GENERATION)
--------------------------------------------------------------------------------------------------------------------------------


* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de génération du rapport d'élimination définitive des unités archivistiques a bien été effectuée (STP_ELIMINATION_ACTION_REPORT_GENERATION.OK = Succès du processus de génération du rapport d'élimination définitive des unités archivistiques)

+ KO : Le processus de génération du rapport d'élimination définitive des unités archivistiques n'a pas été effectuée (STP_ELIMINATION_ACTION_REPORT_GENERATION.KO = Echec du processus de génération du rapport d'élimination définitive des unités archivistiques)

+ WARNING : Le processus de génération du rapport d'élimination définitive des unités archivistiques est en warning (STP_ELIMINATION_ACTION_REPORT_GENERATION.WARNING = Avertissement lors du processus de génération du rapport d'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors du processus de génération du rapport d'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_REPORT_GENERATION.FATAL = Erreur technique lors du processus de génération du rapport d'élimination définitive des unités archivistiques



Génération du rapport d'élimination définitive des unités archivistiques (ELIMINATION_ACTION_REPORT_GENERATION)
---------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : La génération du rapport d'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.OK = Succès de la génération du rapport d'élimination définitive des unités archivistiques)

+ KO : La génération du rapport d'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_REPORT_GENERATION.KO = Echec de la génération du rapport d'élimination définitive des unités archivistiques)

+ WARNING : La génération du rapport d'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_REPORT_GENERATION.WARNING = Avertissement lors de la génération du rapport d'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la génération du rapport d'élimination définitive des unités archivistiques (ELIMINATION_ACTION_REPORT_GENERATION.FATAL = Erreur technique lors de la génération du rapport d'élimination définitive des unités archivistiques)



Processus de finalisation de l'élimination définitive des unités archivistiques (STP_ELIMINATION_ACTION_FINALIZATION)
---------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : Le processus de finalisation de l'élimination définitive des unités archivistiques a bien été effectuée (STP_ELIMINATION_ACTION_FINALIZATION.OK = Succès du processus de finalisation de l'élimination définitive des unités archivistiques) 

+ KO : Le processus de finalisation de l'élimination définitive des unités archivistiques n'a pas été effectuée (STP_ELIMINATION_ACTION_FINALIZATION.KO = Echec du processus de finalisation de l'élimination définitive des unités archivistiques)

+ WARNING : Le processus de finalisation de l'élimination définitive des unités archivistiques est en warning (STP_ELIMINATION_ACTION_FINALIZATION.WARNING = Avertissement lors du processus de finalisation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors du processus de finalisation de l'élimination définitive des unités archivistiques  (STP_ELIMINATION_ACTION_FINALIZATION.FATAL = Erreur technique lors du processus de finalisation de l'élimination définitive des unités archivistiques)


Finalisation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_FINALIZATION)
----------------------------------------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : La finalisation de l'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION_FINALIZATION.OK = Succès de la finalisation de l'élimination définitive des unités archivistiques)

+ KO : La finalisation de l'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION_FINALIZATION.KO = Echec de la finalisation de l'élimination définitive des unités archivistiques)

+ WARNING : La finalisation de l'élimination définitive des unités archivistiques est en warning (ELIMINATION_ACTION_FINALIZATION.WARNING = Avertissement lors de la finalisation de l'élimination définitive des unités archivistiques)

+ FATAL : Une erreur technique est survenue lors de la finalisation de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION_FINALIZATION.FATAL = Erreur technique lors de la finalisation de l'élimination définitive des unités archivistiques)


Elimination définitive des unités archivistiques (ELIMINATION_ACTION)
---------------------------------------------------------------------

* **Type** : bloquant

* **Statuts** :

+ OK : L'élimination définitive des unités archivistiques a bien été effectuée (ELIMINATION_ACTION.OK = Succès lors de l'élimination définitive des unités archivistiques) 

+ KO : L'élimination définitive des unités archivistiques n'a pas été effectuée (ELIMINATION_ACTION.KO = Echec lors de l'élimination définitive des unités archivistiques) 

+ WARNING : L'élimination définitive des unités archivistiques est an warning (ELIMINATION_ACTION.WARNING = Avertissement lors de l'élimination définitive des unités archivistiques) 

+ FATAL :  Une erreur technique est survenue lors de l'élimination définitive des unités archivistiques (ELIMINATION_ACTION.FATAL = Erreur technique lors de l'élimination définitive des unités archivistiques) 

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

Détails du rapport
==================

Chaque section du rapport correspond au résultat de l’élimination 
    "id": Identifiant de l’objet ou groupe d’objets ou unité archivistique
    "originatingAgency" : service producteur
    "opi" : identifiant de l'opération d'élimination 
   
   Les statuts possibles pour les unités archivistiques :
   
      - GLOBAL_STATUS_KEEP 
           "objectGroupId": "id_got_2" : identifiant du groupe d'objet auxquel appartient l'AU éliminée
     
      - NOT_DESTROYABLE_HAS_CHILD_UNIT
      - DELETED Les unités sans enfants ont pour statut d'élimination DELETED et sont supprimées 
           "objectIds": [ "id_got_1_object_1", "id_got_1_object_2" ] : 
  

   Les statuts possibles pour les GOT : 

      - DELETED 
      - PARTIAL_DETACHMENT 
