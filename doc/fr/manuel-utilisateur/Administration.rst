Administration
##############

Cette partie décrit les fonctionnalités d'administration proposées à l'utilisateur :

- Consultation des référentiels (contextes applicatifs, contrats d'entrée. contrats d'accès, formats, profils d'archivage, règles de gestion et services agents)
- Import des référentiels
- Import d'un arbre de positionnement (arbre de positionnement, contextes applicatifs, contrats d'entrée. contrats d'accès, formats, profils d'archivage, règles de gestion et services agents)
- Consultation et suivi des opérations et des opérations de sécurisation

Journal des opérations
======================

Pour consulter le journal des opérations, l'utilisateur survole le menu "Administration", puis le sous-menu "Opérations" et sélectionne "Journal des opérations".

|

.. image:: images/menu_jdo.png

Il permet à l'utilisateur d'accéder à toutes les opérations effectuées dans la solution logicielle Vitam, par catégorie d'opération.

Ces catégories d'opération sont :

- Audit
- Données de base
- Elimination (pas encore développé)
- Entrée
- Export DIP
- Mise à jour des métadonnées de description
- Préservation (pas encore développé)
- Sécurisation
- Vérification (pas encore développé)

Par défaut, l'ensemble des opérations s'affiche, de la date d'opération la plus récente à la plus ancienne.


Recherche d'opérations
-----------------------

Par défaut, les opérations sont affichées sous le formulaire de recherche et sont classées par ordre ante chronologique. Pour effectuer une recherche précise, on utilise les champs "Identifiant" ou "Catégorie d'opération" :

- Identifiant : identifiant de l'opération donné par le système
- Catégories d'opération : présentées sous forme de liste triée alphabétiquement, elles permettent à l'utilisateur de sélectionner une catégorie d'opération

NB : Il est impossible d'effectuer une recherche croisée par identifiant et catégorie d'opération.

Pour initier la recherche, l'utilisateur saisit un critère de recherche et clique sur le bouton "Rechercher". La liste des opérations est alors actualisée avec les résultats correspondants à la recherche.

|

.. image:: images/rechch_jdo.png


Consultation des résultats
--------------------------

Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- la catégorie de l'opération
- l'opération, le type de l'opération
- la date de début d'opération
- le statut de l'opération (en cours, erreur, succès, avertissement)
- Le message de l'opération

|

.. image:: images/res_jdo.png

Le bouton "Informations supplémentaires" permet d'afficher les autres informations du journal des opérations. Il suffit pour cela de cocher dans la liste les informations voulues.

Liste des informations supplémentaires disponibles :

- Identifiant de l'opération
- Code technique
- Identifiant de l'agent interne
- Identifiant interne de l'objet
- Informations complémentaires sur le résultat
- Règles utilisées
- Identifiant de la requête
- Identifiant des agents externes
- Identifiant externe du lot d'objets
- Identifiant du tenant
- Identifiant de l'application
- Identifiant de la transaction
- Rapport

L'utilisateur a la possibilité d'afficher toutes les colonnes supplémentaires en cliquant sur la coche située tout en haut de la liste. Un clic sur le bouton "Informations supplémentaires" ferme la liste.

Une fois l'ensemble des colonnes affichées, l'utilisateur peut cliquer à nouveau sur la première coche afin de revenir à l'affichage d'origine.


Détail d'une opération
-----------------------

Suite à la recherche d'une opération, l'utilisateur peut consulter le détail des événements intervenus durant l'opération.
Pour accéder au détail d'une opération, l'utilisateur clique sur la ligne souhaitée.

Le détail est composé de deux parties, le descriptif de l'opération qui récapitule les informations de l'opération avec la possibilité d'afficher les informations supplémentaires.


.. image:: images/desc_jdo.png

Puis, les détails de l'opération qui sont présentés sous forme de liste comportant, pour chaque événement, les éléments suivants :

- le nom de l'étape
- la date à laquelle l'étape a été effectuée
- le message expliquant le statut de cette étape
- le statut présenté sous forme de pictogramme

Un clic sur la flèche située à côté du message permet d'afficher davantage d'informations concernant cette étape. Un clic sur un signe "+" situé à côté d'un message affiche les détails des données de l'évènement.


.. image:: images/detail_jdo.png

Référentiel des formats
=======================

Cette partie décrit les fonctionnalités d'import et de consultation du référentiel des formats (basé sur une version récente du référentiel des formats PRONOM mis à disposition par les Archives nationales britanniques).

Import du référentiel des formats
---------------------------------

Pour y accéder à l'écran d'import du référentiel, l'utilisateur survole le menu "Administration", puis le sous-menu "Import des référentiels" et sélectionne "Import des formats".

|

.. image:: images/menu_imports.png

L'import du référentiel ne peut être effectué sans le fichier PRONOM. Pour cela, l'utilisateur peut récupérer ce fichier dans sa version la plus récente sur le site des Archives nationales britanniques :

- http://www.nationalarchives.gov.uk
- Section "PRONOM" > "DROID signature files"

Le processus d'import du référentiel se décrit comme ceci :

