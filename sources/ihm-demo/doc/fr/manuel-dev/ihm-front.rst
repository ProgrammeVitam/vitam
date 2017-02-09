IHM Front
#########

Cette documentation décrit la partie front/Angular de l'ihm et en particulier sa configuration et ses modules.
--------------------------------------------------------------------------------------------------------------

Utils et général / Composition du projet Angular
************************************************

TODO

Composition du projet
=====================

NPM + Bower
Gulp + Archi projet (modules, css, ...) => AngularJS
Tests unitaires: Voir ihm-tests.rst


Grunt et déployement à chaud
============================

Le déploiement à chaud est possible via la commande 'gulp serve'.
Si un fichier est modifié pendant que le serve est lancé, les modifications seront automatiquement mises à jour.
Le backend ciblé peut être spécifié en ajoutant un fichier local.json (Voir local.json.sample) et en modifiant la propriété target.

Karma et Tests unitaires
========================

Les tests unitaires se lances vià les commandes:
 - 'gulp test' : Lance un serveur gulp + les tests karma (Unitaires) et Protractor (e2e)
 - 'gulp testKarma' : Lance les tests unitaires seules (Nécéssite un serveur lancé)
 - 'gulp testProtractor' : Lance les tests end to end seuls (Nécéssite un serveur lancé)

Modèle MVC
==========

Le front va petit à petit être migré vers une architecture reprenant le modèle MVC:
 - Une couche Modèle, récupérant les données depuis l'API vitam (Server d'app + dossier resources Angular)
 - Une couche Service, traitant les primise des resources ou proposant des méthodes utilitaires
 - Une couche Vues, proposant l'affichage d'une page avec son controller associé
 
Au final l'arbo type des fichiers devrait être la suivante:
/webapp
   => Fichiers de conf globaux du projet (bower/npm/jshint/index.html/...)
   /core
      => Fichiers de config globaux (core.module.js, main.controller.js, app.config.js, app.module.js, ...)
      /static => Fichiers de traductions front (Key=value pour les champs statiques de l'IHM)
      /services => Services utilitaires partagés de l'application (Faire des modules pour chaque services si externalisables)
      /directives => Directives utilitaires partagés de l'application (Faire des modules pour chaque directives si externalisables)
      /filters => Filtres utilitaires partagés de l'application ?
   /resources
      accession-register.resource.js => Une méthode par endpoint du server d'app (Search / GetAll / GetDetails / ...)
      archive-unit.resource.js => Une méthode par endpoint du server d'app (Search / GetArchive / GetDetails / ...)
      ...
   /services
      accession-register.service.js => Une ou plusieurs méthodes par méthode de la resource fund-register
      archive-unit.service.js =>  Une ou plusieurs méthodes par méthode de la resource archive-unit
      ...
   /pages (Nom à valider)
      /accession-register-detail => Controller + Template de la page Détails de Registre de Fonds
      /archive-unit => Controller + Template de la page archive-unit
      ...
   /styles
      /css
      /img
      /fonts => A migrer dans le /css ?

Internationalisation
====================

Cette partie est gérrée par le module angular-translate

Pour ajouter une traduction, ajouter une clé valeur dans le fichier static/languages_<lang>.json
L'entrée être formatée de la manière suivante "<pageGroup>.<pageDetail>.<bloc>.<key>"="<value>"
où :
 - <pageGroup> est le groupe de page ou du module dans l'application (Exemple archiveSearch ou administration)
 - <pageDetail> est le nom de page dans le groupe (Exemple managementRules ou archiveUnitDetails)
 - <bloc> est le nom du bloc dans la page (Exemple searchForm ou technicalMetadata)
 - <key> est le nom de la clé (Exemple 'id' ou 'evDetData')
 
 Si possible essayez de regrouper les clés définies par groupe/detail/bloc/ordre alphabetique pour s'y retrouver.
 
 
 Pour utiliser une traduction, utilisez dans une instruction angular de votre HTML le filtre translate:
.. code-block:: html

	<div>{{'archiveSearch.searchForm.id' | translate}}</div>
 
 Si votre key est dynamique et présente dans une variable, il est possible d'inserer du js en plus de la chaine:
 .. code-block:: html
 
 	<div>{{'archive.archiveUnitDetails.technicalMetadata.' + metadata[$index] | translate}}</div>
 
 Enfin il est également possible de faire le traitement de traduction en js en appliquant le filtre:
 NB: $filter doit avoir été injecté
 
.. code-block:: javascript
	
	var translatedLabel = $filter('translate')('archiveSearch.searchForm.id');
 
// TODO : Rendre dynamique la langue choisi pour les traductions (actuellement static FR)
// TODO : Utiliser la langue de fallback fr (ou autre ?)
// TODO : Une grosse partie des constantes (js) et des String statiques (html) devraient être mises dans ces fichiers
// TODO : Récupérer la liste des valeurs du référentiel VITAM (Build / Appel API)