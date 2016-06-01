Introduction
############

Le sujet porte notamment sur les **GUID**.

Présentation de la problématique
********************************

Qu’est ce qu’une URL pérenne ?
==============================

* Les URL pérennes sont des adresses internet particulières qui permettent de citer un document numérique, tout en ayant la garantie que ce lien hypertexte ne risque pas de changer.

  * Il existe différents systèmes permettant de créer des URL pérennes. Cela conduit à la gestion d’identifiants pérennes.

Objectifs
=========

* L'objectif de la mise en place de ces URL est de faciliter la "citabilité" et le référencement de documents numériques, donc l’accès (ou mieux encore l’accessibilité, la présence...)

  * Permet d'ajouter un document dans ses favoris, de le citer sur un site Web, dans un mail, sur un blog ou sur les réseaux sociaux (et autres forums), simplement en utilisant l'adresse avec la garantie que l’accès sera préservé dans le temps

* La mise en œuvre d’URL avec identifiants pérennes permet :

  * d'afficher l'identifiant pérenne dans la barre d'URL lors de la consultation d'un document numérisé ;

  * de conserver dans l'URL le nom de domaine du contexte de visualisation (différents services peuvent exposer le même objet numériques avec des visualisations différentes)

  * d'appeler chaque service de visualisation (pagination, table des matières, etc.) dans l'URL à l'aide d'un paramètre simple, nommé "qualifieur" ;

  * d'obtenir plus facilement qu'auparavant l'URL d'une page précise au sein d'un document numérisé.

Préconisation E-ARK
===================

* Requirement 2.4: It SHOULD be possible to identify any Information Package globally uniquely

  * « Globally »  par opposition à « repository » qui vaut pour le SAE en charge à un instant *t*


Solutions envisagées
********************

Identifiants au format ARK et au format "Vitam"

ARK
===

Source : http://tools.ietf.org/id/draft-kunze-ark-15.txt

Forme d'un ARK
--------------

[http://NMAH/]ark:/NAAN/Name[Qualifier]

* [http://NMAH/]

  * Non obligatoire : Indique le lien Web complet, y compris l'URL d'accès.

* ark:/NAAN/Name

  * NAAN indique la référence du contexte (BNF par exemple) via un identifiant attribué

  * Name indique la référence unique de l'objet dans le contexte NAAN

* [Qualifier]

  * Permet de préciser le "type" de ce qu'on veut accéder (métadonnées, original, ...)

Identifiant Vitam
=================

La logique est d'utiliser des GUID (Global Unique Identifier) pour chacun des éléments dans Vitam (Unit, Groupe d'objet, Objets mais aussi Journaux, Logs, Services, ...).


Logique de construction
-----------------------

* Version fixe

* Type d'objet fourni en paramètre

* Domaine métier / Tenant (NAAN) fourni en paramètre et lié au tenant ou à un numéro 0 (interdit sinon) pour le test uniquement.
    * La valeur 1 serait sans doute pour toute la plateforme (information transverse à tous les tenants).

* Identifiant plate-forme fixe par fichier de propriété ou dynamique pour les ccas non Vitam (offres de stockage)

* Processus calculé à l'instanciation de la classe

* Temps UTC dynamique

* Compteur discriminant en fonction du temps UTC (seule zone de calcul en mode "synchronized" pour assurer l'unicité au sein d'une JVM)

* 4 bits de fin à zéro

Logique d'affichage
-------------------

* Vision ARK : ark:/Domaine sur 9 chiffres/reste des informations avec la même logique que la vision Vitam

* Vision Vitam : dans l'ordre et représenté en forme Base 32

  1. Domaine

  #. Version

  #. Type d'objet

  #. Plate-forme

  #. Processus

  #. Temps UTC

  #. Compteur

  #. Non utilisé

Capacité de déconstruction
--------------------------

Il faudra déterminer ce qui pourrait être reconstruit depuis un identifiant Vitam de ce qui ne devrait pas, mais a priori toutes les informations seraient re-constructibles.

1. **Domaine**

   * L'intérêt est de pouvoir déterminer rapidement si un identifiant concerne un Tenant en particulier.

#. Version

   * L'intérêt est de pouvoir déchiffrer très vite sur quelle s'appuie l'identifiant et donc l'extraction des éléments suivants

#. **Type d'objet**

   * Utile dans le cadre d'un service "WhoAmI" calculé sans appel à la base

#. Plate-forme

   * Utile pour la traçabilité des opérations

#. Processus

   * Utile pour la traçabilité des opérations

#. **Temps UTC**

   * Utile pour la détermination a posteriori de l'adéquation du temps "officiel" avec le temps de création de l'ID

#. Compteur

   * A priori sans intérêt particulier (a pour objet uniquement d'éviter les collisions)
