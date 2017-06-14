INGEST : Workflow d'entrée d'un plan de classement
##################################################

Introduction
============

Cette section décrit le processus (workflow-plan) d'entrée d'un plan de classement mis en place dans Vitam. La structure d'un plan de classement diffère de celle d'un SIP dans le fait qu'un plan ne doit pas avoir d'objets et n'utilise pas de profil. Il s'agit plus simplement d'une arborescence représenté par des unités archivistiques. Ce processus partage donc certaines étapes avec celui du versement d'un SIP classique, en ignorent certaines et rajoute des tâches additionnelles.

Le workflow actuel mis en place dans la solution logicielle est défini dans le fichier "DefaultFilingSchemeWorkflow.json".

Processus d'entrée d'un plan de classement (vision métier)
==========================================================

Le processus d'entrée d'un plan est identique au workflow d'entrée d'un SIP:
- Un workflow-plan est un processus composé d’étapes elles-mêmes composées d’une liste d’actions

- Chaque étape et chaque action peuvent avoir les statuts suivants : OK, KO, Warning, FATAL

- Chaque action peut avoir les modèles d'éxécutions : Bloquant ou Non bloquant

Les sections suivantes présentent également sa structure et des actions non encore abordées pour celui de l'entrée d'un SIP.

Le processus d'entrée débute lors du lancement du chargement d'un Submission Information Package dans Vitam. De plus, toutes les étapes et actions sont journalisées dans le journal des opérations.


Traitement additionnel dans la tâche CHECK_DATAOBJECTPACKAGE
------------------------------------------------------------

* Vérification de la non existence d'objets (CHECK_NO_OBJECT)

  + **Règle** : vérifier qu'il n'y a pas d'objet numérique dans le manifest.xml du plan

  + **Statuts** :
	- OK : s'il n'y a pas d'objets numérique dans le manifeste
	- KO : s'il y a des objets numériques dans le manifeste

* Vérification de la cohérence du bordereau (CHECK_MANIFEST)

.. image:: images/Workflow_FilingScheme.jpg
    :align: center

Diagramme d'activité du workflow du plan de classement

- **Step 1** - STP_INGEST_CONTROL_SIP : Check SIP  / distribution sur REF GUID/SIP/manifest.xml

  * CHECK_SEDA (CheckSedaActionHandler.java) :

    + Test de l'existence du manifest.xml,

    + Test de l'existence d'un fichier unique à la racine du SIP

    + Test de l'existence d'un dossier unique à la racine, nommé "Content" (insensible à la casse)

    + Validation XSD du manifeste,

    + Validation de la structure du manifeste par rapport au schema par défaut fourni avec le standard SEDA v. 2.0.

  * CHECK_HEADER (CheckHeaderActionHandler.java)

    + Test de l'existence du service producteur dans le bordereau

    + Contient CHECK_CONTRACT_INGEST (CheckIngestContractActionHandler.java) :

      - Recherche le nom de contrat d'entrée dans le SIP,

      - Vérification de la validité de contrat par rapport la référentiel de contrats importée dans le système

  * CHECK_DATAOBJECTPACKAGE (CheckDataObjectPackageActionHandler.java)

    + Contient CHECK_NO_OBJECT

      - Vérification de la non existence d'objets

    + Contient CHECK_MANIFEST_OBJECTNUMBER (CheckObjectsNumberActionHandler.java) :

      - Comptage des objets (BinaryDataObject) dans le manifest.xml en s'assurant de l'absence de doublon, que le nombre d'objets binaires reçus est strictement égal au nombre d'objets attendus

      - Création de la liste des objets binaires dans le workspace GUID/SIP/content/,

      - Comparaison du nombre et des URI des objets binaires contenus dans le SIP avec ceux définis dans le manifeste.


    * Contient CHECK_MANIFEST (ExtractSedaActionHandler.java) :

      - Extraction des ArchiveUnits, des BinaryDataObject,

      - Création des journaux de cycle de vie des ArchiveUnits et des ObjectGroup,

      - Vérification de la présence de cycles dans les arboresences des Units,

      - Création de l'arbre d'ordre d'indexation,

      - Extraction des métadonnées contenues dans le bloc ManagementMetadata du manifeste pour le calcul des règles de gestion,
        
      - Vérification du GUID de la structure de rattachement,
        
      - Vérification de la cohérence entre l'unit rattachée et l'unit de rattachement.


- **Step 2** - STP_UNIT_CHECK_AND_PROCESS : Contrôle et traitements des units / distribution sur LIST GUID

  * UNITS_RULES_COMPUTE (UnitsRulesComputePlugin.java) :

    + vérification de l'existence de la règle dans le référentiel des règles de gestion

  * calcul des échéances associées à chaque ArchiveUnit.

- **Step 3** - STP_UNIT_STORING : Rangement des unités archivistique / distribution sur LIST GUID/Units

  * UNIT_METADATA_INDEXATION (IndexUnitActionPlugin.java) :

    + Transformation sous la forme Json des ArchiveUnits et intégration du GUID Unit et du GUID ObjectGroup,

    + Enregistrement en base des métadonnées des ArchiveUnits.

- **Step 4** - STP_ACCESSION_REGISTRATION : Alimentation du registre des fonds

  * ACCESSION_REGISTRATION (AccessionRegisterActionHandler.java) :

    + Création/Mise à jour et enregistrement des collections AccessionRegisterDetail et AccessionRegisterSummary concernant les archives prises en compte, par service producteur.

- **Step 5 et finale** - STP_INGEST_FINALISATION : Finalisation de l'entrée. Cette étape est obligatoire et sera toujours exécutée, en dernière position.

  * ATR_NOTIFICATION (TransferNotificationActionHandler.java) :

    + Génération de l'ArchiveTransferReply.xml (peu importe le statut du processus d'entrée, l'ArchiveTransferReply est obligatoirement généré),

    + Stockage de l'ArchiveTransferReply dans les offres de stockage.
