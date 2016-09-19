Outillage de déploiement
========================

Outil
-----

L'outil de déploiement utilisé sur Vitam est ansible. Cette solution de déploiement a les caractéristiques suivantes : 

* Agent-less : la propagation des ordres de déploiement utilise SSH et nécessite sur les serveur un interpréteur Python 2.6+. (Cf. `la documentation officielle <https://docs.ansible.com/ansible/intro_installation.html>`_ pour la liste exhaustive des dépendances requises). 

* Méthode d'authentification : l'authentification est faite par un utilisateur habilité à se connecter à SSH et devant pouvoir avoir les élévations de privilèges nécessaires pour faire les actions (via su ou sudo) :

  + Le choix de la méthode d'authentification (mot de passe, clé publique sans passphrase ou clé publique avec passphrase) peut être choisi en fonction des contraintes d'hébergement. Cependant, certaines méthodes limiteront l'automatisation du déploiement
  + La mise en place de cet utilisateur est un pré-requis à la mise en oeuvre de Vitam

.. hint:: Sur Centos, l'interpréteur Python et les packages python requis pour l'exécution d'ansible sur les noeuds gérés sont inclus dans ceux de yum et sont donc généralement présents sur les Centos 7. 

L'outil de déploiement prend en entrée : 

* La topologie de l'environnement (quel composant est installé sur quel serveur) 
* L'ensemble des paramètres de l'environnement

Ces 2 entrées sont définies par l'utilisateur sous la forme de fichiers ansible (fichier d'inventaire et de variables).

.. caution:: L'utilisation d'ansible nécessite les droits root sur l'environnement cible (soit en tant qu'utilisateur root, soit en sudoer) par l'utilisateur linux faisant le déploiement. Le :term:`DIN` contiendra les informations requises pour prendre en compte cet utilisateur.

.. Question : root OK pour déploiement et configuration initiale de l'OS ; par contre, quid de la configuration applicative, qui pourrait être réglée par un utilisateur appartenant au group vitam-admin ? A résoudre dans une version ultérieure

.. warning:: L'utilisation d'une méthode de déploiement autre n'est pas supportée par le projet VITAM.


Architecture de l'outil
-----------------------

On dispose de 2 types de playbooks : 

* 1 playbook de bootstrap qui est joué au plus une fois lors de l'initialisation de nouveaux services (ex : création des utilisateurs vitam (Cf. :doc:`la section dédiée <02-principles-users-rights>`)
* 1 playbook de déploiement qui est le coeur du déploiement

On dispose de 2 types de rôles : 

* rôle "helper" qui est appelé par les autres rôles et qui n'est pas contenu dans les playbook 
* rôle "service" : 1 rôle par service déployé. 

L'ensemble des fichiers de configuration (devant être instancié) seront géré par l'outil de déploiement (via le language de templating Jinja 2)


Gestion des secrets
-------------------

Pour les variables ayant un criticité (au sens de la sécurité - par exemple : les mots de passe de connexion aux bases de données), le déploiement VITAM est compatible avec l'utilisation du module Ansible Vault : celui-ci permet de chiffrer de manière symétrique les variables sensibles. 

.. warning:: Cette fonctionnalité nécessite d'entrer la passphrase du fichier chiffré et donc est difficilement compatible avec une automatisation forte. 

Les certificats (notamment CA et certificats serveur) devront être fournis au préalable et être placés dans les répertoires d'installation mentionnés dans le :term:`DIN`.

A ce jour, seuls les composants frontaux (i.e. faisant partie de la zone Accès) nécessitent un certificat. Pour tout certificat, l'intégralités des certificats des CA de la chaîne de certification devra également être fournie, ainsi que l'URL des CRL associées.

.. seealso:: La liste des secrets nécessaires au bon fonctionnement de VITAM est décrit dans la :doc:`section dédiée </securite/_toc>`.

