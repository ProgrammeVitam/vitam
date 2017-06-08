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

// Define controller for dsl.query.test
angular.module('dsl.query.test')
  .controller('dslQueryTestController', function($scope, tenantService, $http, dslQueryService, authVitamService) {
    $scope.jsonInput = "";
    $scope.requestResponse = "";
    $scope.tenantId = tenantService.getTenant();

    $scope.displayIdInput = function() {
//      if ($scope.requestMethod === 'GET' && $scope.requestedCollection !== 'profiles') {
      if ($scope.requestMethod === 'GET') {
             return true;
      }
      return false;
    }

    var getContractsSuccessCallback = function (response) {
      $scope.contracts = response.data.$results;
    }
    var getContractsErrorCallback = function (error) {
      console.log("Error while getting contracts.");
    }

    $scope.getContracts = function() {
      dslQueryService.getContracts(getContractsSuccessCallback, getContractsErrorCallback);
    }

    $scope.$watch(function() { return tenantService.getTenant(); }, function(newTenant) {
      $scope.tenantId = newTenant;
      authVitamService.createCookie('tenant', $scope.tenantId);
      $scope.getContracts();
    });

    $scope.isValidJson = function (json) {
        try {
          $scope.jsonInput = JSON.stringify(JSON.parse(json), null, 5);
          $scope.validRequest = "valide";
          return true;
        } catch (e) {
            $scope.validRequest = "non valide";
            $scope.requestResponse = "";
            return false;
        }
    };

    var executeRequestSuccessCallback = function (response) {
      $scope.requestResponse = JSON.stringify(response.data, null, 5);
    }
    var executeRequestErrorCallback = function (error) {
//      $scope.requestResponse = "Error " + error.status + " : " + error.statusText;
      $scope.requestResponse = error.data;
    }

    $scope.getRequestResults = function(query) {
      $scope.isValidJson(query)
      tenantService.setTenant($scope.tenantId);
      authVitamService.createCookie('tenant', $scope.tenantId);
      authVitamService.createCookie('X-Access-Contract-Id', $scope.contractId);

      dslQueryService.executeRequest(executeRequestSuccessCallback, executeRequestErrorCallback,
                                  $scope.tenantId, $scope.contractId, $scope.requestedCollection, $scope.requestMethod,
                                  query, $scope.objectId);
    }

  });
