Workflow de l'audit de l'existence et de l'intégrité des fichiers
#################################################################

Introduction
============

Cette section décrit le processus (workflow) d'audit de l'existence des fichiers mis en place dans la solution logicielle Vitam.

Celui-ci est défini dans le fichier "DefaultAuditWorkflow.json” (situé ici : sources/processing/processing-management/src/main/resources/workflows).

Processus d'audit d'existence des fichiers (vision métier)
==========================================================

Le processus d'audit prend comme point d'entrée l'identifiant d'un tenant ou l'identifiant d'un service producteur. Il est possible de lancer un audit de l'existence des fichiers uniquement, ou de lancer un audit vérifiant l'existence et l'intégrité des fichiers en même temps.

Pour chaque objet du tenant choisi ou chaque objet appartenant au service producteur, l'audit va vérifier :

	- Que la liste des offres de stockage définie dans le groupe d'objets est bien la même que celle définie dans la stratégie de stockage
	- Que tous les fichiers correspondant aux objets existent sur les offres déclarées, dans un nombre de copie spécifié via la stratégie de stockage

Processus d'audit d'intégrité des fichiers (vision métier)
=======================================================================

Si l'audit d'intégrité des objets est lancé, le processus va également vérifier que les empreintes des objets stockés en base de données sont bien les mêmes que les empreintes fournies par les espaces de stockage, alors recalculées à la demande de l'audit.

Dans une première étape technique, il prépare la liste des groupes d'objets à auditer afin de paralléliser la tâche.
Dans un second temps, il effectue la vérification elle même.
Enfin, il sécurise les journaux de cycle de vie qui ont été modifiés.


Processus de préparation de l'audit (STP_PREPARE_AUDIT)
=======================================================

Création de la liste des groupes d'objets LIST_OBJECTGROUP_ID (PrepareAuditActionHandler.java)
------------------------------------------------------------------------------------------------

* **Règle** : Création de la liste des groupes d'objets à auditer
* **Type** : bloquant
* **Statuts** :
	- OK : la liste a été créée avec succès (LIST_OBJECTGROUP_ID.OK=Succès de la création de la liste des groupes d'objets à auditer)
	- FATAL : Une erreur fatale est survenue lors de la création de la liste (LIST_OBJECTGROUP_ID.FATAL=Erreur fatale lors de la création de la liste des groupes d'objets à auditer)

Processus d'éxecution de l'audit (STP_AUDIT)
============================================

Audit de la vérification des objets AUDIT_CHECK_OBJECT (AuditCheckObjectPlugin.java)
--------------------------------------------------------------------------------------

* **Règle** : Tâche technique pour organiser et lancer l'action d'audit de la vérification des objets. A la fin de l'audit de chaque groupe d'objets en KO, la mise à jour en base de son journal du cycle de vie est faite.
* **Type** : bloquant
* **Statuts** :
	- OK : l'action d'audit de la vérification des objets s'est terminée en OK (AUDIT_CHECK_OBJECT.OK=Succès de l'audit de la vérification des objets)
	- KO : l'action d'audit de la vérification des objets s'est terminée en KO (AUDIT_CHECK_OBJECT.KO=Échec de l'audit de la vérification des objets : au moins un objet demandé n'existe pas ou des stratégies de stockage sont incohérentes avec les offres déclarées)
	- FATAL : une erreur fatale est survenue lors du lancement de l'action d'audit (AUDIT_CHECK_OBJECT.FATAL=Erreur fatale lors de l'audit de la vérification des objets)

Audit de l'existence des objets AUDIT_FILE_EXISTING (CheckExistenceObjectPlugin.java)
-------------------------------------------------------------------------------------

* **Règle** : Audit de l'existence des objets permettant la vérification de chaque groupe d'objets audités
	* La stratégie de stockage du groupe d'objets est conforme à celle du moteur de stockage
	* Les fichiers correspondant aux objets, déclarés dans le groupe d'objets, existent bien sous le même nom dans les offres de stockage
* **Type** : bloquant
* **Statuts** :
	- OK : tous les objets de tous les groupes d'objets audités existent bien sur les offres de stockage (AUDIT_CHECK_OBJECT.AUDIT_FILE_EXISTING.OK=Succès de l'audit de l'existence de fichiers )
	- KO : au moins un objet n'existe pas pour au moins un groupe objets (AUDIT_CHECK_OBJECT.AUDIT_FILE_EXISTING.KO=Echec de l'audit de l'existence de fichiers)
	- Warning : il n'y a aucun objet à auditer (cas par exemple d'un producteur sans objets) (AUDIT_CHECK_OBJECT.AUDIT_FILE_EXISTING.WARNING=Avertissement lors de l'audit de l'existence des objets : au moins un groupe d'objets n'a pas d'objet binaire à vérifier)
	- FATAL : une erreur fatale est survenue lors de l'audit de l'existence des objets (AUDIT_CHECK_OBJECT.AUDIT_FILE_EXISTING.FATAL=Erreur fatale lors de l'audit de l'existence des objets)

Audit de l'intégrité des objets AUDIT_CHECK_OBJECT.AUDIT_FILE_INTEGRITY (CheckIntegrityObjectPlugin.java)
-----------------------------------------------------------------------------------------------------------

* **Règle** : Audit de l'intégrité des objets permettant la vérification de chaque groupe d'objets audités
	* L'objet existe bien (voir "AUDIT_FILE_EXISTING")
	* L'empreinte de l'objet enregistrée en base de données est la même pour chaque objet que celle obtenue par l'offre de stockage, recalculée à la demande de l'audit
* **Type** : bloquant
* **Statuts** :
	- OK : tous les objets de tous les groupes d'objet audités existent bien sur les offres de stockage et leurs empreintes sont identiques entre celles enregistrées en base de données et celles recalculées par les offres de stockage (AUDIT_CHECK_OBJECT.AUDIT_FILE_INTEGRITY.OK=Succès de l'audit de l'existence et de l'intégrité des objets)
	- KO : au moins un objet n'existe pas pour au moins un objet (AUDIT_CHECK_OBJECT.AUDIT_FILE_INTEGRITY.KO=Échec de l'audit de l'existence et de l'intégrité des objets)
	- Warning : il n'y a aucun objet à auditer (cas par exemple d'un producteur sans objets) (AUDIT_CHECK_OBJECT.AUDIT_FILE_INTEGRITY.WARNING=Avertissement lors de l'existence et de l'intégrité des objets)
	- FATAL : une erreur fatale est survenue lors de l'audit de l'existence des fichiers (AUDIT_CHECK_OBJECT.AUDIT_FILE_INTEGRITY.FATAL=Erreur fatale lors de l'existence et de l'intégrité des objets)

.. figure:: images/workflow_audit_file_existing.png
	:align: center

Processus de finalisation de l'audit (STP_FINALISE_AUDIT)
=========================================================

Notification de la fin d'audit REPORT_AUDIT (GenerateAuditReportActionHandler.java)
-------------------------------------------------------------------------------------

* **Règle** : génération du rapport d'audit
* **Type** : bloquant
* **Statuts** :
	- OK : le rapport a été créé avec succès (REPORT_AUDIT.OK=Succès de la notification de la fin de l'audit)
	- FATAL : Une erreur fatale est survenue lors de la création du rapport d'audit (REPORT_AUDIT.OK.FATAL=Erreur fatale lors de la notification de la fin de l'audit)
