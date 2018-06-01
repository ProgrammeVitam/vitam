Workflow de création d'un journal sécurisé des groupes d'objets et des unités archivistiques 
############################################################################################

Introduction
============

Cette section décrit le processus (workflow) de sécurisation des journaux mis en place dans la solution logicielle Vitam pour les groupes d'objets et les unités archivistiques.

Celui-ci est défini dans le fichier "LogbookAdministration.java" ( situé ici: sources/logbook/logbook-administration/src/main/java/fr/gouv/vitam/logbook/administration/core/)

Processus de sécurisation des journaux (vision métier)
======================================================

Le processus de sécurisation des journaux consiste en la création d'un fichier .zip contenant l'ensemble des journaux à sécuriser, ainsi que le tampon d'horodatage calculé à partir de l'arbre de Merkle de la liste de ces mêmes journaux. Les journaux concernés par cette sécurisation sont le journal des opérations et le journal des écritures.

Ce fichier zip est ensuite enregistré sur les offres de stockage, en fonction de la stratégie de stockage.

Sécurisation du journal des opérations (STP_OP_SECURISATION)
============================================================

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : le journal des opérations a été sécurisé (STP_OP_SECURISATION.OK = Succès du processus de sécurisation du journal des opérations)


  + KO : pas de cas KO

  + FATAL : une erreur technique est survenue lors de la sécurisation du journal des opérations (STP_OP_SECURISATION.FATAL = Erreur fatale lors du processus de sécurisation du journal des opérations)


OP_SECURISATION_TIMESTAMP (LogbookAdministration.java)
------------------------------------------------------

* **Règle** : calcul du tampon d'horodatage à partir de la racine de l'arbre de Merkle consitué de la liste des journaux qui sont en train d'être sécurisés.
* **Type** : bloquant
* **Status** :
	* OK : le tampon d'horodatage est calculé (OP_SECURISATION_TIMESTAMP.OK=Succès de la création du tampon d'horodatage de l'ensemble des journaux)
	* FATAL : une erreur technique est survenue lors de l'horodatage (OP_SECURISATION_TIMESTAMP.FATAL=Erreur fatale lors de la création du tampon d'horodatage de l'ensemble des journaux)

OP_SECURISATION_STORAGE (LogbookAdministration.java)
------------------------------------------------------

* **Règle** : écriture des journaux sécurisés sur les offres de stockage, en fonction de la stratégie de stockage.
* **Type** : bloquant
* **Status** :
	* OK : le journal sécurisé est écrit sur les offres de stockage (OP_SECURISATION_STORAGE.OK=Succès de l'enregistrement des journaux sur les offres de stockage)
	* FATAL : une erreur technique est survenue lors de l'écriture du journal sécurisé (OP_SECURISATION_STORAGE.FATAL=Erreur fatale lors de l'enregistrement des journaux sur les offres de stockage)

D'une façon synthétique, le workflow est décrit de cette façon.

.. figure:: images/workflow_op_traceability.png
	:align: center
