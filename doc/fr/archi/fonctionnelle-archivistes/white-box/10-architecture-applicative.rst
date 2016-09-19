Architecture applicative
########################

Drivers de l'architecture
=========================

Les principes d'implémentation applicative ont pour but de faciliter, voire d'assurer les enjeux auxquels le système VITAM est confronté :


* Modèle Open-Source pour la réutilisation dans la sphère publique ainsi que conserver la maîtrise dans le temps du socle logiciel ;
* Couplage lâche entre les composants ;
* Nécessité de pouvoir disposer des composants de générations différentes rendant un même service ;
* Usage d’API REST pour la communication entre composants internes à VITAM, ainsi qu'en extrême majorité pour les services exposés à l'extérieur ;
* Exploitabilité de la solution : limiter le coût d’entrée et de maintenance en : 
    - Intégrant un outillage favorisant le déploiement et les mises à jour de la plateforme
    - Intégrant les éléments nécessaires pour l’exploiter (supervision, sauvegarde, ordonnancement)
    - Enfin, à terme, la solution doit pouvoir tirer partie d’une infrastructure élastique et disposant d’offres de services de stockage diverses (externes)


Services
========

Le système VITAM est découpé en services autonomes interagissant pour permettre de rendre le service global; ce découpage applicatif suit en grande partie le découpage présenté plus haut dans l'architecture fonctionnelle :

.. figure:: images/vitam-applicative-architecture.*
	:align: center

	Architecture applicative et flux d'informations entre composants.

	Chaque service possède un nom propre qui l'identifie de manière unique au sein du système VITAM.

Les services sont organisés en zones logiques :

* Les API externes contiennent les services exposés aux clients (ex: au SIA) ; tout accès externe au système VITAM doit passer par eux. Ils sont responsables notamment de la validation de l'authentification des systèmes externes, de la validation du droit d'accès aux API internes et de l'appel des API internes (principe d'API-Gateway);
* Les services métiers internent hébergent la logique métier de gestion des archives ; ils se subdivisent en :

    - Les services de traitement des archives : ils effectuent tous les traitements concernant les archives (unitaires ou de masse) ;
    - Les services de gestion des référentiels et des métadonnées d'archives : ils permettent de travailler sur les métadonnées des archives (au sens large, i.e. comprenant les référentiels et les journaux).

* Les offres de stockage (internes - i.e. fournies par VITAM - ou externes - i.e. fournies par un tiers) stockent les données d'archives gérées par VITAM ; la sélection de l'offre de stockage à utiliser pour une archive donnée est réalisée en amont (dans le moteur de stockage).
* Enfin, les bases de données métiers stockent les données de travail concernant les archives et leurs traitements (notamment : métadonnées d'archives, journaux, référentiels)

Une dernière zone, optionnelle, consiste en une IHM de démonstration de la solution ; elle accède aux services VITAM via les mêmes API qu'un SIA, et se comporte de manière générale comme un SAE externe.


Détail des flux d'information métier
====================================

On distingue globalement 4 types de flux de données différents :

* Les flux de données d'archives : il portent les informations métiers associées aux contenu des archives (données stockées ou métadonnées associées) ;
* Les flux de commande : ils portent les demandes de traitement d'archives et l'état de ces traitements (et comprennent donc notamment les notifications de fin de traitement) ;
* Les flux de journaux : ils portent les journaux d'évènements (traces probantes des actions réalisées sur les archives) ;
* Les flux de référentiels : ils portent les informations des référentiels hébergés au sein de VITAM (référentiels des formats, des contrats, ...)
