/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital
 * archiving back-office system managing high volumetry securely and
 * efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL 2.1 license
 * as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */

// Define controller for search operation
angular
  .module('traceability.operation.search')
  .controller(
    'searchTraceabilityOperationController',
    function($scope, $timeout, $window, lodash, traceabilityOperationSearchService,
      ihmDemoCLient, downloadTraceabilityOperationService, authVitamService) {

      $scope.logType = '--';
      $scope.ctrl = {
        itemsPerPage: 50,
        currentPage: 0,
        searchOptions: {},
        operationList: [],
        resultPages: 0,
        downloadOptions: {}
      };

      function displayError(message) {
        $scope.ctrl.fileNotFoundError = true;
        $scope.ctrl.errorMessage = message;
      }

      $scope.ctrl.goToDetails = function(id) {
        $window.open('#!/admin/traceabilityOperationDetail/' + id)
      };

      $scope.ctrl.getList = function() {
        $scope.ctrl.operationList = [];
        $scope.ctrl.searchOptions = {};
        $scope.ctrl.fileNotFoundError = false;

        if (!!$scope.searchId) {
          // Don't include other criterias if id is given
          $scope.ctrl.searchOptions.TraceabilityId = $scope.searchId;

          if (!!$scope.startDate || !!$scope.endDate || (!!$scope.logType && '--' !== $scope.logType)) {
            displayError("La recherche par ID ignore les autres champs");
          }
        } else {
          if (!!$scope.startDate) {
            $scope.ctrl.searchOptions.TraceabilityStartDate = $scope.startDate;
          }

          if (!!$scope.endDate) {
            // Increment day by one to get the end of the selected day
            var date = new Date($scope.endDate);
            date.setDate(date.getDate() + 1);
            $scope.ctrl.searchOptions.TraceabilityEndDate = date.toISOString().substring(0,10);
          }

          if (!!$scope.logType && '--' !== $scope.logType) {
            $scope.ctrl.searchOptions.TraceabilityLogType = $scope.logType;
          }
        }

        $scope.ctrl.searchOptions.EventType = 'traceability';
        $scope.ctrl.searchOptions.orderby = { field: 'evDateTime',sortType: 'ASC' };
        $scope.ctrl.searchOptions.TraceabilityOk = "true";
        traceabilityOperationSearchService
          .getOperations(
            $scope.ctrl.searchOptions,
            function(response) {
              $scope.ctrl.operationList = response.data.$results;
              if ($scope.ctrl.operationList.length === 0) {
                $scope.ctrl.results = 0;
                displayError("Il n'y a aucun résultat pour votre recherche");
                return;
              }
              $scope.ctrl.resultPages = Math
                .ceil($scope.ctrl.operationList.length
                  / $scope.ctrl.itemsPerPage);
              $scope.ctrl.currentPage = 1;
              $scope.ctrl.results = response.data.$hits.total;
              for (var i = 0, len = $scope.ctrl.operationList.length; i < len; i++) {
                var operation = $scope.ctrl.operationList[i];
                if (operation.evDetData !=null) {
                	$scope.ctrl.operationList[i].events[operation.events.length-1].evDetData =
                        JSON.parse(operation.evDetData);
                } else {
                $scope.ctrl.operationList[i].events[operation.events.length-1].evDetData =
                  JSON.parse($scope.ctrl.operationList[i].events[operation.events.length-1].evDetData);
                }
              }
            },
            function() {
              $scope.ctrl.searchOptions = {};
              $scope.ctrl.resultPages = 0;
              $scope.ctrl.currentPage = 0;
              $scope.ctrl.results = 0;
              if ($scope.ctrl.searchDate) {
                displayError("Veuillez choisir une date");
              } else {
                displayError("Il n'y a aucun résultat pour votre recherche");
              }

            });
      };

      //************************* Download Operation ******************** //

      var successDownloadTraceabilityFile = function(response) {
        var a = document.createElement("a");
        document.body.appendChild(a);
        var url = URL.createObjectURL(new Blob([response.data], { type: 'application/octet-stream', responseType: 'arraybuffer'}));
        a.href = url;

        if(response.headers('content-disposition')!== undefined && response.headers('content-disposition')!== null){
          a.download = response.headers('content-disposition').split('filename=')[1];
          a.click();
        }
      };

      $scope.ctrl.downloadOperation = function(objectId) {
        downloadTraceabilityOperationService.getLogbook(objectId, successDownloadTraceabilityFile);
      };

      $scope.ctrl.reinitTab = function() {
        delete $scope.searchId;
        delete $scope.startDate;
        delete $scope.endDate;
        $scope.logType = '--';
        $scope.ctrl.getList();
      };

      $scope.ctrl.hasPermission = authVitamService.hasPermission;

    }).constant('lodash', window._);
