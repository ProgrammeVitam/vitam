Scenarii hors ingest
####################

Cette partie décrit les scenarii de test correspondant aux processus autres que le processus d'ingest.

Liste des scenarii
==================

Règles de gestion
-----------------
Pour tester les différents cas pour le référentiel des règles de gestion : 

Pré-requis : purge du référentiel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de recette > Administration des collections 
 - Dans la partie Référentiels, purger le référentiel des règles en cliquant sur le bouton Supprimer.
 - Une pop up apparait : "Suppression des règles de gestion ? Êtes-vous certain de vouloir vider la collection des règles de gestion ?"
 - Cliquer sur "Annuler"
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune nouvelle entrée n'a été ajoutée concernant une opération STP_DELETE_RULES.
 - Se rendre sur l'IHM de recette > Administration des collections
 - Dans la partie Référentiels, purger le référentiel des règles en cliquant sur le bouton Supprimer.
 - Une pop-up apparait : "Suppression des règles de gestion ? Êtes-vous certain de vouloir vider la collection des règles de gestion ?"
 - Cliquer sur "Vider"
 - Une pop-up apparaît : "Suppression des règles La base des règles de gestion a bien été purgée".
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_DELETE_RULES de catégorie MASTERDATA. On constate qu'elle est en succès.
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a 2 enregistrements : un STARTED, et un OK.


Cas KO : format de fichier non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au mauvais format (xml, pdf) et cliquer sur valider 
 - Une pop-up apparait avec le message : "Fichier invalide ou référentiel des règles de gestion déjà existant"
 
Cas KO : référentiel des règles de gestion déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier invalide ou référentiel des règles de gestion déjà existant"
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune opération n'a été ajoutée.
 
 Cas OK
^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier valide" et avec 2 choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur "Annuler l'import" (l'import est annulé), rien n'a été effectué.
 - Dans le menu se rendre dans Administration > Référentiel des règles de gestion, et constater qu'aucune règle de gestion n'est présente
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier valide" et avec 2 choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur Lancer l'import
 - Une pop-up apparaît avec le message suivant : "Le référentiel des règles de gestion est importé"
 - Dans le menu se rendre dans Administration > Référentiel des règles de gestion, et constater que des règles de gestion sont présentes
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_RULES de catégorie MASTERDATA. On constate qu'elle est en succès
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a 2 enregistrements : un STARTED, et un OK.

 
Formats
-------
Pour tester les différents cas pour le référentiel des formats. 

Pré-requis : purge du référentiel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de recette > Administration des collections 
 - Dans la partie Référentiels, purger le référentiel des formats en cliquant sur le bouton Supprimer.
 - Une pop up apparait : "Suppression des formats ? Êtes-vous certain de vouloir vider la collection des formats ?"
 - Cliquer sur "Annuler"
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune nouvelle entrée n'a été ajoutée concernant une opération STP_DELETE_FORMAT.
 - Se rendre sur l'IHM de recette > Administration des collections
 - Dans la partie Référentiels, purger le référentiel des formats en cliquant sur le bouton Supprimer.
 - Une pop up apparait : "Suppression des formats ? Êtes-vous certain de vouloir vider la collection des formats ?"
 - Cliquer sur "Vider"
 - Une pop-up apparaît : "Suppression des formats. La base des formats a bien été purgée".
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_DELETE_FORMAT de catégorie MASTERDATA. On constate qu'elle est en succès.
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a 2 enregistrements : un STARTED, et un OK.

Cas KO : format de fichier non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des formats
 - Ajouter un fichier au mauvais format (pdf, csv) et cliquer sur valider 
 - Une pop-up apparait avec le message : "Fichier invalide"

