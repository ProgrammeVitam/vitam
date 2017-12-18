Scenario IHM recette
####################

Cette partie décrit les scenario de test correspondant à ce qu'il est possible de réaliser via l'IHM de ercette.

Liste des scenarii
=====================

Règles de gestion
-----------------
Pour tester les différents cas pour le référentiel des règles de gestion :

Pré-requis : purge du référentiel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de recette > Administration des collections
 - Dans la partie Référentiels, purger le référentiel des règles en cliquant sur le bouton Supprimer.
 - Une pop up apparait : "Suppression des règles de gestion ? Êtes-vous certain de vouloir vider la collection des règles de gestion ?"
 - Cliquer sur le bouton "Annuler"
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune nouvelle entrée n'a été ajoutée concernant une opération STP_DELETE_RULES.
 - Se rendre sur l'IHM de recette > Administration des collections
 - Dans la partie Référentiels, purger le référentiel des règles en cliquant sur le bouton Supprimer.
 - Une pop-up apparait : "Suppression des règles de gestion ? Êtes-vous certain de vouloir vider la collection des règles de gestion ?"
 - Cliquer sur le bouton "Vider"
 - Une pop-up apparaît : "Suppression des règles La base des règles de gestion a bien été purgée".
 - Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_DELETE_RULES de catégorie MASTERDATA. On constate qu'elle est en succès.
 - Cliquer sur le détail et constater dans le nouvel onglet que l'on a deux enregistrements : un STARTED, et un OK.


Cas KO : format de fichier non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo > Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au mauvais format (xml, pdf) et cliquer sur valider
 - Une pop-up apparait avec le message : "Fichier invalide ou référentiel des règles de gestion déjà existant"
 - Se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune opération n'a été ajoutée.

Cas KO : référentiel des règles de gestion déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 - Se rendre sur l'IHM de démo > Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur Valider
 - Une pop-up apparaît indiquant "Fichier invalide ou référentiel des règles de gestion déjà existant"
 - Se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - Aucune opération n'a été ajoutée.

