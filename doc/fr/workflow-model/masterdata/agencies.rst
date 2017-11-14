Workflow d'administration d'un référentiel des services agent
###############################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer un référentiel de services agents

Processus d'import  et mise à jour d'un référentiel de services agents (STP_IMPORT_AGENCIES)
============================================================================================

L'import d'un référentiel de services agent permet de vérifier le formalisme de ce dernier, notamment que les données obligatoires sont bien présentes pour chacun des agents. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape. Cet import concerne aussi bien l'import initial (pas de services agents pré-existant) que la mise à jour du référentiel.


Import d'un référentiel de services agents (STP_IMPORT_AGENCIES)
----------------------------------------------------------------

  + **Type** :  bloquant

  + **Règle** :  le fichier rempli les conditions suivantes :

    * il est au format CSV
    * les informations suivantes sont toutes décrites dans l'ordre exact pour chacun des services agents :

      - Identifier
      - Name
      - Description (optionnel)

    * l'identifiant doit être unique


  + **Status** :

    - OK : le fichier respecte les règles (STP_IMPORT_AGENCIES.OK=Succès de l''import du référentiel des service producteurs)

    - KO :

        - Cas 1 : une information concernant les services agent est manquante (Identifier, Name, Description) (STP_IMPORT_AGENCIES.KO=Échec de l''import du référentiel des service producteurs)
        - Cas 2 : un service agent qui était présent dans la base a été supprimé (STP_IMPORT_AGENCIES.DELETION.KO=Des services agents supprimés sont présents dans le référentiel des services agent)

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des services agents (STP_IMPORT_AGENCIES.FATAL=Erreur fatale lors de l''import du référentiel des service producteurs)

Vérification des contrats utilisés (STP_IMPORT_AGENCIES.USED_CONTRACT)
----------------------------------------------------------------------

  + **Règle** :  contrôle des contrats utilisant des services agents modifiés

  + **Status** :

    - OK : aucun des services agent utilisés par des contrats d'accès n'a été modifié (STP_IMPORT_AGENCIES.USED_CONTRACT.OK=Succès de la vérification des services agents utilisés dans les contrats d'accès)

    - WARNING : un ou plusieurs services agent utilisé par des contrats d'accès ont été modifiés (STP_IMPORT_AGENCIES.USED_CONTRACT.WARNING=Avertissement lors de la vérification des services agents utilisés dans les contrats d'accès)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la vérification des services agents utilisés dans les contrats d'accès (STP_IMPORT_AGENCIES.USED_CONTRACT.FATAL=Erreur fatale lors de la vérificationdes services agents utilisés dans les contrats d'accès)

Vérification des contrats utilisés (STP_IMPORT_AGENCIES.USED_AU)
----------------------------------------------------------------

  + **Règle** :  contrôle des unité archivistiques référençant des serivces agents modifiés

  + **Status** :

    - OK : aucun service agent référencé par les unités archivistiques n'ont été modifiés (STP_IMPORT_AGENCIES.USED_AU.OK=succès de la vérification des services agents utilisés par les unités archivistiques)

    - WARNING : au moins un service agent référencé par une unité archivistique a été modifié (STP_IMPORT_AGENCIES.USED_AU.WARNING=Avertissement lors de la vérification des services agents utilisés par les unités archivistiques)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la vérification des services agents utilisés par les unités archivistiques (STP_IMPORT_AGENCIES.USED_AU.FATAL=Erreur fatale lors de la vérification des services agents utilisés par les unités archivistiques)

Création du rapport au format JSON (STP_AGENCIES_REPORT)
--------------------------------------------------------

  + **Règle** :  création du rapport d'import de référentiel des services agent

  + **Status** :

    - OK : le rapport d'import du référentiel des services agent a bien été créé (STP_AGENCIES_REPORT.OK=Succès de la génération du rapport au format JSON)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création du rapport d'import de référentiel des services agents (STP_AGENCIES_REPORT.FATAL=Erreur fatale lors de la génération du rapport au format JSON)

