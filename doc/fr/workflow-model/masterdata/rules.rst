Workflow d'import d'un référentiel de règles de gestion
#######################################################

Introduction
============

Cette section décrit l'import et la mise à jour d'un référentiel de règles de gestions.

Processus d'import et de mise à jour d'un référentiel de règles de gestion (STP_IMPORT_RULES)
=============================================================================================

Le processus d'import et de mise à jour d'un référentiel de règles de gestion permet de vérifierr que les informations sont formalisée de la bonne manière dans le fichier soumis à la solution logicielle Vitam, que les données obligatoires ont bien été remplies pour chaque enregistrement et que dans le cas d'une mise à jour, aucune règle éventuellement supprimée n'est utilisée par une unité archivistique prise en charge dans la solution logicielle Vitam.

Contrôle des règles de gestion (CHECK_RULES)
--------------------------------------------

+ **Règle** : Contrôle qu'aucune règle supprimée du référentie n'est utilisé par une unité archivistique. Contrôle des règes modifiées utilisées par des unités archivistiques. Vérification que les informations obligatoires ont bien été remplies pour chacune des règle.
  
+ **Type** : bloquant

+ **Statuts** :

    - OK : le fichier rempli les conditions suivantes :

            * il est au format CSV
            * les informations suivantes sont toutes décrites dans cet ordre

                - RuleId
                - RuleType
                - RuleValue
                - RuleDescription
                - RuleDuration
                - RuleMeasurement
      
      Aucune règle supprimée n'est utilisée par une unité archivistique
      
    - KO :  une des règles ci-dessus n'est pas respectée
      
    - FATAL : une erreur technique est survenue lors du contrôle des règles de gestion (CHECK_RULES.FATAL=Erreur fatale lors du contrôle de la conformité du fichier de règles de gestion)

Création du rapport (RULES_REPORT)
-----------------------------------

+ **Règle** : création du rapport d'import des règles
  
+ **Type** : bloquant

+ **Statuts** :

    - OK : Le rapport est généré
      
    - KO : pas de cas KO
      
    - FATAL : une erreur technique est survenue lors de la création du rapport (RULES_REPORT.FATAL = Erreur fatale lors de la génération du rapport d'import du référentiel des règles de gestion)

Persistance des données en base (COMMIT_RULES)
----------------------------------------------

+ **Règle** : enregistrement des données
  
+ **Type** : bloquant

+ **Statuts** :

    - OK : les données sont persistées en base
      
    - KO : pas de cas KO
      
    - FATAL : une erreur technique est survenue lors de la persistance des données en base (COMMIT_RULES.FATAL=Erreur fatale lors de la persistance des données en base)

Sauvegarde du CSV (STP_IMPORT_RULES_CSV)
----------------------------------------

+ **Règle** : enregistrement du CSV d'import
  
+ **Type** : bloquant

+ **Statuts** :

    - OK : le CSV d'import est enregistré
      
    - KO : pas de cas KO
      
    - FATAL : une erreur technique est survenue lors de l'enregistrement du CSV d'import (STP_IMPORT_RULES_CSV.FATAL = Erreur fatale lors de l'enregistrement du fichier d'import du référentiel des règles de gestion)

Sauvegarde du JSON (STP_IMPORT_RULES_JSON)
------------------------------------------

+ **Règle** : enregistrement d'une copie de la base de données
  
+ **Type** : bloquant

+ **Statuts** :

    - OK : une copie de la base de donnée nouvellement importée est enregistrée
      
    - KO : pas de cas KO
      
    - FATAL : une erreur technique est survenue lors de la copie de la base de donnée nouvellement importée (STP_IMPORT_RULES_JSON.FATAL = Erreur fatale lors de l'enregistrement de la copie du référentiel des règles de gestion)