Workflow de création d'un journal sécurisé
##########################################

Introduction
============

Ce chapitre décrit le processus (workflow) de sécurisation des journaux mis en place dans la solution logicielle Vitam.

Celui-ci est défini dans le fichier LogbookAdministration.java.

Processus de sécurisation des journaux (vision métier)
======================================================

Le processus de sécurisation des journaux consiste en la création d'une fichier zip contenant l'ensemble des journaux d'opérations à sécuriser, ainsi que le tampon d'horodatage calculé à partir de l'arbre de Merkle de la liste de ces mêmes journaux.

Ce fichier zip est ensuite enregistré sur les offres de stockage.

Sécurisation des journaux (STP_OP_SECURISATION)
===============================================

OP_SECURISATION_TIMESTAMP (LogbookAdministration.java)
------------------------------------------------------

* Règle : calcule le tampon d'horodatage à partir de la racine de l'arbre de merkle consitué de la liste des journaux qui sont en train d'être sécurisés.
* Type : bloquant
* Status :
	* OK : le tampon d'horodatage est calculé (OP_SECURISATION_TIMESTAMP.OK=Succès de création du tampon d'horodatage de l'ensemble des journaux)
	* FATAL : service d'horodatage indisponible (OP_SECURISATION_TIMESTAMP.FATAL=Erreur fatale lors de création du tampon  d'horodatage de l''ensemble des journaux)

OP_SECURISATION_STORAGE (LogbookAdministration.java)
------------------------------------------------------

* Règle : enregistre les journaux sécurisés sur les offres de stockage.
* Type : bloquant
* Status :
	* OK : le journal sécurisé est enregistré sur les offres de stockage (OP_SECURISATION_STORAGE.OK=Succès du stockage des journaux)
	* FATAL : offres de stockage indisponibles (OP_SECURISATION_STORAGE.FATAL=Erreur fatale du stockage des journaux)

.. figure:: images/workflow_op_traceability.png
	:align: center
