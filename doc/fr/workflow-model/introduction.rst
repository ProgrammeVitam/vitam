Introduction
############

Avertissement
=============

Cette documentation est un travail en cours. Elle est susceptible de changer de manière conséquente.

Objectif du document
====================

Ce document a pour objectif de présenter les différents processus employés par la solution logicielle Vitam.
Il est destiné aux administrateurs aussi bien techniques que fonctionnels, aux archivistes souhaitant une connaissance plus avancée du logiciel et aux développeurs.

Il explicite chaque processus (appellés également "workflow"), et pour chacun leurs tâches et traitements.

Ce document comprend également du matériel additionnel pour faciliter la compréhension des processus comme des fiches récaputilatives et des schémas. Il explique également la manière dont est formée la structure des fichiers de workflow.

Description d'un processus
=========

Un workflow est un processus composé d’étapes (macro-workflow), elles-mêmes composées d’une liste d’actions à exécuter de manière séquentielle, une seule fois ou répétées sur une liste d’éléments (micro-workflow).

Pour chacun de ces éléments, le document décrit :

- La règle générale qui s'applique à cet élément
- Les statuts de sortie possibles (OK,KO...), avec les raisons de ces sorties et les clés associées
- Des informations complémentaires, selon le type d'élément traité

Chaque étape, chaque actions peuvent avoir les statuts suivants :

- OK : le traitement associé s'est passé correctement. Le workflow continue.
- Warning : le traitement associé a généré un avertissement (Par exemple le format de l'objet est mal déclaré dans le bordereau de versement). Le workflow continue.
- KO : le traitement associé a généré une erreur métier. Le workflow s'arrête si le modèle d'execution est bloquant (cf. ci-dessous).
- FATAL : le traitement associé a généré une erreur technique. Le workflow s'arrête.

Chaque action peut avoir les modèles d'éxécutions suivants (toutes les étapes sont par défaut bloquantes) :

- Bloquant

    * Si une action est identifiée en erreur, l'étape en cours est alors arrêtée et le workflow passe à la derniere étape de finalisation de l'entrée. Une notification de l'échec de l'entrée est généré et le statut du processus d’entrée passe à « erreur ».

- Non bloquant

    * Si une action est identifiée en erreur, le reste des actions de l'étape est exécuté avant que le statut de l'étape passe à « erreur ». L'étape en cours est alors arrêtée et le workflow passe à la dernière étape de finalisation de l'entrée. Une notification de l'échec de l'entrée est généré et le statut du processus d’entrée passe à « erreur ».

Structure du fichier Properties du Worflow
==========================================

Le fichier Properties permet de définir la structure du Workflow pour les étapes et actions réalisées dans le module d'Ingest Interne, en excluant les étapes et actions réalisées dans le module d'Ingest externe.

La structure du fichier est la suivante :

.. figure:: images/workflow.jpg
  :align: center

  Structure du fichier de définition du workflow, un exemple est donné dans la figure suivante


Un Workflow est défini en JSON avec la structure suivante :

- un bloc en-tête contenant :

    + ``ID`` : identifiant unique du workflow,

    + ``Comment`` : description du workflow ou toutes autres informations utiles concernant le workflow

- une liste d'étapes dont la structure est la suivante :

    + ``workerGroupId`` : identifiant de famille de Workers,

    + ``stepName`` : nom de l'étape, servant de clé pour identifier l'étape,


    + ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :

      - BLOCKING : le traitement est bloqué en cas d'erreur, il est nécessaire de recommencer le workflow. Les étapes FINALLY (voir plus bas) sont tout de même exécutées

      - NOBLOCKING : le traitement peut continuer malgré les erreurs ou avertissements,

      - FINALLY : le traitement correspondant est toujours exécuté, même si les étapes précédentes se sont terminées en échec


    + ``Distribution`` : modèle de distribution, décrit comme suit :

      - ``Kind`` : un type pouvant être REF (i.e. élément unique) ou LIST (i.e. liste d'éléments)

      - ``Element`` : l'élément de distribution indiquant l'élément unique sous forme d'URI (REF) ou la liste d'éléments en pointant vers un dossier (LIST).


    + une liste d'Actions :

      - ``ActionKey`` : nom de l'action


      - ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :
        - BLOCKING : l'action est bloquante en cas d'erreur. Les actions suivantes (de la meme étape) ne seront pas éxécutées.
        - NOBLOCKING : l'action peut continuer malgrée les erreurs ou avertissements.


      - ``In`` : liste de paramètres d'entrées :
        - ``Name`` : nom utilisé pour référencer cet élément entre différents handlers d'une même étape,

        - ``URI`` : cible comportant un schema (WORKSPACE, MEMORY, VALUE) et un path où chaque handler peut accéder à ces valeurs via le handlerIO :
          - WORKSPACE : path indique le chemin relatif sur le workspace (implicitement un File),
          - MEMORY : path indique le nom de la clef de valeur (implicitement un objet mémoire déjà alloué par un Handler précédent),
          - VALUE : path indique la valeur statique en entrée (implicitement une valeur String).


      - ``Out`` : liste de paramètres de sorties :
        - ``Name`` : nom utilisé pour référencer cet élément entre différents handlers d'une même étape,

        - ``URI`` : cible comportant un schema (WORKSPACE, MEMORY) et un path où chaque handler peut stocker les valeurs finales via le handlerIO :
          - WORKSPACE : path indique le chemin relatif sur le workspace (implicitement un File local),
          - MEMORY : path indique le nom de la clef de valeur (implicitement un objet mémoire).


.. image:: images/Workflow_file_structure.png
        :align: center
        :alt: Exemple partiel de workflow, avec les notions étapes et actions
