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

Module archive-unit
*******************

Ce module ne comprends pas le module 'archive-unit-search'
Ce module permet le processing et l'affichage des données liées à une Archive Unit.
Les directives utilisées sont:
 - display-field qui permet d'afficher un champ en prenant en compte le mode édition
 - display-fieldtree qui permet d'afficher un ensemble de champs en utilisant le directive display-field avec des paramètres standards pour chaque champ

Directive display-field
=======================

Cette directive permet d'afficher un champ 'simple' en mode visualisation ou edition.
Un champ 'simple' est un champ qui à simplement une valeur (Texte/nombre) et pas de sous-élément. 

Usages:
Pour utiliser cette directive il suffit d'appeler la balise '<display-field' en spécifiant les parametres suivants:
- field-label: Surcharge du nom du label
- field-object: L'ensemble des propriétés de l'objet. Doit contenir au moins:
-- isModificationAllowed: vrai si le champ est éditable
-- isFieldEdit: vrai si le champ est en cours d'édition
-- fieldValue: La valeur affichée du champ
- edit-mode: Vrai si le formulaire est en mode édition
- field-size: La valeur du XX dans la classe CSS de bootstrap col-md-XX.
- intercept-user-change: Fonction de callback à appeler lorsque la champ est modifié
	Cette fonction doit prendre un fieldSet en paramètres.

Il est également possible de donner une valeur de surcharge pour la valeur du champ grâce à ce dernier paramètre:
- display-value: Affiche une valeur spécifique à la place de fieldValue (Le mode édition reprends la valeur réelle)

Exemple:
.. code-block:: html

      <div class="col-xs-12">
      	<div class="form-group col-md-6">
      		<display-field field-label="'Service producteur'" field-size="'11'"
      			intercept-user-change="$ctrl.interceptUserChanges(fieldSet)"
      		    field-object="$ctrl.mainFields['OriginatingAgency'].content[0]" edit-mode="$ctrl.isEditMode">
      		</display-field>
      	</div
      </div>

Directive display-fieldtree
===========================

Cette directive permet d'afficher un champ et leurs sous élément si nécessaire de manière récursive.
- field-object: L'ensemble des propriétés de l'objet. Doit contenir au moins:
-- isModificationAllowed: vrai si le champ est éditable
-- isFieldEdit: vrai si le champ est en cours d'édition
-- fieldValue: La valeur affichée du champ
-- typeF: Le type de champ
	'P' correspond à un champs 'parent' avec des sous éléments.
	'S' correspond à un champ simple.
-- content: Tableau de fieldObject contenant les enfants de ce champ.
- edit-mode: Vrai si le formulaire est en mode édition
- intercept-user-change: Fonction de callback à appeler lorsque la champ est modifié.
	Cette fonction doit prendre un fieldSet en paramètres.

Exemple:
.. code-block:: html

      <div class="row archiveDesc panel-collapse collapse in" id="{{'box' + key}}">
      	<div ng-repeat="fieldSet in $ctrl.managmentItems">
      	    <display-fieldtree intercept-user-change="$ctrl.interceptUserChanges(fieldSet)"
      	    	field-object="fieldSet" edit-mode="$ctrl.isEditMode">
      	    </display-fieldtree>
      	</div>
      </div>

Affichage des Libéllés des champs
=================================

La fonction self.displayLabel du controller archive-unit permet de récupérer la valeur française des champs à afficher.
- key: nom technique du champ à afficher 
- parent: nom technique de son parent direct.
	permet de reconstituer la clé parent.key pour les champs 'parent'
- constantes: Nom du fichier de constantes à utiliser.
	Cela permet d'avoir plusieurs _id (par exemple) en fonction du context.
	Les fichiers de constantes sont définis dans archive-unit.constant.js.
	Les clés des constantes équivalent à "key" pour les champs simples et à 'parent.key' pour les champs parent.
- retourne le label si présent dans le fichier de constantes ou la clé (key) sinon.

Exemple:
.. code-block:: javascript

      var key = fieldSet.fieldId;
      var parent = fieldSet.parent;
      var constants = ARCHIVE_UNIT_MODULE_OG_FIELD_LABEL;
      fieldSet.fieldName = self.displayLabel(key, parent, constants);
      
Affichage dynamiqueTable
========================

Cette directive permet de dynamiser les tableaux de données pour sélectionner les colonnes à afficher.
- custom-fields: Ce sont les champs dynamiques pour le tableau.
  Ces objets doivent au moins avoir les champs 'id' (Valeur technique et unique) et 'label' (Valeur affichable à l'utilisateur).
 selected-objects: Ce sont les objets sélectionnés à afficher. L'objet en etrée peut être un tableau vide et sera nourri par la directive
 
Attention, pour des raisons d'ergonomie, il est demandé d'ajouter la classe CSS 'dynamic-table-box' au div 'panel-default' englobant.
Cela permet à ce div de devenir dynamique et de dépasser de la page si plus de colones sont affichés. Ainsi la scrollbar horizontale est accessible directement.
