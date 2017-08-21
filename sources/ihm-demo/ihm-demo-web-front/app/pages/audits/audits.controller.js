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

// Define controller for audits
angular.module('audits')
  .controller('auditsController', function ($http, $scope, $mdDialog, $filter, $window, ihmDemoCLient, $cookies,
                                            ihmDemoFactory, ITEM_PER_PAGE, loadStaticValues, $translate, processSearchService) {

    $scope.form = {
      auditType: '',
      objectId: ''
    };

    $scope.sps= [];
    $scope.enableLaunchAudit = false;
    $scope.tenant = $cookies.get('tenantId');

    $scope.launchAudit = function () {
      if ($scope.form.auditType == 'tenant') {
        $scope.form.objectId = $scope.tenant;
      }
      $mdDialog.show($mdDialog.alert()
        .title("Le processus d'audit est lancé")
        .ok("Fermer"));
      ihmDemoCLient.getClient('audits').all('').post($scope.form);
    };


    $scope.search = {
      form: {
        serviceProducerCriteria: '',
        orderby: {
          field: 'OriginatingAgency',
          sortType: 'ASC'
        }
      }, pagination: {
        currentPage: 0,
        resultPages: 0,
        itemsPerPage: ITEM_PER_PAGE
      }, error: {
        message: '',
        displayMessage: false
      }, response: {
        data: [],
        hints: {},
        totalResult: 0
      }
    };

    var preSearch = function() {
      var requestOptions = {
        ACCESSIONREGISTER : 'ACCESSIONREGISTER'
      };
      requestOptions.orderby = $scope.search.form.orderby;
      return requestOptions;
    };

    var successCallback = function(response) {
      $scope.search.response.data = response.data.$results;
      $scope.search.response.data.forEach(function(sp) {
        $scope.sps.push(sp.OriginatingAgency);
      });
      $scope.form.objectId = $scope.sps[0];
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun service producteur';
    };

    var searchService = processSearchService.initAndServe(ihmDemoFactory.getAccessionRegisters, preSearch,
      successCallback, computeErrorMessage, $scope.search, true, null, null, null, true);

    $scope.searchRegistersByCriteria = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;
  });
