Workflow d'import d'un référentiel de règles de gestion
#######################################################

Introduction
============

Cette section décrit l'import et la mise à jour d'un référentiel de règles de gestions.

Processus d'import et de mise à jour d'un référentiel de règles de gestion (STP_IMPORT_RULES)
=============================================================================================

Le processus d'import et de mise à jour d'un référentiel de règles de gestion permet de vérifierr que les informations sont formalisée de la bonne manière dans le fichier soumis à la solution logicielle Vitam, que les données obligatoires ont bien été remplies pour chaque enregistrement et que dans le cas d'une mise à jour, aucune règle éventuellement supprimée n'est utilisée par une unité archivistique prise en charge dans la solution logicielle Vitam.

Import d'un référentiel de règles de gestion (STP_IMPORT_RULES)
---------------------------------------------------------------

* Vérification de la conformité du fichier de règle de gestion importé

  + **Règle** : le fichier doit être au format CSV et contenir les informations minimales
    
  + **Statuts** :
    
    - OK : le fichier rempli les conditions suivantes :

            * il est au format CSV
            * les informations suivantes sont toutes décrites dans cet ordre

                - RuleId
                - RuleType
                - RuleValue
                - RuleDescription

            * dans le cas d'une mise à jour, aucune des règles éventuellement supprimée ne devra être utilisée par une unité archivistique

    - KO : une des règles ci-dessus n'est pas respectée
      
    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_IMPORT_RULES.FATAL=Erreur fatale lors de l''import des règles de gestion)