Cas OK
^^^^^^
 - Se rendre sur l'IHM de démo > Administration > Import du Référentiel des Règles de gestion
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur le bouton "Annuler l'import" (l'import est annulé), rien n'a été effectué.
 - Se rendre dans Administration > Référentiel des règles de gestion, et constater l'absence du référentiel.
 - Dans le menu se rendre dans Administration > Référentiel des règles de gestion, et constater qu'aucune règle de gestion n'est présente
 - Ajouter un fichier au format csv, correct (ex. :download:'<files/jeu_donnees_regles_CSV.csv>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux choix , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur le bouton "Lancer l'import"
 - Une pop-up apparaît avec le message suivant : "Le référentiel des règles de gestion est importé"
 - Se rendre dans Administration > Référentiel des règles de gestion, et constater le référentiel est présent.
 - Se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_RULES de catégorie MASTERDATA. Elle est en succès.
 - Cliquer sur cette opération et constater dans le nouvel onglet que cette opération a été effectuée avec succès.


Formats
-------
Pour tester les différents cas pour le référentiel des formats.

Pré-requis : purge du référentiel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Se rendre sur l'IHM de recette > Administration des collections
- Dans la partie Référentiels, purger le référentiel des formats en cliquant sur le bouton "Supprimer".
- Une pop up apparait : "Suppression des formats ? Êtes-vous certain de vouloir vider la collection des formats ?"
- Cliquer sur le bouton "Annuler"
- Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
- Aucune nouvelle entrée n'a été ajoutée concernant une opération STP_DELETE_FORMAT.
- Se rendre sur l'IHM de recette > Administration des collections
- Dans la partie Référentiels, purger le référentiel des formats en cliquant sur le bouton Supprimer.
- Une pop up apparait : "Suppression des formats ? Êtes-vous certain de vouloir vider la collection des formats ?"
- Cliquer sur le bouton "Vider"
- Une pop-up apparaît : "Suppression des formats. La base des formats a bien été purgée".
- Se rendre sur l'IHM de démo > Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
- La première ligne correspond à une opération STP_DELETE_FORMAT de catégorie MASTERDATA. On constate qu'elle est en succès.
- Cliquer sur l'opération et constater dans le nouvel onglet qu'elle a bien été modifiée.

Cas KO : format de fichier non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Se rendre sur l'IHM de démo, Administration > Import du Référentiel des formats
- Ajouter un fichier au mauvais format (pdf, csv) et cliquer sur valider
- Une pop-up apparait avec le message : "Fichier invalide"

Cas KO : référentiel des formats déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Se rendre sur l'IHM de démo > Administration > Import du Référentiel des formats
- Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur le bouton "Valider".
- Une pop-up apparaît indiquant "Fichier valide" et avec deux options, "Annuler l'import" ou "Lancer l'import".
- Cliquer sur le bouton "Lancer l'import"
- Une pop-up apparaît avec le message suivant : "Referentiel de formats déjà existant"
- Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
- La première ligne correspond à une opération STP_REFERENTIAL_FORMAT_IMPORT de catégorie MASTERDATA. On constate qu'elle est en échec
- Cliquer sur le détail et constater dans le nouvel onglet que l'opération est en échec.

Cas OK
^^^^^^

- Se rendre sur l'IHM de démo, Administration > Import du Référentiel des formats
- Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur le bouton "Valider"
- Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import"
- Cliquer sur le bouton "Annuler l'import" (l'import est annulé), rien n'a été effectué.
- Dans le menu se rendre dans Administration > Référentiel des formats, et constater qu'aucun format n'est présent.
- Ajouter un fichier au format xml, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur le bouton "Valider"
- Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import".
- Cliquer sur le bouton "Lancer l'import"
- Une pop-up apparaît avec le message suivant : "Referentiel de formats importé"
- Dans le menu se rendre dans Administration > Référentiel des formats, et constater que des formats sont présents.
- Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
- La première ligne correspond à une opération STP_REFERENTIAL_FORMAT_IMPORT de catégorie MASTERDATA. On constate qu'elle est en succès
- Cliquer sur le détail et constater dans le nouvel onglet que cette opération a été effectuée avec succès.

Contrats d'entrée
------------------
Pour tester les différents cas pour le référentiel des contrats d'entrée.

Cas KO : contrat d'entrée non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des contrats
 - Ajouter un fichier au mauvais contrat (pdf, csv) et cliquer sur valider
 - Une pop-up apparait avec le message : "Fichier invalide"

Cas KO : référentiel des contrat d'entrée déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

 - Se rendre sur l'IHM de démo > Administration > Import du Référentiel des contrats d'entrée
 - Ajouter un fichier au format json, correct (ex. :download:'<files/referential_contracts_ingest_ok.json>'.) et cliquer sur le bouton "Valider".
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options, "Annuler l'import" ou "Lancer l'import".
 - Cliquer sur le bouton "Lancer l'import"
 - Une pop-up apparaît avec le message suivant : "Referentiel de contrats déjà existant"
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_INGEST_CONTRACT de catégorie MASTERDATA. On constate qu'elle est en échec
 - Cliquer sur le détail et constater dans le nouvel onglet que l'opération est en échec.

Cas OK
^^^^^^

 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des contrat d'entrée
 - Ajouter un fichier au format json, correct (ex. :download:'<files/referential_contracts_ingest_ok.json>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur le bouton "Annuler l'import" (l'import est annulé), rien n'a été effectué.
 - Dans le menu se rendre dans Administration > Référentiel des contrat d'entrée, et constater qu'aucun contrat n'est présent.
 - Ajouter un fichier au format json, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import".
 - Cliquer sur le bouton "Lancer l'import"
 - Une pop-up apparaît avec le message suivant : "Referentiel de contrat importé"
 - Dans le menu se rendre dans Administration > Référentiel des contrats, et constater que des contrats sont présents.
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_INGEST_CONTRACT de catégorie MASTERDATA. On constate qu'elle est en succès
 - Cliquer sur le détail et constater dans le nouvel onglet que cette opération a été effectuée avec succès.

Contrats d'accès
----------------

Pour tester les différents cas pour le référentiel des contrats d'accès.

Cas KO : contrat d'accès non reconnu
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des contrats
 - Ajouter un fichier au mauvais contrat (pdf, csv) et cliquer sur valider
 - Une pop-up apparait avec le message : "Fichier invalide"

Cas KO : référentiel des contrat d'accès déjà présent
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

 - Se rendre sur l'IHM de démo > Administration > Import du Référentiel des contrats d'accès
 - Ajouter un fichier au format json, correct (ex. :download:'<files/referential_contracts_access_ok.json>'.) et cliquer sur le bouton "Valider".
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options, "Annuler l'import" ou "Lancer l'import".
 - Cliquer sur le bouton "Lancer l'import"
 - Une pop-up apparaît avec le message suivant : "Referentiel de contrats déjà existant"
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_ACCESS_CONTRACT de catégorie MASTERDATA. On constate qu'elle est en échec
 - Cliquer sur le détail et constater dans le nouvel onglet que l'opération est en échec.

Cas OK
^^^^^^
 - Se rendre sur l'IHM de démo, Administration > Import du Référentiel des contrat d'accès
 - Ajouter un fichier au format json, correct (ex. :download:'<files/referential_contracts_access_ok.json>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import"
 - Cliquer sur le bouton "Annuler l'import" (l'import est annulé), rien n'a été effectué.
 - Dans le menu se rendre dans Administration > Référentiel des contrat d'accès, et constater qu'aucun contrat n'est présent.
 - Ajouter un fichier au format json, correct (ex. :download:'<files/DROID_SignatureFile_V86.xml>'.) et cliquer sur le bouton "Valider"
 - Une pop-up apparaît indiquant "Fichier valide" et avec deux options , "Annuler l'import" ou "Lancer l'import".
 - Cliquer sur le bouton "Lancer l'import"
 - Une pop-up apparaît avec le message suivant : "Referentiel de contrat importé"
 - Dans le menu se rendre dans Administration > Référentiel des contrats, et constater que des contrats sont présents.
 - Dans le menu se rendre dans Administration > Journal des Opérations, et lancer une recherche (sans spécifier de critère)
 - La première ligne correspond à une opération STP_IMPORT_ACCESS_CONTRACT de catégorie MASTERDATA. On constate qu'elle est en succès
 - Cliquer sur le détail et constater dans le nouvel onglet que cette opération a été effectuée avec succès.

Purge des collections
---------------------

Plusieurs boutons sont disponibles dans l'ihm de recette, permettant de vider les collections MongoDB (attention, ceci n'est à effectuer que dans le cadre de la recette).

Les scenarios de tests disponibles se divisent en 4 parties :
 - référentiels
 - journaux
 - objets
 - globale

Référentiels
^^^^^^^^^^^^
Trois suppressions sont exécutables : formats, règles et registre des fonds.
La suppression des différentes collection est visible ainsi dans le journal des opérations :

 - formats : MASTERDATA / STP_DELETE_FORMAT
 - règles : MASTERDATA / STP_DELETE_RULES
 - registres des fonds : MASTERDATA / STP_DELETE_ACCESSION_REGISTER_SUMMARY
 - registres des fonds : MASTERDATA / STP_DELETE_ACCESSION_REGISTER_DETAIL

Journaux
^^^^^^^^
3 suppressions sont exécutables : journaux d'opérations, journaux des cycles de vie (unité archivistique), journaux des cycles de vie (groupe d'objets).
La suppression des différentes collection est visible ainsi dans le journal des opérations :

 - opérations : MASTERDATA / STP_DELETE_LOGBOOK_OPERATION
 - journaux des cycles de vie (archive unit) : MASTERDATA / STP_DELETE_LOGBOOK_LIFECYCLE_UNIT
 - journaux des cycles de vie (groupe d'objets) : MASTERDATA / STP_DELETE_LOGBOOK_LIFECYCLE_OG

Objets
^^^^^^
Deux suppressions sont exécutables : purge des unités archivistiques, purge des groupes d'objets;
La suppression des différentes collection est visible ainsi dans le journal des opérations :

 - Unités archivistiques : MASTERDATA / STP_DELETE_METADATA_OG
 - Groupes d'objets : MASTERDATA / STP_DELETE_METADATA_UNIT
