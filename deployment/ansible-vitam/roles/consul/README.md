Consul
======

Ce rôle a pour but le déploiement d'un agent (ou serveur) consul

Variables
-----------

Les variables attendues en entrée du rôle sont les suivantes :

* {{ vitam_defaults.folder.root_path }} : Racine du dossier où seront déposés les répertoires de log / données / autres
* {{ vitam_site_name }} : Nom du datacenter

Les variables possibles sont :

* {{ server }} = false : mode du noeud consul (serveur ou agent)


Dépendances
-----------

* Le rôle "normalize-host" doit déjà avoir été exécuté sur l'hôte sur lequel s'exécute ce rôle.


License
-------

Cecill 2.1

Auteur
------

Projet VITAM
