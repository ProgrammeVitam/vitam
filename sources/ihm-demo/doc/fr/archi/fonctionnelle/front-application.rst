Architecture fonctionnelle de l'application Front
#################################################

But de cette documentation
==========================
Cette documentation présente l'architecture fonctionnelle de l'application Front du programme VITAM. 

Modules AngularJS déclarés
==========================

Afin d'assurer la modularité et la séparation des différentes fonctionnalités de l'application Front, on a opté pour créer des modules AngularJS par fonctionnalité. Dans la suite les modules spécifiques créés: 

- **ihm.demo** : Module principal
- **core** : Module qui regroupe les factories, services (fonctionnalités partagées entre les controllers)
- **archiveSearch** : Module de recherche d'archives
- **archive.unit** : Module du formulaire d'une archive

Routage
=======
Les routes sont déclarées dans le fichier */modules/parent/app.config.js*:

- **/archiveSearch** : Recherche sur les Archive Units
- **/importPronoun** : Import du référentiel PRONOUN
- **/uploadSIP** : Import de SIP
- **/archiveunit/:archiveId** : Affichage des détails d'une archive unit
- **/admin/logbookOperations** : Journal des opérations
- **/admin/formats** : Recherche sur le référentiel PRONOUN
- **/admin/logbookLifecycle** : en cours de construction
- **/admin/managementrules** : en cours de construction

Factories/Services
==================
On a eu recours à des factories et des services pour assurer certaines fonctionnalités qui nécessitent un passage de données entre les controllers définis.

- **ihm-demo-factory.js** : trois factories ont été déclarées dans ce fichier:
    1. ihmDemoFactory : définit les appels http aux services REST suivants:
        - POST /ihm-demo/v1/api/archivesearch/units
        - GET /ihm-demo/v1/api/archivesearch/unit/id
        - PUT /ihm-demo/v1/api/archiveupdate/units/id
        - GET modules/config/archive-details.json
    2. ihmDemoCLient : crée un client RESTAngular configurable
    3. idOperationService : recherche l'id d'une opération logbook dans une liste de résultat

- **ihm-demo-service.js** : un seul service a été déclaré dans ce fichier:
    - archiveDetailsService : définit la fonction *findArchiveUnitDetails* qui assure la récupération et l'affichage des détails d'une archive unit et qui prend en paramètre :
        1. archiveUnitId : id de l'archive unit à afficher
        2. displayFormCallBack :  fonction callback qui gère l'affichage du détail à la réception du retour de l'appel REST
        3. failureCallback : fonction callback qui gère l'echec de l'appel REST de récupération des détails d'une archive unit

Controllers
===========
- **import-pronoun-controller.js** : le controller "MyController" déclaré dans ce fichier assure la création d'une instance FileUploader (composant qui gère l'import de fichier ) et la définition de ses évènements *onSuccessItem* et *onErrorItem*.
- **upload-sip-controller.js**
- **archive-unit.controller.js** : définit le controller *ArchiveUnitController* qui assure l'affichage récursif des détails d'une Archive Unit et la sauvegarde des données modifiées dans le formulaire.
- **archive-search.controller.js** : définit le controller *ArchiveUnitSearchController* qui prend en charge la recherche par titre (mot exact) sur les archive units et aussi le lancement de l'affichage du formulaire d'une archive unit sélectionnée.
- **main.controller.js** : définit le controller *mainViewController* rattaché à la page principale index.html qui gère l'affichage du menu principal. Ce menu ne doit pas être affiché pour l'écran du formulaire d'une archive unit
- **file-format-controller.js**
- **logbook-controller.js**

Components
==========
Les components ont été introduits à partir de la version 1.5.3 d'AngularJS pour apporter une solution plus simple pour développer des directives. Pour plus d'information, référez-vous à ce lien `Component AngularJS <https://docs.angularjs.org/guide/component>`_.

- **archive-unit.component.js**
- **archive-search.component.js**
- **fileformat-component.js**
- **logbook-component.js**
