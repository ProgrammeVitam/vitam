Introduction
############

.. warning::
	Cette documentation reflète l'état actuel de la solution Vitam. Elle est susceptible de changer dans les prochaines releases pour tenir compte des développements de la solution logicielle Vitam.

Objectif du document
====================

Ce document a pour objectif de présenter les différents processus employés par la solution logicielle Vitam.
Il est destiné aux administrateurs aussi bien techniques que fonctionnels, aux archivistes souhaitant une connaissance plus avancée du logiciel ainsi qu'aux développeurs.

Il explicite chaque processus (appelé également "workflow"), et pour chacun leurs tâches, traitements et actions.

Ce document comprend du matériel additionnel pour faciliter la compréhension des processus comme des fiches récapitulatives et des schémas. Il explique également la manière dont est formée la structure des fichiers de workflow.

Description d'un processus
===========================

Un workflow est un processus composé d’étapes (macro-workflow), elles-mêmes composées d’une liste de tâches et d'actions à exécuter de manière séquentielle, unitairement ou de manière itérative sur une liste d’éléments (micro-workflow).

Pour chacun de ces éléments, le document décrit :

- La règle générale qui s'applique à cet élément
- Les statuts de sortie possibles (OK, KO...), avec les raisons de ces sorties et les clés associées
- Des informations complémentaires, selon le type d'élément traité

Un "traitement" désigne ci-dessous une opération, une étape ou une tâche. Chaque traitement peut avoir à son issue un des statuts suivant :

- OK : le traitement s'est déroulé comme attendu et le système a été modifié en conséquence.
- Warning : le traitement a atteint son objectif mais le système émet une réserve. Soit :

  * Le système suspecte une anomalie lors du déroulement du traitement sans pouvoir le confirmer lui même et lève une alerte à destination de l'utilisateur afin que celui ci puisse valider qu'il s'agit du comportement souhaité.

  Exemple : un SIP versé sans objets provoque une opération en warning car le fait de ne verser qu'une arborescence d'unités archivistiques sans aucun objet peut être suspecte (au sens métier).

  * Le système a effectué un traitement entraînant une modification de données initialement non prévue par l'utilisateur.

  Exemple : la solution logicielle Vitam a détecté un format de fichier en contradiction avec le format décrit dans le bordereau de transfert. Elle enregistre alors ses propres valeurs en base de données au lieu de prendre celles du bordereau et utilise le warning pour en avertir l'utilisateur.

  * Le système a effectué un traitement dont seule une partie a entraîné une modification de données. L'autre partie de ce traitement s'est terminée en échec sans modification (KO).

  Exemple : une modification de métadonnées en masse d'unités archivistiques dont une partie de la modification est OK et une partie est KO : le statut de l'étape et de l'opération sera Warning.

- KO : le traitement s'est terminé en échec et le système n'a pas été modifié en dehors des éléments de traçabilités tels que les journaux et les logs. L'intégralité du traitement pourrait être rejouée sans provoquer l'insertion de doublons.
- Fatal : le traitement s'est terminé en échec a cause d'un problème technique. L'état du système dépend de la nature du traitement en fatal et une intervention humaine est requise pour expertiser et résoudre la situation. Lorsque le statut FATAL survient à l’intérieur d’une étape (par exemple dans une des tâches ou une des actions de l’étape), c’est toute l’étape qui est mise en pause. Si cette étape est rejouée, les objets déjà traités avant le fatal ne sont pas traités à nouveau : le workflow reprend exactement là où il s’était arrêté et commence par rejouer l’action sur l’objet qui a provoqué l’erreur.

Un workflow peut être terminé, en cours d'exécution ou être en pause. Un workflow en pause représente le processus arrêté à une étape donnée. Chaque étape peut être mise en pause : ce choix dépend du mode de versement (le mode pas à pas marque une pause à chaque étape), ou du statut (le statut FATAL met l'étape en pause). Les workflows en pause sont visibles dans l'IHM dans l'écran "Gestion des opérations".

Chaque action peut avoir les modèles d'exécutions suivants (toutes les étapes sont par défaut bloquantes) :

- Bloquant

    * Si une action bloquante est identifiée en erreur, le workflow est alors arrêté en erreur. Seules les actions nécessaire à l'arrêt du workflow sont alors éxecutées.

- Non bloquant

    * Si une action non bloquante est identifiée en erreur, elle seule sera en erreur et le workflow continuera normalement.

Structure d'un fichier Properties du Worflow
=============================================

Les fichiers **Properties** (par exemple *DefaultIngestWorkflow.json*) permettent de définir la structure du Workflow pour les étapes, tâches et traitements réalisées dans le module d'Ingest Interne, en excluant les étapes et traitements réalisées dans le module d'Ingest externe.

Un Workflow est défini en JSON avec la structure suivante :

- un bloc en-tête contenant :

    + ``ID`` : identifiant unique du workflow,

    + ``Identifier`` : clé du workflow,

    + ``Name`` : nom du workflow,

    + ``TypeProc`` : catégorie du workflow,

    + ``Comment`` : description du workflow ou toutes autres informations utiles concernant le workflow

- une liste d'étapes dont la structure est la suivante :

    + ``WorkerGroupId`` : identifiant de famille de Workers,

    + ``StepName`` : nom de l'étape, servant de clé pour identifier l'étape,

    + ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :

      - BLOCKING : le traitement est bloqué en cas d'erreur, il est nécessaire de recommencer à la tâche en erreur. Les étapes FINALLY (définition ci-dessous) sont tout de même exécutées

      - NOBLOCKING : le traitement peut continuer malgré les éventuels erreurs ou avertissements,

      - FINALLY : le traitement correspondant est toujours exécuté, même si les étapes précédentes se sont terminées en échec


    + ``Distribution`` : modèle de distribution, décrit comme suit :

      - ``Kind`` : un type pouvant être REF (un élément unique) ou LIST (une liste d'éléments hiérarchisés) ou encore LIST_IN_FILE (liste d'éléments)

      - ``Element`` : l'élément de distribution indiquant l'élément unique sous forme d'URI (REF) ou la liste d'éléments en pointant vers un dossier (LIST).

      - ``Type`` : le type des objets traités (ObjectGroup uniquement pour le moment).

      - ``StatusOnEmptyDistribution`` : permet dans le cas d'un traitement d'une liste vide, de surcharger le statut WARNING par un statut prédéfini.

      - ``BulkSize``: taille de la liste, valeur à spécifier ex: "bulkSize": 1000. La valeur par défault est de 16.


    + une liste d'Actions :

      - ``ActionKey`` : nom de l'action

      - ``Behavior`` : modèle d'exécution pouvant avoir les types suivants :

       		 - BLOCKING : l'action est bloquante en cas d'erreur. Les actions suivantes (de la même étape) ne seront pas éxécutées.

       		 - NOBLOCKING : l'action peut continuer malgré les éventuels erreurs ou avertissements.

      -  ``lifecycleLog``: action indiquant le calcul sur les LFC. Valeur du champ "DISABLED".

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
