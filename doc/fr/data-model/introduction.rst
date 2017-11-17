Introduction
############

Avertissement
=============

Ce document fait état du travail en cours. Il est susceptible de changer de manière conséquente au fur et à mesure de l'avancée des développements.
   
Objectif du document
====================

Ce document a pour objectif de présenter la structure générale des collections utilisées dans la solution logicielle Vitam.
Il est destiné principalement aux développeurs, afin de leur présenter l'organisation des données dans la solution logicielle Vitam, ainsi qu'à tous les autres acteurs du programme pour leur permettre de connaître ce qui existe en l'état actuel.

Il explicite chaque champ, précise la relation avec les sources (par exemple bordereau de transfert conforme au standard SEDA v.2.0, référentiels Pronom, etc...) et la structuration JSON stockée dans la base de données MongoDB. Ce document est structuré de façon à suivre l'ordre des bases et collections dans Mongo.

Pour chacun des champs, cette documentation apporte :

- Une liste des valeurs licites
- La sémantique ou syntaxe du champ
- La codification en JSON

Il décrit aussi parfois une utilisation particulière faite à une itération donnée.
Cette indication diffèrant de la cible finale, auquel cas le numéro de l'itération de cet usage est mentionné.

Création des index
==================

Les différents index sont créés par ansible, plate-forme logicielle libre.
Les fichiers à renseigner pour rajouter un nouvel index sont stockés dans le répertoire deployment/ansible-vitam/roles/mongo_configure/templates/init-{nom-base}-database.js.j2

Généralités
===========

Cardinalité
------------

La cardinalité présentée pour chacun des champs correspond aux exigences de la base de données de la base de donnée.

Certains champs ayant une cardinalité 1-1 seront directement renseignés par la solution logiciel Vitam et seront donc obligatoirement présents dans la base de données, mais ne le sont pas forcement dans les données envoyées.

Nommage des champs
------------------

Les champs des fichiers JSON présents dans les collections peuvent être nommés de deux manières :

* "champ" : un champ sans underscore est modifiable via les API.
* "_champ" : un champ ayant avec un underscore n'est pas modifiable via les API. Une fois renseigné dans Vitam par le bordereau de transfert ou la solution logicielle Vitam, il ne pourra plus être modifié depuis l’extérieur.
  
Collections et bases
--------------------

Les bases Mongo sont organisées par bases et collections.

Les bases contiennent différentes collections. Les collections peuvent être rapprochées du concept de table en SQL.

Identifiants
------------

Il existe plusieurs types d'identifiants :

	* GUID : identifiant unique de 36 caractères généré par la solution logicielle Vitam
	* PUID : identifiant des formats dans le référentiel pronom
	* PID : identifiant de processus Unix

Dates 
-----

Toutes les dates décrites dans ce document sont au format ISO 8601.
``Exemple : "2017-11-02T13:50:28.922"``

Limite de caractères acceptés dans les champs
---------------------------------------------

Mongo est un type de base de données dite "schemaless", soit sans-schéma. Ainsi, les champs contenus dans les collections décrites dans ce document sont, sauf mention contraire, sans limite de caractères.

Type d'indexation dans elasticsearch
------------------------------------

Les champs peuvent être indexés de deux façons différentes dans ElasticSearch :

	* **Les champs analysés :** les informations contenues dans ces champs peuvent être retrouvés par une recherche full-text. Par exemple, les champs *Description*, *Name*.
	* **Les champs non analysés :** les informations contenues dans ces champs peuvent être retrouvés par une recherche exacte uniquement. Par exemple, les champs *Identifier* ou *OriginatingAgency*.