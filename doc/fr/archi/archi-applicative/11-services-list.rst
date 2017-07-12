Services métiers
################

Les services métiers sont présentés dans les sections suivantes ; pour chaque service, est indiqué son nom commun (en français), ainsi que le nom de service correspondant (en anglais, basé sur les usages :term:`OAIS`).


Moteur d’exécution (processing)
===============================

Rôle :

* Exécution massive de processus métiers complexes
* Utilisé notamment lors du versement et de la préservation

Fonctions :

* Découpage en micro tâches de processus métier (en fonction d’un référentiel)
* Supervision de l’état d’exécution de chaque « job »
* Reprise sur incident
* Traçabilité de l’ensemble des actions effectuées

Contraintes techniques :

* Grand nombre de tâches
* La durée d’exécution d’un ensemble de tâches peut être long (ex: une campagne de transformation de document peut durer plusieurs semaines, voire mois)
* Possibilité de devoir gérer des objets lourds ; cela implique notamment l'usage de l’espace de travail pour passer des informations entre tâches, et des optimisations (colocalisations ou copies directes) permettant de limiter les contraintes sur le réseau.

Données gérées :

* Etat des workflow en cours d'exécution


Moteur de stockage (storage)
============================

Rôle :

* Stockage des données (Méta Données, Objets Numériques et journaux SAE et de l’archive)

Fonctions :

* Utilisation de stratégie de stockage (abstraction par rapport aux offres de stockage sous-jacentes)
* Gestion des différentes offres de stockage

Données gérées :

* Journaux d'écriture


Espace de travail (workspace)
=============================

Rôle :

* Fourniture d'un espace pour l'échange de fichiers (et faire un appel par pointeur lors des appels entre composants) entre les différents composants de Vitam

Fonctions :

* Utilisation du moteur de stockage dans un mode minimal (Opérations CREATE, READ, DELETE sur 1 seule offre de stockage)

Contraintes techniques :

* Être résilient à une panne simple

Données gérées :

* Données temporaires en cours de traitement


Worker (worker)
===============

Rôle :

* Effectuer les traitements de masse sur les archives & paquets d'archive (SIP / ...)

Fonction :

* Déclenchement des opérations sur requête du moteur d'exécution ;
* Gestion d'un cache local des éléments traités, en interactions avec l'espace de travail ;

Données gérées :

* Aucune ; il s'agit d'un composant de traitement pur


Moteur de données (metadata)
============================

Rôle :

* Stocker de manière requêtable et rapide les métadonnées des objets (également stockées mais de manière pérenne dans l’offre de stockage)

Fonctions :

* Fournit une API agrégeant une technologie de base de données et un moteur d’indexation
* Fournit un cache des requêtes pour optimisation

Données gérées :

* Métadonnées et structures des archives : Archive Units, Object Group ;


Moteur de journalisation (logbook)
==================================

Rôle :

* Gérer les journaux métiers à fort besoin d'intégrité et potentiellement à valeur probante : journal du cycle de vie, journal métier (SAE/opérations + écritures)

Fonctions :

* Appel uniquement à partir de l’application

Contraintes techniques :

* Besoin fort de fiabilité

Données gérées :

* Journaux de cycle de vie (JCV)
* Journaux d'opérations (JOP)
* Eléments de preuve issus de la sécurisation des journaux précédents


Gestion des référentiels (functional-administration)
====================================================

Rôle :

* Gérer les réfentiels métier de la plate-forme

Fonctions :

* Gestion du référentiel des formats (PRONOM)
* Gestion des règles de gestion des archives

Données gérées :

* Référentiels techniques et métiers :

    - Formats
    - Règles de gestion
    - Contrats (d'entrée, d'accès)
    - Contextes
    - Profils
    - Arbre de positionnement
    - ...


Offre de stockage par défaut (storage-offer-default)
====================================================

Rôle :

* Fournir une offre de stockage par défaut permettant la persistance des objets sur un système de fichier local

Fonctions :

* Offre de stockage fournie par défaut
* Stockage simple des objets numériques sur un système de fichiers local

Données gérées :

* Tout ce qui doit être conservé à long terme (mais uniquement pour la gestion technique de ces données)


Moteur d'entrée (ingest-internal)
=================================

Rôle :

* Permettre l'entrée d'une archive SEDA dans le SAE

Fonctions :

* Upload HTTP de fichiers au format SEDA
* Sas de validation antivirus des fichiers entrants
* Persistance du SEDA dans workspace
* Lancement des workflows de traitements liés à l'entrée dans processing

Données gérées :

* Aucune


Moteur d'accès (access-internal)
================================

Rôle :

* Permettre l'accès aux données du système VITAM

Fonction :

* Exposition des fonctions de recherche d'archives offertes par metadata ;
* Exposition des fonctions de parcours de journaux offertes par logbook.

Données gérées :

* Aucune

.. A terme, il y aura les journaux d'accès


API externes (ingest-external et access-external)
=================================================

Rôle :

* Exposer les API publiques du système
* Sécuriser l'accès aux API de VITAM

Contraintes techniques :

* Authentification forte requise de la part des clients
* WAF

Données gérées :

* Pour ingest-external : SIP dans le sas d'entrée (conservés uniquement pendant leur analyse antivirus)


Interface de démonstration (ihm-demo)
=====================================

Rôle :

* Permettre une utilisation basique de VITAM, notamment sans SIA

Fonctions :

* Représentation des arborescences et des graphes
* Formulaires dynamiques
* Suivi des opérations
* Gestion des référentiels

Contraintes techniques :

* IHM intuitive (sans workflows métiers), accessible (au sens RGAA), « responsive design» (gestion des résolutions différentes tout en restant sur des écrans « PC » (15’’ et +))
* Compatibilité avec les navigateurs actuels
* Pas d’applets/clients lourds

Données gérées :

* Aucune
