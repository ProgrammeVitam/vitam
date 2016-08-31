Pré-requis
##########

Description
===========

* Plate-forme Linux CentOS 7
* Packages VITAM (au moment de la rédaction du document, aucun formalisme n'a été défini pour ce point.)
* Solution de déploiement vitam

Le déploiement est orchestré depuis un poste ou serveur d'administration ; les pré-requis suivants doivent y être présents :

 * ansible (version 2.0.2 minimale et conseillée)
 * un accès ssh vers un utilisateur d'administration (« sudoer » sans mot de passe) sur les serveurs cible
 * il est vivement conseillé d'avoir configuré une authentification ssh par certificat vers les serveurs cible
 * tous les serveurs cible doivent avoir accès au registry docker vitam (docker.programmevitam.fr) => A commenter, car cela n'est nécessaire que dans une installation docker.


.. include:: ../archi/technique/10-it-services.rst