- Accéder à l'écran d'import du référentiel des formats et cliquer sur le bouton "sélectionner un fichier" ou faire glisser le fichier sur l'espace de téléchargement
- Sélectionner le fichier .xml PRONOM récupéré précédemment
- Cliquer sur le bouton "Importer"

|

.. image:: images/import_formats.png
   :scale: 50
   
A l'issue du contrôle de cohérence et d'intégrité du fichier, plusieurs cas sont possibles :

- En cas d'erreur de fichier, la solution logicielle Vitam détecte des erreurs contenues dans le fichier, l'import de ce dernier n'est pas possible, un message d'erreur s'affiche. L'utilisateur doit corriger ces erreurs et soumettre à nouveau le fichier s'il souhaite toujours effectuer son import.

- En cas d'erreur pour cause de référentiel déjà existant détecté par la solution logicielle Vitam. Un message d'erreur s'affiche. L'import devient impossible.

|

.. image:: images/import_ko.png
   :scale: 50

- En cas de succès : La solution logicielle Vitam indique à l'utilisateur que son fichier est valide et lui propose d'importer définitivement le fichier. L'utilisateur peut ainsi accepter l'import définitif et le référentiel des formats est créé à partir des informations contenues dans le fichier XML soumis.


Recherche d'un format
---------------------

La recherche d'un format dans le référentiel des formats s'effectue depuis l'écran "Formats". Pour y accéder, l'utilisateur survole le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Formats".

|

.. image:: images/menu_formats.png

Par défaut, les formats sont affichés sous le formulaire de recherche et sont classés par ordre alphabétique de leur intitulé.

La page affiche un formulaire de recherche composé des champs suivants :

- Intitulé  : permet d'effectuer une recherche approchante sur les intitulés des formats disponibles dans la solution logicielle Vitam
- PUID (identifiant unique du format dans PRONOM) : permet d'effectuer une recherche exacte sur l'identifiant d'un format

NB : Il est impossible d'effectuer une recherche croisée par PUID et intitulé. La recherche par intitulé peut être approximative : chaîne de caractères avec ou sans accent, mots au singulier comme au pluriel, voire une légère tolérance de faute.

|

.. image:: images/rechch_formats.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, les résultats sont affichés dans un tableau comportant les informations suivantes :

- PUID : identifiant unique du format
- Intitulé
- Version : version du format
- MIME : Identifiant de format de données (Type MIME)
- Extension(s)

|

.. image:: images/res_formats.png

Détail d'un format
--------------------

Pour accéder au détail d'un format, l'utilisateur clique sur la ligne souhaitée.

|

.. image:: images/detail_format.png

Le détail d'un format est composé des informations suivantes :

- PUID
- Intitulé
- Type MIME
- Priorité sur les versions précédentes
- Extension(s)
- Date de création
- Version de PRONOM : contient un lien renvoyant vers la fiche correspondante au format dans le référentiel des formats PRONOM sur le site des Archives nationales britanniques.


Référentiel des règles de gestion
=================================

Cette partie décrit les fonctionnalités d'import et de recherche du référentiel des règles de gestion, matérialisé par un fichier CSV, permettant de le consulter et de rechercher des règles de gestion spécifiques.

Import du référentiel des règles de gestion
-------------------------------------------

L'import des règles de gestion est une fonctionnalité réservée à un utilisateur ayant des droits d'administration. La structure et les valeurs des règles de gestion sont décrites dans la documentation du modèle de données.

Pour y accéder, l'utilisateur survole le menu "Administration", puis le sous-menu "Import des référentiels" et sélectionne "Import des règles de gestion". 

|

.. image:: images/menu_imports.png

L'utilisateur doit au préalable créer le référentiel des règles de gestion au format CSV afin de l'importer dans Vitam. Plusieurs critères doivent être respectés pour s'assurer de la bonne construction du référentiel des règles de gestion :

- Identifiants de la règle (obligatoire et unique)
- Types de règle (obligatoire)
- Intitulé de la règle (obligatoire)
- Durée associée à la règle (obligatoire)
- Unité de valeur associée: jours, mois, année (obligatoire)


Un fichier valide est un fichier respectant toutes les conditions suivantes :

- Format CSV dont la structure est bien formée
- Valeurs dont le format est correct
- Tous les champs obligatoires ont des valeurs
- Valeurs cohérentes avec les besoins métier


Le processus d'import du référentiel se décrit comme ceci :

- Accéder à l'interface d'import du référentiel des règles de gestion et cliquer sur le bouton "sélectionner un fichier" ou faire glisser le fichier sur l'espace de téléchargement
- Sélectionner le fichier CSV précédemment décrit
- Cliquer sur le bouton "Importer"

|

.. image:: images/import_rg.png
   :scale: 50
   
   
Une fenêtre modale s'ouvre alors pour indiquer soit :

- Que le référentiel a bien été importé
- Un échec de l'import du fichier, pouvant être causé par :
	- Le fait que les règles de gestion mentionnées existent déjà pour le tenant
	- Le fait que le fichier est invalide (mauvais format)

Cette opération est journalisée et disponible dans le Journal des opérations.


