/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

'use strict';

angular.
module('ihm.demo').
config(['$locationProvider' ,'$routeProvider',
  function config($locationProvider, $routeProvider) {
    $locationProvider.hashPrefix('!');

    $routeProvider.
    when('/login', {
      templateUrl: 'views/login.html'
    }).
    when('/archiveSearch', {
      template: '<archive-search></archive-search>',
      title: 'Recherche d\'archives'
    }).
    when('/admin', {
      templateUrl: "views/admin.html"
    }).
    when('/archiveGestion', {
      templateUrl: "views/in-progress.html"
    }).
    when('/uploadSIP', {
      templateUrl: "views/upload-sip.html",
      controller: "uploadController",
      title: 'Transfert'
    }).
    when('/uploadHoldingScheme', {
      templateUrl: "views/upload-sip.html",
      controller: "uploadController",
      title: 'Transfert de l\'arbre de positionnement'
    }).
    when('/uploadFilingsScheme', {
      templateUrl: "views/upload-sip.html",
      controller: "uploadController",
      title: 'Transfert du plan de classement'
    }).
    when('/uploadSIP2', {
      templateUrl: "views/upload-sip-2.html",
      controller: "uploadController"
    }).
    when('/archiveunit/:archiveId', {
      template: '<archive-unit></archive-unit>',
      title: 'Détail de l\'unité archivistique'
    }).
    when('/admin/logbookOperations', {
      template: '<logbook-operations></logbook-operations>',
      title: 'Suivi des opérations d\'entrées'
    }).
    when('/admin/formats', {
      template: '<file-format></file-format>',
      title: 'Référentiel des formats'
    }).
    when('/admin/rules', {
      template: '<file-rules></file-rules>',
      title: 'Référentiel des Règles de gestion'
    }).
    when('/admin/logbookLifecycle', {
      templateUrl:  "views/in-progress.html"
    }).
    when('/admin/managementrules', {
      templateUrl:  "views/in-progress.html"
    }).
    when('/admin/importPronoun', {
      templateUrl: "views/import-Pronoun.html",
      title: 'Import du Référentiel des formats'
    }).
    when('/admin/importFileRules', {
      templateUrl: "views/import-FileRules.html",
      title: 'Import du Référentiel des Règles de gestion'
    }).
    when('/admin/importContracts', {
        templateUrl: "views/import-contracts.html",
        title: 'Import des contrats d\'entrée'
    }).
    when('/admin/importAccessContracts', {
        templateUrl: "views/import-access-contracts.html",
        title: 'Import des contrats d\'accès'
    }).
    when('/admin/journalOperations', {
      template: '<all-logbook-operation></all-logbook-operation>',
      title: 'Journal des Opérations'
    }).
    when('/admin/detailOperation/:entryId', {
      templateUrl: 'views/logbookEntryFull.html',
      controller: 'OperationLogbookEntryController',
      title: 'Détail d\'une opération'
    }).
    when('/lifecycle/:type/:lifecycleId', {
      template: '<lifecycle></lifecycle>',
      title: 'Journal du cycle de vie '
    }).
    when('/uploadperf', {
      template: '<upload-sip-perf></upload-sip-perf>'
    }).
    when('/accessionRegister/search', {
      template: '<accession-register-search></accession-register-search>',
      title: 'Recherche Registre des Fonds'
    }).
    when('/accessionRegister/detail/:accessionRegisterId', {
      template: '<accession-register-details></accession-register-details>',
      title: 'Détail du Fonds'
    }).
    when('/admin/logbookOperations/:entryId', {
      templateUrl: 'views/logbookEntry.html',
      controller: 'logbookEntryController as entryCtrl',
      title: 'Détail d\'une opération d\'entrée'
    }).
    when('/admin/workflows', {
        template: '<workflows></workflows>',
        title: 'Gestion des versements'
    }).
     when('/admin/entryContracts', {
       templateUrl: 'pages/entry-contract-search/entry-contract.template.html',
       controller: 'entryContractsController',
       title: 'Contrats d\'entrée'
     }).
    when('/admin/entryContracts/:id', {
      templateUrl: 'pages/entry-contract-details/entry-contract-details.template.html',
      controller: 'entryContractsDetailsController',
      title: 'Détail d\'un contrat d\'entrée'
    }).
    when('/admin/accessContracts', {
      templateUrl: 'pages/access-contract-search/access-contract.template.html',
      controller: 'accessContractsController',
      title: 'Contrats d\'accès'
    }).
    when('/admin/accessContracts/:id', {
      templateUrl: 'pages/access-contract-details/access-contract-details.template.html',
      controller: 'accessContractsDetailsController',
      title: 'Détail d\'un contrat d\'accès'
    }).
    when('/admin/traceabilityOperationSearch', {
      template: '<traceability-operation-search></traceability-operation-search>',
      title: 'Opérations de sécurisation'
    }).
    when('/admin/traceabilityOperationDetail/:operationId', {
      template: '<traceability-operation-details></traceability-operation-details>',
      title: 'Vérification d\'une opération de sécurisation'
    }).
    otherwise('/uploadSIP');
  }
])
  .config(function($translateProvider) {
      $translateProvider.useSanitizeValueStrategy('sanitizeParameters');
      $translateProvider.useLoader('MessagesResource', {});
      // prefered language set options for useLoader
      $translateProvider.preferredLanguage('fr');
    }
  )
  .config(function($httpProvider) {
    $httpProvider.interceptors.push('HttpRequestErrorInterceptor');
  })
  .config(['flowFactoryProvider', function (flowFactoryProvider) {
    flowFactoryProvider.defaults = {
      target: '/ihm-demo/v1/api/ingest/upload2',
      permanentErrors: [404, 500, 501, 502],
      maxChunkRetries: 1,
      chunkRetryInterval: 5000,
      simultaneousUploads: 4
    };
    flowFactoryProvider.on('catchAll', function (event) {
      console.log('catchAll', arguments);
    });
    // Can be used with different implementations of Flow.js
    // flowFactoryProvider.factory = fustyFlowFactory;
  }]);
