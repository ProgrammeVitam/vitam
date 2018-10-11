Workflow d'administration d'un référentiel des services agent
###############################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un référentiel de services agents

Processus d'import  et mise à jour d'un référentiel de services agents (IMPORT_AGENCIES)
=========================================================================================

L'import d'un référentiel de services agent permet de vérifier le formalisme de ce dernier, notamment que les données obligatoires sont bien présentes pour chacun des agents. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape. Cet import concerne aussi bien l'import initial (pas de services agents pré-existant) que la mise à jour du référentiel.


Import d'un référentiel de services agents IMPORT_AGENCIES (AgenciesService.java)
----------------------------------------------------------------------------------


  + **Règle** :  le fichier remplit les conditions suivantes :

    * il est au format CSV
    * les informations suivantes sont toutes décrites dans l'ordre exact pour chacun des services agents :

    	- Identifier
    	- Name
    	- Description (optionnel)

    * l'identifiant doit être unique

  + **Type** :  bloquant

  + **Statuts** :

    - OK : le fichier respecte les règles (STP_IMPORT_AGENCIES.OK = Succès du processus d'import du référentiel des services agents)

    - KO :

      - Cas 1 : une information concernant les services agents est manquante (Identifier, Name, Description) (STP_IMPORT_AGENCIES.KO = Échec du processus d'import du référentiel des services agents). De plus le rapport d'import du référentiel contiendra la clé "STP_IMPORT_AGENCIES_MISSING_INFORMATIONS".
      - Cas 2 : un service agent qui était présent dans la base a été supprimé (STP_IMPORT_AGENCIES.DELETION.KO = Échec du processus d'import du référentiel des services agents : Des services agents supprimés sont présents dans le référentiel des services agents)

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des services agents (STP_IMPORT_AGENCIES.FATAL = Erreur technique lors du processus d'import du référentiel des service agents)

Vérification des contrats utilisés IMPORT_AGENCIES.USED_CONTRACT
----------------------------------------------------------------------

 

  + **Règle** :  contrôle des contrats utilisant des services agents modifiés

  + **Type** :  bloquant

  + **Statuts** :

    - OK : aucun des services agents utilisés par des contrats d'accès n'a été modifié (STP_IMPORT_AGENCIES.USED_CONTRACT.OK = Succès du processus de vérification des services agents utilisés dans les contrats d'accès)

    - WARNING : un ou plusieurs services agents utilisés par des contrats d'accès ont été modifiés (STP_IMPORT_AGENCIES.USED_CONTRACT.WARNING = Avertissement lors du processus de vérification des services agents utilisés dans les contrats d'accès)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la vérification des services agents utilisés dans les contrats d'accès (STP_IMPORT_AGENCIES.USED_CONTRACT.FATAL = Erreur technique lors du processus de vérification des services agents utilisés dans les contrats d'accès)


Vérification des unités archivistiques IMPORT_AGENCIES.USED_AU
-----------------------------------------------------------------

 

  + **Règle** :  contrôle des unités archivistiques référençant des serivces agents modifiés

  + **Type** :  bloquant  

  + **Statuts** :

    - OK : aucun service agent référencé par les unités archivistiques n'a été modifié (STP_IMPORT_AGENCIES.USED_AU.OK = Succès du processus de vérification des services agents référencés par les unités archivistiques)

    - WARNING : au moins un service agent référencé par une unité archivistique a été modifié (STP_IMPORT_AGENCIES.USED_AU.WARNING =A vertissement lors du processus de vérification des services agents référencés par les unités archivistiques)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la vérification des services agents utilisés par les unités archivistiques (STP_IMPORT_AGENCIES.USED_AU.FATAL=Erreur technique lors du processus de vérification des services agents référencés par les unités archivistiques)


Création du rapport au format JSON AGENCIES_REPORT (AgenciesService.java)
-----------------------------------------------------------------------------



  + **Règle** :  création du rapport d'import de référentiel des services agent

  + **Type** :  bloquant

  + **Statuts** :

    - OK : le rapport d'import du référentiel des services agents a bien été créé (STP_AGENCIES_REPORT.OK = Succès du processus de génération du rapport d'import du référentiel des services agents)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création du rapport d'import du référentiel des services agents (STP_AGENCIES_REPORT.FATAL = Erreur technique lors du processus de génération du rapport d'import du référentiel des services agents)


Sauvegarde du CSV d'import IMPORT_AGENCIES_BACKUP_CSV (AgenciesService.java)
--------------------------------------------------------------------------------



  + **Règle** : sauvegarde de fichier d'import de référentiel des services agent

  + **Type** :  bloquant

  + **Statuts** :

    - OK : le fichier d'import du référentiel des services agent a bien été sauvegardé (STP_IMPORT_AGENCIES_BACKUP_CSV.OK = Succès du processus de sauvegarde du fichier d'import de référentiel des services agents)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la sauvegarde de fichier d'import de référentiel des services agent (STP_AGENCIES_REPORT.FATAL = Erreur technique lors du processus de sauvegarde du fichier d''import de référentiel des services agents)