Recherche d'une règle de gestion
--------------------------------

Pour consulter et rechercher les règles de gestion, l'utilisateur survole le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Règles de gestion".

|

.. image:: images/menu_rg.png

Par défaut, les règles de gestion sont affichées sous le formulaire de recherche et sont classées par ordre alphabétique de leur intitulé.

Pour effectuer une recherche précise, on utilise le champ "Intitulé" et/ou le champ "Type".

NB : La recherche par intitulé peut être approximative : chaîne de caractères avec ou sans accent, mots au singulier comme au pluriel, voire une légère tolérance de faute.

|

.. image:: images/rechch_rg.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, les résultats sont affichés dans un tableau comportant les informations suivantes :

- Intitulé
- Type
- Durée de la règle
- Description
- Identifiant

|

.. image:: images/res_rg.png

Détail d'une règle de gestion
-------------------------------

Pour accéder au détail de chaque règle de gestion, l'utilisateur clique sur la ligne souhaitée.

|

.. image:: images/detail_rg.png

Le détail d'une règle de gestion est composé des informations suivantes :

- #id (identifiant donné à la règle de gestion par la solution logicielle Vitam)
- Identifiant
- Type
- Intitulé
- Durée
- Mesure
- Date de création de la règle (correspond à la date d'import du référentiel de règle de gestion)
- Date de dernière modification


Contrats
=========

Les contrats permettent de gérer les droits donnés aux utilisateurs et applicatifs. Deux types de contrats sont disponibles dans la solution logicielle Vitam :

- Contrats d'accès
- Contrats d'entrée


Contrats d'entrée
==================

Import de contrats d'entrée
-----------------------------

L'import d'un contrat est une fonctionnalité réservée à un utilisateur ayant des droits d'administration. La structure et les valeurs des contrats sont décrites dans la documentation du modèle de données.

Pour importer un contrat d'entrée, l'utilisateur survole le menu "Administration", puis le sous-menu "Import des référentiels" et sélectionne "Import des contrats d'entrée".

|

.. image:: images/menu_imports.png

Plusieurs critères doivent être respectés pour s'assurer de la bonne construction du fichier :

- Nom (obligatoire)
- Description (obligatoire)
- Statut (facultatif) : si aucun statut n'est défini, le contrat sera inactif par défaut

L'utilisateur sélectionne le fichier (.json) à importer en cliquant sur "sélectionner un fichier" ou en le faisant glisser sur l'espace de téléchargement, puis clique sur "Importer" pour lancer l'opération.

.. image:: images/import_ce.png
   :scale: 50

Une fenêtre modale s'ouvre alors pour indiquer soit :

- Que les contrats ont bien été importés
- Un échec de l'import du fichier, pouvant être causé par :
	- Le fait que les contrats mentionnés existent déjà pour le tenant
	- Le fait que le fichier est invalide (mauvais format ou champ obligatoire absent)

Cette opération est journalisée et disponible dans le Journal des opérations.

Recherche d'un contrat d'entrée
--------------------------------

Pour consulter et rechercher les contrats d'entrée, l'utilisateur survole sur le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Contrats d'entrée".

|

.. image:: images/menu_ce.png

Par défaut, les contrats d'entrée sont affichés sous le formulaire de recherche et sont classés par ordre alphabétique de leur intitulé.

La page affiche un formulaire de recherche composé des champs suivants :

- Intitulé : permet d'effectuer une recherche approchante sur les intitulés des contrats d'entrée disponibles dans la solution logicielle
- Identifiant : permet d'effectuer une recherche exacte sur l'identifiant d'un contrat

NB : Il est impossible d'effectuer une recherche croisée entre identifiant et intitulé. La recherche par intitulé peut être approximative : chaîne de caractères avec ou sans accent, mots au singulier comme au pluriel, voire une légère tolérance de faute.

|

.. image:: images/rechch_ce.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- Intitulé
- Identifiant
- Tenant
- Statut
- Date de création
- Dernière modification

|

.. image:: images/res_ce.png

Détail d'un contrat d'entrée
-----------------------------

Pour accéder au détail d'un contrat, l'utilisateur clique sur la ligne souhaitée. La page "Détail d'un contrat d'entrée" contient les informations suivantes :

- Identifiant
- Intitulé
- Description
- Statut
- Date de création
- Date de mise à jour
- Profils d'archivage
- Nœud de rattachement
- Tenant

|

.. image:: images/detail_ce.png

**Modifier un contrat d'entrée**

Il est possible de modifier un contrat d'entrée en cliquant sur le bouton "Modifier" sur l'écran de détail du contrat. L'interface permet la modification de plusieurs champs du contrat, l'ajout d'un noeud de rattachement, ainsi que de changer son statut (actif/inactif). Il est également possible d'ajouter ou de supprimer des profils d'archivage (identifiant) au travers d'un système de tag.

|
 .. image:: images/ce_update.png
 
Une fois les modifications saisies, un clic sur le bouton "Sauvegarder" permet de les enregistrer. A l'inverse, le bouton "Annuler" permet de retourner à l'état initial de l'écran du détail du contrat.

*Activation / désactivation*

L'administrateur a la possibilité d'activer / désactiver un contrat. Un bouton permet de sélectionner le statut actif ou inactif. Un clic sur ce bouton change la valeur du statut.

*Restriction d'entrée par profil d'archivage*

Il est possible d'ajouter dans ce champ un ou plusieurs identifiants de profils d'archivage. Les SIP qui utilisent ce contrat d'entrée doivent obligatoirement avoir l'un des profils d'archivage autorisé dans son bordereau.

*Nœud de rattachement*

Il est possible d'ajouter dans ce champ l'identifiant (GUID) d'une unité archivistique de plan de classement ou d'arbre de positionnement. Les SIP qui utilisent ce contrat d'entrée sont automatiquement rattaché à l'unité archivistique déclarée dans le nœud de rattachement.


Contrats d'accès
=================

Import de contrats d'accès
---------------------------

L'import de contrats est une fonctionnalité réservée à un utilisateur ayant des droits d'administration. La structure et les valeurs des contrats sont décrites dans la documentation du modèle de données.

Pour importer un contrat d'accès, l'utilisateur survole le menu "Administration", puis le sous-menu "Import des référentiels" et sélectionne "Import des contrats d'accès".

|

.. image:: images/menu_imports.png

Plusieurs critères doivent être respectés pour s'assurer de la bonne construction du fichier :

- Nom (obligatoire)
- Description (obligatoire)
- Statut (facultatif) : si aucun statut n'est défini, le contrat sera inactif par défaut

L'utilisateur sélectionne ensuite le fichier (.json) à importer en cliquant sur "sélectionner un fichier" ou en le faisant glisser sur l'espace de téléchargement, puis clique sur "Importer" pour lancer l'opération.


.. image:: images/import_ca.png
   :scale: 50

Une fenêtre modale s'ouvre alors pour indiquer soit :

- Que les contrats ont bien été importés
- Un échec de l'import du fichier, pouvant être causé par :
	- Le fait que les contrats mentionnés existent déjà pour le tenant
	- Le fait que le fichier est invalide (mauvais format ou champ obligatoire absent)

Cette opération est journalisée et disponible dans le Journal des opérations.

Recherche d'un contrat d'accès
------------------------------

Pour consulter et rechercher les contrats d'accès, l'utilisateur survole sur le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Contrats d'accès".

|

.. image:: images/menu_ca.png

Par défaut, les contrats d'accès sont affichés sous le formulaire de recherche et sont classés par ordre alphabétique de leur intitulé.

La page affiche un formulaire de recherche composé des champs suivants :

- Intitulé : permet d'effectuer une recherche approchante sur les intitulés des contrats d'accès disponibles dans la solution logicielle Vitam
- Identifiant : permet d'effectuer une recherche exacte sur l'identifiant d'un contrat

NB : Il est impossible d'effectuer une recherche croisée entre identifiant et intitulé. La recherche par intitulé peut être approximative : chaîne de caractères avec ou sans accent, mots au singulier comme au pluriel, voire une légère tolérance de faute.

|

.. image:: images/rechch_ca.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- Intitulé
- Identifiant
- Tenant
- Statut
- Date de création
- Dernière modification

|

.. image:: images/res_ca.png

Détail d'un contrat d'accès
---------------------------

Pour accéder au détail d'un contrat, l'utilisateur clique sur la ligne souhaitée. La page "Détail d'un contrat d'accès" contient les informations suivantes :

- Identifiant
- Intitulé
- Description
- Statut
- Tous les services producteurs ou une liste blanche de services producteurs
- Date de création
- Date de dernière modification
- Droit d'écriture
- Tous les usages autorisés ou une liste blanche d'usages
- Nœuds de consultation

|

.. image:: images/detail_ca.png

**Modifier un contrat d'accès**

Il est possible de modifier un contrat d'accès en cliquant sur le bouton "Modifier" sur l'écran de détail du contrat. L'interface permet la modification de plusieurs champs du contrat, ainsi que de changer son statut (actif/inactif). Il est également possible d'ajouter ou de supprimer des services producteurs (identifiant) et des noeuds de consultation (identifiant) au travers d'un système de tag ainsi que des usages à sélectionner via une liste.

|

 .. image:: images/ca_update.png
 
 
Une fois les modifications saisies, un clic sur le bouton "Sauvegarder" permet de les enregistrer. A l'inverse, le bouton "Annuler" permet de retourner à l'état initial de l'écran du détail du contrat.

*Activation / désactivation*

L'administrateur a la possibilité d'activer / désactiver un contrat. Un bouton permet de sélectionner le statut actif ou inactif. Un clic sur ce bouton change la valeur du statut.

*Restriction d'accès par service producteur*

Un contrat peut autoriser l'accès à tous ou certains services producteurs d'objets inclus dans une liste blanche. Deux options sont disponibles :

 - accès à tous les services producteurs en cliquant sur le bouton "Tous les services producteurs" afin de changer sa valeur à "oui"
 - accès à une sélection de services producteurs en cliquant sur le bouton "Tous les services producteurs" afin de changer sa valeur à "non", puis en cochant dans la liste déroulante les valeurs souhaitées

*Restriction d'accès par usage de l'objet*

Un contrat peut autoriser l'accès à tous ou certains usages d'objets inclus dans une liste blanche. (Ex. : l'utilisateur peut accéder aux usages de diffusion mais pas à la source de l'objet). Deux options sont disponibles:

 - accès à tous les services producteurs en cliquant sur le bouton "Tous les usages"
 - accès à une sélection de services producteurs en cliquant sur le bouton "Liste blanche uniquement"

*Restriction par nœud de consultation*

Un contrat peut restreindre l'accès aux unités archivistiques listées en tant que nœuds de consultation ainsi qu'à leurs enfants. Chaque unité archivistique renseignée est identifiée par son identifiant. Si aucune unité archivistique n'est renseignée, alors l'accès du détenteur du contrat n'est pas restreint à des nœuds de consultation.


Contextes applicatifs
=======================

Import de contextes
--------------------

L'import de contextes est une fonctionnalité réservée à un utilisateur ayant des droits d'administration. La structure et les valeurs des contextes sont décrites dans la documentation du modèle de données.

Pour importer un contexte, l'utilisateur survole le menu "Administration", puis le sous-menu "Import des référentiels" et sélectionne "Import des contextes applicatifs".

|

.. image:: images/menu_imports.png

L'utilisateur sélectionne ensuite le fichier (.json) à importer en cliquant sur "sélectionner un fichier" ou en le faisant glisser sur l'espace de téléchargement, puis clique sur "Importer" pour lancer l'opération.

|

.. image:: images/import_contextes.png
   :scale: 50
   
Une fenêtre modale s'ouvre alors pour indiquer soit :

- Que les contextes ont bien été importés
- Un échec de l'import du fichier, pouvant être causé par :

	- Le fait que le contexte existe déjà dans le système
    - Le fait que le fichier est invalide (mauvais format ou champ obligatoire absent)
    - Le fait que le contexte déclare des contrats d'entrée ou des contrats d'accès qui n'existent pas dans les référentiels des contrats de leur tenant.

Cette opération est journalisée et disponible dans le Journal des opérations.

Rechercher un contexte applicatif
-----------------------------------

Pour consulter et rechercher les contextes applicatifs, l'utilisateur survole le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Contextes applicatifs".

|

.. image:: images/menu_contextes.png

Par défaut, les contextes applicatifs sont affichés sous le formulaire de recherche et sont classés par ordre alphabétique de leur intitulé.

La page affiche un formulaire de recherche composé des champs suivants :

    - Intitulé : permet d’effectuer une recherche approchante sur les noms des contextes applicatifs disponibles dans la solution logicielle Vitam.
    - Identifiant : permet d’effectuer une recherche exacte sur l'identifiant d'un contexte applicatif

|

.. image:: images/rechch_contextes.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

    - Intitulé
    - Identifiant
    - Statut
    - Contrat d'accès
    - Contrat d'entrée
    - Date de création
    - Dernière modification

NB : une coche indique la présence d'au moins un contrat, une croix indique qu'aucun contrat n'est présent

Le bouton "Informations supplémentaires" permet d'afficher les autres informations du journal des opérations. Il suffit de cocher dans la liste les informations voulues.

Les informations supplémentaires disponibles sont :

- GUID

|

.. image:: images/res_contextes.png


Détail d'un contexte
---------------------

Pour accéder au détail d'un contexte applicatif, l'utilisateur clique sur la ligne souhaitée. La page "Détail du contexte applicatif" contient les informations suivantes :

- Identifiant
- Intitulé
- Date de création
- Dernière modification
- Statut
- Profil de sécurité
- Activation des permissions

Les tenants sont affichés par bloc. Chaque bloc contenant les informations suivantes :

- L'identifiant du tenant
- La liste des contrats d'accès associés à ce tenant
- La liste des contrats d'entrée associés à ce tenant

|

.. image:: images/detail_contexte.png

**Modifier un contexte applicatif**

Il est possible de modifier un contexte applicatif depuis son l'écran de son détail en cliquant sur le bouton "Modifier" sur l'écran de détail d'un contexte. L'interface permet la modification de plusieurs champs du contexte, ainsi que de changer ses permissions (actif/inactif).

*Activation / désactivation du contexte applicatif*

L'administrateur a la possibilité d'activer / désactiver un contexte. Un bouton permet de sélectionner le statut actif ou inactif. Un clic sur ce bouton change la valeur du statut.

*Activation / désactivation du contrôle des permissions*

L'administrateur a la possibilité d'activer / désactiver le contrôle du contexte. Un bouton permet de sélectionner son état actif ou inactif. Un clic sur ce bouton change la valeur du statut.

*Tenants*

Il est possible d'ajouter ou supprimer des tenants concernés par le contexte en sélectionnant un identifiant de tenant en haut à droite et en cliquant sur "Ajouter". Il est impossible d'ajouter un tenant qui se trouve déjà dans la liste des tenants de ce contexte.
Pour supprimer un tenant, il suffit de cliquer sur le bouton supprimer correspondant au tenant à retirer, et de valider cette suppression en utilisant le bouton "enregistrer".
Au sein de chacun de ces tenant, il est possible d'ajouter ou supprimer des contrats d'accès et des contrats d'entrée au travers un système de tag.


.. image:: images/contexte_update.png

Une fois les modifications saisies, un clic sur le bouton "Sauvegarder" permet de les enregistrer. A l'inverse, le bouton "Annuler" permet de retourner à l'état initial de l'écran du détail du contexte.


Profils d'archivage
===================

Importer un profil d'archivage
------------------------------

L'import de notice détaillant les profils d'archivage est une fonctionnalité réservée à un utilisateur ayant des droits d'administration. La structure et les valeurs des notices descriptives de profils d'archivages sont décrites dans la documentation du modèle de données.

Pour importer une notice descriptive de profil d'archivage, l'utilisateur survole le menu "Administration", puis le sous-menu "Import de référentiels" et sélectionne "Import des profils d'archivage".

|

.. image:: images/menu_imports.png

Plusieurs critères doivent être respectés pour s'assurer de la bonne construction du fichier :

- Nom : intitulé du profil d'archivage (obligatoire)
- Description : description du profil d'archivage (obligatoire)
- Format : format attendu pour le profil SEDA (XSD ou RNG) (obligatoire)
- Statut (facultatif) : si aucun statut n'est défini, le profil sera inactif par défaut

L'utilisateur sélectionne ensuite le fichier (.json) à importer en cliquant sur "sélectionner un fichier" ou en le faisant glisser sur l'espace de téléchargement, puis clique sur "Importer" pour lancer l'opération.


.. image:: images/import_profils.png
   :scale: 50
   
Une fenêtre modale indique alors soit :

- Les profils ont bien été importés
- Échec de l’import du fichier, pouvant être causé par :
	- le fait que le(s) profil(s) d'archivage mentionné(s) existe(nt) déjà pour le tenant
	- le fait que le fichier est invalide (mauvais format ou champ obligatoire absent)

Cette opération est journalisée et disponible dans le Journal des opérations.


Recherche d'un profil d'archivage
---------------------------------

Pour consulter et rechercher les profils d'archivage, l'utilisateur survole le menu "Administration", puis le sous-menu "Référentiels" et sélectionne "Profils d'archivage".

|

.. image:: images/menu_profil.png

Par défaut, les notices descriptives de profils d'archivage sont affichées sous le formulaire de recherche et sont classées par ordre alphabétique de leur intitulé.

La page affiche un formulaire de recherche composé des champs suivants :

- Intitulé : permet d’effectuer une recherche approchante sur les noms des notices descriptives de profils d'archivage disponibles dans la solution logicielle Vitam.
- Identifiant : permet d’effectuer une recherche exacte sur les identifiants des notices descriptives de profils d'archivage.

NB : Il est impossible d'effectuer une recherche croisée entre identifiant et intitulé. La recherche par intitulé peut être approximative : chaîne de caractères avec ou sans accent, mots au singulier comme au pluriel, voire une légère tolérance de faute.

|

.. image:: images/rechch_profil.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondant à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- Intitulé
- Identifiant
- Statut
- Date de de création
- Dernière modification
- Profil

Lorsqu'un profil SEDA de règle a été associé au profil, une flèche indiquant la possibilité de le télécharger apparaît dans la colonne "Profil". L'utilisateur peut lancer le téléchargement en cliquant dessus.

|

.. image:: images/res_profil.png

Détail d'un profil d'archivage
-------------------------------

Pour accéder au détail d'un profil d'archivage, l'utilisateur clique sur la ligne souhaitée. La page "Détail du profil d'archivage" contient les informations suivantes :

- Identifiant
- Intitulé
- Description
- Statut
- Tenant
- Date de création
- Dernière modification
- Format
- Fichier

|

.. image:: images/detail_profil.png

**Modifier un profil d'archivage**

Il est possible de modifier un profil d'archivage en cliquant sur le bouton "Modifier" sur l'écran de détail du profil d'archivage. L'interface permet la modification de plusieurs champs du profil.

Une fois les modifications saisies, un clic sur le bouton "Sauvegarder" permet de les enregistrer. A l'inverse, le bouton "Annuler" permet de retourner à l'état initial de l'écran du détail du contrat.

*Associer un fichier XSD ou RNG à un profil d'archivage*

Pour importer un profil au format XSD ou RNG à associer à une notice descriptive de profil d'archivage, l'utilisateur clique sur le bouton "Parcourir" à côté du champ "Fichier" puis clique sur "Importer". Le format du fichier doit correspondre au format attendu, indiqué dans le champ format.

A la fin de l'opération d'import, une fenêtre modale indique un des deux messages suivants :

- Le profil a bien été importé
- Echec de l'import du fichier

L'opération est journalisée et disponible depuis l'écran de consultation du journal des opérations.

En cas de succès de l'import du profil XSD ou RNG, la date de mise à jour de la notice descriptive de profil est ajustée en conséquence. Si l'utilisateur importe un profil XSD ou RNG alors qu'un autre profil SEDA a déjà été importé, alors le nouveau fichier remplace l'ancien.

Import d'un arbre de positionnement
===================================

Pour importer un arbre de positionnement, l'utilisateur survole le menu "Administration", puis le sous-menu "Import de référentiels" et sélectionne "Arbre de positionnement".

|

.. image:: images/menu_imports.png

L'utilisateur sélectionne ensuite le dossier à importer en cliquant sur "sélectionner un fichier" ou en le faisant glisser sur l'espace de téléchargement.

Plusieurs options sont présentes sur l'écran :

- Mode d'exécution :
	- le mode d'exécution "pas à pas" permettant de réaliser progressivement l'entrée en passant d'une étape à une autre. (NB : Les actions liées au processus d'entrée en mode "pas à pas" se retrouvent dans la partie Administration du manuel utilisateur).
	- le mode d'exécution "en continu" permettant de lancer le processus d'entrée dans sa globalité en une seule fois. Dans la grande majorité des cas, ce mode d'exécution sera le choix adopté.

