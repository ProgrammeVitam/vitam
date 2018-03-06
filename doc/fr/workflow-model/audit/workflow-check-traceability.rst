Workflow de contrôle d'intégrité d'un journal sécurisé
######################################################

Introduction
============

Cette section décrit le processus (workflow) de contrôle d'intégrité d'un journal sécurisé mis en place dans la solution logicielle Vitam.

Celui-ci est défini dans le fichier “DefaultCheckTraceability.json” (situé ici : sources/processing/processing-management/src/main/resources/workflows).

Processus de contrôle d'intégrité d'un journal sécurisé (vision métier)
=======================================================================

Le processus de contrôle d'intégrité débute lorsqu'un identifiant d'opération de sécurisation des journaux d'opération, des journaux de cycles de vie, ou du journal des écritures est soumis au service de contrôle d'intégrité des journaux sécurisés. Le service permet de récupérer le journal sécurisé, d'en extraire son contenu et de valider que son contenu n'a pas été altéré.

Pour cela, il calcule un arbre de Merkle à partir des journaux d'opérations que contient le journal sécurisé, puis en calcule un second à partir des journaux correspondants disponibles dans la solution logicielle Vitam. Une comparaison est ensuite effectuée entre ces deux arbres et celui contenu dans les métadonnées du journal sécurisé.

Ensuite, dans une dernière étape, le tampon d'horodatage est vérifié et validé.

Processus de préparation de la  vérification des journaux sécurisés (STP_PREPARE_TRACEABILITY_CHECK)
====================================================================================================

PREPARE_TRACEABILITY_CHECK (PrepareTraceabilityCheckProcessActionHandler.java)
------------------------------------------------------------------------------

* **Règle** : vérification que l'opération donnée en entrée est de type TRACEABILITY. Récupération du zip associé à cette opération et extraction de son contenu.
* **Type** : bloquant
* **Statuts** :
	* OK : l'opération donnée en entrée est une opération de type TRACEABILITY, le zip a été trouvé et son contenu extrait (PREPARE_TRACEABILITY_CHECK.OK=Succès de la préparation de la vérification des journaux sécurisés)
	* KO : l'opération donnée en entrée n'est pas une opération de type TRACEABILITY (PREPARE_TRACEABILITY_CHECK.KO=Échec de la préparation de la vérification des journaux sécurisés)
	* FATAL : une erreur technique est survenue lors de la préparation du processus de vérification (PREPARE_TRACEABILITY_CHECK.FATAL=Erreur fatale lors de la préparation de la vérification des journaux sécurisés)

Processus de vérification de l'arbre de Merkle (STP_MERKLE_TREE)
================================================================

CHECK_MERKLE_TREE (VerifyMerkleTreeActionHandler.java)
------------------------------------------------------

* **Règle** : Recalcul de l'arbre de Merkle des journaux contenus dans le journal sécurisé, calcul d'un autre arbre à partir des journaux indexés correspondants et vérification que tous deux correspondent à celui stocké dans les métadonnées du journal sécurisé
* **Type** : bloquant
* Statuts :
	* OK : les arbres de Merkle correspondent (CHECK_MERKLE_TREE.OK=Succès de la vérification de l'arbre de MERKLE)
	* KO : les arbres de Merkle ne correspondent pas (CHECK_Merkle_TREE.KO=Échec de la vérification de l'arbre de MERKLE)
	* FATAL : une erreur technique est survenue lors de la vérification des arbres de Merkle (CHECK_MERKLE_TREE.FATAL=Erreur fatale lors de la vérification de l'arbre de MERKLE)

**La tâche contient les traitements suivants**

* Comparaison de l'arbre de MERKLE avec le Hash enregistré
	* **Règle** : Vérification que l'arbre de Merkle calculé à partir des journaux contenus dans le journal sécurisé est identique à celui stocké dans les métadonnées du journal sécurisé
	* **Type** : bloquant
	* **Statuts** :

		* OK : l'arbre de Merkle des journaux contenus dans le journal sécurisé correspond à celui stocké dans les métadonnées du journal sécurisé (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_SAVED_HASH.OK=Succès de la comparaison de l'arbre de MERKLE avec le Hash enregistré)
		* KO : l'arbre de Merkle des journaux contenus dans le journal sécurisé ne correspond pas à celui stocké dans les métadonnées du journal sécurisé (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_SAVED_HASH.KO=Échec de la comparaison de l'arbre de MERKLE avec le Hash enregistré)
		* FATAL : une erreur technique est survenue lors de la comparaison de l'arbre de MERKLE avec le Hash enregistré (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_SAVED_HASH.FATAL=Erreur fatale lors de la comparaison de l'arbre de MERKLE avec le Hash enregistré)

