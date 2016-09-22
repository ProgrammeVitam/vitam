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

Pour ignorer les tests d'intégration:

``mvn install -DskipITs``

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

Deploiement vitam sur poste de dev
==================================

Pré-requis
----------

Docker 1.12 minimum nécessaire

Build
-----

- Lancer :  /vitam/dev-deployment/run-compose.sh
- Le script demande "Please enter the location of your vitam git repository" exemple : "/$HOME/git/vitam"
	=> Le script Pull le docker dev-rpm-base et le lance
- Dans le docker executer "vitam-build-repo"
	=> le script build 
- A l'issue de l'étape suivante, se positionner dans "/code/deployment"
- Comme indiquer dans le README.rst, executer la commande "ansible-playbook ansible-vitam-rpm/vitam.yml -i environments-rpm/hosts.local"
	=> ansible déploie les composants dans le docker
