Principes généraux
##################

Le schéma ci-dessous décrit le principe général d'une montée de version de la solution logicielle :term:`VITAM`. 

.. figure:: ../images/update_workflow_xmind.*
    :align: center
    :scale: 50 %


Etats attendus
==============

Pré-migration
-------------

Avant toute migration, il est attendu de la part des exploitants de vérifier :

- Que la solution logicielle :term:`VITAM` fonctionne normalement
- Que l'ensemble des `timers` systemd sont stoppés
- Qu'aucun `workflow` n'est ni en cours, ni en statut **FATAL**

.. seealso:: Se référer au chapitre « Suivi de l'état du système » du :term:`DEX` pour plus d'informations. 

.. seealso:: Se référer au chapitre "Suivi des Workflows" du :term:`DEX`, pour plus d'informations sur la façon de vérifier l'état des statuts des *workflows*.

Post-migration
--------------

A l'issue de toute migration, il est attendu de la part des exploitants de vérifier :

- Que la solution logicielle :term:`VITAM` fonctionne normalement
- Que l'ensemble des `timers` systemd sont bien redémarrés (les redémarrer, le cas échéant)
- Qu'aucun `workflow` n'est en statut **FATAL**

Se référer au chapitre « Suivi de l'état du système » du :term:`DEX` pour plus d'informations. 


Montées de version *bugfix*
============================

Au sein d'une même *release*, la montée de version depuis une version *bugfix* vers une version *bugfix* supérieure est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d’installation classique décrite dans le :term:`DIN`. 

Les montées de version *bugfix* ne contiennent à priori pas d'opérations de migration ou de reprises de données particulières. Toutefois, des spécificités propres aux différentes versions *bugfixes* peuvent s'appliquer ; elles sont explicitées dans le chapitre :ref:`bugfixes_updates`. 

.. caution:: Parmi les versions *bugfixes* publiées au sein d'une même *release*, seuls les chemins de montées de version d'une version *bugfix* à la version *bugfix* suivante sont qualifiés par :term:`VITAM`. 

Montées de version mineure
==========================

La montée de version depuis une version mineure (de type *release*) vers une version mineure supérieure est réalisée par réinstallation de la solution logicielle :term:`VITAM` grâce aux playbooks ansible fournis, et selon la procédure d’installation classique décrite dans le :term:`DIN`. 

Ce document décrit les chemins de montées de version depuis une version mineure, vers la version mineure maintenue supérieure. 

Les montées de version mineure doivent être réalisées en s'appuyant sur les dernières versions *bugfixes* publiées. 

Les opérations de migration ou de reprises de données propres aux différentes versions *releases* sont explicitées dans le chapitre :ref:`releases_updates`. 

.. caution:: Parmi les versions mineures publiées au sein d'une même version majeure, seuls les chemins de montées de version depuis une version mineure maintenue, vers la version mineure maintenue suivante sont qualifiés par :term:`VITAM`. 

Montées de version majeure
==========================

La montée de version depuis une version majeure vers une version majeure supérieure s'appuie sur les chemins de montées de version mineure décrits dans le chapitre :ref:`releases_updates`. 
