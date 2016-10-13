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
      template: '<archive-search></archive-search>'
    }).
    when('/admin', {
      templateUrl: "views/admin.html"
    }).
    when('/archiveGestion', {
      templateUrl: "views/in-progress.html"
    }).
    when('/uploadSIP', {
      templateUrl: "views/upload-sip.html",
      controller: "uploadController"
    }).
    when('/archiveunit/:archiveId', {
      template: '<archive-unit></archive-unit>'
    }).
    when('/admin/logbookOperations', {
      template: '<logbook-operations></logbook-operations>'
    }).
    when('/admin/formats', {
      template: '<file-format></file-format>'
    }).
    when('/admin/rules', {
      template: '<file-rules></file-rules>'
    }).
    when('/admin/logbookLifecycle', {
      templateUrl:  "views/in-progress.html"
    }).
    when('/admin/managementrules', {
      templateUrl:  "views/in-progress.html"
    }).
    when('/admin/importPronoun', {
      templateUrl: "views/import-Pronoun.html",
    }).
    when('/admin/importFileRules', {
      templateUrl: "views/import-FileRules.html",
    }).
    when('/admin/recette', {
      templateUrl: "views/recette-features.html",
    }).
    when('/admin/journalOperations', {
      template: '<all-logbook-operation></all-logbook-operation>',
    }).
    when('/lifecycle/:type/:lifecycleId/:lifecycleTitle', {
      template: '<lifecycle></lifecycle>',
    }).
    when('/uploadperf', {
      template: '<upload-sip-perf></upload-sip-perf>'
    }).
    otherwise('/uploadSIP');
  }
])
  .config(function($translateProvider) {
    // TODO Need to update the file loaded when API will throw vitam references
      $translateProvider.useStaticFilesLoader({
        'prefix':'static/languages_',
        'suffix':'.json'
      });
      $translateProvider.preferredLanguage('en');
      $translateProvider.fallbackLanguage('fr');
   }
  );
