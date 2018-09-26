Workflow de valeur probante
###########################

Introduction
============

Cette section décrit le processus (workflow) de relevé de valeur probante. 
L’objectif est de rendre prouvable toute opération effectuée sur toute unité archivistique ou tout
objet qui lui est associé. Ce relevé de valeur probante réunit les éléments permettant de fournir à un auditeur externe une présomption de confiance dans ce qui lui est communiqué.


Processus de préparation du relevé de valeur probante (STP_PROBATIVE_VALUE_PREPARE)
===================================================================================


* **Type** : bloquant
* **Statuts** :

 - OK : Le processus de préparation du relevé de valeur probante a bien été effectué (STP_PROBATIVE_VALUE_PREPARE.OK=Succès du processus de préparation du relevé de valeur probante)
 - KO : Le processus de préparation du relevé de valeur probante n'a pas été effectué (STP_PROBATIVE_VALUE_PREPARE.KO=Échec du processus de préparation du relevé de valeur probante)
 - WARNING : Le processus de préparation du relevé de valeur probante est en warning (STP_PROBATIVE_VALUE_PREPARE.WARNING=Avertissement lors du processus du relevé de valeur probante)
 - FATAL : Une erreur technique est survenue lors du processus de préparation du relevé de valeur probante (STP_PROBATIVE_VALUE_PREPARE.FATAL=Erreur technique lors du processus de préparation du relevé de valeur probante)



Création de la liste des objects du relevé de valeur probante (PROBATIVE_VALUE_LIST_OBJECT)  
-------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : la liste a bien été effectué (PROBATIVE_VALUE_LIST_OBJECT.OK=Création de la liste des objects du relevé de valeur probante)
	- KO : la liste Echec de la création de la liste (PROBATIVE_VALUE_LIST_OBJECT.KO=Echec lors de la création de la liste des objects du relevé de valeur probante)
	- FATAL : Une erreur technique est survenue lors de la création de la liste (PROBATIVE_VALUE_OBJECT.FATAL=Une Erreur technique est survenue lors de la création de la liste des objects du relevé de valeur probante)


Début de la récupération des données dans la base de donnée (STP_PROBATIVE_VALUE_CHECK_OBJECT_GROUP)
====================================================================================================


Récupération des données dans la base de donnée (PROBATIVE_VALUE_CHECK_OBJECT_GROUP)
------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : La récupération des données dans la base de données est un succès (PROBATIVE_VALUE_CHECK_OBJECT_GROUP.OK=Succès de la récupération des données dans la base de donnée) 
	- KO : La récupération des données dans la base de donnée est un échec (PROBATIVE_VALUE_CHECK_OBJECT_GROUP.KO=Echec de la récupération des données dans la base de donnée)
	- WARNING : La récupération des données dans la base de donnée est en warning (PROBATIVE_VALUE_CHECK_OBJECT_GROUP.WARNING=Avertissement lors la récupération des données dans la base de donnée)
	- FATAL : Une erreur technique est survenue lors de la récupération des données dans la base de données (PROBATIVE_VALUE_CHECK_OBJECT_GROUP.FATAL=Erreur technique lors de la récupération des données dans la base de donnée


Processus de préparation de la liste des signatures dans les fichiers sécurisés (STP_PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD)
====================================================================================================================================

Préparation de la liste des signatures dans les fichiers sécurisés (PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD)
-------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :


	- OK : La préparation de la liste des signatures dans les fichiers sécurisés est un succès (PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD.OK=Succès de la préparation de la liste des signatures dans les fichiers sécurisés) 
	- KO : La préparation de la liste des signatures dans les fichiers sécurisés est un échec (PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD.KO=Echec de la préparation de la liste des signatures dans les fichiers sécurisés)
	- WARNING : La préparation de la liste des signatures dans les fichiers sécurisés est en warning (PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD.WARNING=Avertissement lors de la préparation de la liste des signatures dans les fichiers sécurisés)
	- FATAL : Une erreur technique est survenue lors de la préparation de la liste des signatures dans les fichiers sécurisés (PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD.FATAL=Erreur fatale lors de la préparation de la liste des signatures dans les fichiers sécurisés)



Extraction des signatures à partir des fichiers sécurisés des unités archivistiques (STP_PROBATIVE_VALUE_EXTRACT_ZIP_FILE)
==========================================================================================================================

Extraction des signatures à partir des fichiers sécurisés des unités archivistiques (PROBATIVE_VALUE_EXTRACT_ZIP_FILE)
----------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : L'extraction des signatures à partir des fichiers sécurisés des unités archivistiques a bien été effectué (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.OK=Extraction des signatures à partir des fichiers sécurisés)
	- KO : L'extraction des signatures à partir des fichiers sécurisés des unités archivistiques n'a pas été effectué (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.KO=Echec de l'extraction des signatures à partir des fichiers sécurisés)
	- WARNING :  L'extraction des signatures à partir des fichiers sécurisés des unités archivistiques est en warning (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.WARNING=Avertissement lors de l'extraction des signatures à partir des fichiers sécurisés)
	- FATAL : Une erreur technique est survenue lors de la préparation de l'extraction des signatures à partir des fichiers sécurisés des unités archivistiques (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.FATAL=Erreur technique lors de la préparation de l'extraction des signatures à partir des fichiers sécurisés)



