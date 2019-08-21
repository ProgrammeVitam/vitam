Etats attendus
###############

Pré-migration
==============

Avant toute migration, il est attendu de la part des exploitants de vérifier :

- Que la solution logicielle :term:`VITAM` fonctionne normalement
- Que l'ensemble des `timers` systemd sont stoppés
- Qu'aucun `workflow` n'est ni en cours, ni en statut **FATAL**

.. seealso:: Se référer au chapitre « Suivi de l'état du système » du :term:`DEX` pour plus d'informations. 

.. seealso:: Se référer au chapitre "Suivi des Workflows" du :term:`DEX`, pour plus d'informations sur la façon de vérifier l'état des statuts des *workflows*.

Post-migration
==============

A l'issue de toute migration, il est attendu de la part des exploitants de vérifier :

- Que la solution logicielle :term:`VITAM` fonctionne normalement
- Que l'ensemble des `timers` systemd sont bien redémarrés (les redémarrer, le cas échéant)
- Qu'aucun `workflow` n'est en statut **FATAL**

Se référer au chapitre « Suivi de l'état du système » du :term:`DEX` pour plus d'informations. 