- Destination : actuellement, seule l'option "production" est disponible pour verser directement l'arbre de positionnement.

Le mode d'exécution et la destination sont obligatoires.

Pour lancer le transfert de l'arbre, l’utilisateur clique sur le bouton « Importer ».

Les informations visibles à l'écran sont :

- Un tableau comportant les champs suivants :

  - Nom du fichier,
  - Taille : Affiche la taille de l'arbre en Ko, Mo ou Go en fonction de la taille arrondie au dixième près,
  - Statut (succès, erreur ou avertissement)

Une barre de progression affiche l’avancement du téléchargement de l'arbre dans Vitam (une barre de progression complète signifie que le téléchargement est achevé).

NB : Suite au téléchargement de l'arbre, un temps d'attente est nécessaire, correspondant au traitement de l'arbre par le système avant affichage du statut final. Dans ce cas, une roue de chargement est affichée au niveau du statut.

|

.. image:: images/import_arbre.png

Les formats de SIP attendus sont : ZIP, TAR, TAR.GZ, TAR.BZ2, TAR.GZ2

Si l'utilisateur tente d'importer un arbre dans un format non conforme, alors le système empêche le téléchargement et une fenêtre modale s'ouvre indiquant que le fichier est invalide.

Toute opération d'entrée (succès, avertissement et erreur technique ou métier) fait l'objet d'une écriture dans le journal des opérations et génère une notification qui est proposée en téléchargement à l'utilisateur.

