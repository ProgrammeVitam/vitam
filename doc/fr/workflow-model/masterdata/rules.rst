Workflow d'administration d'un référentiel de règles de gestion
###############################################################

Introduction
============

Cette section décrit le processus (workflow) permettant d'importer et de mettre à jour un référentiel de règles de gestions dans la solution logicielle Vitam.

Processus d'administration d'un référentiel de règles de gestion (STP_IMPORT_RULES)
===================================================================================

L'import d'un référentiel de règles de gestion permet de vérifier le formalisme de ce dernier, notamment que les données obligatoires sont bien présentes pour chacune des règles. Tous les éléments réalisés au cours de ce processus sont exécutés dans une seule étape. Cet import concerne aussi bien l'import initial (aucune règles de gestion pré-existantes) que la mise à jour du référentiel.

Ce processus d'import débute lors du lancement du téléchargement du fichier CSV contenant le référentiel dans la solution logicielle Vitam. Par ailleurs, toutes les étapes, tâches et traitements sont journalisés dans le journal des opérations.

La fin du processus peut prendre plusieurs statuts :

* **Statuts** :

  + OK : le référentiel des règles de gestion a été importé (STP_IMPORT_RULES.OK = Succès du processus d'import du référentiel des règles de gestion)

  + Warning : le référentiel des règles de gestion a été importé et ce nouvel import modifie des règles de gestions préalablement utilisées par des unités archivistique dans la solution logicielle Vitam (STP_IMPORT_RULES.WARNING = Avertissement lors du processus d'import des règles de gestion, des règles de gestions ont été modifiées et sont utilisées par des unitées archivistiques existantes)

  + KO : le référentiel des règles de gestion n'a pas été importé (STP_IMPORT_RULES.KO = Échec du processus d'import du référentiel des règles de gestion)

  + FATAL : une erreur technique est survenue lors de l'import du référentiel des règles de gestion (STP_IMPORT_RULES.FATAL = Erreur fatale lors du processus d'import du référentiel des règles de gestion)


Création du rapport RULES_REPORT (RulesManagerFileImpl.java)
------------------------------------------------------------

+ **Règle** : création du rapport d'import des règles

+ **Type** : bloquant

+ **Statuts** :

    - OK : le rapport est généré (RULES_REPORT.OK = Succès de la génération du rapport d'analyse du référentiel des règles de gestion)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la création du rapport (RULES_REPORT.FATAL = Erreur fatale lors de la génération du rapport d'analyse du référentiel des règles de gestion)

Contrôle des règles de gestion CHECK_RULES (UnitsRulesComputePlugin.java)
-------------------------------------------------------------------------

+ **Règle** : contrôle qu'aucune règle supprimée du référentie n'est utilisé par une unité archivistique. Contrôle des règles modifiées utilisées par des unités archivistiques. Vérification que les informations obligatoires minimales ont bien été remplies pour chacune des règles, conformément aux exigences du référentiel des règles de gestion. La liste de ces exigences est décrite dans le document modèle de données.

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
    * Aucune opération d'import de référentiel de règle de gestion n'a lieu en même temps
    * Si le tenant définit des durées minimales pour des catégories de règles de gestion (configuration de sécurité), les règles de gestion importées doivent avoir des durées supérieures ou égales à ces durées minimales de sécurité

+ **Type** : bloquant

+ **Statuts** :

    - OK : les règles ci-dessus sont respectées (CHECK_RULES = Contrôle de la conformité du fichier des règles de gestion)

    - WARNING : une règle modifiée par l'import du référentiel est actuellement utilisée par une unité archivistique (CHECK_RULES.WARNING = Avertissement lors du contrôle de la conformité du fichier des règles de gestion)

    - KO :

      - Cas 1 : une des règles ci-dessus n'est pas respectée. Le détail des erreurs est inscrit dans le rapport d'import du référentiel (CHECK_RULES.KO = Échec du contrôle de la conformité du fichier des règles de gestion)
      - Cas 2 : le fichier CSV n'est pas reconnu comme un CSV valide (CHECK_RULES.INVALID_CSV.KO=Échec du contrôle de la conformité du fichier des règles de gestion : fichier CSV invalide)
      - Cas 3 : une opération de mise à jour du référentiel est déjà en cours (CHECK_RULES.IMPORT_IN_PROCESS.KO=L'import est impossible car une mise à jour du référentiel est déjà en cours
      - Cas 4 : au moins une règle de gestion a une durée qui est inférieure à la durée minimale requise sur ce tenant. Selon la configuration de la solution logicielle Vitam, ce cas peut provoquer une alerte de sécurité, enregistrée dans les logs de sécurité. (CHECK_RULES.MAX_DURATION_EXCEEDS.KO=Echec lors du contrôle de sécurité des règles de gestion. Les durées des règles de gestion doivent être supérieures ou égales aux durées minimales requises par le tenant)

    - Dans le rapport de l'import du référentiel des règles de gestion, des clés plus détaillées peuvent y être inscrites, selon les erreurs rencontrées :

      * Le fichier importé n'est pas au format CSV (STP_IMPORT_RULES_NOT_CSV_FORMAT.KO = Le fichier importé n'est pas au format CSV)
      * Il existe plusieurs fois le même RuleId (STP_IMPORT_RULES_RULEID_DUPLICATION.KO = Il existe plusieurs fois le même RuleId. Ce RuleId doit être unique dans l'ensemble du référentiel)
      * Au moins une RuleType est incorrecte (STP_IMPORT_RULES_WRONG_RULETYPE_UNKNOW.KO = Au moins une RuleType est incorrecte. RuleType autorisés : AppraisalRule, AccessRule, StorageRule, DisseminationRule, ReuseRule, ClassificationRule)
      * Au moins une valeur obligatoire est manquante (STP_IMPORT_RULES_MISSING_INFORMATION.KO = Au moins une valeur obligatoire est manquante. Valeurs obligatoires : RuleID, RuleType, RuleValue, RuleDuration, RuleMeasurement)
      * Des valeurs de durée sont incorrectes pour RuleMeasurement (STP_IMPORT_RULES_WRONG_RULEMEASUREMENT.KO = Au moins un champ RuleDuration a une valeur incorrecte. La valeur doit être un entier positif ou nul, ou être indiquée unlimited)
      * Au moins un champs RuleDuration a une valeur incorrecte (STP_IMPORT_RULES_WRONG_RULEDURATION.KO = Au moins un champ RuleDuration a une valeur incorrecte. La valeur doit être un entier positif ou nul, ou être indiquée unlimited)
      * L'association de RuleDuration et de RuleMeasurement doit être  inférieure ou égale à 999 ans (STP_IMPORT_RULES_WRONG_TOTALDURATION.KO = L'association de RuleDuration et de RuleMeasurement doit être  inférieure ou égale à 999 ans)
      * Des règles supprimées sont actuellement utilisées (STP_IMPORT_RULES_DELETE_USED_RULES.KO = Des régles supprimées sont actuellement utilisées)
      * Des durées sont inférieures ou égales aux durées minimales autorisées dans la configuration de la plateforme (STP_IMPORT_RULES_RULEDURATION_EXCEED.KO = Echec lors du contrôle de sécurité des règles de gestion. Les durées des règles de gestion doivent être supérieures ou égales aux durées minimales requises par le tenant)

    - FATAL : une erreur technique est survenue lors du contrôle des règles de gestion (CHECK_RULES.FATAL=Erreur fatale lors du contrôle de la conformité du fichier de règles de gestion)


