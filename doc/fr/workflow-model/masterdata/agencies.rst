Workflow d'import d'un référentiel des services agent
#####################################################

Introduction
============

Cette section décrit l'import d'un référentiel de services agents

Processus d'import  et mise à jour d'un référentiel de services agents
======================================================================

L'import d'un référentiel de profil permet de vérifier le formalisme du référentiel des services agent et que les données obligatoires sont bien présentes pour chacun des agents.

Import d'un référentiel de services agents (STP_IMPORT_AGENCIES)
----------------------------------------------------------------

  + **Règle** :  Le fichier doit être au format CSV et contenir les informations obligatoires

  + **Statuts** :

    - OK : Le fichier rempli les conditions suivantes :

            * il est au format CSV
            * les informations suivantes sont toutes décrites dans cet ordre pour chacun des services agent :

                - Identifier
                - Name
                - Description (optionnel)
            
            * l'indentifier doit être unique
    
    - KO : une des règles ci-dessus n'est pas respectée

    - FATAL : une erreur technique est survenue lors de l'import du référentiel des formats (STP_IMPORT_AGENCIES.FATAL=Erreur fatale lors de l''import du service producteur)