Cette notification ou ArchiveTransferReply (ATR) est au format XML conforme au schéma SEDA 2.0.
Lors d'une entrée en succès dans la solution logicielle Vitam, l'ATR comprend les informations suivantes :

- Date : date d'émission de l'ATR
- MessageIdentifier : identifiant de l'ATR. Cet identifiant correspond à l'identification attribuées à la demande de transfert par la solution logicielle Vitam
- ArchivalAgreement : contrat d'entrée
- CodeListVersion : la liste des référentiels utilisés
- La liste des unités archivistiques avec l'identifiant fourni dans la demande de transfert et l'identifiant généré par la solution logicielle Vitam (SystemId)
- ReplyCode : statut final de l'entrée
- GrantDate : date de prise en charge du plan
- ArchivalAgency : service d'archives
- TransferringAgency : service de transfert d'archives

En cas de rejet de l'entrée, l'ATR contient les mêmes informations que l'ATR en succès ainsi que la liste des problèmes rencontrés :

- Outcome : statut de l'étape ou de la tâche ayant rencontré au moins une erreur
- OutcomeDetail : code interne à la solution logicielle Vitam correspondant à l'erreur rencontrée
- OutcomeDetailMessage : message d'erreur

La notification comprend ensuite la liste des erreurs rencontrées (échec ou avertissement), au niveau des unités archivistiques sous la forme de blocs <event>.


