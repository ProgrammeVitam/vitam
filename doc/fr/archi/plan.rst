Proposition de plan
###################

.. note:: Le principe de ce plan est de séparer le document en différentes sections, chaque section ciblant une population particulière de lecteur (architecte fonctionnel, architecte technique, ...)


Présentation générale
=====================

* VITAM
* Objectif et structure du document
* Document(s) de référence
* Modalités de diffusion / license


Périmètre de la solution
========================

* Périmètre métier archivistique
   - Vision métier
      + (référence à la documentation métier ?)
* Périmètre métier exploitation
* Limites de la solution
   - Volumétrie supportée
   - Contraintes sur les interfaces

Architecture fonctionnelle
==========================

* Archi fonctionnelle archivistique
   - Externe
      + Interfaces externes
         * Exposées
            - Principes d'authentification [#urgent1]_
            - (référence à la doc d'API externe ?)
         * Consommées
            - Référentiel d'authentification externe [#urgent1]_
      + Conformité au RGI
   - Interne
      + Composants [#urgent1]_
         * Séparer les composants "natifs" (mandatory) des composants optionnels (offre de stockage Vitam, ...) ?
      + Flux [#urgent1]_
      + Vision des données [#urgent1]_
         * Silos de données des composants
         * Preuve systémique
      + Principes de résilience
      + Dépendances logicielles fournies [#urgent1]_
         * COTS
         * Bibliothèques structurantes
* Archi fonctionnelle exploitation
   - Principes de déploiement
      + Affinités / anti-affinités suggérées
      + Contraintes de zoning proposées
      + Clustering
      + Service discovery
   - Principes du processus de déploiement
      + Installation initiale
      + Principes de maj à chaud
      + Tests de validation du déploiement
   - Principes de paramétrage & configuration des composants
   - Principes de packaging [#urgent1]_
      + RPM
      + DEB
      + Docker (Sur périmètre worker)
   - Principes de suivi de l'état du système
      + Métriques
         * Identification
            - Métier
            - Techniques
            - Indicateurs de SLO
            - Indicateurs de performance
            -    * /stat
         * Centralisation des métriques
      + Logs [#urgent1]_
         * Centralisation des logs
      + Audit
      + Etat des composants
         * status [#urgent1]_
            - /status
         * healthcheck [#urgent1]_
            - /autotest
      + Suivi de l'état de sécurité
      + Suivi de l'état de déploiement
      + Alerting
   - Principes d'administration technique des composants

Archi technique
===============

* Resources
   - Stockage
      + Dossiers & données
      + Droits sur dossiers
      + Abbaques
   - Compute
      + Abbaques
   - Réseau
* Services d'infra
   - DNS
   - NTP
   - Mails
   - Ordonnanceurs techniques / batchs [#urgent1]_
   - Socle d'exécution [#urgent1]_
      + Java
      + OS
      + matrices de compatibilité associées
   - Sécurité
      + PKI
         * HSM ?
* Besoins en résilience / LBHA / ...
* Vision des données
   - Notamment secrets
   - Besoins associés
      + Sauvegarde
      + ...
          
Sécurité
========

* DICT ?
   - Analyse EBIOS "cadre"
* Bonnes pratiques de sécurisation
* Gestion des comptes
* Gestion des secrets
* Principes de cloisonnement
* Normes
   - Normes métier archivistique
   - Normes SI
      + Conformité au RGS
* Principes de MCS

Annexes
=======

* Définitions et Références


.. [#urgent1] Ces items sont les plus urgents
