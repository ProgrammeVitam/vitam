Workflow de Modification d'arborescence
#######################################

Introduction
============

Cette section décrit le processus permettant l'action de modification d'arborescence d'archives, c'est à dire de modifier les rattachements d'une unité archivistique présente dans le système. 


Le processus de modification d'arborescence est lié à des ajouts et des suppressions de liens de parenté, en sachant que:
-Les unités archivistiques modifiées ou rattachées doivent être accessibles par le contrat d'accès.


Etape de préparation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_PREPARATION)
===================================================================================================================

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l''étape de préparation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_PREPARATION.OK)

	+ KO: Échec de l''étape de préparation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_PREPARATION.KO): si un des évènements est KO

	+ FATAL: Erreur technique lors de l''étape de préparation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_PREPARATION.FATAL)


Vérification des processus concurrents ( CHECK_CONCURRENT_WORKFLOW_LOCK )
----------------------------------------------------------------------------------

Le but est de vérifier s'il n'y a pas d'autre processus de modification d'arborescence en cours.Si tel est le cas, le processus n'est pas lancé afin d'éviter les cycles rattachements/detachements concernant plusieurs unités archivistiques.

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : il n'y a pas d'autre processus de reclassement en cours,la modification d'arborescence peut s'effectuer. (CHECK_CONCURRENT_WORKFLOW_LOCK.OK=Succès de la vérification des processus concurrents)

  + KO : le reclassement ne peut pas s'effectuer car un autre processus de modification d'arborescence est en cours (CHECK_CONCURRENT_WORKFLOW_LOCK.KO=Échec lors de la vérification des processus concurrents)

  + FATAL : une erreur technique est survenue lors de la vérification des processus concurrents (CHECK_CONCURRENT_WORKFLOW_LOCK.FATAL=Erreur fatale lors de la vérification des processus concurrents)



Chargement des unités archivistiques : RECLASSIFICATION_PREPARATION_LOAD_REQUEST
--------------------------------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Le chargement des unités archivistiques a bien été effectué (RECLASSIFICATION_PREPARATION_LOAD_REQUEST.OK=Succès du chargement des unités archivistiques)

  + KO : le chargement des unités archivistiques n'a pas pu s'effectuer (RECLASSIFICATION_PREPARATION_LOAD_REQUEST.KO=Erreur lors du chargement des unités archivistiques)

  + FATAL : une erreur fatale est survenue lors du chargement des unités archivistiques (RECLASSIFICATION_PREPARATION_LOAD_REQUEST.FATAL=Erreur technique lors du chargement des unités archivistiques)




Vérification de la cohérence du graphe : RECLASSIFICATION_PREPARATION_CHECK_GRAPH
---------------------------------------------------------------------------------

