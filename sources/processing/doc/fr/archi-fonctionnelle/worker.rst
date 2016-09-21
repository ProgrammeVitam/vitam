Introduction
############

But de cette documentation
**************************

L'objectif de cette documentation est d'expliquer l'architecture fonctionnelle de ce module.

Ce module lui-même traite une tache/opération précise dans l'ensemble des opérations de workflow. 
Le worker se compose de plusieurs ActionHandler qui permet de traiter une tâche précis. 

Le worker est désormais appelé via du rest. Un client est fourni et permet l'utilisation de l'API Rest mise en place.
Un module Worker séparé est mis en place.