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

// Define controller for workflows
angular.module('workflows')
.constant('WORKFLOWS_MODULE_CONST', {
  'COMPLETED' : 'Terminé',
  'FAILED' : 'Terminé en erreur',
  'PAUSE' : 'En attente',
  'CANCELLED' : 'Annulé',
  'RUNNING' : 'En cours d\'exécution',
  'STARTED' : 'En cours',
  'UNKNOWN' : 'Inconnu',
  'INIT' : 'Initialisation',
  '200' : 'OK',
  '500' : 'FATAL',
  '404' : 'KO',
  '400' : 'KO',
  '206' : 'WARNING',
  'RESUME' : 'Continu',
  'NEXT' : 'Pas à pas'
})
  .controller('workflowsController', function($scope, ihmDemoFactory, $mdDialog, WORKFLOWS_MODULE_CONST, ITEM_PER_PAGE,
                                              $filter, processSearchService, resultStartService) {
    $scope.startFormat = resultStartService.startFormat;
    $scope.operations = [];
    $scope.search = {
      form: {
            orderby: {
              field: 'processDate',
              sortType: 'ASC'
            }
      },
      pagination: {
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
    }
    $scope.orderByField = 'processDate';
    $scope.reverseSort = false;

    $scope.getTranslatedText = function(key){
      return WORKFLOWS_MODULE_CONST[key];
    }


    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);

      requestOptions.RULES = "all";
      if (requestOptions.RuleType) {
        requestOptions.RuleType = requestOptions.RuleType.toString();
      }
      return requestOptions;
    };

    var customPost = function(criteria, headers) {
      return ihmDemoFactory.getWorkflows();
    };
    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);
      return requestOptions;
    };
    var successCallback = function(response) {
      $scope.search.response.data = response.data.$results[0].$results[0].$results[0];
      $scope.search.response.totalResult = $scope.search.response.data.length;
      $scope.search.pagination.resultPages = Math.ceil($scope.search.response.totalResult / $scope.search.pagination.itemsPerPage);
      if ($scope.search.pagination.resultPages > 0) {
        $scope.search.pagination.currentPage = Math.floor($scope.search.pagination.startOffset / $scope.search.pagination.itemsPerPage) + 1;
      } else {
        $scope.search.pagination.currentPage = 0;
      }
      return true;
    };
    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var searchService = processSearchService.initAndServe(customPost, preSearch, successCallback, computeErrorMessage, $scope.search, true, null, null, null, true);
    $scope.getList = searchService.processSearch;

    //******************************* Display an alert ******************************* //
    $scope.showAlert = function($event, dialogTitle, message) {
      $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
        .clickOutsideToClose(true)
        .title(dialogTitle)
        .textContent(message)
        .ariaLabel('Alert Dialog Demo')
        .ok('OK')
        .targetEvent($event)
      );
    };
    // **************************************************************************** //

    function downloadATR(response){
      var a = document.createElement("a");
      document.body.appendChild(a);
      var url = URL.createObjectURL(new Blob([response.data], { type: 'application/xml' }));
      a.href = url;

      if(response.headers('content-disposition')!== undefined && response.headers('content-disposition')!== null){
        a.download = response.headers('content-disposition').split('filename=')[1];
        a.click();
      }
    }

    $scope.executeAction = function($event, operationId, action, $index){
      $scope.$index = $index;
      var a = _.find( $scope.search.response.data, function(o) { return o.operation_id === operationId;  });
      a.inProgress = true;
      ihmDemoFactory.executeAction(a.operation_id, action)
      .then(function(response) {
          // Update operation status
          // Download ATR eventually
          a.stepStatus = response.status;
          a.globalStatus = response.headers('x-global-execution-status');
          a.inProgress = false;

          $scope.getList();

          // IF Completed process download ATR
          downloadATR(response);
      }, function (error) {
          // Display error message
          console.log(error);
          if(error.status === 401){
            a.inProgress = false;
            $scope.showAlert($event, "Erreur", "Action non autorisée");
          } else {
            a.stepStatus = error.status;
            a.globalStatus = error.headers('x-global-execution-status');
            a.inProgress = false;
            downloadATR(error);
          }
      });
    };

    $scope.stopProcess = function($event, operationId, $index){
      $scope.$index = $index;
      ihmDemoFactory.stopProcess(operationId)
      .then(function(response) {
          // Update operation status
          $scope.search.response.data[$index].stepStatus = response.status;
          $scope.search.response.data[$index].globalStatus = response.headers('x-global-execution-status');
      },
        function (error) {
          // Display error message
          console.log(error);
          if(error.status === 401){
            $scope.showAlert($event, "Erreur", "Action non autorisée");
          } else{
            $scope.showAlert($event, "Erreur", "Une erreur est survenue lors de cette action.");
          }
      });
    };

    $scope.orderByFunction = function(operation) {
      switch ($scope.orderByField) {
        case 'stepStatus':
          if ($scope.getTranslatedText(operation.stepStatus) !== undefined) {
            return $scope.getTranslatedText(operation.stepStatus);
          }
          return operation.stepStatus;
        case 'executionMode':
          if ($scope.getTranslatedText(operation.executionMode) !== undefined) {
            return $scope.getTranslatedText(operation.executionMode);
          }
          return operation.executionMode;
        case 'nextStep':
          if ($filter('translate')(operation.nextStep) !== undefined) {
            return $filter('translate')(operation.nextStep);
          }
          return operation.nextStep;
        case 'previousStep':
          if ($filter('translate')(operation.previousStep) !== undefined) {
            return $filter('translate')(operation.previousStep);
          }
          return operation.previousStep;
        case 'processType':
          return operation.processType;
        default:
          return $scope.orderByField;
      }
    };

  });