Administration
##############

Cette partie décrit les fonctionnalités d'administration suivantes proposées à l'utilisateur :

- Consultation du journal des opérations
- Import et consultation du référentiel des formats
- Import et consultation du référentiel des règles de gestion

Journal des opérations
======================

Le journal des opérations permet à l'utilisateur d'accéder à toutes les opérations effectuées dans la solution logicielle Vitam, par type d'opération.

Ces opérations sont :

- Entrée
- Mise à jour  des métadonnées de description
- Données de base
- Sécurisation
- Audit (non encore développée)
- Elimination (non encore développée)
- Préservation (non encore développée)
- Vérification (non encore développée)

Pour consulter le journal des opérations, l'utilisateur clique sur le menu "Administration", puis sur le sous-menu "Journal des opérations".

.. image:: images/menu_op.png

Formulaire de recherche
-----------------------

Pour effectuer une recherche précise, on utilise les champs "ID" ou "Type d'opération" :

- L'ID est un champ libre, correspondant à l'ID de l'opération donné par le système.
- Les types d'opération sont présentés sous forme de liste, permettant à l'utilisateur de sélectionner un type d'opération en particulier.

NB : Il est impossible d'effectuer une recherche croisée par ID et type d'opération.

Pour initier la recherche, l'utilisateur saisit son critère de recherche et clique sur le bouton "Rechercher".

.. image:: images/op_recherche.jpg

La recherche met à jour le tableau, affichant le résultat de la requête, trié par date de fin d'opération, de la plus récente à la plus ancienne.
Les opérations en cours sont affichées en haut de la liste et sont triées chronologiquement par ordre d'opération, de la plus récente à la plus ancienne.

Si aucun résultat n'est trouvé par la solution logicielle Vitam, alors un message indique à l'utilisateur qu'aucun résultat n'est disponible pour sa recherche.

.. image:: images/op_recherche_KO.jpg

Affichage des résultats
-----------------------
Suite à la recherche, le résultat de la recherche est affiché sous forme de tableau, comportant les colonnes suivantes :

- la catégorie de l'opération (actuellement : Entrée ou INGEST, Mise à jour  des métadonnées de description ou  UPDATE, Données de base ou MASTERDATA, Sécurisation ou TRACEABILITY)
- le type d'opération
- la date de fin d'opération
- le statut de l'opération (en cours, échec, succès, avertissement)
- Le message de l'opération (Identifiant de l'opération)

.. image:: images/op_resultat.png

Consultation du détail d'une opération
--------------------------------------

Suite à la recherche d'une opération, l'utilisateur peut choisir de consulter le détail des événements intervenus durant l'opération.
Pour cela, il clique sur la ligne voulue.
Un nouvel onglet s'ouvre, pour présenter le détail de l'opération, sans quitter la liste globale des résultats.

Le détail est affiché sous forme de tableau comportant pour chaque événement les éléments suivants :

- Etape : nom de l'étape correspondante
- Date : date à laquelle l'étape a été effectuée
- Statut : statut final de l'étape
- Message : message expliquant le statut de cette étape

.. image:: images/op_detail.png

Référentiel des formats
=======================

Cette partie décrit les fonctionnalités d'import du référentiel des formats, basé sur une version récente du référentiel des formats PRONOM mis à disposition par les Archives nationales britanniques, pour ensuite le consulter et rechercher des formats spécifiques.

Import du référentiel des formats
---------------------------------

L'import du référentiel des formats s'effectue depuis l'écran "Import du référentiel des formats".
Pour cela, l'utilisateur clique sur le menu "Administration", puis sur le sous-menu "Import du référentiel des formats".

.. image:: images/menu_import_rf.png

L'import du référentiel ne peut être effectué sans le fichier PRONOM.
Pour cela, l'utilisateur peut récuperer ce fichier dans sa version la plus récente sur le site des Archives nationales britanniques :

- http://www.nationalarchives.gov.uk/
- Section "PRONOM" > "DROID signature files"

Le processus d'import du référentiel se décrit comme ceci :

- L'utilisateur accède à l'écran d'import du référentiel des formats et clique sur le bouton "Choisissez un fichier" pour sélectionner un fichier
- Le fichier à selectionner est le fichier XML PRONOM récupéré précédemment
- L'utilisateur valide son choix
- La solution logicielle Vitam vérifie l'intégrité et la cohérence du fichier

.. image:: images/import_rf_format.png

A l'issue du contrôle de cohérence et d'intégrité du fichier, plusieurs cas sont possibles :

- En cas d'erreur de fichier : La solution logicielle Vitam détecte des erreurs contenues dans le fichier, l'import de ce dernier n'est pas possible, un message d'erreur s'affiche. L'utilisateur doit corriger ces erreurs et soumettre à nouveau le fichier s'il souhaite toujours effectuer son import.

.. image:: images/import_rf_format_KO.png

