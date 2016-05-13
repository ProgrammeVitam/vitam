Storage
=======

Ce rôle a pour but le déploiement du service "storage" de la solution VITAM


Requirements
------------

* Le démon docker doit avoir été installé sur l'hôte.


Variables
---------

Les variables attendues en entrée du rôle sont les suivantes :

* {{vitam_folder_root}} : Racine du dossier où seront déposés les répertoires de log / données / autres
* {{vitam_environment}} : Environnement de déploiement


Dépendances
-----------

* Le rôle "host-base" doit déjà avoir été exécuté sur l'hôte sur lequel s'exécute ce rôle.


License
-------

Cecill 2.0


Auteur
------

Projet VITAM