Pré-requis plate-forme
######################

Les pré-requis suivants sont nécessaires :


Base commune
============

* Tous les serveurs hébergeant la solution logicielle :term:`VITAM` doivent êre synchronisés sur un serveur de temps (protocole :term:`NTP`, pas de *stratum* 10)
* Disposer de la solution de déploiement basée sur ansible

.. penser à ajouter une note sur /etc/hostname

Le déploiement est orchestré depuis un poste ou serveur d'administration ; les pré-requis suivants doivent y être présents :

* packages nécessaires :

  + **ansible** (version **2.14** minimale et conseillée ; se référer à la `documentation ansible <http://docs.ansible.com/ansible/latest/intro_installation.html>`_ pour la procédure d'installation)
  + **openssh-client** (client SSH utilisé par ansible)
  + **JRE OpenJDK 21** et **openssl** (du fait de la génération de certificats / *stores*, l'utilitaire ``keytool`` est nécessaire)

* un accès ssh vers un utilisateur d'administration avec élévation de privilèges vers les droits ``root``, ``vitam``, ``vitamdb`` (les comptes ``vitam`` et ``vitamdb`` sont créés durant le déploiement) sur les serveurs cibles.
* Le compte utilisé sur le serveur d'administration doit avoir confiance dans les serveurs sur lesquels la solution logicielle :term:`VITAM` doit être installée (fichier ``~/.ssh/known_hosts`` correctement renseigné)

.. note:: Se référer à la `documentation d'usage <http://docs.ansible.com/ansible/latest/intro_getting_started.html>`_ pour les procédures de connexion aux machines-cibles depuis le serveur ansible.

.. caution:: Les adresses :term:`IP` des machines sur lesquelles la solution logicielle :term:`VITAM` sera installée ne doivent pas changer d'adresse IP au cours du temps. En cas de changement d'adresse IP, la plateforme ne pourra plus fonctionner.

.. caution:: Aucune version pré-installée de la JRE OpenJDK ne doit être présente sur les machines cibles où sera installé :term:`VITAM`.

.. caution:: La solution :term:`VITAM` ne tolère qu'une très courte désynchronisation de temps entre les machines (par défaut, 10 secondes). La configuration NTP doit être finement monitorée. Idéalement une synchronisation doit être planifiée chaque 5/10 minutes.

.. caution:: Dans le cadre de l'installation des packages "extra", il est nécessaire, pour les partitions hébergeant des containeurs docker (mongo-express, head), qu'elles aient un accès internet (installation du paquet officiel ``docker``, récupération des images).

.. caution:: Dans le cadre de l'installation des packages "extra", il est nécessaire, pour les partitions hébergeant le composant ``ihm-recette``, qu'elles aient un accès internet (installation du `repository` et installation du `package` ``git-lfs`` ; récupération des :term:`TNR` depuis un dépôt git).

.. warning:: Dans le cas d'une installation du composant ``vitam-offer`` en ``filesystem-hash``, il est fortement recommandé d'employer un système de fichiers ``xfs`` pour le stockage des données. Se référer au :term:`DAT` pour connaître la structuration des *filesystems* dans la solution logicielle :term:`VITAM`. En cas d'utilisation d'un autre type, s'assurer que le filesystem possède/gère bien l'option ``user_xattr``.

.. warning:: Dans le cas d'une installation du composant ``vitam-offer`` en ``tape-library``, il est fortement recommandé d'installer au préalable sur les machines cible associées les paquets pour les commandes ``mt``, ``mtx`` et ``dd``. Ces composants doivent également apporter le groupe système ``tape``. Se reporter également à :ref:`prerequisoffrefroide`.

PKI
===

La solution logicielle :term:`VITAM` nécessite des certificats pour son bon fonctionnement (cf. :term:`DAT` pour la liste des secrets et :doc:`/annexes/10-overview_certificats` pour une vue d'ensemble de leur usage.) La gestion de ces certificats, par le biais d'une ou plusieurs :term:`PKI`, est à charge de l'équipe d'exploitation. La mise à disposition des certificats et des chaînes de validation :term:`CA`, placés dans les répertoires de déploiement adéquats, est un pré-requis à tout déploiement en production de la solution logicielle :term:`VITAM`.

.. seealso:: Veuillez vous référer à la section :doc:`/annexes/10-overview_certificats` pour la liste des certificats nécessaires au déploiement de la solution VITAM, ainsi que pour leurs répertoires de déploiement.


Systèmes d'exploitation
=======================

Seules deux distributions Linux suivantes sont supportées à ce jour :

* AlmaLinux 9
* Debian 12 (bookworm)

SELinux doit être configuré en mode ``permissive`` ou ``disabled``. Toutefois depuis la release R13, la solution logicielle :term:`VITAM` prend désormais en charge l'activation de SELinux sur le périmètre du composant worker et des processus associés aux *griffins* (greffons de préservation).

.. note:: En cas de changement de mode SELinux, redémarrer les machines pour la bonne prise en compte de la modification avant de lancer le déploiement.

.. Sujets à adresser : préciser la version minimale ; donner une matrice de compatibilité -> post-V1

.. caution:: En cas d'installation initiale, les utilisateurs et groupes systèmes (noms et :term:`UID`) utilisés par VITAM (et listés dans le :term:`DAT`) ne doivent pas être présents sur les serveurs cible. Ces comptes sont créés lors de l'installation de VITAM et gérés par VITAM.

Déploiement sur environnement AlmaLinux
---------------------------------------

* Disposer d'une plate-forme Linux AlmaLinux 9 installée selon la répartition des services souhaités. En particulier, ces serveurs doivent avoir :

  + une configuration de temps synchronisée (ex: en récupérant le temps à un serveur centralisé)
  + Des autorisations de flux conformément aux besoins décrits dans le :term:`DAT`
  + une configuration des serveurs de noms correcte (cette configuration sera surchargée lors de l'installation)
  + un accès à un dépôt (ou son miroir) AlmaLinux 9 (base et extras) et EPEL 9

* Disposer des binaires VITAM : paquets :term:`RPM` de VITAM (vitam-product) ainsi que les paquets d'éditeurs tiers livrés avec VITAM (vitam-external)
* Disposer, si besoin, des binaires pour l'installation des *griffins*

Déploiement sur environnement Debian
------------------------------------

* Disposer d'une plate-forme Linux Debian 12 "bookworm" installée selon la répartition des services souhaitée. En particulier, ces serveurs doivent avoir :

  + une configuration de temps synchronisée (ex: en récupérant le temps à un serveur centralisé)
  + Des autorisations de flux conformément aux besoins décrits dans le :term:`DAT`
  + une configuration des serveurs de noms correcte (cette configuration sera surchargée lors de l'installation)
  + un accès à un dépôt (ou son miroir) Debian (base et extras) et bookworm-backports
  + un accès internet, car le dépôt docker sera ajouté

* Disposer des binaires VITAM : paquets deb de VITAM (vitam-product) ainsi que les paquets d'éditeurs tiers livrés avec VITAM (vitam-external)
* Disposer, si besoin, des binaires pour l'installation des *griffins*

Présence d'un agent antiviral
-----------------------------

Dans le cas de partitions sur lesquelles un agent antiviral est déjà configuré (typiquement, *golden image*), il est recommandé de positionner une exception sur l'arborescence ``/vitam`` et les sous-arborescences, hormis la partition hébergeant le composant ``ingest-external`` (emploi d'un agent antiviral en prérequis des *ingest* ; se reporter à :ref:`confantivirus`).

Matériel
========

Les prérequis matériel sont définis dans le :term:`DAT` ; à l'heure actuelle, le minimum recommandé pour la solution Vitam est 2 CPUs. Il également est recommandé de prévoir (paramétrage par défaut à l'installation) 512Mo de RAM disponible par composant applicatif :term:`VITAM` installé sur chaque machine (hors elasticsearch et mongo).

Concernant l'espace disque, à l'heure actuelle, aucun pré-requis n'a été défini ; cependant, sont à prévoir par la suite des espaces de stockage conséquents pour les composants suivants :

* offer
* solution de centralisation des logs (`cluster` elasticsearch de log)
* workspace
* worker (temporairement, lors du traitement de chaque fichier à traiter)
* `cluster` elasticsearch et mongodb des données :term:`VITAM`

L'arborescence associée sur les partitions associées est : ``/vitam/data/<composant>``

Librairie de cartouches pour offre froide
=========================================

Des prérequis sont à réunir pour utiliser l'offre froide de stockage "tape-library" définie dans le :term:`DAT`.

* La librairie de cartouches doit être opérationnelle et chargée en cartouches.
* La librairie et les lecteurs doivent déjà être configurés sur la machine devant supporter une instance de ce composant. La commande ``lsscsi -g`` peut permettre de vérifier si des périphériques sont détectés.
* Le dossier ``/vitam/data/offer/`` doit correspondre à une seule partition de système de fichiers (i.e. tout le contenu du dossier ``/vitam/data/offer`` doit appartenir au même point de montage). Le système de fichiers doit supporter les opérations de atomiques (type atomic rename / move) et la création de liens symboliques (ex. XFS, EXT4...)
