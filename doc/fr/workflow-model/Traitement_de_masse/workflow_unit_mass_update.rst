Workflow de mise à jour de masse des unités archivistiques
###########################################################

Introduction
============

Cette section décrit le processus permettant d'effectuer des actions sur un grand nombre d'unités archivistiques stockées dans la solution logicielle Vitam. Cette fonctionnalité nécessite d'avoir les droits requis pour pouvoir intervenir sur les métadonnées. Les autorisations de modifications en masse portent soit sur les métadonnées descriptives soit sur les métadonnées de gestion. 

Chaque traitement de masse donne lieu à une entrée dans le journal des opérations. Lorsque le traitement génère un rapport, celui-ci est disponible dans le journal des opérations et détaille les cas de succès et les cas d'échec en explicitant la raison de la non application du traitement de masse. 


Processus de mise à jour en masse des métadonnées descriptives des unités archivistiques  (vision métier)
=========================================================================================================


Le processus de mise à jour en masse des métadonnées descriptives des unités archivistiques permet d'effectuer des modifications sur un ensemble conséquent d'archives en une seule opération. 


Processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification STP_CHECK_AND_COMPUTE
===========================================================================================================================================


+ **Règle** : Processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification

+ **Type** : bloquant

+ **Statuts** :


  + OK : le processus de préparation de la liste des unités archivistiques à mettre à jour a bien été effectué (STP_CHECK_AND_COMPUTE.OK = Succès du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)

  + KO : le processus de préparation de la liste des unités archivistiques à mettre à jour n'a pas été effectué en raison d'une erreur (STP_CHECK_AND_COMPUTE.KO = Échec du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)

  + FATAL : une erreur technique est survenue lors du processus de préparation de la liste des unités archivistiques à mettre à jour (STP_CHECK_AND_COMPUTE.FATAL=Erreur technique lors du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)



Vérification des droits de mise à jour des métadonnées descriptives des unités archivistiques UNIT_METADATA_UPDATE_CHECK_PERMISSION
------------------------------------------------------------------------------------------------------------------------------------


