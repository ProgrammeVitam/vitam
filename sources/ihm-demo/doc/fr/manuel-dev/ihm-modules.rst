Modules IHM Front
#################

Cette documentation décrit les principaux modules réutilisables de l'IHM front (js)



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

.. sourcecode:: html

    <div class="col-xs-12">
      <div class="form-group col-md-6">
         <display-field field-label="'Service producteur'" field-size="'11'"
            intercept-user-change="$ctrl.interceptUserChanges(fieldSet)"
            field-object="$ctrl.mainFields['OriginatingAgency'].content[0]" edit-mode="$ctrl.isEditMode">
         </display-field>
      </div>
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

.. sourcecode:: html

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

.. sourcecode:: javascript

    var key = fieldSet.fieldId;
    var parent = fieldSet.parent;
    var constants = ARCHIVE_UNIT_MODULE_OG_FIELD_LABEL;
    fieldSet.fieldName = self.displayLabel(key, parent, constants);

Affichage dynamiqueTable
************************

Cette directive permet de dynamiser les tableaux de données pour sélectionner les colonnes à afficher.

- custom-fields: Ce sont les champs dynamiques pour le tableau.

  Ces objets doivent au moins avoir les champs 'id' (Valeur technique et unique) et 'label' (Valeur affichable à l'utilisateur).

 selected-objects: Ce sont les objets sélectionnés à afficher. L'objet en etrée peut être un tableau vide et sera nourri par la directive

Attention, pour des raisons d'ergonomie, il est demandé d'ajouter la classe CSS 'dynamic-table-box' au div 'panel-default' englobant.
Cela permet à ce div de devenir dynamique et de dépasser de la page si plus de colones sont affichés. Ainsi la scrollbar horizontale est accessible directement.

Service de recherche
********************

Le service ProcessSearchService (process-search.service.js) permet de factoriser les actions de recherche et de globaliser son fonctionnement. Tout écran de recherche doit l'utiliser.

Il met à disposition une fonction d'initialisation (initAndServe) du service de recherche qui renvoie 3 functions possibles:

* processSearch - Lance la requête HTTP et traite le comportement d'erreur si besoin (Affichage du message / vider les résultats / ...)
* reinitForm - Efface tout les champs de recherche pour reprendre les valeurs initiales des champs et relance une recherche (si besoin).
* onInputChange - Fonction qui peut être appelée par le contrôleur lors d'une modification d'un champ pour déclancher une réinitialisation de la recherche si le formulaire est revenu à son état initial.

Aussi, en plus des autres paramètres (voir JS doc de la fonction initAndServe), l'initialisation prends en paramètre un objet 'searchScope' qui doit être lié au scope et doit être de la forme suivante:

.. code-block:: javascript

   searchScope = {
      form: {/* Valeurs initiales des champs de recherche (seront donc mises à jour par la vue et par le service) */},
      pagination: { /* Valeurs des variables de pagination */ },
      error: { /* Mise à jour des message d'erreur */ },
      response: { /*  */ }
   }

Ce service permet d'effectuer les actions suivantes de manière uniforme quelque soit le controller qui l'appelle:

* Obliger d'utiliser la chaîne de fonctions fournies (Evite d'avoir une implem differente sur chaque controller)
* Gérer la réinitialisation des messages d'erreur lors du lancement d'une nouvelle recherche (searchScope.error)
* Gérer la réinitialisation du nombre de résultats lors de chaque recherches (searchscope.response)
* Gestion de la recherche automatique à l'initialisation de la page (Ou à la réinitialisation du formulaire)

Par la suite, ce service pourra être complété par des directives (liste non exaustive) pour automatiser l'affichage des informations similaires:

* Messages d'erreur (On peut imaginer une directive à assossier à un formulaire qui affiche les boutons d'effactement multi-champs, le bouton de résultat et le message d'erreur en se basant sur le searchScope.form et searchScope.error)
* Affichage des résultats (On peut imaginer une directive se basant sur searchScope.response déffinissant un pattern pour le tableau de résultat et le titre + Nb résultats).
* Gestion de la pagination (On peut imaginer une directive se basant sur le searchScope.pagination et searchScope.response pour calculer les éléments de pagination).

Service d'affichage des mesures d'un objet physique
****************************************************

Le service uneceMappingService à pour but d'aller chercher les unitées de mesures contenu dans le fichiers unece.json pour l'afficher dans une valeur compréhensible pour les utilisateurs