Extraction des signatures à partir des fichiers sécurisés des journaux sécurisés (STP_PROBATIVE_VALUE_EXTRACT_ZIP_FILE)
=======================================================================================================================

Extraction des signatures à partir des fichiers sécurisés des journaux sécurisés (PROBATIVE_VALUE_EXTRACT_ZIP_FILE)
-------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : L'extraction des signatures à partir des fichiers sécurisés des journaux sécurisés a bien été effectué (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.OK=Extraction des signatures à partir des fichiers sécurisés)
	- KO : L'extraction des signatures à partir des fichiers sécurisés des journaux sécurisésn'a pas été effectué (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.KO=Echec de l'extraction des signatures à partir des fichiers sécurisés)
	- WARNING :  L'extraction des signatures à partir des fichiers sécurisés des journaux sécurisésest en warning (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.WARNING=Avertissement lors de l'extraction des signatures à partir des fichiers sécurisés)
	- FATAL : Une erreur technique est survenue lors de la préparation de l'extraction des signatures des journaux sécurisés à partir des fichiers sécurisés (PROBATIVE_VALUE_EXTRACT_ZIP_FILE.FATAL=Erreur technique lors de la préparation de l'extraction des signatures à partir des fichiers sécurisés)


Processus de création du rapport pour chaque unité archivistique ou objet ou groupe d'objets (STP_PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS)
===========================================================================================================================================

Création du rapport pour chaque unité archivistique ou objet ou groupe d'objets (PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS)
--------------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : La création du rapport pour chaque unité archivistique ou objet ou groupe d'objets a bien été effectué (PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS.OK=Succès de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objets) 
	- KO :  La création du rapport pour chaque unité archivistique ou objet ou groupe d'objets n'a pas été effectué (PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS.KO=Echec de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objets)
	- WARNING : La création du rapport pour chaque unité archivistique ou objet ou groupe d'objets est en warning (PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS.WARNING=Avertissement lors de  la création du rapport pour chaque unité archivistique ou objet ou groupe d'objets )
	- FATAL : Une erreur technique est survenue lors de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objets (PROBATIVE_VALUE_PREPARE_GENERATE_REPORTS.FATAL=une Erreur technique est survenue de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objets)



Processus de vérification de l'arbre de MERKLE des unités archivistiques (STP_PROBATIVE_VALUE_CHECK_MERKLE_TREE)
================================================================================================================

