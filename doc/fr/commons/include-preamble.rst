Rappels
#######

Information concernant les licences
===================================

Le logiciel :term:`VITAM` est publié sous la license `CeCILL 2.1 <http://www.cecill.info/licences/Licence_CeCILL_V2.1-fr.html>`_ ; la documentation associée (comprenant le présent document) est publiée sous license `CC-BY-SA 3.0 <https://creativecommons.org/licenses/by-sa/3.0/fr/legalcode>`_.

Documents de référence
======================

Documents internes
------------------

.. csv-table:: Documents de référence VITAM
   :header: "Nom", "Lien"
   :widths: 10, 20

   ":term:`DAT`","(à renseigner)"
   ":term:`DIN`","(à renseigner)"
   ":term:`DEX`","(à renseigner)"
   "Release notes","(à renseigner)"


Référentiels externes
---------------------

   Référentiel Général d’Interopérabilité [RGI]
      V1.0 du 12 juin 2009 approuvé par arrêté du Premier ministre du 9 novembre 2009
      
      Règles d’interopérabilité (format, protocoles, encodages, etc.) rentrant dans le champ d’application de l’ordonnance n°2005-1516 du 8 décembre 2005 relative aux échanges électroniques entre les usagers et les autorités administratives et entre les autorités administratives. 
      
      https://references.modernisation.gouv.fr/rgi-interoperabilite


   Référentiel Général de Sécurité [RGS]
      V2.0 du 13 juin 2014 approuvé par arrêté du Premier ministre du 13 juin 2014
      
      Le RGS précise les règles de sécurité s’imposant aux autorités administratives dans la sécurisation de leur SI et notamment sur les dispositifs de sécurité relatifs aux mécanismes cryptographiques et à l’utilisation de certificats électroniques et contremarques de temps. Le RGS propose également des bonnes pratiques en matière de SSI.
      Le RGS découle de l’application de l’ordonnance n°2005-1516 du 8 décembre 2005 relative aux échanges électroniques entre les usagers et les autorités administratives et entre les autorités administratives.
      
      https://references.modernisation.gouv.fr/rgs-securite


   Norme OAIS (ISO 14721:2012 – 1 septembre 2012)
      Systèmes de transfert desinformations et données spatiales -- Système ouvert d'archivage d'information (SOAI) - Modèle de référence
      

   Standard d’échange de données pour l’archivage (SEDA)
      Transfert, communication, élimination, restitution, modification – Version 1.0 – Septembre 2012

      Cadre normatif pour les différents échanges d’informations entre les services d’archives publics et leurs partenaires : entités productrices des archives, entités gestionnaires, entités de contrôle des processus, et enfin entités qui utilisent ces archives. Il concerne également les échanges entre plusieurs services d’archives (services publics d'archives, prestataires d'archivage, archivage intermédiaire, archivage définitif).
      
      http://www.archivesdefrance.culture.gouv.fr/seda/

Glossaire
=========

.. glossary::

   COTS
      Component Off The Shelves ; il s'agit d'un composant "sur étagère", non développé par le projet :term:`VITAM`, mais intégré à partir d'un binaire externe. Par exemple : MongoDB, ElasticSearch.

   DIN
      Dossier d'Installation

   DEX
      Dossier d'EXploitation

   DAT
      Dossier d'Architecture Technique

   IHM
     Interface Homme Machine

   VITAM
     Valeurs Immatérielles Transférées aux Archives pour Mémoire

   RPM
     Red Hat Package Manager ; il s'agit du format de packets logiciels nativement utilisé par les distributions CentOS (entre autres)

   API
      Application Programming Interface

   BDD
      Base De Données

   JRE
      Java Runtime Environment ; il s'agit de la machine virtuelle Java permettant d'y exécuter les programmes compilés pour.

   JVM
      Java Virtual Machine ; Cf. :term:`JRE`

   PDMA
      Perte de Données Maximale Admissible ; il s'agit du pourcentage de données stockées dans le système qu'il est acceptable de perdre lors d'un incident de production.

   NoSQL
      Base de données non-basée sur un paradigme classique des bases relationnelles. `Définition <https://fr.wikipedia.org/wiki/NoSQL>`_

   MitM
      L'attaque de l'homme du milieu (HDM) ou `man-in-the-middle attack` (MITM) est une attaque qui a pour but d'intercepter les communications entre deux parties, sans que ni l'une ni l'autre ne puisse se douter que le canal de communication entre elles a été compromis. Le canal le plus courant est une connexion à Internet de l'internaute lambda. L'attaquant doit d'abord être capable d'observer et d'intercepter les messages d'une victime à l'autre. L'attaque « homme du milieu » est particulièrement applicable dans la méthode d'échange de clés Diffie-Hellman, quand cet échange est utilisé sans authentification. Avec authentification, Diffie-Hellman est en revanche invulnérable aux écoutes du canal, et est d'ailleurs conçu pour cela. `Explication <https://fr.wikipedia.org/wiki/Attaque_de_l'homme_du_milieu>`_

   DNSSEC 
      `Domain Name System Security Extensions` est un protocole standardisé par l'IETF permettant de résoudre certains problèmes de sécurité liés au protocole DNS. Les spécifications sont publiées dans la RFC 4033 et les suivantes (une version antérieure de DNSSEC n'a eu aucun succès). `Définition DNSSEC <https://fr.wikipedia.org/wiki/Domain_Name_System_Security_Extensions>`_

   PKI
      Une infrastructure à clés publiques (ICP) ou infrastructure de gestion de clés (IGC) ou encore Public Key Infrastructure (PKI), est un ensemble de composants physiques (des ordinateurs, des équipements cryptographiques logiciels ou matériel type HSM ou encore des cartes à puces), de procédures humaines (vérifications, validation) et de logiciels (système et application) en vue de gérer le cycle de vie des certificats numériques ou certificats électroniques. `Définition PKI <https://fr.wikipedia.org/wiki/Infrastructure_%C3%A0_cl%C3%A9s_publiques>`_

   SIA
      Système d'Informations Archivistique

   OAIS
      `Open Archival Information System`, acronyme anglais pour Systèmes de transfert desinformations et données spatiales -- Système ouvert d'archivage d'information (SOAI) - Modèle de référence.

   TNR
      Tests de Non-Régression