Sauvegarde du CSV d'import (STP_AGENCIES_CSV)
---------------------------------------------

  + **Règle** : sauvegarde de fichier d'import de référentiel des services agent

  + **Status** :

    - OK : le fichier d'import du référentiel des services agent a bien été sauvegardé (STP_AGENCIES_CSV.OK=Succès de l''enregistrement du fichier d''import du référentiel des services agent)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la sauvegarde de fichier d'import de référentiel des services agent (STP_AGENCIES_CSV.FATAL=Erreur fatale lors de l''enregistrement du fichier d''import du référentiel des services agent)

Sauvegarde d'une copie de la base de donnée (STP_AGENCIES_JSON)
---------------------------------------------------------------

  + **Règle** : création d'une copie de la base de données contenant le référentiel des services agent

  + **Status** :

    - OK : la copie de la base de donnée contenant le référentiel des services agent a été crée avec succès (STP_AGENCIES_JSON.OK=Succès de l''enregistrement de la base de donnée contenant le référentiel des services agent)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création d'une copie de la base de données contenant le référentiel des services agent (STP_AGENCIES_JSON.FATAL=Erreur fatale lors de l''enregistrement de la base de donnée contenant le référentiel des services agent)

Structure du rapport d'administration du référentiel des règles de gestion
===========================================================================

Lorsqu'un nouveau référentiel est importé, la solution logicielle Vitam génère un rapport de l'opération. Ce rapport est en 3 parties :

  - "Journal des opérations" contient :

    * evType : le type d'opération. Dans le cadre de ce rapport, il s'agit toujours de "STP_IMPORT_AGENCIES"
    * evDateTime : la date et l'heure de l'opération d'import
    * evId : l'identifiant de l'opération

  - "Error" : détail les erreurs en indiquant :

    * line : le numéro de la ligne du rapport CSV générant l'erreur
    * Code : le code d'erreur
    * Message : le message associée à l'erreur
    * Information additionnelle : une précision sur l'erreur, comme par exemple le contenu du champs qui l'a provoquée

  - "InsertAgencies" : contient l'identifiant des services agents ajoutés
  - "UpdatedAgencies" : liste l'identifiant des services agents modifiés
  - "UsedAgencies By Contrat" : liste l'identifiant des services agents modifiés qui sont utilisés par des contrats d'accès
  - "UsedAgencies By AU" : liste l'identifiant des services agents modifiés qui sont utilisés dans des unités archivistique

**Exemple 1 : modification et ajout d'un service agent**

Le rapport généré est :

::
  {
  	"Journal des op\u00E9rations": {
  		"evType": "STP_IMPORT_AGENCIES",
  		"evDateTime": "2017-11-02T15:28:34.523",
  		"evId": "aecaaaaaacevq6lcaamxsak7pvmsdbqaaaaq"
  	},
  	"InsertAgencies": ["fr.gouv.vitam.common.model.administration.AgenciesModel@5c13a55d"],
  	"UpdatedAgencies": ["Identifier0"],
  	"UsedAgencies By Contrat": ["Identifier0"],
  	"UsedAgencies By AU": []
  }

**Exemple 2 : tentative d'ajout d'un service agent**

Le rapport généré est :

::
  {
  	"JDO": {
  		"evType": "STP_IMPORT_AGENCIES",
  		"evDateTime": "2017-11-02T15:36:03.976"
  	},
  	"error": {
  		"line 4": [{
  			"Code": "STP_IMPORT_AGENCIES_MISSING_INFORMATIONS.KO",
  			"Message": "!STP_IMPORT_AGENCIES_MISSING_INFORMATIONS!",
  			"Information additionnelle": "Name"
  		}]
  	},
  	"usedAgenciesByContracts": [],
  	"usedAgenciesByAU": []
  }
