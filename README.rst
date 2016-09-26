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

2 méthodes existent pour déployer vitam sur un poste de développement.

Alternative 1 : docker
----------------------

Cette méthode permet de construire et déployer un système VITAM de manière presque automatique au sein d'un conteneur docker qui héberge l'intégralité des outils requis.

Pré-requis
**********

* Docker 1.12 minimum
* OS récent (des problèmes ont été rencontrés avec Ubuntu 12.04)
* Répertoire contenant un clone du dépôt git vitam/vitam

Procédure
*********

- Lancer :  /vitam/dev-deployment/run-compose.sh
- Le script demande "Please enter the location of your vitam git repository" exemple : "/$HOME/git/vitam"
    => Le script récupère le conteneur docker ``docker.programmevitam.fr/vitam/dev-rpm-base`` et le lance
- Une fois le shell ouvert dans le conteneur, executer ``vitam-build-repo`` pour construire l'intégralité des rpm requis.
- A l'issue de l'étape suivante, se positionner dans ``/code/deployment``
- Suivre les indications du README.rst présent dans ce répertoire. Les composants sont déployés dans le conteneur ; les ports d'écoute des composants sont mappés à l'extérieur du conteneur, sur les mêmes ports. 


Alternative 2 : manuelle (virtualisation)
-----------------------------------------

Pré-requis
**********

* Virtualbox ou équivalent, avec une machine virtuelle Centos 7 installée et configurée
* Installer le paquet ``rpm-build`` et ``rpmdevtools`` sur le poste local (normalement présent dans toutes les distributions)
* Répertoire contenant un clone du dépôt git vitam/vitam

Configuration initiale de la VM
*******************************

* Contraintes sur la VM :

    - le répertoire contenant le dépôt vitam doit être mappé sur un répertoire à l'intérieur de la VM (par la suite, on considérera que le point de montage dans la VM est ``/code``)

* Dans la VM

    - Installer les dépôts epel : ``yum install -y epel-release``
    - Installer ansible : ``yum install -y ansible`` ; valider que la version installée est bien au moins la version 2.1 (``ansible --version``)
    - Installer les dépendances requises pour la construction d'un dépôt : ``yum install -y createrepo initscripts.x86_64``
    - Déclarer un dépôt yum local pointant vers ``/code/target`` ; pour cela, insérer le contenu suivant dans un fichier ``devlocal.repo`` dans le répertoire ``/etc/yum.repos.d`` :
    
    [local]
    name=Local repo
    baseurl=file:///code/target
    enabled=1
    gpgcheck=0
    protect=1

Procédure
*********

* Sur le poste de développement :

    - Exécuter la compilation des sources et la construction de tous les paquets RPM : dans le répertoire racine
      
    pushd sources ; mvn clean package rpm:rpm -DskipTests ; popd    # pour contstruire les paquets RPM VITAM
    pushd rpm/vitam-external ; ./build_repo.sh ; popd               # pour récupérer les paquets externes


* Dans la VM :

	- Se connecter en root dans /code
	- Builder les composants restant :
	
	pushd rpm/vitam-product ; ./build.sh vitam-user-vitam ; popd    # pour construire le paquet vitam-user-vitam
    pushd rpm/vitam-product ; ./build.sh vitam-user-vitamdb ; popd  # pour construire le paquet vitam-user-vitamdb
    pushd rpm/vitam-product ; ./build.sh vitam-consul ; popd        # pour construire le paquet vitam-consul

    - Puis rassembler les fichiers rpm produits dans le répertoire ``target/packages``:
    
    rm -rf target/packages
    mkdir -p target/packages
    find . -name '*.rpm' -type f -exec cp {} target/packages \;

    - Construire l'index du répôt rpm :
    
    createrepo -x '.git/*' .

    - Nettoyer le cache yum pour prendre en compte les modifications de dépôt :
      
    yum clean all

    - Puis valider la liste des rpm présents dans le dépôt local :
      
    yum --disablerepo="*" --enablerepo="local" list available

    - Enfin, se positionner dans le répertoire ``deployment`` et suivre les indications du README.rst présent dans ce répertoire.
      
L'accès aux composants une fois démarrés dépend de la nature de la connexion réseau présentée par la VM (bridge, NAT ou host).


