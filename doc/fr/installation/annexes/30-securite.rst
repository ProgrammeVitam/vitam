Ansible & :term:`SSH`
#####################

En fonction de la méthode d'authentification sur les serveurs et d'élevation de privilège, il faut rajouter des options aux lignes de commande ansible. Ces options seront à rajouter pour toutes les commandes ansible du document .

Pour chacune des 3 sections suivantes, vous devez être dans l'un des cas décrits


Authentification du compte utilisateur utilisé pour la connexion :term:`SSH`
============================================================================

Pour le login du compte utilisateur, voir  la section :ref:`inventaire`.

Par clé :term:`SSH` avec passphrase
------------------------------------

Dans le cas d'une authentification par clé avec passphrase, il est nécessaire d'utiliser ssh-agent pour mémoriser la clé privée. Pour ce faire, il faut :

  * exécuter la commande ``ssh-agent <shell utilisé>`` (exemple ``ssh-agent /bin/bash``) pour lancer un shell avec un agent de mémorisation de la clé privée associé à ce shell
  * exécuter la commande ``ssh-add`` et renseigner la passphrase de la clé privée

Vous pouvez maintenant lancer les commandes ansible comme décrites dans ce document.

A noter : ssh-agent est un démon qui va stocker les clés privées (déchiffrées) en mémoire et que le client :term:`SSH` va interroger pour récupérer les informations privées pour initier la connexion. La liaison se fait par un socket UNIX présent dans /tmp (avec les droits 600 pour l'utilisateur qui a lancé le ssh-agent). Cet agent disparaît avec le shell qui l'a lancé.

Par login/mot de passe
----------------------

Dans le cas d'une authentificatioon par login/mot de passe, il est nécessaire de spécifier l'option --ask-pass (ou -k en raccourci) aux commandes ansible ou ansible-playbook de ce document .

Au lancement de la commande ``ansible`` (ou ``ansible-playbook``), il sera demandé le mot de passe

Par clé :term:`SSH` sans passphrase
------------------------------------

Dans ce cas, il n'y a pas de paramétrage particulier à effectuer.

Authentification des hôtes
==========================

Pour éviter les attaques de type :term:`MitM`, le client :term:`SSH` cherche à authentifier le serveur sur lequel il se connecte. Ceci se base généralement sur le stockage des clés publiques des serveurs auxquels il faut faire confiance (~/.ssh/known_hosts).

Il existe différentes méthodes pour remplir ce fichier (vérification humaine à la première connexion, gestion centralisée, :term:`DNSSEC`). La gestion de fichier est hors périmètre Vitam mais c'est un pré-requis pour le lancement d'ansible.


Elevation de privilèges
=======================

Une fois que l'on est connecté  sur le serveur cible, il faut définir la méthode pour accéder aux droits root

Par sudo avec mot de passe
--------------------------

Dans ce cas, il faut rajouter les options ``--ask-sudo-pass``

Au lancement de la commande ``ansible`` (ou ``ansible-playbook``), il sera demandé le mot de passe demandé par ``sudo``

Par su
------

Dans ce cas, il faut rajouter les options ``--become-method=su --ask-su-pass``

Au lancement de la commande ansible (ou ansible-playbook), il sera demandé le mot de passe root

Par sudo sans mot de passe
--------------------------

Il n'y a pas d'option à rajouter (l'élévation par sudo est la configuration par défaut)

Déjà Root
---------

Dans ce cas, il n'y a pas de paramétrages supplémentaires à effectuer.
