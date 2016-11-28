Architecture technique détaillée
################################

L'architecture technique s'appuie sur la vision des flux d'information entre les composants d'une part, et le diagramme de déploiement applicatif d'autre part. Les schémas suivants décrivent les connexions réseau établies entre les différents clusters de composants (connexion tcp ou udp). Les détails sur les communications intra-cluster sont abordées plus en détail dans les paragraphes dédiés aux différents composants.

.. figure:: images/technical-architecture-legend.*
    :align: center
    :height: 15 cm

    Flux réseau : légende

Flux métier
===========

Les flux réseaux "métier" sont divisés en 3 schémas pour plus de clarté ; tout d'abord, les flux généraux :

.. figure:: images/technical-architecture-archivistes-archivistes.*
    :align: center

    Architecture technique : flux (1/4 : flux métiers généraux)


Ensuite, les flux dédiés au dépôt des journaux dans le composant "logbook" :

.. figure:: images/technical-architecture-archivistes-logbook.*
    :align: center

    Architecture technique : flux (2/4 : flux métiers de dépôt des journaux)


Enfin, les flux dédiés à la lecture des référentiels en interne de VITAM :

.. figure:: images/technical-architecture-archivistes-referentiels.*
    :align: center

    Architecture technique : flux (3/4 : flux métiers de lecture des référentiels métier)


Flux techniques
===============

A l'inverse des flux métier qui relient les composants instanciés de manière indépendante de leur topologie de déploiement (et notamment de leur colocalication possible), les flux réseaux techniques sont centrés sur la communication entre des composants techniques d'exploitation liés à un hôte (OS) et des composants d'administration ; par conséquent, le schéma ci-dessous se répète pour tout serveur hébergeant un ou plusieurs composant(s) VITAM :

.. figure:: images/technical-architecture-exploitation.*
    :align: center
    :height: 15 cm

    Architecture technique : flux (4/4 : flux techniques)

.. todo Les flux techniques d'accès aux CRL externes ne sont pas représentés sur les schémas ci-dessus. (à intégrer dans une prochaine version)
