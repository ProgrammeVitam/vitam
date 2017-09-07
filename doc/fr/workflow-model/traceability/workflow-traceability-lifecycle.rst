Workflow de création de journal des cycles de vie sécurisé
##########################################################

Introduction
============

Cette section décrit le processus (DefaultLifecycleTraceability) permettant la sécurisation des journaux de cycle de vie mis en place dans la solution logicielle Vitam.
Le workflow mis en place dans la solution logicielle Vitam est défini dans le fichier "DefaultLifecycleTraceability.json". 
Ce fichier est disponible dans : sources/processing/processing-management/src/main/resources/workflows.

Processus de sécurisation des journaux des cycles de vie (vision métier)
========================================================================

Le processus de sécurisation des journaux consiste en la création d'un fichier .zip contenant l'ensemble des journaux de cycles de vie à sécuriser, ainsi que le tampon d'horodatage

Ce fichier zip est ensuite enregistré sur les offres de stockage, en fonction de la stratégie de stockage.


Préparation des listes des cycles de vie
----------------------------------------
- **Step 1** - STP_PREPARE_LC_TRACEABILITY -  distribution sur REF

* Liste cycles de vie à sécuriser - PREPARE_LC_TRACEABILITY - fichier out : GUID/Operations/lastOperation.json & Operations/traceabilityInformation.json
  
  + **Règle** : Récupération des cycles de vie à sécuriser, et récupération des informations concernant les dernières opérations de sécurisation.

  + **Statuts** :

    - OK : les fichiers des cycles de vie ont été exportés (dans UnitsWithoutLevel et ObjectGroup) ainsi que les informations concernant les dernières opérations de sécurisation (PREPARE_LC_TRACEABILITY.OK=Succès du processus de listage des cycles de vie)

    - KO : les informations sur la dernière opération de sécurisation n'ont pas pu être obtenues / exportées, ou un problème a été rencontrée avec un cycle de vie (PREPARE_LC_TRACEABILITY.KO=Échec du processus de listage des cycles de vie)

    - FATAL : une erreur technique est survenue (PREPARE_LC_TRACEABILITY.FATAL=Erreur fatale lors du processus de listage des cycles de vie)


- **Step 2** - STP_OG_CREATE_SECURED_FILE -  distribution sur LIST - fichiers présents dans GUID/ObjectGroup

* Traitement des cycles de vie groupe d'objets - OG_CREATE_SECURED_FILE
  
  + **Règle** : On traite, cycle de vie par cycle de vie, et on génère un fichier sécurisé.

  + **Statuts** :

    - OK : le fichier sécurisé pour le cycle de vie en cours a été généré (STP_OG_CREATE_SECURED_FILE.OK=Succès du processus de gestion des groupes d''objets)

    - KO : le fichier pour le groupe d'objet n'a pas pu être trouvé (STP_OG_CREATE_SECURED_FILE.KO=Échec du processus de gestion des groupes d''objets)

    - FATAL : une erreur technique est survenue (STP_OG_CREATE_SECURED_FILE.FATAL=Erreur fatale lors du processus de gestion des groupes d''objets)

- **Step 3** - STP_UNITS_CREATE_SECURED_FILE -  distribution sur LIST - fichiers présents dans GUID/ObjectGroup

* Traitement des cycles de vie pour les units - UNITS_CREATE_SECURED_FILE
  
  + **Règle** : On traite, cycle de vie par cycle de vie, et on génère un fichier sécurisé.

  + **Statuts** :

    - OK : le fichier sécurisé pour le cycle de vie en cours a été généré (UNITS_CREATE_SECURED_FILE.OK=Succès du processus de sécurisation des cycles de vie des unités archivistiques)

    - KO : le fichier pour le groupe d'objet n'a pas pu être trouvé (UNITS_CREATE_SECURED_FILE.KO=Échec du processus de sécurisation des cycles de vie des unités archivistiques)

    - FATAL : une erreur technique est survenue (UNITS_CREATE_SECURED_FILE.FATAL=Erreur fatale lors du processus de sécurisation des cycles de vie des unités archivistiques)

- **Step 4** - STP_GLOBAL_SECURISATION -  distribution sur REF

* Finalisation de la sécurisation - FINALIZE_LC_TRACEABILITY - fichier in : GUID/Operations/lastOperation.json & Operations/traceabilityInformation.json
  
  + **Règle** : Récupération des différents fichiers générés en Step 2 et 3 puis calcul du timestamp

  + **Statuts** :

    - OK : le fichier zip final a été créé et sauvegarder sur les offres de stockage (FINALIZE_LC_TRACEABILITY.OK=Succès du processus de sécurisation des cycles de vie)

    - KO : le fichier zip n'a pas pu être généré ou sauvegardé sur les offres (FINALIZE_LC_TRACEABILITY.KO=Echec du processus de sécurisation des cycles de vie)

    - FATAL : une erreur technique est survenue (FINALIZE_LC_TRACEABILITY.FATAL=Erreur fatale lors du processus de sécurisation des cycles de vie)

D'une façon synthétique, le workflow est décrit de cette façon :

  .. figure:: images/workflow_lfc_traceability.png
    :align: center

    Diagramme d'activité du workflow de sécurisation des cycles de vie