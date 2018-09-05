workflow d'élimination
######################

Introduction
============

Cette section décrit les processus en lien avec les phases d'analyses des unités archivistiques potentiellement éliminables, et l'action d'élimination en masse d'unités archivistiques. 

Ces deux étapes ne sont pas liées , mais la phase d'analyse peut servir à déterminer une liste d'unités archivistiques qui peuvent être éliminables. 


Analyse
========


Lors de cette étape, le système va effectuer pour chaque unité archivistique une vérification des règles de gestion et de l'héritage de celle-ci.

Les unités archivistiques éliminables qui ont un enfant non éliminable ne seront pas éliminées, pour éviter tout cas d'orphelinage.
Les unités archivistiques qui sont réellement éliminables et qui apparaissent dans l'écran d'affichage des résultats d'élimination sont :
 
 - les unités archivistiques qui ont une règle de gestion de type "Durée d'utilité administrative" arrivée à échéance et dont le sort final est "détruire"
 - les unités archivistiques qui ne laissent pas d'orphelins



STP_ELIMINATION_ANALYSIS_ELIMINATION
-------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : la préparation de l'analyse de l'unité archivistique a bien été effectuée (ELIMINATION_ANALYSIS_PREPARATION.OK = la préparation de l'analyse de l'unité archivistique a bien été effectuée)

  + KO : la préparation de l'analyse de l'unité archivistique n'a pas été effectuée (ELIMINATION_ANALYSIS_PREPARATION.KO = Echec lors de la préparation de l'analyse de l'unité archivistique)

  + FATAL : une erreur fatale est survenue lors de la préparation de l'analyse de l'unité archivistique (ELIMINATION_ANALYSIS_PREPARATION.FATAL = Erreur technique lors de la préparation de l'analyse )


STP_ELIMINATION_ANALYSIS_INDEXATION
------------------------------------

Cette étape à pour but d'indexer les unités archivistiques qui répondent aux critères d'éliminabilité cités plus haut. 

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : l'indexation des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_INDEXATION.OK = l'indexation des unités archivistiques a bien été effectuée)

  + KO : Erreur lors de l'indexation des unités archivistiques (ELIMINATION_ANALYSIS_INDEXATION.KO = erreur lors de l'indexation des unités archivistiques)

  + FATAL : une erreur fatale est survenue lors de l'indexation des unités archivistiques (ELIMINATION_ANALYSIS_INDEXATION.FATAL = erreur fatale lors de la préparation de l'analyse de l'unité archivistique)


STP_ELIMINATION_ANALYSIS_FINALIZATION
-------------------------------------

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : la finalisation de l'analyse des unités archivistiques a bien été effectuée (ELIMINATION_ANALYSIS_FINALIZATION.OK = la finalisation des unités archivistiques a bien été effectuée)

  + KO : Erreur lors de la finalisation de l'analyse des unités archivistiques (ELIMINATION_ANALYSIS_FINALIZATION.KO = erreur lors de la finalisation de l'analyse des unités archivistiques)

  + FATAL : une erreur fatale est survenue lors de l'analyse des unités archivistiques (ELIMINATION_ANALYSIS_FINALIZATION.FATAL = erreur fatale lors de l'analyse de l'unité archivistique)





