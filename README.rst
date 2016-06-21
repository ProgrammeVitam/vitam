#####
VITAM
#####

Build du logiciel VITAM
=======================

Pré-requis
----------

Pour construire la documentation, il est nécessaire d'avoir les pré-requis suivants installés :

* jdk
* maven

Build
-----

``mvn install``

Build de la documentation
=========================

Pré-requis
----------

Pour construire la documentation, il est nécessaire d'avoir les pré-requis suivants installés :

* sphinx-build
* Pour construire le pdf : latex
* make

Build
-----

Ensuite, la commande à lancer est ``make <format de sortie> MODULE=<nom de la documentation>``, avec ``format de sortie`` :

* html
* latexpdf

Le module par défaut est la documentation globale ``MODULE=.``
Le résultat est disponible dans le dossier ``MODULE/doc/fr/target/<format de sortie>``

Par exemple : 

        make html MODULE=workspace

Nettoyage
---------

Pour supprimer les documents cibles, la commande est la suivante : ``make clean MODULE=<nom de la documentation>``
