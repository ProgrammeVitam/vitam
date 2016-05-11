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

Ensuite, la commande à lancer est ``make <format de sortie> DOC=<nom de la documentation>``, avec ``format de sortie`` :

* html
* latexpdf

Le résultat est disponible dans le dossier ``doc/fr/<format de sortie>/target/<format de sortie>``

Par exemple : 

        make html DOC=archi-fonctionnelle

Nettoyage
---------

Pour supprimer les documents cibles, la commande est la suivante : ``make clean DOC=<nom de la documentation>``