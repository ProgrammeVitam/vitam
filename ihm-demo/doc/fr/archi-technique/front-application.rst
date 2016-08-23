Architecture technique de l'application Front
#############################################

But de cette documentation
==========================

Cette documentation présente la structure technique de l'application Front Single Page développée avec AngularJS 1.

Le Framework Front : AngularJS 1.5.3
====================================
Les modules AngularJS utilisés:
*******************************
- angular-animate
- angular-resource
- angular-route

Autres frameworks Front utilisés
********************************
- bootstrap (3.3.x) : Responsive feature + CSS + Composants graphiques (bouton + label + zone de saisie)
- jquery (2.2.x)
- angular-material (1.1.0) : Les alertes affichées (de confirmation, d'erreur et d'information) et l'écran de détails d'une opération logbook
- angular-file-upload (2.3.4) : Composant pour l'import des fichiers (SIP, référentiels)
- restangular (1.5.2) : Client REST
- v-accordion (1.6.0) : Composant de regroupement (effet accordion) utilisé dans l'écran du formulaire d'une archive
- bootstrap-material-design-icons: Les îcones utilisées dans le menu et les boutons

Organisation de l'application
=============================

/webapp

    **/archives** : les fichiers json utilisés pour tester l'affichage du formulaire d'une archive unit côté Front
    
    **/bower_components** : librairies et dépendances (téléchargées en exécutant npm install ou bower install)

    **/css** : feuilles de styles

    **/images** : images affichées dans l'application

    /js
		**/controller** : controllers AngularJS
    /modules		
		**/archive-unit** : module qui gére l'écran du formulaire d'une Archive Unit

		**/archive-unit-search** : module qui gére l'écran de recherche des Archive Units

		**/config**: contient les éventuels fichiers utilisés pour customiser l'affichage. Actuellement, le fichier de traduction d'un premier lot de labels de meta données a été ajouté.

		**/core** : contient les factories et les services

		**/file-format** : module qui gére l'écran d'import du référentiel PRONOUN

		**/logbook** : module qui gére l'écran de recherche d'opérations Logbook

		**/parent**	: répertoire qui contient les fichiers app.module.js et app.config.js qui définissent respectivement les modules Angular et les routes déclarés dans l'application.

	/views : templates HTML

	bower.json : dépendances gérées par bower
	
	index.html : page principale	

	package.json : fichier de configuration nodejs