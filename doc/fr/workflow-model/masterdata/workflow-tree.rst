Workflow d'import d'un arbre de positionnement
##############################################

Introduction
============

Cette section décrit le processus (workflow-tree) permettant d'importer un arbre de positionnement dans la solution logicielle Vitam. La structure d'un arbre de positionnement diffère de celle d'un SIP en plusieurs points.

Un arbre ne doit pas avoir d'objet, ni de service producteur, ni de contrat. Il s'agit plus simplement d'une arborescence représentée par des unités archivistiques. Ce processus partage donc certaines étapes avec celui du transfert d'un SIP classique, en ignore certaines et rajoute des tâches additionnelles.

Le workflow mis en place dans la solution logicielle Vitam est défini dans le fichier "DefaultHoldingSchemeWorkflow.json". Ce fichier est disponible à sources/processing/processing-management/src/main/resources/workflows.

Processus d'import d'un arbre (vision métier)
=============================================

Le processus d'import d'un arbre est identique au workflow d'entrée d'un SIP. Il débute lors du lancement du téléchargement de l'arbre dans la solution logicielle Vitam. De plus, toutes les étapes et actions sont journalisées dans le journal des opérations.

Les étapes et actions associées ci-dessous décrivent le processus d'import (clé et description de la clé associée dans le journal des opérations), non encore abordées dans la description de l'entrée d'un SIP.


Traitement additionnel dans la tâche CHECK_DATAOBJECTPACKAGE
------------------------------------------------------------

* Vérification de la non existence d'objets (CHECK_NO_OBJECT)

  + **Règle** : vérification qu'il n'y a pas d'objet numérique dans le manifest.xml du plan

  + **Statuts** :

    - OK : aucun objet numérique n'est présent dans le manifeste (CHECK_DATAOBJECTPACKAGE.CHECK_NO_OBJECT.OK=Succès de la vérification de l'absence d'objet)

    - KO : des objets numériques sont présent dans le manifeste (CHECK_DATAOBJECTPACKAGE.CHECK_NO_OBJECT.KO=Échec de la vérification de l'absence  d''objet : objet(s) trouvé(s))

    - FATAL : une erreur technique est survenue lors de la vérification de la non existence d'objet numérique (CHECK_DATAOBJECTPACKAGE.CHECK_NO_OBJECT.FATAL=Erreur fatale lors de la vérification de l'absence d''objet)


D'une façon synthétique, le workflow est décrit de cette façon :

  .. figure:: images/Workflow_HoldingScheme.jpg
    :align: center

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

    + Contient CHECK_NO_OBJECT (CheckNoObjectsActionHandler.java)

      - Vérification de l'absence d'objets

    + Contient CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

      - Comptage des objets (BinaryDataObject) dans le manifest.xml en s'assurant de l'absence de doublon, que le nombre d'objets binaires reçus est strictement égal au nombre d'objets attendus

      - Création de la liste des objets binaires dans le workspace GUID/SIP/content/,

      - Comparaison du nombre et des URI des objets binaires contenus dans le SIP avec ceux définis dans le manifeste.

    * Contient CHECK_MANIFEST (ExtractSedaActionHandler.java) :

      - Extraction des unités archivistiques, des BinaryDataObject, des PhysicalDataObject,

      - Création des journaux du cycle de vie des unités archivistiques et des groupes d'objets,

      - Vérification de la présence de cycles dans les arboresences des Units,

      - Création de l'arbre d'ordre d'indexation,

      - Extraction des métadonnées contenues dans le bloc ManagementMetadata du manifeste pour le calcul des règles de gestion,

      - Vérification du GUID de la structure de rattachement

      - Vérification de la cohérence entre l'unité archivistique rattachée et l'unité archivistique de rattachement.

      - Vérification des problèmes d'encodage dans le manifeste

      - Vérification que les objets ayant un groupe d'objets ne référencent pas directement les unités archivistiques

- **Step 2** - STP_UNIT_METADATA : Indexation des unités archivistique

  * UNIT_METADATA_INDEXATION (IndexUnitActionPlugin.java) :

    + Transformation sous la forme Json des unités archivistiques et intégration du GUID Unit et du GUID des groupes d'objets

- **Step 3** - STP_UNIT_STORING : Rangement des unités archivistique / distribution sur LIST GUID/Units

  * COMMIT_LIFE_CYCLE_UNIT (CommitLifeCycleUnitActionHandler.java)

    + Sécurisation en base des journaux du cycle de vie des unités archivistiques

  * UNIT_METADATA_STORAGE (StoreMetaDataUnitActionPlugin.java.java) :

    + Sauvegarde sur les offres de stockage des métadonnées des unités archivistiques.

  * COMMIT_LIFE_CYCLE_UNIT (CommitLifeCycleUnitActionHandler.java)

    + Sécurisation en base des journaux du cycle de vie des unités archivistiques

- **Step 4 et finale** - STP_INGEST_FINALISATION : Finalisation de l'entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :

    + Génération de l'ArchiveTransferReply.xml (peu importe le statut du processus d'entrée, l'ArchiveTransferReply est obligatoirement généré),

    + Écriture de l'ArchiveTransferReply sur les offres de stockage.