Gestion des opérations
======================

Cette partie décrit les fonctionnalités de la page “Gestion des opérations”. Elle permet de suivre l’évolution des opérations et d’utiliser le mode pas à pas.


Recherche d'une opération
-------------------------

Pour consulter et rechercher une opération, l'utilisateur survole le menu "Administration", puis le sous-menu "Opérations" et sélectionne "Gestion des opérations".

|

.. image:: images/menu_gestion.png

Par défaut, les opérations d’entrée sont classées par ordre ante chronologique selon leur date d'entrée et seules les opérations en cours de traitement sont affichées sur cet écran.

La page affiche un formulaire de recherche composé des champs suivants :

- Identifiant : identifiant unique de l’opération d’entrée
- Catégorie : indique le type d’opération
- Statut : statut actuel de l'opération
- Etats : état actuel de l'opération
- Dernière étape : dernière étape à laquelle le workflow s'est arrêté
- Dates de début : date de début de l'opération
- Dates de fin : date de fin de l'opération

NB : Il est impossible d'effectuer une recherche croisée par identifiant et tout autre champ.

|

.. image:: images/rechch_gestion.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste des opérations est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- Identifiant de la demande d'entrée : identifiant unique de l’opération
- Catégorie de l’opération : indique le type d’opération
	- Entrée : indique une opération d’entrée normale
	- Entrée test : indique une opération d’entrée en test à blanc
