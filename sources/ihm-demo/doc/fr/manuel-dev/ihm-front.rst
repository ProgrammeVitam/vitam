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
Grunt + Archi projet (modules, css, ...) => AngularJS
Tests unitaires (Voir partie Karma)
TODO: Comment le front est buildé / integré dans la webapp ?

Grunt et déployement à chaud
============================

A priori les configurations actuelles ne sont pas suffisantes pour lancer un serveur grunt et faire des déployement à chaud sur les postes de dev.
De plus certaines dépendances npm/bower sont manquantes.
TODO A voir par la suite si cela deviens une priorité.

Karma et Tests unitaires
========================

A priori les configurations actuelles ne sont pas suffisantes pour lancer les TU du front.
TODO A voir par la suite si cela deviens une priorité.

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
 
// TODO : Rendre dynamique la langue choisie pour les traductions (actuellement static FR)
// TODO : Utiliser la langue de fallback fr (ou autre ?)
// TODO : Une grosse partie des constantes (js) et des String statiques (html) devraient être mises dans ces fichiers
// TODO : Récupérer la liste des valeurs du référentiel VITAM (Build / Appel API)

Module archive-unit
*******************

Ce module ne comprends pas le module 'archive-unit-search'
Ce module permet le processing et l'affichage des données liées à une Archive Unit.
Les directives utilisées sont:
 - archive-unit-field qui permet d'afficher un champ en prenant en compte le mode édition
 - archive-unit-fieldtree qui permet d'afficher un ensemble de champs en utilisant le directive archive-unit-field avec des paramètres standards pour chaque champ

Si la suite du projet le permet, ces directives peuvent être déplacées hors du module (Ne pas oublier de changer le angular.module('archive.unit') dans ce cas).
Elles pourront alors être plus générique et si besoin prendre plus de paramètres.

Directive archive-unit-field
============================

Cette directive permet d'afficher un champ 'simple' en mode visualisation ou edition.
Un champ 'simple' est un champ qui à simplement une valeur (Texte/nombre) et pas de sous-élément. 

Usages:
Pour utiliser cette directive il suffit d'appeler la balise '<archive-unit-field' en spécifiant les parametres suivants:
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
      		<archive-unit-field field-label="'Service producteur'" field-size="'11'"
      			intercept-user-change="$ctrl.interceptUserChanges(fieldSet)"
      		    field-object="$ctrl.mainFields['OriginatingAgency'].content[0]" edit-mode="$ctrl.isEditMode">
      		</archive-unit-field>
      	</div
      </div>

Directive archive-unit-fieldtree
================================

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
      	    <archive-unit-fieldtree intercept-user-change="$ctrl.interceptUserChanges(fieldSet)"
      	    	field-object="fieldSet" edit-mode="$ctrl.isEditMode">
      	    </archive-unit-fieldtree>
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
 