Cas KO : référentiel des formats déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier valide" et avec 2 choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur Lancer l'import
 - Une pop-up apparaît avec le message suivant : "Referentiel de formats importé"
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_REFERENTIAL_FORMAT_IMPORT de catégorie MASTERDATA. On constate qu'elle est en erreur
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a 2 enregistrements : un STARTED, et un KO (Erreur de l'import du référentiel de format).
 
 Cas OK
^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des formats
 - Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier valide" et avec 2 choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur "Annuler l'import" (l'import est annulé), rien n'a été effectué.
 - Dans le menu se rendre dans Administration > Référentiel des formats, et constater qu'aucun format n'est présent
 - Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier valide" et avec 2 choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur Lancer l'import
 - Une pop-up apparaît avec le message suivant : "Referentiel de formats importé"
 - Dans le menu se rendre dans Administration > Référentiel des formats, et constater que des formats sont présents
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_REFERENTIAL_FORMAT_IMPORT de catégorie MASTERDATA. On constate qu'elle est en succès
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a 2 enregistrements : un STARTED, et un OK.
 
Purge des collections
---------------------
Plusieurs boutons sont disponibles dans l'ihm de recette, permettant de vider les collections MongoDB (attention, ceci n'est à effectuer que dans le cadre de la recette).

Les scenarii de tests pour cette partie se divisent en 4 parties : 
 - référentiels
 - journaux
 - objets
 - globale

Référentiels
^^^^^^^^^^^^
3 suppressions sont exécutables : formats, règles, registres des fonds.
En supprimant les différentes collections, en regardant le journal des opérations, voici ce que l'on obtient : 
 - formats : MASTERDATA / STP_DELETE_FORMAT
 - règles : MASTERDATA / STP_DELETE_RULES
 - registres des fonds : MASTERDATA / STP_DELETE_ACCESSION_REGISTER_SUMMARY
 - registres des fonds : MASTERDATA / STP_DELETE_ACCESSION_REGISTER_DETAIL

Journaux
^^^^^^^^
3 suppressions sont exécutables : journaux d'opérations, journaux des cycles de vie (archive unit), journaux des cycles de vie (groupe d'objets).
En supprimant les différentes collections, en regardant le journal des opérations, voici ce que l'on obtient : 
 - opérations : MASTERDATA / STP_DELETE_LOGBOOK_OPERATION
 - journaux des cycles de vie (archive unit) : MASTERDATA / STP_DELETE_LOGBOOK_LIFECYCLE_UNIT
 - journaux des cycles de vie (groupe d'objets) : MASTERDATA / STP_DELETE_LOGBOOK_LIFECYCLE_OG

Objets
^^^^^^
2 suppressions sont exécutables : purge des unités archivistiques, purge des groupes d'objets
En supprimant les différentes collections : 
 - Unités archivistiques : MASTERDATA / STP_DELETE_METADATA_OG
 - Groupes d'objets : MASTERDATA / STP_DELETE_METADATA_UNIT
 
Mise à jour d'une ArchiveUnit
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Il s'agit ici de lancer Postman et de réaliser un update (PUT / cf. postman) sur une url (d'access) de ce type : 
 - {{accessServiceUrl}}{{accessResourcePath}}{{serviceVersion}}{{unitsCollection}}/aeaaaaaaaaaam7mxabjduakysg5qp7aaaaaq

Et avec un body de ce type :

.. code-block:: json
   {
     "$roots": [
       "aeaaaaaaaaaam7mxabjduakysg5qp7aaaaaq"
     ],
     "$query": [],
     "$filter": {
       "$orderby": {
         "TransactedDate": 1
       }
     },
     "$action": [
       {
         "$set": {
           "Title": "A new Title for my Unit"
         }
       }
     ]
   }

Une réponse est renvoyée par le serveur : code OK, avec un body de ce type : 

.. code-block:: json 
   {
     "$hits": {
       "total": 1,
       "offset": 0,
       "limit": 1,
       "size": 1
     },
     "$results": [
       {
         "#id": "aeaaaaaaaaaam7mxabjduakysg5qp7aaaaaq",
         "#diff": "-  Title : Title Before\n+  Title : A new Title for my Unit"
       }
     ],
     "$context": {
       "$roots": [
         "#id"
       ],
       "$query": [],
       "$filter": {},
       "$action": [
         {
           "$set": {
             "Title": "A new Title for my Unit"
           }
         }
       ]
     }
   }