- Date de l’entrée : date à laquelle l’entrée a été soumise à la solution logicielle Vitam
- Mode d’exécution : indique le mode d’exécution choisi, celui-ci peut être
	- Continu
	- Pas à pas
- Etat : indique l'état actuel de l'opération
    - Pause
    - En cours
    - Terminé
- Statut : indique le statut actuel de l'opération
    - Succès
    - Echec
    - Avertissement
    - Erreur
- Précédente étape du workflow / étape en cours
- Prochaine étape du workflow
- Action : Contient des boutons d’action permettant d’interagir avec l'entrée réalisée en mode d’exécution pas à pas

|

.. image:: images/res_gestion.png

Utilisation du mode pas à pas
-----------------------------

Lorsque l’entrée est réalisée en mode d’exécution pas à pas, l’utilisateur doit alors utiliser les boutons d’actions disponibles afin de faire avancer son traitement.
Les boutons disponibles sont :

- Suivant : permet de passer à l’étape suivante du workflow - lorsqu’une étape est terminée, il faut cliquer sur “suivant” pour continuer l’entrée
- Pause : permet de mettre l’opération d’entrée en pause
- Rejouer : permet de rejouer l'étape dernièrement exécutée du workflow - lorsque cette étape est terminée, il faut cliquer sur “suivant” pour continuer l’entrée
- Reprise : permet de reprendre une entrée en pause
- Arrêt : permet d’arrêter complètement une opération d’entrée. Elle passera alors en statut “terminée” et il sera impossible de la redémarrer


