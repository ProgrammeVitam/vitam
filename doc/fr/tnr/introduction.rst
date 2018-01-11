Introduction
############

Principes généraux
==================

Tests de non régression
-----------------------

Les tests de non régression (TNR) ont pour objectif de tester la continuité des fonctionnalités de Vitam. L'ajout de nouvelles fonctionnalités pouvant entraîner des bugs ou indisponibilités (régressions) sur des fonctionnalités déjà existantes, l'outil de test de non regression va permettre de tester automatiquement le périmètre fonctionnel pré-existant afin de s'arrurer de son bon fonctionnement dans le temps. 

L'aujout d'une nouvelle fonctionnalité doit donc s'accompagner d'un ou plusieurs TNR.

Idéalement, les développeurs doivent lancer les TNR avant d'effectuer une Merge Request visant à intégrer une nouvelle fonctionnalité.

Behavior-Driven Development (BDD)
---------------------------------

Le BDD est une méthode de collaboration s'appuyant sur un langage de programation naturel permettant aux intervenants non techniques de décrire des scénarios de fonctionnement.

Les mots de ce langage permettent de mobiliser des actions techniques qui sont, elles, réalisées par les développeurs.

Le BDD est utilisé pour la réalisation des TNR, ce qui permet à tout intervenant du projet de pouvoir en réaliser.

L'outil utilisé dans le cadre de Vitam est Cucumber (https://cucumber.io/) qui utilise le langage Gherkin.

Pré-Requis
==========

Dépot vitam-itest
-----------------

L'ajout et la modification de TNR sont à éffectuer dans le dépôt vitam-itest. 

Il est donc nécessaire de le cloner avant toute chose.

Git LFS
-------

Afin de permettre la gestion de fichiers volumineux dans git, il est nécessaire d'installer l'extention Git-LFS (https://git-lfs.github.com/).

Une fois git lfs installé, il est néccessaire de l'activer pour le dépôt vitam-itest sur votre machine. Pour réaliser cette opération, se placer à la racine du dépôt et exéctuer la commande :

::

	git lfs install

Méthodologie de test
=====================

Purge des bases
----------------

Les bases sont vidée avant le lancement de chaque campagne de test. Il est donc nécessaire que toutes les ressources mobilisées pour un test (SIP, référentiels...) soient chargées dans VITAM en amont de l’exécution de ce test. 