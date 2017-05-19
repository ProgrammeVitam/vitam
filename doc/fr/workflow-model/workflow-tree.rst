Workflow d'entrée d'un arbre de positionnement
##############################################

Introduction
============

Ce document décrit le processus (workflow-tree) d'entrée d'un arbre de positionement mis en place dans la solution logicielle Vitam,
l'implémentation mise en place pour celui-ci dans la version V1 de cette solution.

Processus d'entrée d'un arbre (vision métier)
=============================================

Le processus d'entrée d'un arbre est identique au workflow d'entrée d'un SIP:

- Un workflow-tree est un processus composé d’étapes elles-mêmes composées d’une liste d’actions

- Chaque étape, chaque action peut avoir les statuts suivants : OK, KO, Warning, FATAL

- Chaque action peut avoir les modèles d'éxécutions : Bloquant ou Non bloquant

Ce workflow se compose des différentes étapes et actions propres à celui d'un upload de SIP.
Les sections suivantes présentent également sa structure et des actions non encore abordées pour celui de l'entrée d'un SIP.

Le processus d'entrée débute lors du lancement du chargement d'un Submission Information Package dans la solution Vitam. De plus, toutes les étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent le processus d'entrée (clé et description de la clé associée dans le journal des opérations) tel qu'implémenté dans la version bêta de la solution logicielle :


Traitement additionnel dans la tâche CHECK_DATAOBJECTPACKAGE
------------------------------------------------------------

* Vérification de la non existence d'objets (CHECK_NO_OBJECT)

  + **Règle** : vérifier s'il y a d'un objet numérique dans le manifest.xml du plan

  + **Statuts** :
	- OK : s'il y a des objects numériques dans le manifest
	- KO : s'il n'y a pas d'objects numérique dans le manifest

  D'une façon synthétique, le workflow est décrit de cette façon :

  .. figure:: images/Workflow_HoldingScheme.jpg
    :align: center
    :height: 22 cm
    :target: images/Workflow_HoldingScheme.jpg

    Diagramme d'activité du workflow de l'arbre de positionnement

- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  * CHECK_SEDA (CheckSedaActionHandler.java) :

    + Test de l'existence du manifest.xml,

    + Test de l'existence d'un fichier unique à la racine du SIP

    + Test de l'existence d'un dossier unique à la racine, nommé "Content" (insensible à la casse)

    + Validation XSD du manifeste,

    + Validation de la structure du manifeste par rapport au schema par défaut fourni avec le standard SEDA v. 2.0.

  * CHECK_HEADER (CheckHeaderActionHandler.java)

    + Test de l'existence du service producteur dans le bordereau

  * CHECK_DATAOBJECTPACKAGE (CheckDataObjectPackageActionHandler.java)

    + Contient CHECK_NO_OBJECT

      - Vérification de la non existence d'objets

    + Contient CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

      - Comptage des objets (BinaryDataObject) dans le manifest.xml en s'assurant de l'absence de doublon, que le nombre d'objets binaires reçus est strictement égal au nombre d'objets attendus

      - Création de la liste des objets dans le workspace GUID/SIP/content/,

      - Comparaison du nombre et des URI des objets binaires contenus dans le SIP avec ceux définis dans le manifeste.

    * Contient CHECK_MANIFEST (ExtractSedaActionHandler.java) :

      - Extraction des ArchiveUnits, des BinaryDataObject, des PhysicalDataObject,

      - Création des journaux de cycle de vie des ArchiveUnits et des ObjectGroup,

      - Vérification de la présence de cycles dans les arboresences des Units,

      - Création de l'arbre d'ordre d'indexation,

      - Extraction des métadonnées contenues dans le bloc ManagementMetadata du manifeste pour le calcul des règles de gestion.


- **Step 2** - STP_UNIT_STORING : Rangement des unités archivistique / distribution sur LIST GUID/Units

  * UNIT_METADATA_INDEXATION (IndexUnitActionPlugin.java) :

    + Transformation sous la forme Json des ArchiveUnits et intégration du GUID Unit et du GUID ObjectGroup,

    + Enregistrement en base des métadonnées des ArchiveUnits.

- **Step 3 et finale** - STP_INGEST_FINALISATION : Finalisation de l'entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :

    + Génération de l'ArchiveTransferReply.xml (peu importe le statut du processus d'entrée, l'ArchiveTransferReply est obligatoirement généré),

    + Stockage de l'ArchiveTransferReply dans les offres de stockage.



Structure du Workflow d'un arbre (Implémenté en V1)
========================================

Le workflow actuel mis en place dans la solution Vitam est défini dans le fichier "DefaultHoldingSchemeWorkflow.json".
Il décrit le processus d'entrée pour téléchager un arbre, indexer les métadonnées et stocker les objets contenus dans l'arbre.