Vérification de l'arbre de MERKLE des unités archivistiques PROBATIVE_VALUE_CHECK_MERKLE_TREE 
----------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : La vérification de l'arbre de MERKLE des unités archivistiques a bien été effectué (PROBATIVE_VALUE_CHECK_MERKLE_TREE.OK=Succès de la vérification de l'arbre de MERKLE)
	- KO : La vérification de l'arbre de MERKLE des unités archivistiques n'a pas été effectué (PROBATIVE_VALUE_CHECK_MERKLE_TREE.KO=Échec de la vérification de l'arbre de MERKLE)
	- WARNING : La vérification de l'arbre de MERKLE des unités archivistiques est en warning (PROBATIVE_VALUE_CHECK_MERKLE_TREE.WARNING=Avertissement lors de la vérification de l'arbre de MERKLE)
	- FATAL : une erreur technique est survenue lors de la vérification de l'arbre de MERKLE des unités archivistiques (PROBATIVE_VALUE_CHECK_MERKLE_TREE.FATAL=Erreur technique lors de la vérification de l'arbre de MERKLE)


Processus de vérification de l'arbre de MERKLE des journaux sécurisés (STP_PROBATIVE_VALUE_CHECK_MERKLE_TREE)
=============================================================================================================

Vérification de l'arbre de MERKLE des unités archivistiques des journaux sécurisés PROBATIVE_VALUE_CHECK_MERKLE_TREE 
---------------------------------------------------------------------------------------------------------------------

* **Type** : bloquant
* **Statuts** :

	- OK : La vérification de l'arbre de MERKLE des journaux sécurisés a bien été effectué (PROBATIVE_VALUE_CHECK_MERKLE_TREE.OK=Succès de la vérification de l'arbre de MERKLE)
	- KO : La vérification de l'arbre de MERKLE des journaux sécurisés n'a pas été effectué (PROBATIVE_VALUE_CHECK_MERKLE_TREE.KO=Échec de la vérification de l'arbre de MERKLE)
	- WARNING : La vérification de l'arbre de MERKLE des journaux sécurisés est en warning (PROBATIVE_VALUE_CHECK_MERKLE_TREE.WARNING=Avertissement lors de la vérification de l'arbre de MERKLE)
	- FATAL : une erreur technique est survenue lors de la vérification de l'arbre de MERKLE des journaux sécurisés (PROBATIVE_VALUE_CHECK_MERKLE_TREE.FATAL=Erreur technique lors de la vérification de l'arbre de MERKLE)


Processus de finalisation de l'audit et génération du rapport final (STP_EVIDENCE_AUDIT_FINALIZE)
=================================================================================================


Création du rapport de l'audit de cohérence EVIDENCE_AUDIT_FINALIZE
-------------------------------------------------------------------

* **Règle** : Tâche consistant à créer le rapport permettant de comparer les signatures extraites des fichiers sécurisés avec les données de la base de données et de l'offre de stockage. 
* **Type** : bloquant
* **Statuts** :

	- OK : La création du rapport d'audit de cohérence a bien été effectué (EVIDENCE_AUDIT_FINALIZE.OK=Succès de la création du rapport de l'audit de cohérence)
	- KO : La création du rapport d'audit de cohérence n'a pas été effectué (EVIDENCE_AUDIT_FINALIZE.KO=Echec de la création du rapport de l'audit de cohérence)
	- FATAL : une erreur technique est survenue lors de la création du rapport d'audit de cohérence (EVIDENCE_AUDIT_FINALIZE.FATAL=Erreur technique lors de la création du rapport d'audit de cohérence)


Relevé de valeur probante (EXPORT_PROBATIVE_VALUE)
==================================================


* **Type** : bloquant
* **Statuts** :

	- OK : L'export du relevé de valeur probante a bien été effectué (EXPORT_PROBATIVE_VALUE.OK=Succès du processus de l'export du relevé de valeur probante)
	- KO :  L'export du relevé de valeur probante n'a pas été effectué (EVIDENCE_AUDIT_FINALIZE.KO=Echec de la création du rapport de l'audit de cohérence)
	- FATAL : une erreur technique est survenue lors de l'export du relevé de valeur probante (EVIDENCE_AUDIT_FINALIZE.FATAL=Erreur technique lors de la création du rapport d'audit de cohérence)

.. figure:: images/workflow_probative_value.png
	:align: center