Recherche et vérification des opérations de sécurisation
========================================================

La sécurisation des journaux permet de garantir la valeur probante des archives prises en charge dans la solution logicielle Vitam.

Le fichier produit par une opération de sécurisation des journaux est appelé un "journal sécurisé".

Les administrateurs ont la possibilité d'accéder aux fonctionnalités suivantes :

- Recherche de journaux sécurisés
- Consultation du détail d'un journal sécurisé
- Vérification de l'intégrité d'un journal sécurisé

Recherche de journaux sécurisés
--------------------------------

Pour accéder à la page de “Opérations de sécurisation”, l'utilisateur survole le menu "Administration", puis le sous-menu "Opérations" et sélectionne "Opérations de sécurisation".

|

.. image:: images/menu_secu.png

Par défaut, les journaux sont affichés sous le formulaire de recherche et sont classés par ordre ante chronologique.
La page affiche un formulaire de recherche composé des champs suivants :

- Identifiant de l’objet : identifiant du fichier recherché
- Date de début et date de fin : intervalle de dates permettant de rechercher sur les dates du premier et du dernier journal pris en compte dans l'opération de sécurisation
- Type de journal sécurisé : liste déroulante permettant de sélectionner le type de journal sécurisé à afficher.

|

.. image:: images/rechch_secu.png

Pour initier la recherche, l'utilisateur saisit ses critères de recherche et clique sur le bouton "Rechercher". La liste du référentiel est alors actualisée avec les résultats correspondants à la recherche souhaitée. Suite à la recherche, le résultat est affiché sous forme de tableau, comportant les informations suivantes :

- Type de journal sécurisé : affiche le type de journal sécurisé
- Date de début : indique la date de début de l’opération de sécurisation
- Date de fin : indique la date de fin de l’opération de sécurisation
- Télécharger : icône permettant de télécharger le journal sécurisé

|

.. image:: images/res_secu.png


Chaque ligne comporte un symbole de téléchargement. En cliquant sur ce symbole, le journal est téléchargé sous forme de zip. Le nom de ce fichier correspond à la valeur du champ FileName du dernier event du journal de l'opération.

Détail d'un journal sécurisé
----------------------------

Pour accéder au détail d'un journal sécurisé, l'utilisateur clique sur la ligne souhaitée. La page "Détail de l'opération" est composée de 3 parties et contient les informations suivantes :

- Opération
    - Date de début : date du premier journal pris en compte dans l'opération de sécurisation
    - Date de fin : date du dernier journal pris en compte dans l'opération de sécurisation
    - Nombre d'opération : il s'agit du nombre de journaux pris en compte dans l'opération de sécurisation
- Fichier
    - Nom du fichier : nom du journal sécurisé
    - Taille du fichier : taille du journal sécurisé
- Sécurisation
    - Algorithme de hashage : indique l'algorithme utilisé
    - Date du tampon d'horodatage
    - CA signataire : l'autorité de certification
- Hash de l'arbre de Merkle

|

.. image:: images/detail_secu.png

Vérification d'un journal sécurisé
----------------------------------

En cliquant sur le bouton "Lancer la vérification", la solution logicielle Vitam vérifie que les informations de l'arbre de hashage sont à la fois conformes au contenu du journal sécurisé et aux journaux disponibles dans la solution logicielle Vitam.

Une fois l'opération terminée, son détail est affiché. Il est également disponible dans le Journal des opérations.

Un clic sur le bouton "Télécharger" permet d'obtenir le journal sécurisé.

|

.. image:: images/verif_secu.png
