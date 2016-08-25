API d'Administration
======================

*L'API d'Administration* propose les points d'entrées et les méthodes pour gérer le système, sauf les aspects IaaS/PaaS.

----------

**Configuration**
-------------
**Configuration** est le point d'entrée pour gérer la Configuration globale.


**\*\_Contracts**
-------------
**Access\_Contracts**, **Ingest\_Contracts** et **Preservation\_Contracts** sont les points d'entrées de tous les contrats.
**Freeze\_Access\_Contracts** et **Freeze\_Preservation\_Contracts** sont les points d'entrées des contrats spécifiques liés au Gel.
Il faut y ajouter les **Ingest\_Operations** (configuration technique des opérations à réaliser lors d'un versement) et les **Schemas** (définitions des schémas de métadonnées selon 3 profils :
  - Métadonnées de gestion : a priori un schema unique
  - Métadonnées de description technique : a priori un schema unique
  - Métadonnées de description métier : a priori un par type d'archives (RH/Paye, RH/Congés, Marchés/Facture, Marchés/Commande, ...).


**Access Groups**
-------------
**Access\_Groups** est le point d'entrée pour gérer les groupes d'accès (ensemble de contrats de versement plus les racines associées).


**Authentifications** et **Autorisations**
----------------------------------------
**Applications**, **Roles** et **Profiles** sont les points d'entrées pour toutes les authentifications et les autorisations.


**Formats**
-------------
**Formats** est le point d'entrée pour gérer les formats (description, identifiant).


**Transformations**
-------------
**Transformations** est le point d'entrée sous **Formats** pour gérer les divers règles de transformations pour un format donné. Plusieurs règles peuvent exister, y compris pour obtenir un même autre format. Ces règles sont définies ici mais elles sont ensuite rassemblées en des "Consolidated Transformations" pour former des règles homogènes par usage (conservation, diffusion, ...).


**Consolidated Transformations**
-------------
**Consolidated\_Transformations** est le point d'entrée pour gérer les offres de transformations consolidées (groupes de règles de transformation formant un tout cohérent comme 'Conservation', 'Diffusion', 'Format TXT', 'Diffusion Vignette', ...).


**Transfers**
-------------
**Transfers**  est le point d'entrée pour gérer les transferts utilisant une autre API (comme FTP ou Waarp).


**Consolidated Storages**
-------------
**Consolidated\_Storages** est le point d'entrée pour gérer les offres consolidées de stockage (groupes d'offres de stockage formant un tout cohérent comme 'Conservation', 'Conservation sécurisée', 'Diffusion', 'Diffusion sécurisée').
