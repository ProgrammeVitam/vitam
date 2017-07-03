Introduction
############

Avertissement
=============

Ce document fait état du travail en cours. Il est susceptible de changer de manière conséquente.
   
Objectif du document
====================

Ce document a pour objectif de présenter la structure générale des collections utilisées dans la solution logicielle Vitam.
Il est destiné principalement aux développeurs, afin de leur présenter l'organisation des données dans la solution logicielle Vitam, ainsi qu'à tous les autres acteurs du programme pour leur permettre de connaître ce qui existe en l'état actuel.

Il explicite chaque champ, précise la relation avec les sources (manifest conforme au standard SEDA v.2.0, référentiels Pronom, etc...) et la structuration JSON stockée dans la base de données MongoDB.

Pour chacun des champs, cette documentation apporte :

- Une liste des valeurs licites
- La sémantique ou syntaxe du champ
- La codification en JSON

Il décrit aussi parfois une utilisation particulière faite à une itération donnée.
Cette indication diffère de la cible finale, auquel cas le numéro de l'itération de cet usage est mentionné.

Création des index
==================

Les différents index sont créés par ansible.
Les fichiers à renseigner pour rajouter un nouvel index sont stockés dans le répertoire deployment/ansible-vitam/roles/mongo_configure/templates/init-{nom-base}-database.js.j2

Généralités
===========

Nommage des champs
------------------

Les champs des fichiers JSON présents dans les collections peuvent être nommés de deux manières :

* "champ" : un champ sans préfixe est modifiable via les API.
* "_champ" : un champ ayant pour préfixe "_" n'est pas modifiable via les API.