Sauvegarde d'une copie de la base de donnée BACKUP_AGENCIES (AgenciesService.java)
--------------------------------------------------------------------------------------

  + **Règle** : création d'une copie de la base de données contenant le référentiel des services agents

  + **Type** :  bloquant

  + **Statuts** :

    - OK : la copie de la base de donnée contenant le référentiel des services agents a été crée avec succès (STP_BACKUP_AGENCIES.OK = Succès du processus de sauvegarde du référentiel des services agents)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création d'une copie de la base de données contenant le référentiel des services agent (STP_BACKUP_AGENCIES.FATAL = Erreur technique lors du processus de sauvegarde du référentiel des services agents)

Structure du rapport d'administration du référentiel des services agents
========================================================================

Lorsqu'un nouveau référentiel est importé, la solution logicielle Vitam génère un rapport de l'opération. Ce rapport est en plusieurs parties :

  - "Operation" contient :

    * evType : le type d'opération. Dans le cadre de ce rapport, il s'agit toujours de "STP_IMPORT_AGENCIES"
    * evDateTime : la date et l'heure de l'opération d'import
    * evId : l'identifiant de l'opération

  - "AgenciesToImport" : contient la liste des identifiants contenue dans le fichier
  - "InsertAgencies" : contient les identifiants des services agents ajoutés
  - "UpdatedAgencies" : liste les identifiants des services agents modifiés
  - "UsedAgencies By Contrat" : liste les identifiants des services agents modifiés qui sont utilisés par des contrats d'accès
  - "UsedAgencies By AU" : liste les identifiants des services agents modifiés qui sont utilisés dans des unités archivistiques
  - "UsedAgencies to Delete" : liste les identifiants des services agents supprimés qui sont utilisés dans des unités archivistiques

**Exemple 1 : modification et ajout d'un service agent**

Le rapport généré est :

::

  {
  	"Operation": {
  		"evType": "STP_IMPORT_AGENCIES",
  		"evDateTime": "2017-11-02T15:28:34.523",
  		"evId": "aecaaaaaacevq6lcaamxsak7pvmsdbqaaaaq"
  	},
  	"InsertAgencies": ["Identifier1"],
  	"UpdatedAgencies": ["Identifier0"],
  	"UsedAgencies By Contrat": ["Identifier0"],
  	"UsedAgencies By AU": []
  }


**Exemple 2 : ajout en erreur d'un service agent, causé par un champ obligatoire qui est manquant**


Le rapport généré est :

::

  {
  	"Operation": {
      "evId":"aecaaaaaacflvhgbabrs6alb6vdoehyaaaaq",
  		"evType": "STP_IMPORT_AGENCIES",
  		"evDateTime": "2017-11-02T15:36:03.976"
  	},
  	"AgenciesToImport": ["AG-TNR0002"],
  	"UsedAgencies to Delete":["AG-TNR0002"]
  }