Cette vérification a pour but de vérifier la cohérence du graphe, c'est à dire vérifier que les rattachements s'effectuent de façon cohérente entre les différents types d'unités archivistiques: Plan, classement, unités simples. 

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Le contrôle de la cohérence du graphe a bien été effectué (RECLASSIFICATION_PREPARATION_CHECK_GRAPH.OK=Succès du contrôle de cohérence du graphe)

  + KO : Échec lors du contrôle de cohérence du graphe (RECLASSIFICATION_PREPARATION_CHECK_GRAPH.KO=Échec lors du contrôle de cohérence du graphe)

  + FATAL : une erreur technique est survenue lors du contrôle de cohérence du graphe (RECLASSIFICATION_PREPARATION_CHECK_GRAPH.FATAL=Erreur technique lors du contrôle de cohérence du graphe


Préparation de la mise à jour du graphe : RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION
------------------------------------------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Succès de la préparation de la mise à jour du graphe (RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION.OK=Succès de la préparation de la mise à jour du graphe)

  + KO : Echec de la préparation de la mise à jour du graphe (RECLASSIFICATION_PREPARATION_UPDATE_DISTRIBUTION.KO=Échec lors de la préparation de la mise à jour du graphe)

  + FATAL : Erreur technique lors de la préparation de la mise à jour du graphe



Etape de détachement des unités archivistiques (STP_UNIT_DETACHMENT)
====================================================================

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l''étape de préparation de détachement des unités archivistiques (STP_UNIT_DETACHMENT.OK=Succès de l''étape de détachement des unités archivistiques)

	+ KO: Échec de l''étape de préparation de détachement des unités archivistiques (STP_UNIT_DETACHMENT.KO=Échec lors de l''étape de détachement des unités archivistiques)

	+ FATAL: Erreur technique lors de l''étape de préparation de détachement des unités archivistiques (STP_UNIT_DETACHMENT.FATAL=Erreur technique lors de l''étape de détachement des unités archivistiques)


Détachement des unités archivistiques : UNIT_DETACHMENT
-------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Succès du détachement des unités archivistiques (UNIT_DETACHMENT.OK=Succès du détachement des unités archivistiques)

  + KO : Echec du détachement des unités archivistiques  (UNIT_DETACHMENT.KO=Échec lors du détachement des unités archivistiques)

  + FATAL : Erreur technique lors du détachement des unités archivistiques (UNIT_DETACHMENT.FATAL=Erreur technique lors du détachement des unités archivistiques)


Etape d' attachement des unités archivistiques (STP_UNIT_ATTACHMENT)
====================================================================

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l''Etape de rattachement des unités archivistiques (STP_UNIT_ATTACHMENT.OK=Succès de l''étape de rattachement des unités archivistiques)

	+ KO: Échec de l''étape de rattachement des unités archivistiques (STP_UNIT_ATTACHMENT.KO=Échec lors de l''étape de rattachement des unités archivistiques)

	+ FATAL: Erreur technique lors de l''étape de rattachement des unités archivistiques (STP_UNIT_ATTACHMENT.FATAL=Erreur fatale lors de l''étape de rattachement des unités archivistiques



Rattachement des unités archivistiques : UNIT_ATTACHMENT
--------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : Succès du rattachement des unités archivistiques (UNIT_ATTACHMENT.OK=Succès du détachement des unités archivistiques)

  + KO : Echec du rattachement des unités archivistiques  (UNIT_ATTACHMENT.KO=Échec lors du détachement des unités archivistiques)

  + FATAL : Erreur technique lors du rattachement des unités archivistiques (UNIT_ATTACHMENT.FATAL=Erreur technique lors du détachement des unités archivistiques)



Mise à jour des graphes des unités archivistiques (STP_UNIT_GRAPH_COMPUTE)
==========================================================================

Cette étape a pour but de recalculer les graphes des unités archivistiques.

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de la mise à jour des graphes des unités archivistiques (STP_UNIT_ATTACHMENT.OK=Succès de l'étape de mise à jour des graphes des unités archivistiques)

	+ KO: Succès de la mise à jour des graphes des unités archivistiques (STP_UNIT_ATTACHMENT.KO= échec de l'étape de mise à jour des graphes des unités archivistiques)

	+ FATAL: Erreur technique lors de la mise à jour des graphes des unités archivistiques (STP_UNIT_GRAPH_COMPUTE.FATAL=Erreur fatale lors de l'étape de mise à jour des graphes des unités archivistiques)



Calcul du graphe des unités archivistiques : UNIT_GRAPH_COMPUTE
----------------------------------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

	+ OK: Succès de la mise à jour des graphes des unités archivistiques (UNIT_GRAPH_COMPUTE.OK=Succès de l'étape de mise à jour des graphes des unités archivistiques)

	+ KO: Succès de la mise à jour des graphes des unités archivistiques (UNIT_GRAPH_COMPUTE.KO= échec de l'étape de mise à jour des graphes des unités archivistiques)

	+ FATAL: Erreur technique lors de la mise à jour des graphes des unités archivistiques (UNIT_GRAPH_COMPUTE.FATAL=Erreur fatale lors de l'étape de mise à jour des graphes des unités archivistiques)



Mise à jour des graphes des groupes d'objets (STP_OBJECT_GROUP_GRAPH_COMPUTE)
=============================================================================

Cette étape a pour but de recalculer les graphes des groupes d'objets. 

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l'étape de mise à jour des graphes du groupe d'objets (STP_OBJECT_GROUP_GRAPH_COMPUTE.OK=Succès de l'étape de mise à jour des graphes des groupes d'objets)

	+ KO: Succès de la mise à jour de mise à jour des graphes du groupe d'objets (STP_OBJECT_GROUP_GRAPH_COMPUTE.KO= échec de l'étape de mise à jour des graphes du groupe d'objets)

	+ FATAL: Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets (STP_OBJECT_GROUP_GRAPH_COMPUTE.FATAL=Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets)



Calcul des graphes des groupes d'objets (OBJECT_GROUP_GRAPH_COMPUTE)
--------------------------------------------------------------------

Cette étape a pour but de recalculer les graphes des groupes d'objets. 

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l'étape de mise à jour des graphes du groupe d'objets (OBJECT_GROUP_GRAPH_COMPUTE.OK=Succès de l'étape de mise à jour des graphes des groupes d'objets)

	+ KO: Succès de la mise à jour de mise à jour des graphes du groupe d'objets (OBJECT_GROUP_GRAPH_COMPUTE.KO= échec de l'étape de mise à jour des graphes du groupe d'objets)

	+ FATAL: Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets (OBJECT_GROUP_GRAPH_COMPUTE.FATAL=Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets)



Finalisation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_FINALIZATION)
============================================================================================================

Cette étape a pour but de finaliser le processus de modification d'arborescence pour des unités archivistiques existantes dans le système. 

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l'étape de finalisation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_FINALIZATION.OK=Succès de l'étape de finalisation de la modification d'arborescence des unités archivistiques)

	+ KO: Échec lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques (STP_RECLASSIFICATION_FINALIZATION.KO=Échec lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques)

	+ FATAL: Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets (STP_RECLASSIFICATION_FINALIZATION.FATAL=Erreur fatale lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques)


Finalisation de la modification d'arborescence des unités archivistiques (RECLASSIFICATION_FINALIZATION)
------------------------------------------------------------------------------------------------------------

Cette étape a pour but de finaliser le processus de modification d'arborescence pour des unités archivistiques existantes dans le système. 

La fin du processus peut prendre plusieurs statuts: 


* **Statuts** 

	+ OK: Succès de l'étape de finalisation de la modification d'arborescence des unités archivistiques (RECLASSIFICATION_FINALIZATION.OK=Succès de l'étape de finalisation de la modification d'arborescence des unités archivistiques)

	+ KO: Échec lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques (RECLASSIFICATION_FINALIZATION.KO=Échec lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques)

	+ FATAL: Erreur technique lors de l'étape de mise à jour des graphes du groupe d'objets (RECLASSIFICATION_FINALIZATION.FATAL=Erreur fatale lors de l'étape de finalisation de la modification d'arborescence des unités archivistiques)



D'une façon synthétique, le workflow est décrit de cette façon :

  .. figure:: images/workflow_modification_arborescence.png
    :align: center

    Diagramme d'activité du workflow de modification d'arborescence








