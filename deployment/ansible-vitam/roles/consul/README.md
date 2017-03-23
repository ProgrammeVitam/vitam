Consul
======

Ce rôle a pour but le déploiement d'un agent (ou serveur) consul


Requirements
------------


Variables
---------

Les variables attendues en entrée du rôle sont les suivantes :

* {{vitam_folder_root}} : Racine du dossier où seront déposés les répertoires de log / données / autres
* {{environnement}} : Environnement de déploiement

Les variables possibles sont :

* {{server}} = false : mode du noeud consul (serveur ou agent)


Dépendances
-----------

* Le rôle "host-base" doit déjà avoir été exécuté sur l'hôte sur lequel s'exécute ce rôle.


License
-------

Cecill 2.0


Auteur
------

Projet VITAM