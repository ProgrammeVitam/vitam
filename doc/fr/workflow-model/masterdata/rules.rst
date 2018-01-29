Workflow d'administration d'un référentiel de règles de gestion
##################################################################

Introduction
============

Cette section décrit le processus permettant d'importer et de mettre à jour un référentiel de règles de gestions dans la solution logicielle Vitam

Processus d'administration d'un référentiel de règles de gestion (STP_IMPORT_RULES)
=============================================================================================

Le processus d'import et de mise à jour d'un référentiel de règles de gestion permet de vérifier que les informations sont formalisées de la bonne manière dans le fichier soumis à la solution logicielle Vitam, que les données obligatoires ont bien été remplies pour chaque enregistrement et que dans le cas d'une mise à jour, on ne souhaite éliminer aucune règle déjà utilisée par une unité archivistique prise en charge dans la solution logicielle Vitam.

Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape.

Création du rapport (RULES_REPORT)
-----------------------------------

+ **Règle** : création du rapport d'import des règles

+ **Type** : bloquant

+ **Statuts** :

    - OK : Le rapport est généré

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création du rapport (RULES_REPORT.FATAL = Erreur fatale lors de la génération du rapport d'import du référentiel des règles de gestion)

Contrôle des règles de gestion (CHECK_RULES)
--------------------------------------------

+ **Règle** : contrôle qu'aucune règle supprimée du référentie n'est utilisé par une unité archivistique. Contrôle des règes modifiées utilisées par des unités archivistiques. Vérification que les informations obligatoires minimales ont bien été remplies pour chacune des règle, conformément aux exigences du référentiel des règles de gestion. La liste de ces exigences est décrite dans le document modèle de données.

De plus le fichier rempli les conditions suivantes :

  * il est au format CSV
  * les informations suivantes sont toutes décrites dans cet ordre

      - RuleId
      - RuleType
      - RuleValue
      - RuleDescription
      - RuleDuration
      - RuleMeasurement

    * Aucune règle supprimée n'est utilisée par une unité archivistique

+ **Type** : bloquant

+ **Statuts** :

    - OK : les règles ci-dessus sont respectées

    - WARNING : une règle modifiée par l'import du référentiel est actuellement utilisée par une unité archivistique (STP_IMPORT_RULES_UPDATED_RULES.WARNING)

    - KO : une des règles ci-dessus n'est pas respectée

      * Le fichier importé n'est pas au format CSV (STP_IMPORT_RULES_NOT_CSV_FORMAT.KO)
      * Il existe plusieurs fois le même RuleId (STP_IMPORT_RULES_RULEID_DUPLICATION.KO)
      * Au moins une RuleType est incorrecte (STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW.KO)
      * Au moins une valeur obligatoire est manquante (STP_IMPORT_RULES_MISSING_INFORMATION.KO)
      * Des valeurs de durée sont incorrectes pour RuleMeasurement (STP_IMPORT_RULES_WRONG_RULEMEASUREMENT.KO)
      * Au moins un champs RuleDuration a une valeur incorrecte (STP_IMPORT_RULES_WRONG_RULEDURATION.KO)
      * L'association de RuleDuration et de RuleMeasurement doit être  inférieure ou égale à 999 ans (STP_IMPORT_RULES_WRONG_TOTALDURATION.KO)
      * Des règles supprimées sont actuellement utilisées (STP_IMPORT_RULES_DELETE_USED_RULES.KO)
      * Des durées sont inférieures ou égales aux durées minimales autorisées dans la configuration de la plateforme (STP_IMPORT_RULES_RULEDURATION_EXCEED.KO). Ce cas provoque de plus une alerte de sécurité, enregistrée dans les logs de sécurité.

    - FATAL : une erreur technique est survenue lors du contrôle des règles de gestion (CHECK_RULES.FATAL=Erreur fatale lors du contrôle de la conformité du fichier de règles de gestion)


{"JDO":{"evType":"STP_IMPORT_RULES","evDateTime":"2017-11-02T13:50:22.389"},"error":{},"usedDeletedRules":[],"usedUpdatedRules":[]}

Persistance des données en base (COMMIT_RULES)
----------------------------------------------

+ **Règle** : enregistrement des données

+ **Type** : bloquant

+ **Statuts** :

    - OK : les données sont persistées en base
      
    - WARNING : 

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la persistance des données en base (COMMIT_RULES.FATAL=Erreur fatale lors de la persistance des données en base)

Sauvegarde du CSV (STP_IMPORT_RULES_BACKUP_CSV)
----------------------------------------

+ **Règle** : enregistrement du CSV d'import

+ **Type** : bloquant

+ **Statuts** :

    - OK : le CSV d'import est enregistré

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de l'enregistrement du CSV d'import (STP_IMPORT_RULES_BACKUP_CSV.FATAL = Erreur fatale lors de l'enregistrement du fichier d'import du référentiel des règles de gestion)

Sauvegarde du JSON (STP_IMPORT_RULES_BACKUP)
------------------------------------------

+ **Règle** : enregistrement d'une copie de la base de données

+ **Type** : bloquant

+ **Statuts** :

    - OK : une copie de la base de donnée nouvellement importée est enregistrée

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_IMPORT_RULES_BACKUP.FATAL = Erreur fatale lors de l'enregistrement de la copie du référentiel des règles de gestion)


Structure du rapport d'administration du référentiel des règles de gestion
===========================================================================

Lorsqu'un nouveau référentiel est importé, la solution logicielle Vitam génère un rapport de l'opération. Ce rapport est en 3 parties :

  - "JDO" contient :

    * evType : le type d'opération. Dans le cadre de ce rapport, il s'agit toujours de "STP_IMPORT_RULES"
    * evDateTime : la date et l'heure de l'opération d'import

  - "Error" : détail les erreurs en indiquant :

    * line : le numéro de la ligne du rapport CSV générant l'erreur
    * Code : le code d'erreur
    * Message : le message associée à l'erreur
    * Information additionnelle : une précision sur l'erreur, comme par exemple le contenu du champs qui l'a provoquée

    - "usedDeletedRules" : contient l'intégralité des règles en cours d'utilisation dont la suppression a été demandée lors de la mise à jour du référentiel des règles de gestion. Chaque détail précise en plus la date de création de la règle, sa dernière mise à jour et sa version.
    - "usedUpdatedRules" : contient l'intégralité des règles en cours d'utilisation dont une mise à jour a été effectuée. Chaque détail précise en plus la date de création de la règle, sa dernière mise à jour et sa version.

Exemples
-------------

**Exemple 1 : import initial d'un référentiel**

Le rapport généré est :

::

  {"JDO":{"evType":"STP_IMPORT_RULES","evDateTime":"2017-11-02T13:50:22.389"},"error":{},"usedDeletedRules":[],"usedUpdatedRules":[]}


**Exemple 2 : mise à jour d'un référentiel existant**

Dans cette exemple, la mise à jour :

  - Essaye de modifier une RuleType d'une règle en lui mettant "AccessRulez" au lieu de "AccessRule"
  - Met à jour une règle de gestion en cours d'utilisation

Le rapport généré est :

::

  {
  	"JDO": {
  		"evType": "STP_IMPORT_RULES",
  		"evDateTime": "2017-11-02T14:03:53.326"
  	},
  	"error": {
  		"line 6": [{
  			"Code": "STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW.KO",
  			"Message": "Au moins une RuleType est incorrecte. RuleType autorisés : AppraisalRule, AccessRule, StorageRule, DisseminationRule, ReuseRule, ClassificationRule",
  			"Information additionnelle": "AccessRulez"
  		}]
  	},
  	"usedDeletedRules": [],
  	"usedUpdatedRules": ["id=null, tenant=0, ruleId=APP-00001, ruleType=AppraisalRule, ruleValue=Dossier individuel d’agent civil, ruleDescription=Durée de conservation des dossiers individuels d’agents. L’échéance est calculée à partir de la date de naissance de l’agent, ruleDuration=70, ruleMeasurement=YEAR, creationDate=2017-11-02T14:03:52.374, updateDate=2017-11-02T14:03:52.374, version=0"]
  }