Persistance des données en base COMMIT_RULES (RulesManagerFileImpl.java)
------------------------------------------------------------------------

+ **Règle** : enregistrement des données en base

+ **Type** : bloquant

+ **Statuts** :

    - OK : les données sont persistées en base (COMMIT_RULES=OK=Succès de la persistance des données en base

    - FATAL : une erreur technique est survenue lors de la persistance des données en base (COMMIT_RULES.FATAL=Erreur fatale lors de la persistance des données en base)


Processus d'enregistrement du fichier d'import du référentiel des règles de gestion STP_IMPORT_RULES_BACKUP_CSV (RulesManagerFileImpl.java)
-------------------------------------------------------------------------------------------------------------------------------------------

+ **Règle** : enregistrement du CSV d'import du référentiel des règles de gestion

+ **Type** : bloquant

+ **Statuts** :

    - OK : le CSV d'import est enregistré (STP_IMPORT_RULES_BACKUP_CSV.OK=Succès du processus d'enregistrement du fichier d'import du référentiel des règles de gestion)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de l'enregistrement du CSV d'import (STP_IMPORT_RULES_BACKUP_CSV.FATAL = Erreur fatale lors du processus d'enregistrement du fichier d'import du référentiel des règles de gestion)

Sauvegarde du JSON STP_IMPORT_RULES_BACKUP (RulesManagerFileImpl.java)
----------------------------------------------------------------------

+ **Règle** : enregistrement d'une copie de la base de données sur le stockage

+ **Type** : bloquant

+ **Statuts** :

    - OK : une copie de la base de donnée nouvellement importée est enregistrée (STP_IMPORT_RULES_BACKUP.OK = Succès du Processus de sauvegarde du référentiel des règles de gestion)

    - KO : pas de cas KO

    - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_IMPORT_RULES_BACKUP.FATAL=Erreur fatale lors du processus de sauvegarde du référentiel des règles de gestion)


Structure du rapport d'administration du référentiel des règles de gestion
==========================================================================

Lorsqu'un nouveau référentiel est importé, la solution logicielle Vitam génère un rapport de l'opération. Ce rapport est en 3 parties :

  - "Operation" contient :

    * evType : le type d'opération. Dans le cadre de ce rapport, il s'agit toujours de "STP_IMPORT_RULES".
    * evDateTime : la date et l'heure de l'opération d'import.
    * evId : l'identifiant de l'opération.
    * outMessg : message final de l'opération (Succès/Avertissement/Échec du processus d'import du référentiel des règles de gestion)

  - "Error" : détail les erreurs en indiquant :

    * line : le numéro de la ligne du rapport CSV générant l'erreur
    * Code : le code d'erreur
    * Message : le message associée à l'erreur
    * Information additionnelle : une précision sur l'erreur, comme par exemple le contenu du champ qui l'a provoquée

    - "usedDeletedRules" : contient l'intégralité des règles en cours d'utilisation dont la suppression a été demandée lors de la mise à jour du référentiel des règles de gestion. Chaque détail précise en plus la date de création de la règle, sa dernière mise à jour et sa version.
    - "usedUpdatedRules" : contient l'intégralité des règles en cours d'utilisation dont une mise à jour a été effectuée. Chaque détail précise en plus la date de création de la règle, sa dernière mise à jour et sa version.

Exemples
--------

**Exemple 1 : import initial d'un référentiel**

Le rapport généré est :

::

  {"Operation":{"evType":"STP_IMPORT_RULES","evDateTime":"2017-11-02T13:50:22.389"},"error":{},"usedDeletedRules":[],"usedUpdatedRules":[]}


**Exemple 2 : mise à jour d'un référentiel existant**

Dans cette exemple, la mise à jour :

  - Essaye de modifier une RuleType d'une règle en lui mettant "AccessRules" au lieu de "AccessRule"
  - Met à jour une règle de gestion en cours d'utilisation

Le rapport généré est :

::

  {
  	"Operation": {
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