+ **Règle** : Vérification des droits de mise à jour des métadonnées descriptives des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la vérification des droits de mise à jour des métadonnées des unités archivistiques a bien été effectuée (UNIT_METADATA_UPDATE_CHECK_PERMISSION.OK = Succès de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

  + KO : la vérification des droits de mise à jour des métadonnées des unités archivistiques n'a pas été effectuée en raison d'une erreur (UNIT_METADATA_UPDATE_CHECK_PERMISSION.KO = Échec de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.FATAL = Erreur technique lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques)



Vérification des seuils de limitation de traitement des unités archivistiques CHECK_DISTRIBUTION_THRESHOLD
-----------------------------------------------------------------------------------------------------------


+ **Règle** : Vérification des seuils de limitation de traitement des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la vérification des seuils de limitation de traitement des unités archivistiques a bien été effectuée (CHECK_DISTRIBUTION_THRESHOLD.OK = Succès de la vérification des seuils de limitation de traitement des unités archivistiques)

  + KO : la vérification des seuils de limitation de traitement des unités archivistiques n'a pas été effectuée en raison d'une erreur (CHECK_DISTRIBUTION_THRESHOLD.KO = Échec de la vérification des seuils de limitation de traitement des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la vérification des seuils de limitation de traitement des unités archivistiques (CHECK_DISTRIBUTION_THRESHOLD.FATAL = Erreur technique lors de la vérification des seuils de limitation de traitement des unités archivistiques)



Préparation de la liste des unités archivistiques à mettre à jour PREPARE_UPDATE_UNIT_LIST
-------------------------------------------------------------------------------------------


+ **Règle** : Préparation de la liste des unités archivistiques à mettre à jour

+ **Type** : bloquant

* **Statuts** :

  + OK : la préparation de la liste des unités archivistiques à mettre à jour a bien été effectuée (PREPARE_UPDATE_UNIT_LIST.OK = Succès de la préparation de la liste des unités archivistiques à mettre à jour)

  + KO : la préparation de la liste des unités archivistiques à mettre à jour n'a pas été effectuée en raison d'une erreur (PREPARE_UPDATE_UNIT_LIST.KO = Échec de la préparation de la liste des unités archivistiques à mettre à jour)

  + FATAL : une erreur technique est survenue lors de la préparation de la liste des unités archivistiques à mettre à jour (PREPARE_UPDATE_UNIT_LIST.FATAL = Erreur technique lors de la préparation de la liste des unités archivistiques à mettre à jour)



Processus de traitement de mise à jour des unités archivistiques STP_UPDATE
============================================================================


Préparation de la liste des unités archivistiques à mettre à jour MASS_UPDATE_UNIT
-----------------------------------------------------------------------------------


+ **Règle** : Mise à jour des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la mise à jour des unités archivistiques a bien été effectué (MASS_UPDATE_UNITS.OK = Succès de la mise à jour des unités archivistiques)

  + KO : la mise à jour des unités archivistiques n'a pas été effectuée en raison d'une erreur (MASS_UPDATE_UNITS.KO = Échec de la mise à jour des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la mise à jour des unités archivistiques (MASS_UPDATE_UNITS.FATAL = Erreur technique lors de la mise à jour des unités archivistiques)



Processus de génération du rapport de mise à jour des métadonnées descriptives des unités archivistiques STP_MASS_UPDATE_FINALIZE
===================================================================================================================================

Génération du rapport de mise à jour des métadonnées descriptives des unités archivistiques MASS_UPDATE_FINALIZE
----------------------------------------------------------------------------------------------------------------

+ **Règle** : Processus de génération du rapport de mise à jour des métadonnées descriptives des unités archivistiques 

+ **Type** : bloquant

* **Statuts** :

  + OK : le processus de traitement de mise à jour des unités archivistiques à mettre à jour a bien été effectué (MASS_UPDATE_FINALIZE.OK = Succès du processus de traitement de mise à jour des unités archivistiques)

  + KO : le processus de traitement de mise à jour des unités archivistiques à mettre à jour n'a pas été effectuée en raison d'une erreur (MASS_UPDATE_FINALIZE.KO = Échec du processus de traitement de mise à jour des unités archivistiques)

  + FATAL : une erreur technique est survenue lors du processus de traitement de mise à jour des unités archivistiques à mettre à jour (MASS_UPDATE_FINALIZE.FATAL = Erreur technique lors du processus de traitement de mise à jour des unités archivistiques)




Structure du workflow de mise à jour en masse des métadonnées descriptives des unités archivistiques
=====================================================================================================
.. image:: images/workflow_unit_mass_update_desc.png
    :align: center



Processus de mise à jour en masse des métadonnées de gestion des unités archivistiques  (vision métier)
=========================================================================================================


Le processus de mise à jour en masse des métadonnées descriptives des unités archivistiques permet d'effectuer des modifications sur un ensemble conséquent d'archives en une seule opération. 


Processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification STP_CHECK_AND_COMPUTE
===========================================================================================================================================


+ **Règle** : Processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification

+ **Type** : bloquant

+ **Statuts** :


  + OK : le processus de préparation de la liste des unités archivistiques à mettre à jour a bien été effectuée (STP_CHECK_AND_COMPUTE.OK = Succès du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)

  + KO : le processus de préparation de la liste des unités archivistiques à mettre à jour n'a pas été effectuée en raison d'une erreur (STP_CHECK_AND_COMPUTE.KO = Échec du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)

  + FATAL : une erreur technique est survenue lors du processus de préparation de la liste des unités archivistiques à mettre à jour (STP_CHECK_AND_COMPUTE.FATAL=Erreur technique lors du processus de préparation de la liste des unités archivistiques à mettre à jour et des autorisations de modification)



Vérification des droits de mise à jour des métadonnées descriptives des unités archivistiques UNIT_METADATA_UPDATE_CHECK_PERMISSION
------------------------------------------------------------------------------------------------------------------------------------


+ **Règle** : Vérification des droits de mise à jour des métadonnées descriptives des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la vérification des droits de mise à jour des métadonnées des unités archivistiques a bien été effectué (UNIT_METADATA_UPDATE_CHECK_PERMISSION.OK = Succès de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

  + KO : la vérification des droits de mise à jour des métadonnées des unités archivistiques n'a pas été effectuée en raison d'une erreur (UNIT_METADATA_UPDATE_CHECK_PERMISSION.KO = Échec de la vérification des droits de mise à jour des métadonnées des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques (UNIT_METADATA_UPDATE_CHECK_PERMISSION.FATAL = Erreur technique lors de la vérification des droits de mise à jour des métadonnées des unités archivistiques)


Vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques CHECK_RULES_ID
-----------------------------------------------------------------------------------------------------------------------------


+ **Règle** : Vérification des identifiants de règles de gestion demandées à la mise à jour des unités archivistiques 

+ **Type** : bloquant

* **Statuts** :

  + OK : la vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques a bien été effectué (CHECK_RULES_ID.OK = Succès de la vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques)

  + KO : la vérificationdes identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques n'a pas été effectuée en raison d'une erreur (CHECK_RULES_ID.KO = Échec de la vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques (CHECK_RULES_ID.FATAL = Erreur technique lors de la vérification des identifiants de règles de gestion demandées lors de la mise à jour des unités archivistiques)


Vérification des seuils de limitation de traitement des unités archivistiques CHECK_DISTRIBUTION_THRESHOLD
-----------------------------------------------------------------------------------------------------------


+ **Règle** : Vérification des seuils de limitation de traitement des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la vérification des seuils de limitation de traitement des unités archivistiques a bien été effectué (CHECK_DISTRIBUTION_THRESHOLD.OK = Succès de la vérification des seuils de limitation de traitement des unités archivistiques)

  + KO : la vérification des seuils de limitation de traitement des unités archivistiques n'a pas été effectuée en raison d'une erreur (CHECK_DISTRIBUTION_THRESHOLD.KO = Échec de la vérification des seuils de limitation de traitement des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la vérification des seuils de limitation de traitement des unités archivistiques (CHECK_DISTRIBUTION_THRESHOLD.FATAL = Erreur technique lors de la vérification des seuils de limitation de traitement des unités archivistiques)



Préparation de la liste des unités archivistiques à mettre à jour PREPARE_UPDATE_UNIT_LIST
-------------------------------------------------------------------------------------------


+ **Règle** : Préparation de la liste des unités archivistiques à mettre à jour

+ **Type** : bloquant

* **Statuts** :

  + OK : la préparation de la liste des unités archivistiques à mettre à jour a bien été effectuéE (PREPARE_UPDATE_UNIT_LIST.OK = Succès de la préparation de la liste des unités archivistiques à mettre à jour)

  + KO : la préparation de la liste des unités archivistiques à mettre à jour n'a pas été effectuée en raison d'une erreur (PREPARE_UPDATE_UNIT_LIST.KO = Échec de la préparation de la liste des unités archivistiques à mettre à jour)

  + FATAL : une erreur technique est survenue lors de la préparation de la liste des unités archivistiques à mettre à jour (PREPARE_UPDATE_UNIT_LIST.FATAL = Erreur technique lors de la préparation de la liste des unités archivistiques à mettre à jour)



Processus de traitement de mise à jour des unités archivistiques STP_UPDATE
============================================================================


Préparation de la liste des unités archivistiques à mettre à jour MASS_UPDATE_UNIT_RULES
-----------------------------------------------------------------------------------------


+ **Règle** : mise à jour des règles de gestion des unités archivistiques

+ **Type** : bloquant

* **Statuts** :

  + OK : la mise à jour des règles de gestion des unités archivistiques a bien été effectué (MASS_UPDATE_UNITS_RULES.OK = Succès de la mise à jour des règles de gestion des unités archivistiques)

  + KO : la mise à jour des règles de gestion des unités archivistiques n'a pas été effectuée en raison d'une erreur (MASS_UPDATE_UNITS_RULES.KO = Échec de la mise à jour des règles de gestion des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la mise à jour des règles de gestion des unités archivistiques (MASS_UPDATE_UNITS_RULES.FATAL = Erreur technique lors de la mise à jour des règles de gestion des unités archivistiques)



Processus de génération du rapport de mise à jour des métadonnées de gestion des unités archivistiques STP_MASS_UPDATE_FINALIZE
==================================================================================================================================

Génération du rapport de mise à jour des métadonnées descriptives des unités archivistiques MASS_UPDATE_FINALIZE
-----------------------------------------------------------------------------------------------------------------


+ **Règle** : Génération du rapport de mise à jour des métadonnées descriptives des unités archivistiques 

+ **Type** : bloquant

* **Statuts** :

  + OK : le processus de traitement de mise à jour des unités archivistiques à mettre à jour a bien été effectué (MASS_UPDATE_FINALIZE.OK=Succès du processus de traitement de mise à jour des unités archivistiques)

  + KO : le processus de traitement de mise à jour des unités archivistiques à mettre à jour n'a pas été effectuée (MASS_UPDATE_FINALIZE.KO=Échec du processus de traitement de mise à jour des unités archivistiques)

  + FATAL : une erreur technique est survenue lors du processus de traitement de mise à jour des unités archivistiques à mettre à jour (MASS_UPDATE_FINALIZE.FATAL=Erreur technique lors du processus de traitement de mise à jour des unités archivistiques)


Mise à jour en masse des métadonnées des règles de gestion des unités archivistiques MASS_UPDATE_UNIT_RULE
==========================================================================================================


+ **Règle** : Processus de mise à jour des métadonnées des règles de gestion des unités archivistiques de masse 

+ **Type** : bloquant

+ **Statuts** :


  + OK : la mise à jour des métadonnées des règles de gestion des unités archivistiques a bien été effectuée (MASS_UPDATE_UNIT_RULE.OK = Succès de la mise à jour des métadonnées de gestion des unités archivistiques)

  + KO : la mise à jour des métadonnées des règles de gestion des unités archivistiques n'a pas été effectuée en raison d'une erreur (MASS_UPDATE_UNIT_RULE.KO = Échec de la mise à jour des métadonnées de gestion des unités archivistiques)

  + FATAL : une erreur technique est survenue lors de la mise à jour des métadonnées des règles de gestion des unités archivistiques (MASS_UPDATE_UNIT_RULE.FATAL = Erreur technique lors de la mise à jour des métadonnées de gestion des unités archivistiques)



Structure de workflow de mise à jour en masse des métadonnées de gestion des unités archivistiques
===================================================================================================

.. image:: images/workflow_unit_mass_update_rules.png
    :align: center

