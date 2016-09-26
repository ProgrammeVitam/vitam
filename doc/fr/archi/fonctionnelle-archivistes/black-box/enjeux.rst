Drivers du projet
#################

Enjeux
======

Les enjeux de VITAM se répartissent en 3 grandes catégories :

- Les enjeux liés au respect des processus métier d'archivage ; il s'agit de permettre l'identification, le maintien de la disponibilité et de la sécurité, ainsi que le maintien du contrôle sur les documents confiés à VITAM. Dans le cas particulier de l’archivage définitif, VITAM doit permettre l'utilisation des documents à des fins historiques liée à leur réutilisation, et permettre la conservation de documents dont la DUA est échue mais ayant vocation à être conservés indéfiniment.

- Les enjeux liés au volume, à la variété et aux besoin de performances des traitements des données gérées par VITAM :

    + VITAM doit pouvoir gérer la conservation et l'accès de volumes élevés d'archives numériques (> 10 :sup:`10` objets, 10 To => 10 Po), tout en garantissant une perte de données nulle (:term:`PDMA` ~ 0) pour les données qui ont été « acceptées » par VITAM après acquittement d’un versement, ainsi que pour l’ensemble des données nécessaires pour assurer la preuve systémique de la plateforme (journaux des opérations, du cycle de vie, du SAE) ;
    + VITAM doit pouvoir gérer un large éventail de types de données archivées, et ce notamment dans le temps, et notamment une forte variété de métadonnées descriptives des archives et une forte variété de type de format des objets numériques ;
    + VITAM doit être performant dans ses capacités de gestion des données archivées, et notamment permettre de répondre à des requêtes des recherches simples en quelques secondes, à des recherches complexes archivistiques en quelques minutes et à une demande d'accès à quelconque contenu quelconques en une dizaine de secondes.

- Les enjeux liés à la sécurité, en fournissant un accès sécurisé et contrôlé ainsi qu'en garantissant la traçabilité des actions (gestion notamment de documents devant conserver leur valeur probante). En outre, VITAM doit permettre de garantir une très longue durée d’accès et de conservation ( > 50 ans) des archives, et doit notamment pouvoir de résister à l’obsolescence informatique.


Contraintes et objectifs
========================

L'accès aux archives numériques doit être facile :

* Adapté : Services Web, Nouveaux média

* Interopérable : RGI et respect des standards ou normes d'échange et de communication

* Requêtable : le SAE doit fournir un service de recherche, tout comme un SGBD fournit une interface de requêtes des bases qu'il héberge

* Mutualisable :

   - le SAE doit pouvoir fournir un plan de classement multiple et une capacité d'accès depuis plusieurs applications
   - le SAE doit pouvoir gérer des dizaines de milliards d'entrées et leur métadonnées associées avec une variabilité des formats des unités d'archives (objets numériques) et des descriptions associées (métadonnées)

L'accès aux archives numériques doit être rapide :

* Le temps d'accès pour une archive unitaire (un document) ou des métadonnées doit être compatible avec les technologies actuelles (Cf. le paragraphe précédent) ;
* Pour les accès à des lots d'archives, les moyens utilisés doivent être appropriés :

   - Via un support physique
   - Via un téléchargement de masse

* Du fait de la sensibilité des données :

   - L'accès doit être sécurisé (Réseau, Protocolaire, Filtrage)
   - L'accès doit être contrôlé (sur la base de contrats et de filtres métiers associés)


Positionnement
==============

VITAM est un back-office pouvant s’interfacer à tout front-office (utilisateur) devant accéder à des données archivées (pas nécessairement pour de l’archivage définitif). Il disposera cependant des IHM d’administration pour l’administration technique et fonctionnelle de la plate forme ainsi que d’une IHM minimale pouvant pallier à l’absence temporaire d’un front-office.

VITAM a pour but d'être largement réutilisable, et ce notamment en se basant sur l'usage de standards métiers (ex : SEDA pour les versements)

Enfin, le socle logiciel doit pouvoir être utilisable pendant 20 ans (en incluant les évolutions technologiques). 



 