- En cas d'erreur pour cause de référentiel déjà existant : la solution logicielle Vitam détecte la présence d'un référentiel des formats. Par conséquent un message d'erreur indiquant "Référentiel des formats déjà existant" est affiché. L'import est alors impossible. 

.. image:: images/import_rf_deja_existant.png

- En cas de succès : La solution logicielle Vitam indique à l'utilisateur que son fichier est valide et lui propose d'importer définitivement le fichier. L'utilisateur accepte l'import définitif, le référentiel des formats est créé à partir des informations contenues dans le fichier XML soumis.

.. image:: images/import_rf_format_OK.jpg

Recherche d'un format
---------------------

La recherche d'un format dans le référentiel des formats s'effectue depuis l'écran "Référentiel des formats".
Pour cela, l'utilisateur clique sur le menu "Administration", puis sur le sous-menu "Référentiel des formats".

.. image:: images/menu_rf.png

Par défaut, les formats sont affichés sous le formulaire de recherche et sont classés par ordre alphabétique.

Pour effectuer une recherche précise, on utilise le champ "Nom de format" ou le champ "PUID" (Le PUID étant l'ID unique du format dans PRONOM). Si on renseigne les deux champs de façon simultanée, l'opérateur booléen "ET" est implicitement utilisé. La recherche alliera donc le nom du format avec le PUID et donnera ainsi un résultat plus précis.

NB : La recherche n'a pas besoin d'être exacte. L'utilisateur peut saisir une chaîne de caractères avec ou sans accent, des mots au singulier comme au pluriel, voire avec une légère tolérance de faute : la solution logicielle Vitam pourra trouver des résultats correspondants.

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher" ou appuie sur la touche "Entrée" si son curseur est positionné dans le champ de recherche.

.. image:: images/rf_format.png

Il est possible de vider le contenu des champs "Nom du format" et "PUID" en cliquant sur la croix située à droite de chacun des champs de recherche.

.. image:: images/FORMATS_champs_recherche.png

Affichage du résultat
---------------------

Suite à la recherche, les résultats sont affichés dans un tableau comportant les informations suivantes :

- PUID : ID unique du format
- Nom de format
- Version : version du format
- MIME : Identifiant de format de données (Type MIME)
- Extensions

.. image:: images/rf_format_resultat.png

Affichage d'un détail du format
-------------------------------

Pour accéder au détail de chaque format, l'utilisateur clique sur la ligne du format désiré.

Une fenêtre de type "modale" s'ouvre, pour présenter le détail du format, tout en conservant la liste des résultats.

.. image:: images/rf_format_detail.png

Le détail d'un format est composé des informations suivantes :

- PUID
- Nom du format
- Type MIME
- Extensions
- Priorité sur les versions précédentes
- Version de PRONOM : contient un lien renvoyant vers la fiche correspondante au format dans le référentiel des formats PRONOM sur le site des Archives nationales britanniques

Un clic sur le bouton "Fermer" ou hors de la fenêtre "modale" referme celle-ci.

Référentiel des règles de gestion
=================================

Cette partie décrit les fonctionnalités d'import du référentiel des règles de gestion, matérialisé par un fichier CSV, permettant de le consulter et de rechercher des règles de gestion spécifiques.

Import du référentiel des règles de gestion
-------------------------------------------

L'import du référentiel des règles de gestion s'effectue depuis le menu "Administration", puis en cliquant sur le sous-menu "Import du référentiel des règles de gestion".

.. image:: images/menu_import_rg.png


L'utilisateur doit au préalable créer le référentiel des règles de gestion au format CSV, puis l'importer dans Vitam.

Plusieurs critères doivent être respectés pour s'assurer de la bonne construction des règles de gestion :

- Identifiants de la règle (obligatoire et unique)
- Types de règle (Obligatoire) :

  - Durée d'utilité Administrative (DUA) : AppraisalRule
  - Délai de Communicabilité (DCOMM) : AccessRule
  - Durée d'utilité courante (DUC) : StorageRule
  - Délai de diffusion (DDIFF) : DisseminationRule
  - Durée de réutilisation (DREUT) : ReuseRule
  - Durée de classification (DCLASS) : ClassificationRule

- Intitulé de la règle (Obligatoire)
- Durée associée à la règle (Obligatoire)
- Unité de valeur associée: jours, mois, année (Obligatoire)
- Description (Optionnel)

Un fichier valide est un fichier respectant toutes les conditions suivantes :

- Il s'agit d'un format CSV dont la structure est bien formée
- Il possède des valeurs dont le format est correct 
- Il comporte des valeurs dans tous les champs obligatoires
- Il possède des valeurs cohérentes avec les besoins métier

Le processus d'import du référentiel se décrit comme ceci :

- L'utilisateur accède à l'interface d'import du référentiel des règles de gestion et clique sur le bouton "Choisissez un fichier" pour sélectionner un fichier
- Le fichier à selectionner est le fichier CSV précédemment décrit
- L'utilisateur valide son choix
- Le système vérifie l'intégrité et la cohérence du fichier

.. image:: images/Import_rf_gestion.jpg

A l'issue du contrôle de cohérence et d'intégrité du fichier, deux cas sont possibles :

- En cas d'erreur : La solution logicielle Vitam détecte des erreurs contenues dans le fichier, l'import de ce dernier n'est pas possible. Un message d'erreur est alors affiché. L'utilisateur doit corriger ses erreurs et procéder à nouveau au import du fichier.

.. image:: images/Import_rf_gestion_KO.jpg

- En cas de succès : La solution logicielle Vitam indique à l'utilisateur que son fichier est valide et lui propose l'import définitif ou son annulation. Si l'utilisateur lance l'import définitif, le référentiel des règles de gestion est créé à partir des informations contenues dans le fichier CSV soumis.

.. image:: images/Import_rf_gestion_OK.jpg

Recherche d'une règle de gestion
--------------------------------

La recherche d'une règle de gestion dans le référentiel des règles de gestion s'effectue depuis l'écran "Référentiel des règles de gestion".
Pour cela, l'utilisateur clique sur le menu "Administration", puis sur le sous-menu "Référentiel des règles de gestion".

.. image:: images/menu_rg.png

Par défaut, les règles de gestion sont affichées sous le formulaire de recherche et sont classées par ordre alphabétique.

Pour effectuer une recherche précise, on utilise le champ "Intitulé" et/ou le champ "Type".

NB : La recherche n'a pas besoin d'être exacte. L'utilisateur peut saisir une chaîne de caractères avec ou sans accent, des mots au singulier comme au pluriel, voir même avec une légère tolérance de faute : la solution logicielle Vitam pourra trouver des résultats correspondants.

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher".
La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée.

.. image:: images/rg_recherche.png

Affichage du résultat
---------------------

Suite à la recherche, les résultats sont affichés dans un tableau comportant les informations suivantes :

- Intitulé de la règle
- Type de règle
- Durée de la règle
- Description de la règle
- Identifiant de la règle

Les résultats sont triés par défaut par ordre alphabétique des intitulés des règles de gestion.

.. image:: images/rg_resultat.jpg

Affichage du détail d'une règle de gestion
------------------------------------------

Pour accéder au détail de chaque règle de gestion, l'utilisateur clique sur la ligne de la règle désirée.

Une fenêtre de type "modale" s'ouvre, pour présenter le détail de la règle de gestion, tout en conservant la liste des résultats.

.. image:: images/rf_gestion_detail.jpg

Le détail d'une règle de gestion est composé des informations suivantes :

- Intitulé de la règle
- Identifiant de la règle
- Description de la règle
- Durée de la règle
- Type de règle
- Mesure
- Date de création de la règle, correspond à la date d'import du référentiel de règle de gestion
- Date de dernière modification

Un clic sur le bouton "Fermer" ou hors de la fenêtre "modale" referme celle-ci.

Gestion des versements
======================

Cette partie décrit les fonctionnalités de la page “Gestion des versements”. Elle permet de suivre l’évolution des
opérations d’entrée, d’utiliser le mode pas à pas.

Affichage des versements
------------------------

Pour accéder à la page de “Gestion des versements”, l’utilisateur clique sur le menu “Administration”, puis sur le
sous-menu “Gestion des versements”.

La page affiche la liste de toutes les opérations d’entrée en cours d’éxécution et déjà réalisées.
La liste est composée des colonnes suivantes :

* Identifiant de l’opération - identifiant unique de l’opération d’entrée
* Catégorie de l’opération - indique le type d’opération d’entrée
* INGEST - indique une opération d’entrée normale
* INGEST_TEST - indique une opération d’entrée en test à blanc
* Date de l’entrée - date à laquelle l’entrée à été soumise à la solution logicielle Vitam
* Mode d’éxécution - indique le mode d’éxécution choisi. Celui-ci peut-être
	* Continu
	* Pas à pas
* Etat global de l’opération d’entrée - indique si l’opération est :
	* En attente
	* En cours
	* Terminée
* Statut : Statut de la dernière étape du workflow réalisée au cours de l’opération d’entrée
* Actions : Contient des boutons d’action permettant d’interragir avec l'entrée réalisée en mode d’éxécution pas à
pas

Les opérations d’entrée sont classées par ordre alphabétique selon leur identifiant.

Utilisation du mode pas à pas
-----------------------------

Lorsque l’entrée est ralisée en mode d’éxécution pas à pas, l’utilisateur doit alors utiliser les boutons d’actions dispo-
nibles afin de faire avancer son traitement.

Les boutons disponibles sont :

* Suivant : permet de passer à l’étape suivante du workflow. Lorsqu’une étape est terminée, il faut cliquer sur
“suivant” pour continuer l’entrée
* Pause : permet de mettre l’opération d’entrée en pause
* Reprise : Permet de reprendre une entrée en pause
* Arrêt : permet d’arrêter complétement une opération d’entrée. Elle passera alors en statut “terminée” et il sera
impossible de la redémarrer.