Pré-requis
##########

Description
===========

Les pré-requis logiciels suivants sont nécessaires :

* Disposer d'une plate-forme Linux CentOS 7 installée selon la répartition des services souhaitée . En particulier, ces serveurs doivent avoir :

  + une configuration de temps synchronisée (ex: en récupérant le temps à un serveur centralisé)
  + Des autorisations de flux conformément aux besoins décrits dans le :term:`DAT`
  + une configuration des serveurs de noms correcte (cette configuration sera surchargée lors de l'installation)
  + un accès à un dépôt (ou son mirroir) Centos 7 (base et extras) et EPEL 7

* Disposer des binaires VITAM : paquets RPM Vitam (vitam-product) ainsi que les paquets d'éditeurs tiers livrés avec Vitam (vitam-external)
* Disposer de la solution de déploiement basé sur ansible

Le déploiement est orchestré depuis un poste ou serveur d'administration ; les pré-requis suivants doivent y être présents :

* packages RPM nécessaires (fournis par la distribution Centos 7) :

  + ansible (version 2.0.2 minimale et conseillée)
  + openssh-clients (client SSH utilisé par ansible)
  + java-1.8.0-openjdk & openssl (du fait de la génération de certificats / stores, l'utilitaire ``keytool`` est nécessaire)

* un accès ssh vers un utilisateur d'administration avec élévation de privilèges vers les droits root, vitam, vitamdb sur les serveurs cibles.
* Le compte utilisé sur le serveur d'administration doit avoir confiance dans les serveurs cibles (fichier ~/.ssh/known_hosts rempli)

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

Mise en place du déploiement VITAM
==================================

* Sur la machine "ansible", décompacter le tar.gz livré.
* Sur le repository "VITAM", récupérer depuis le tar.gz les rpm et les faire prendre en compte par le repository.
