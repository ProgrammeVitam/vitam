Pré-requis
##########

Description
===========

Les pré-requis logiciels suivants sont nécessaires :

Base commune
-------------

* Tous les serveurs hébergeant la solution :term:`VITAM` doivent êre synchronisés sur un serveur de temps (pas de stratum 10)
* Disposer de la solution de déploiement basé sur ansible

.. penser à ajouter une note sur /etc/hostname

Le déploiement est orchestré depuis un poste ou serveur d'administration ; les pré-requis suivants doivent y être présents :

* packages nécessaires :

  + ansible (version 2.2 minimale et conseillée)
  + openssh-clients (client SSH utilisé par ansible)
  + java-1.8.0-openjdk & openssl (du fait de la génération de certificats / stores, l'utilitaire ``keytool`` est nécessaire)

* un accès ssh vers un utilisateur d'administration avec élévation de privilèges vers les droits root, vitam, vitamdb sur les serveurs cibles.
* Le compte utilisé sur le serveur d'administration doit avoir confiance dans les serveurs cibles (fichier ~/.ssh/known_hosts correctement renseigné)

.. caution:: dans le cadre de l'installation des packages "extra", il est nécessaire, pour les partitions hébergeant des containeurs docker (mongo-express, head), qu'elles aient un accès internet.

Déploiement sur environnement CentOS
-------------------------------------

* Disposer d'une plate-forme Linux CentOS 7 installée selon la répartition des services souhaitée. En particulier, ces serveurs doivent avoir :

  + une configuration de temps synchronisée (ex: en récupérant le temps à un serveur centralisé)
  + Des autorisations de flux conformément aux besoins décrits dans le :term:`DAT`
  + une configuration des serveurs de noms correcte (cette configuration sera surchargée lors de l'installation)
  + un accès à un dépôt (ou son miroir) CentOS 7 (base et extras) et EPEL 7

* Disposer des binaires VITAM : paquets RPM de VITAM (vitam-product) ainsi que les paquets d'éditeurs tiers livrés avec Vitam (vitam-external)

Déploiement sur environnement Debian
-------------------------------------

* Disposer d'une plate-forme Linux Debian "jessie" installée selon la répartition des services souhaitée. En particulier, ces serveurs doivent avoir :

  + une configuration de temps synchronisée (ex: en récupérant le temps à un serveur centralisé)
  + Des autorisations de flux conformément aux besoins décrits dans le :term:`DAT`
  + une configuration des serveurs de noms correcte (cette configuration sera surchargée lors de l'installation)
  + un accès à un dépôt (ou son miroir) Debian (base et extras) et jessie-backports
  + un accès internet, car le dépôt docker sera ajouté

* Disposer des binaires VITAM : paquets deb de VITAM (vitam-product) ainsi que les paquets d'éditeurs tiers livrés avec Vitam (vitam-external)


Matériel
========

Les prérequis matériel sont définis dans le :term:`DAT` ; à l'heure actuelle, le minimum recommandé pour la solution Vitam est 2 CPUs. Il également est recommandé de prévoir (paramétrage par défaut à l'installation) 512Mo de RAM disponible par composant applicatif :term:`VITAM` installé sur chaque machine (hors elasticsearch et mongo).

Concernant l'espace disque, à l'heure actuelle, aucun pré-requis n'a été défini ; cependant, sont à prévoir par la suite des espaces de stockage conséquents pour les composants suivants :

* storage-offer-default
* solution de centralisation des logs (elasticsearch)
* workspace
* worker (temporairement, lors du traitement de chaque fichier à traiter)
* elasticsearch des données Vitam

L'arborescence associée sur les partitions associées est : ``/vitam/data/<composant>``


