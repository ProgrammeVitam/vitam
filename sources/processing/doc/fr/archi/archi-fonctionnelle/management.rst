Processing Management
#####################

Ce module est pour le but d'organiser l'exécution d'un process de traitement avec les workflows fournis et un ensemble de paramètres passés par le service d'appel (Ingest : traitement des saisies d'archives).

Lors de l'initialisation du processus avec un workflow donné, ProcessManagement crée une instance de ProcessEngine et de StateMachine qui sont fortement liée au ProcessWorkflow avec une cardinalité un-à-un.

Pour chaque ProcessWorkflow, une et une seule machine à état (StateMachine) est rattachée.
Une instance d'une machine à état ne peut gérer qu'un seul PocessWorkflow.
Pour une instance d'une machine à état, une et une seul instance de ProcessEngine est crée.
Un ProcessEngine ne peut être rattaché qu'une et une seule machine à état

Un ProcessManagement peut avoir zéro ou plusieurs ProcessWorkflow


ProcessManagement (0..n)
    (1..1) ProcessWorkflow (1..1)
            (1..1) StateMachine (1..1)
                        (1..1) ProcessEngine
