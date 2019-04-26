Architecture applicative
########################


Drivers de l'architecture
=========================

Les principes d'implémentation applicative ont pour but de faciliter, voire d'assurer les enjeux auxquels la solution logicielle :term:`VITAM` est confrontée :


* Modèle *Open-Source* pour la réutilisation dans la sphère publique ainsi que pour conserver la maîtrise dans le temps du socle logiciel ;
* Couplage lâche entre les composants ;
* Nécessité de pouvoir disposer de composants de générations différentes rendant un même service ;
* Usage d’API :term:`REST` pour la communication entre composants internes à :term:`VITAM`, ainsi qu'en extrême majorité pour les services exposés à l'extérieur ;
* Exploitabilité de la solution : limiter le coût d’entrée et de maintenance en : 

    - Intégrant un outillage favorisant le déploiement et les mises à jour de la plateforme ;
    - Intégrant les éléments nécessaires pour l’exploiter (supervision, sauvegarde, ordonnancement) ;
    - Enfin, à terme, la solution doit pouvoir tirer partie d’une infrastructure élastique et disposant d’offres de services de stockage diverses (externes).


Services
========

La solution logicielle :term:`VITAM` est découpée en services autonomes interagissant pour permettre de rendre le service global ; ce découpage applicatif suit en grande partie le découpage présenté plus haut dans l'architecture fonctionnelle. 

Les schémas suivants présentent l'architecture applicative et les flux d'informations entre composants. Tous les composants jaunes sont fournis dans le cadre de la solution logicielle VITAM ; tous sont requis pour le bon fonctionnement de la solution, à l'exception de deux d'entre eux : ``ihm-demo`` et ``storage-offer-default`` (selon les choix de déploiement). Enfin, chaque service possède un nom propre qui l'identifie de manière unique au sein de la solution logicielle :term:`VITAM`.


.. figure:: images/vitam-applicative-architecture-legend.*
    :align: center

    Architecture applicative : légende 


.. figure:: images/vitam-applicative-architecture-datacmd.*
    :align: center
    :height: 10 cm

    Architecture applicative : flux de données d'archives et de commandes    

.. figure:: images/vitam-applicative-architecture-logbook.*
    :align: center
    :height: 10 cm

    Architecture applicative : flux de données de journalisation   
 
.. figure:: images/vitam-applicative-architecture-ref.*
    :align: center
    :height: 10 cm

    Architecture applicative : flux de données de référentiels    



Les services sont organisés en zones logiques :

* Les :term:`API` externes contiennent les services exposés aux clients (ex: à un :term:`SIA`) ; tout accès externe à la solution logicielle :term:`VITAM` doit passer par eux. Ils sont responsables notamment de la validation de l'authentification des systèmes externes, de la validation du droit d'accès aux API internes et de l'appel des API internes (principe d'API-Gateway);
* Les services métiers internes hébergent la logique métier de gestion des archives ; ils se subdivisent en :

    - Les services de traitement des archives : ils effectuent tous les traitements concernant les archives (unitaires ou de masse) ;
    - Les services de recherche et d'accès aux archives : ils permettent de consulter les métadonnées et le contenu des archives ;
    - Les services de gestion des référentiels et des métadonnées d'archives : ils permettent de travailler sur les métadonnées des archives (au sens large, i.e. comprenant les référentiels et les journaux).

* Les offres de stockage (internes - i.e. fournies par VITAM - ou externes - i.e. fournies par un tiers) stockent les données d'archives gérées par VITAM ; la sélection de l'offre de stockage à utiliser pour une archive donnée est réalisée en amont (dans le moteur de stockage).
* Enfin, les bases de données métiers stockent les données de travail concernant les archives et leurs traitements (notamment : métadonnées d'archives, journaux, référentiels)

Une dernière zone, optionnelle, consiste en une IHM de démonstration de la solution. Du point de vue de la solution VITAM, elle se comporte comme un application métier externe ; elle accède notamment aux services VITAM via les mêmes API qu'une application métier.


Détail des flux d'information métier
====================================

On distingue globalement 4 types de flux de données différents :

* Les flux de données d'archives : ils  portent les informations métiers associées aux contenu des archives (données stockées ou métadonnées associées) ;
* Les flux de commande : ils portent les demandes d'exécution de traitement d'archives et l'état de ces exécutions (et comprennent donc notamment les notifications de fin d'exécution de ces traitements) ;
* Les flux de journaux : ils portent les journaux d'évènements (traces probantes des actions réalisées sur les archives) ;
* Les flux de référentiels : ils portent les informations des référentiels hébergés au sein de VITAM (référentiels des formats, des contrats, ...)

.. KWA TODO : présenter des éléments d'architecture complémentaires (notamment sur le fonctionnement workers / processing).


Données métier
==============

Le modèle de donnés métier est décrit dans `un document dédié <http://www.programmevitam.fr/ressources/DocCourante/html/data-model>`_ . 

