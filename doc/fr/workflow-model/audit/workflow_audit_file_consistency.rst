Workflow de l'audit de cohérence des fichiers
#############################################

Introduction
============

Cette section décrit le processus (workflow) d'audit de cohérence des fichiers mis en place dans la solution logicielle Vitam.

Celui-ci est défini dans le fichier "EvidenceAuditWorkflow.json” (situé ici : sources/processing/processing-management/src/main/resources/workflows).

Processus d'audit de cohérence des fichiers (vision métier)
===========================================================

Le processus d'audit de cohérence permet de vérifier la cohérence entre les signatures calculées pour chaque objet, en comparant celle présente dans le journal sécurisé, avec celle présente dans la base de donnée , et celle de l'offre de stockage. 

L'audit s'applique au niveau des unités archivistiques, des objets, et des groupes d'objets. 


Processus de préparation de l'audit (STP_EVIDENCE_AUDIT_PREPARE)
================================================================

EVIDENCE_AUDIT_PREPARE
----------------------

* **Règle** : Création de la liste à auditer
* **Type** : bloquant
* **Statuts** :
	* OK : la liste a été créée avec succès 
	* KO : Echec de la création de la liste à auditer. 
	* FATAL : Une erreur technique est survenue lors de la création de la liste 


Récupération des données de la base (STP_EVIDENCE_AUDIT_CHECK_DATABASE)
=======================================================================

EVIDENCE_AUDIT_CHECK_DATABASE
-----------------------------

* **Règle** : Tâche consistant à récupérer les informations nécéssaires à l'audit dans la base de donnée. 
* **Type** : bloquant
* **Statuts** :
	* OK : Succès de la récupération des données dans la base de donnée (EVIDENCE_AUDIT_CHECK_DATABASE.OK=Succès de la récupération des données dans la base de donnée)
	* KO : Echecde la récupération des données dans la base de donnée (EVIDENCE_AUDIT_CHECK_DATABASE.KO=Echec de la récupération des données dans la base de donnée)
	* FATAL : une erreur technique est survenue dans la récupération des données dans la base de donnée (EVIDENCE_AUDIT_CHECK_DATABASE.FATAL=Erreur technique lors de la récupération des données dans la base de donnée)


Préparation des signatures à partir des fichiers sécurisés (STP_EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD)
==============================================================================================================

EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD
---------------------------------------------

* **Règle** : Tâche consistant à préparer la liste des signatures des objets, groupes d'objets ou unités archivistiques archivées, dans les fichiers sécurisés. 
* **Type** : bloquant
* **Statuts** :
	* OK : Succès de la préparation de la liste des signatures dans les fichiers sécurisés (EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD.OK=Succès de la préparation de la liste des signatures dans les fichiers sécurisés)
	* KO : Echec de la préparation de la liste des signatures dans les fichiers sécurisés (EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD.KO=Echec de la préparation de la liste des signatures dans les fichiers sécurisés)
	* FATAL : une erreur technique est survenue lors de la préparation de la liste des signatures dans les fichiers sécurisés (EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD.FATAL=Echec de la préparation de la liste des signatures dans les fichiers sécurisés)



Extraction des signatures à partir des fichiers sécurisés (STP_EVIDENCE_AUDIT_EXTRACT_ZIP_FILE)
===============================================================================================

EVIDENCE_AUDIT_EXTRACT_ZIP_FILE
-------------------------------

* **Règle** : Tâche consistant à extraire les signatures des objets, groupes d'objets ou unités archivistiques archivées, dans les fichiers sécurisés. 
* **Type** : bloquant
* **Statuts** :
	* OK : Succès de l'extraction des signatures à partir des fichiers sécurisés (EVIDENCE_AUDIT_EXTRACT_ZIP_FILE.OK=Succès de l'extraction des signatures à partir des fichiers sécurisés )
	* KO : Echec de l'extraction des signatures à partir des fichiers sécurisés (EVIDENCE_AUDIT_EXTRACT_ZIP_FILE.KO=Echec de l'extraction des signatures à partir des fichiers sécurisés )
	* FATAL : une erreur technique est survenue de l'extraction des signatures à partir des fichiers sécurisés  (EVIDENCE_AUDIT_EXTRACT_ZIP_FILE.FATAL=une erreur technique est survenue de l'extraction des signatures à partir des fichiers sécurisés)


Préparation des rapports pour chaque objet, groupe d'objet ou unité audité (STP_EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS)
========================================================================================================================

EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS
---------------------------------------

* **Règle** : Tâche consistant à créer le rapport pour chaque unité archivistique, objet ou groupe d'objet audité 
* **Type** : bloquant
* **Statuts** :
	* OK : Succès de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objet(EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS.OK=Succès de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objet)
	* KO : Echec de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objet(EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS.KO=Echec de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objet)
	* FATAL : une erreur technique est survenue de la création du rapport pour chaque unité archivistique ou objet ou groupe d'objet (EVIDENCE_AUDIT_PREPARE_GENERATE_REPORTS.FATAL=une erreur technique est survenue de l'extraction des signatures à partir des fichiers sécurisés)


Finalisation du rapport - Comparaison des signatures et des données (STP_EVIDENCE_AUDIT_FINALIZE)
=================================================================================================

EVIDENCE_AUDIT_FINALIZE
-----------------------

* **Règle** : Tâche consistant à créer le rapport permettant de comparer les signatures extraites des fichiers sécurisés avec les données de la base de données et de l'offre de stockage. 
* **Type** : bloquant
* **Statuts** :
	* OK : Succès de la création du rapport d'audit de cohérence (EVIDENCE_AUDIT_FINALIZE.OK=Succès de la création du rapport de l'audit de cohérence)
	* KO : Echec de la création du rapport d'audit de cohérence (EVIDENCE_AUDIT_FINALIZE.KO=Echec de la création du rapport de l'audit de cohérence)
	* FATAL : une erreur technique est survenue lors de la création du rapport d'audit de cohérence) (EVIDENCE_AUDIT_FINALIZE.FATAL=une erreur technique est survenue lors de la création du rapport d'audit de cohérence)



.. figure:: images/workflow_audit_file_consistency.png
	:align: center


