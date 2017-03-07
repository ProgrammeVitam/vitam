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
      when('/uploadperf', {
        template: '<upload-sip-perf></upload-sip-perf>',
        title: 'Téléchargement - Tests de perfs'
      }).
      when('/adminHome', {
        template: '<admin-home></admin-home>',
        title: 'Administration des collections'
      }).
      when('/soapUi', {
        template: '<soap-ui></soap-ui>',
        title: 'Tests SOAP-UI'
      }).
      when('/operationTraceability', {
          template: '<operation-traceability></operation-traceability>',
          title: 'Génération journal des opérations sécurisé'
      }).
      when('/searchOperation', {
          template: '<search-operation></search-operation>',
          title: 'Recherche d\'un journal sécurisé'
      }).
      when('/applicativeTest', {
        template: '<functional-test></functional-test>',
        title: 'Tests fonctionnels'
      }).
      when('/applicativeTest/:reportName', {
        templateUrl: 'pages/functional-test/functional-tests-details.template.html',
        controller: 'FunctionalTestDetailsController',
        title: 'Détail des tests fonctionnels'
      }).
      when('/searchOperation/detailOperation/:entryId', {
          templateUrl: 'pages/search-operation/detailOperation.html',
          controller: 'DetailOperationController',
          title: 'Détail du journal sécurisé'
      })
      .otherwise('/adminHome');
    }
  ])
  .config(function($translateProvider, $mdThemingProvider) {
      $translateProvider.useSanitizeValueStrategy('sanitizeParameters');
      $translateProvider.useLoader('MessagesResource', {});
      // prefered language set options for useLoader
      $translateProvider.preferredLanguage('fr');
      console.log($mdThemingProvider);
      $mdThemingProvider.theme('success-toast');
    }
  );
