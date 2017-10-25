Workflow d'export d'un DIP
##########################

Introduction
============

Cette section décrit le processus (workflow) d'export, utilisé lors de l'export d'un Dissemination Information Package (DIP) dans la solution logicielle Vitam.

Toutes les étapes et actions sont journalisées dans le journal des opérations.
Les étapes et actions associées ci-dessous décrivent le processus d'export de DIP (clé et description de la clé associée dans le journal des opérations) tel qu'implémenté dans la version actuelle de la solution logicielle Vitam.

Création du Bordereau (STP_CREATE_MANIFEST)
============================================

Création du Bordereau (CREATE_MANIFEST)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : Création un bordereau contenant les unités archivistiques soumises au service d'export de DIP, ainsi que le groupes d'objets techniques et objets-données qui leurs sont associés

+ **Type** : bloquant

+ **Statuts** :

  - OK : le bordereau contenant les descriptions des unités archivistiques, groupes d'objet techniques et objets-données a été créé avec succès (CREATE_MANIFEST.OK=Succès de la création du manifest)

  - KO : la création du bordereau contenant les descriptions des unités archivistiques, groupes d'objet techniques et objets-données a échouée car des informations étaient manquantes, érronées ou inconnues (CREATE_MANIFEST.KO=Echec de la création du manifest)

  - FATAL : une erreur technique est survenue lors de la création du bordereau (CREATE_MANIFEST.FATAL=Erreur fatale lors de la création du manifest)

Déplacement des objets binaires vers le workspace (STP_PUT_BINARY_ON_WORKSPACE)
========================================================================================

Déplacement des objets binaires vers le workspace (PUT_BINARY_ON_WORKSPACE)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : Déplacement des objets-données mentionnées dans le bordereau vers le workspace

+ **Type** : bloquant

+ **Statuts** :

  - OK : les objets-données ont été déplacés vers le workspace avec succès (PUT_BINARY_ON_WORKSPACE.OK=Fin du déplacement des objets binaires de stockage vers workspace)

  - KO : le déplacement des objet-données vers le workspace a échoué car un ou plusieurs de ces objets étaient introuvables (PUT_BINARY_ON_WORKSPACE.KO=Echec du déplacement des objets binaires de stockage vers workspace)

  - FATAL : une erreur technique est survenue lors du déplacement des objets binaires de stockage vers le workspace (PUT_BINARY_ON_WORKSPACE.FATAL=Echec du déplacement des objets binaires de stockage vers workspace)
    
Stockage du manifest compressé (STP_STORE_MANIFEST)
===================================================

Stockage du manifest compressé (STORE_MANIFEST)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

+ **Règle** : Création et enregistrement du DP sur les offres de stockage

+ **Type** : bloquant

+ **Statuts** :

  - OK : Le DIP a été créé et stocké sur les offres de stockages avec succès (STORE_MANIFEST.OK=Succès du stockage du manifest compressé)

  - KO : Pas de cas KO

  - FATAL :  une erreur technique est survenue lors du déplacement des objets binaires de stockage vers workspace (STORE_MANIFEST.FATAL=Erreur fatale lors du stockage du manifest compressé)

Structure du Workflow d'export de DIP
=====================================

Le workflow d'export de DIP actuel mis en place dans la solution logicielle Vitam est défini dans l’unique fichier “ExportUnitWorkflow.json”. Ce fichier est disponible dans /sources/processing/processing-management/src/main/resources/workflows.

D’une façon synthétique, le workflow est décrit de cette façon :