* Comparaison de l'arbre de MERKLE avec le Hash indexé

	* **Règle** : Vérification que l'arbre de Merkle calculé à partir des journaux indexés est identique à celui stocké dans les métadonnées du journal sécurisé
	* **Type** : bloquant
	* **Statuts** :

		* OK : l'arbre de Merkle des journaux indexés correspond à celui stocké dans les métadonnées du journal sécurisé (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_INDEXED_HASH.OK=Succès de la comparaison de l'arbre de MERKLE avec le Hash indexé)
		* KO : l'arbre de Merkle des journaux indexés ne correspond pas à celui stocké dans les métadonnées du journal sécurisé (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_INDEXED_HASH.KO=Échec de la comparaison de l'arbre de MERKLE avec le Hash indexé)
		* FATAL : une erreur technique est survenue lors de la comparaison l'arbre de Merkle des journaux indexés et de celui stocké dans les métadonnées du journal sécurisé (CHECK_MERKLE_TREE.COMPARE_MERKLE_HASH_WITH_INDEXED_HASH.FATAL=Erreur fatale lors de la comparaison de l'arbre de MERKLE avec le Hash indexé)

PRocesus de vérification de l'horodatage (STP_VERIFY_STAMP)
===========================================================

VERIFY_TIMESTAMP (VerifyTimeStampActionHandler.java)
----------------------------------------------------

* **Règle** : Vérification et validation du tampon d'horodatage.
* **Type** : bloquant
* **Statuts** :

    * OK : le tampon d'horadatage est correct (VERIFY_TIMESTAMP.OK=Succès de la vérification de l'horodatage)
    * KO : le tampon d'horadatage est incorrect (VERIFY_TIMESTAMP.KO=Échec de la vérification de l'horodatage)
    * FATAL : une erreur technique est survenue lors de la vérification du tampon d'horodatage (VERIFY_TIMESTAMP.FATAL=Erreur fatale lors de la vérification de l'horodatage)

**La tâche contient les traitements suivants**

* Comparaison du tampon du fichier (token.tsp) par rapport au tampon enregistré dans le logbook (COMPARE_TOKEN_TIMESTAMP)

	* **Règle** : Vérification que le tampon enregistré dans la collection logbookOperation est le même que celui présent dans le fichier zip généré
	* **Type** : bloquant
	* **Status** :

		* OK : les tampons sont identiques (VERIFY_TIMESTAMP.COMPARE_TOKEN_TIMESTAMP.OK=Succès de la comparaison des tampons d'horodatage)
		* KO : les tampons sont différents (VERIFY_TIMESTAMP.COMPARE_TOKEN_TIMESTAMP.KO=Échec de la comparaison des tampons d'horadatage)
		* FATAL : Erreur technique lors de la vérification des tampons (VERIFY_TIMESTAMP.COMPARE_TOKEN_TIMESTAMP.FATAL=Erreur fatale lors de la comparaison des tampons d'horadatage)

* Validation du tampon d'horodatage (VALIDATE_TOKEN_TIMESTAMP)

	* **Règle** : Vérification cryptographique du tampon et vérification de la chaîne de certification
	* **Type** : bloquant
	* **Status** :
		* OK : le tampon est validé (VERIFY_TIMESTAMP.VALIDATE_TOKEN_TIMESTAMP.OK=Succès de la validation du tampon d'horodatage)
		* KO : le tampon est invalidé (VERIFY_TIMESTAMP.VALIDATE_TOKEN_TIMESTAMP.KO=Échec de la validation du tampon d'horodatage)

D'une façon synthétique, le workflow est décrit de cette façon :

.. figure:: images/workflow_traceability.png
	:align: center
