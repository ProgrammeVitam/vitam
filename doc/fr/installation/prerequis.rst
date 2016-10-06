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
 * présence du package java-1.8.0-openjdk (du fait de la génération de certificats / stores, l'utilitaire ``keytool`` est nécessaire)
 * présence du package openSSL
 * tous les serveurs cible doivent avoir accès au registry docker vitam (docker.programmevitam.fr) => A commenter, car cela n'est nécessaire que dans une installation docker.

Matériel
========

Les prérequis matériel sont définis dans le :term:`DAT` ; à l'heure actuelle, le minimum recommandé pour la solution Vitam est 2 CPUs et 512Mo de RAM disponible par composant applicatif installé sur chaque machine.

Concernant l'espace disque, à l'heure actuelle, aucun pré-requis n'a été défini ; cependant, sont à prévoir par la suite des espace de stockage conséquents pour les composant suivants :

* default-offer
* solution de centralisation des logs (elasticsearch)
* workspace
* elasticsearch des données Vitam

L'arborescence associée sur les partitions associées est : ``/vitam/data/<composant>``


.. include:: ../archi/technique/10-it-services.rst

