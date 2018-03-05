Introduction
############

Avertissement
=============

Cette documentation est un travail en cours. Elle est susceptible de changer dans les prochaines releases pour tenir compte des développements de la solution logicielle Vitam.

Objectif du document
====================

Ce document a pour objectif de présenter les différents processus employés par la solution logicielle Vitam.
Il est destiné aux administrateurs aussi bien techniques que fonctionnels, aux archivistes souhaitant une connaissance plus avancée du logiciel ainsi qu'aux développeurs.

Il explicite chaque processus (appelés également "workflow"), et pour chacun leurs tâches, traitements et actions.

Ce document comprend également du matériel additionnel pour faciliter la compréhension des processus comme des fiches récapitulatives et des schémas. Il explique également la manière dont est formée la structure des fichiers de workflow.

Description d'un processus
===========================

Un workflow est un processus composé d’étapes (macro-workflow), elles-mêmes composées d’une liste de tâches et d'actions à exécuter de manière séquentielle, une seule fois ou répétées sur une liste d’éléments (micro-workflow).

Pour chacun de ces éléments, le document décrit :

- La règle générale qui s'applique à cet élément
- Les statuts de sortie possibles (OK, KO...), avec les raisons de ces sorties et les clés associées
- Des informations complémentaires, selon le type d'élément traité

Chaque étape, chaque action peuvent avoir les statuts suivants :

- OK : le traitement associé s'est passé correctement. Le workflow continue.
- WARNING : le traitement associé a généré un avertissement (par exemple le format de l'objet est mal déclaré dans le bordereau de transfert). Le workflow continue.
- KO : le traitement associé a généré une erreur métier. Le workflow s'arrête si le modèle d'exécution est bloquant (cf. ci-dessous).
- FATAL : le traitement associé a généré une erreur technique. Le workflow se met en pause.

Un workflow peut être terminé, en cours d'exécution ou être en pause. Un workflow en pause représente le processus arrêté à une étape donnée. Chaque étape peut être mise en pause : ce choix dépend du mode de versement (le mode pas à pas marque une pause à chaque étape), ou du statut (le statut FATAL met l'étape en pause). Les workflows en pause sont visibles dans l'IHM dans l'écran "Gestion des opérations".

Lorsque le statut FATAL survient à l'intérieur d'une étape (par exemple dans une des tâches ou un des traitements de l'étape), c'est toute l'étape qui est mise en pause. Si cette étape est rejouée, les objets déjà traités avant le fatal ne sont pas traités à nouveau : le workflow reprend exactement là où il s'était arrêté et commence par rejouer l'action sur l'objet qui a provoqué l'erreur.


Chaque action peut avoir les modèles d'exécutions suivants (toutes les étapes sont par défaut bloquantes) :

- Bloquant

    * Si une action est identifiée en erreur, l'action et l'étape en cours sont alors arrêtées dans un statut "KO" ou "fatal" et le workflow passe à l'étape suivante. Dans certains cas, il est directement terminé en erreur alors que dans d'autres, il passe à une étape de finalisation. Ces comportements spécifiques sont décrits dans chaque workflow.

- Non bloquant

    * Si une action est identifiée en erreur, elle passe en erreur puis le reste des actions de l'étape en cours est exécuté. Dans certains cas, le workflow est directement terminé en erreur alors que dans d'autres, il passe à une étape de finalisation. Dans les deux cas  Ces comportements spécifiques sont décrits dans chaque workflow.

Structure d'un fichier Properties du Worflow
=============================================

Les fichiers **Properties** (Par exemple *DefaultIngestWorkflow.json*) permettent de définir la structure du Workflow pour les étapes, tâches et actions réalisées dans le module d'Ingest Interne, en excluant les étapes et actions réalisées dans le module d'Ingest externe.

La structure du fichier est la suivante :

.. figure:: images/workflow.jpg
  :align: center

  Structure du fichier de définition du workflow


Un Workflow est défini en JSON avec la structure suivante :

- un bloc en-tête contenant :

    + ``ID`` : identifiant unique du workflow,

    + ``Identifier`` : clé du workflow,

    + ``Name`` : nom du workflow,

    + ``TypeProc`` : catégorie du workflow,

    + ``Comment`` : description du workflow ou toutes autres informations utiles concernant le workflow

- une liste d'étapes dont la structure est la suivante :

    + ``workerGroupId`` : identifiant de famille de Workers,

    + ``stepName`` : nom de l'étape, servant de clé pour identifier l'étape,

    + ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :

      - BLOCKING : le traitement est bloqué en cas d'erreur, il est nécessaire de recommencer à la tâche en erreur. Les étapes FINALLY (définition ci-dessous) sont tout de même exécutées

      - NOBLOCKING : le traitement peut continuer malgré les éventuels erreurs ou avertissements,

      - FINALLY : le traitement correspondant est toujours exécuté, même si les étapes précédentes se sont terminées en échec


    + ``Distribution`` : modèle de distribution, décrit comme suit :

      - ``Kind`` : un type pouvant être REF (un élément unique) ou LIST (une liste d'éléments hiérarchisés) ou encore LIST_IN_FILE (liste d'éléments)

      - ``Element`` : l'élément de distribution indiquant l'élément unique sous forme d'URI (REF) ou la liste d'éléments en pointant vers un dossier (LIST).

      - ``Type`` : le type des objets traités (ObjectGroup uniquement pour le moment).

      - ``statusOnEmptyDistribution`` : permet dans le cas d'un traitement d'une liste vide, de surcharger le statut WARNING par un statut prédéfini.


    + une liste d'Actions :

      - ``ActionKey`` : nom de l'action


      - ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :

        - BLOCKING : l'action est bloquante en cas d'erreur. Les actions suivantes (de la même étape) ne seront pas éxécutées.

        - NOBLOCKING : l'action peut continuer malgré les éventuels erreurs ou avertissements.


      - ``In`` : liste de paramètres d'entrées :

        - ``Name`` : nom utilisé pour référencer cet élément entre différents handlers d'une même étape,

        - ``URI`` : cible comportant un schéma (WORKSPACE, MEMORY, VALUE) et un path où chaque handler peut accéder à ces valeurs via le handlerIO :

          - WORKSPACE : path indiquant le chemin relatif sur le workspace (implicitement un File),

          - MEMORY : path indiquant le nom de la clef de valeur (implicitement un objet mémoire déjà alloué par un handler précédent),

          - VALUE : path indiquant la valeur statique en entrée (implicitement une valeur String).


      - ``Out`` : liste de paramètres de sorties :

        - ``Name`` : nom utilisé pour référencer cet élément entre différents handlers d'une même étape,

        - ``URI`` : cible comportant un schéma (WORKSPACE, MEMORY) et un path où chaque handler peut stocker les valeurs finales via le handlerIO :

          - WORKSPACE : path indiquant le chemin relatif sur le workspace (implicitement un File local),

          - MEMORY : path indiquant le nom de la clé de valeur (implicitement un objet mémoire).


.. image:: images/Workflow_file_structure.png
        :align: center
        :alt: Exemple partiel de workflow, avec les notions étapes et actions
