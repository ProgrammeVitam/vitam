Workflow de création d'un journal sécurisé
##########################################

Introduction
============

Cette section décrit le processus (workflow) de sécurisation des journaux mis en place dans la solution logicielle Vitam.

Celui-ci est défini dans le fichier "LogbookAdministration.java" ( situé ici: sources/logbook/logbook-administration/src/main/java/fr/gouv/vitam/logbook/administration/core/)

Processus de sécurisation des journaux (vision métier)
======================================================

Le processus de sécurisation des journaux consiste en la création d'un fichier .zip contenant l'ensemble des journaux à sécuriser, ainsi que le tampon d'horodatage calculé à partir de l'arbre de Merkle de la liste de ces mêmes journaux. Les journaux concernés par cette sécurisation sont le journal des opérations et le journal des écritures.

Ce fichier zip est ensuite enregistré sur les offres de stockage, en fonction de la stratégie de stockage.

Sécurisation des journaux (STP_OP_SECURISATION)
===============================================

OP_SECURISATION_TIMESTAMP (LogbookAdministration.java)
------------------------------------------------------

* **Règle** : calcul du tampon d'horodatage à partir de la racine de l'arbre de merkle consitué de la liste des journaux qui sont en train d'être sécurisés.
* **Type** : bloquant
* **Status** :
	* OK : le tampon d'horodatage est calculé (OP_SECURISATION_TIMESTAMP.OK=Succès de création du tampon d'horodatage de l'ensemble des journaux)
	* FATAL : une erreur technique est survenue lors de l'horodatage (OP_SECURISATION_TIMESTAMP.FATAL=Erreur fatale lors de création du tampon  d'horodatage de l'ensemble des journaux)

OP_SECURISATION_STORAGE (LogbookAdministration.java)
------------------------------------------------------

* **Règle** : écriture des journaux sécurisés sur les offres de stockage, en fonction de la stratégie de stockage.
* **Type** : bloquant
* **Status** :
	* OK : le journal sécurisé est écrit sur les offres de stockage (OP_SECURISATION_STORAGE.OK=Succès du stockage des journaux)
	* FATAL : une erreur technique est survenue lors de l'écriture du journal sécurisé (OP_SECURISATION_STORAGE.FATAL=Erreur fatale du stockage des journaux)

D'une façon synthétique, le workflow est décrit de cette façon.

.. figure:: images/workflow_op_traceability.png
	:align: center
