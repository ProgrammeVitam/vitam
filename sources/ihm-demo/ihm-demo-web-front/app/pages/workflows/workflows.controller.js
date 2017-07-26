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
    'COMPLETED': 'Terminé',
    'FAILED': 'Terminé en erreur',
    'PAUSE': 'En attente',
    'CANCELLED': 'Annulé',
    'RUNNING': 'En cours d\'exécution',
    'STARTED': 'En cours',
    'UNKNOWN': 'Inconnu',
    'INIT': 'Initialisation',
    '200': 'OK',
    '500': 'FATAL',
    '404': 'KO',
    '400': 'KO',
    '206': 'WARNING',
    'RESUME': 'Continu',
    'NEXT': 'Pas à pas'
  })
  .controller('workflowsController', function($scope, ihmDemoFactory, $mdDialog, WORKFLOWS_MODULE_CONST, ITEM_PER_PAGE,
                                              $filter, processSearchService, resultStartService, _, responseValidator) {

    $scope.statuses = [{
      id: '', name: 'Tous'
    }, {
      id: 'OK', name: 'Succès'
    }, {
      id: 'WARNING', name: 'Avertissement'
    }, {
      id: 'STARTED', name: 'En cours'
    }, {
      id: 'KO', name: 'Échec'
    }, {
      id: 'FATAL', name: 'Erreur Technique'
    }];

    $scope.states = [{
      id: '', name: 'Tous'
    }, {
      id: 'PAUSE', name: 'Pause'
    }, {
      id: 'RUNNING', name: 'En cours'
    }, {
      id: 'COMPLETED', name: 'Terminé'
    }];

    $scope.workflowCategories = [{
      id: '', name: 'Tous'
    }];

    $scope.workflowSteps = [{
      id: '', name: 'Tous'
    }];

    $scope.startFormat = resultStartService.startFormat;
    $scope.operations = [];
    $scope.search = {
      form: {
        id: '',
        statuses: [$scope.statuses[0]],
        states: [$scope.states[1], $scope.states[2]],
        categories: [$scope.workflowCategories[0]],
        steps: [$scope.workflowSteps[0]],
        startDateMin: '',
        startDateMax: '',
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
    };

    $scope.orderByField = 'processDate';
    $scope.reverseSort = false;

    $scope.getTranslatedText = function(key) {
      return WORKFLOWS_MODULE_CONST[key];
    };

    var customPost = function(criteria, headers) {
      console.log(criteria, headers);
      return ihmDemoFactory.getWorkflows(criteria);
    };

    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);

      console.log(requestOptions);

      delete requestOptions.orderby;

      // Handle id
      if (!!requestOptions.id) {
        return {id: requestOptions.id};
      }
      delete requestOptions.id;

      // Handle workflows
      if (!!requestOptions.categories) {
        requestOptions.workflows = _.map(requestOptions.categories, 'id');
        if (requestOptions.workflows.length === 1 && requestOptions.workflows[0] === '') {
          delete requestOptions.workflows;
        }
      }
      delete requestOptions.categories;

      // Handle steps
      if (!!requestOptions.steps) {
        requestOptions.lastSteps = _.map(requestOptions.steps, 'id');
        if (requestOptions.lastSteps.length === 1 && requestOptions.lastSteps[0] === '') {
          delete requestOptions.lastSteps;
        }
      }
      delete requestOptions.steps;

      // Handle states
      if (!!requestOptions.states) {
        requestOptions.states = _.map(requestOptions.states, 'id');
        if (requestOptions.states.length === 1 && requestOptions.states[0] === '') {
          delete requestOptions.states;
        }
      }

      // Handle statuses
      if (!!requestOptions.statuses) {
        requestOptions.statuses = _.map(requestOptions.statuses, 'id');
        if (requestOptions.statuses.length === 1 && requestOptions.statuses[0] === '') {
          delete requestOptions.statuses;
        }
      }

      // Handle Dates
      if (!requestOptions.startDateMin) {
        delete requestOptions.startDateMin;
      }
      if (!requestOptions.startDateMax) {
        delete requestOptions.startDateMax;
      }

      return requestOptions;
    };

    var successCallback = function(response) {
      if (!responseValidator.validateReceivedResponse(response) || response.data.$hits.total === 0) {
        return false;
      } else {
        $scope.search.response.data = response.data.$results;
        $scope.search.response.totalResult = $scope.search.response.data.length;
        $scope.search.pagination.resultPages =
          Math.ceil($scope.search.response.totalResult / $scope.search.pagination.itemsPerPage);
        if ($scope.search.pagination.resultPages > 0) {
          $scope.search.pagination.currentPage =
            Math.floor($scope.search.pagination.startOffset / $scope.search.pagination.itemsPerPage) + 1;
        } else {
          $scope.search.pagination.currentPage = 0;
        }
        return true;
      }
    };
    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var searchService = processSearchService.initAndServe(customPost, preSearch, successCallback, computeErrorMessage,
      $scope.search, true, null, null, null, true);
    $scope.getList = searchService.processSearch;
    $scope.reset = searchService.processReinit;

    var initWorkflowCategories = function(results) {
      $scope.workflowCategories = [{name: 'Tous', id: ''}];
      for (var workProp in results) {
        if (results.hasOwnProperty(workProp)) {
          var workflow = results[workProp];
          $scope.workflowCategories.push({name: workflow.name, id: workflow.identifier});
          for (var stepProp in workflow.steps) {
            if (workflow.steps.hasOwnProperty(stepProp)) {
              var step = workflow.steps[stepProp];
              $scope.workflowSteps.push({parent: workflow.identifier, name: step.stepName, id: step.stepName});
            }
          }
        }
      }
    };

    $scope.updateSelectableSteps = function(categories) {
      var selectableSteps = [{name: 'Tous', id: ''}];
      var categoriesId = _.map(categories, 'id');
      for (var i = 0, len = $scope.workflowSteps.length; i < len; i++) {
        var step = $scope.workflowSteps[i];
        if (categoriesId[0] !== '' && categoriesId.indexOf(step.parent) === -1) {
          continue;
        }

        if (_.includes(_.map(selectableSteps, 'id'), step.id)) {
          continue;
        }
        selectableSteps.push(step);
      }
      $scope.workflowSelectableSteps = selectableSteps;
      $scope.search.form.steps = [$scope.workflowSelectableSteps[0]];
    };

    var getWorkflowsDefinition = function() {
      ihmDemoFactory.getWorkflowsDefinition().then(
        function onSuccess(result) {
          initWorkflowCategories(result.data.$results[0]);
          $scope.updateSelectableSteps($scope.search.form.categories);
        }
      );
    };

    $scope.search.form.categories = [$scope.workflowCategories[0]];

    getWorkflowsDefinition();
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

    function downloadATR(response) {
      var a = document.createElement("a");
      document.body.appendChild(a);
      var url = URL.createObjectURL(new Blob([response.data], {type: 'application/xml'}));
      a.href = url;

      if (response.headers('content-disposition') !== undefined && response.headers('content-disposition') !== null) {
        a.download = response.headers('content-disposition').split('filename=')[1];
        a.click();
      }
    }

    $scope.executeAction = function($event, operationId, action, $index) {
      $scope.$index = $index;
      var a = _.find($scope.search.response.data, function(o) {
        return o.operation_id === operationId;
      });
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
        }, function(error) {
          // Display error message
          console.log(error);
          if (error.status === 401) {
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

    $scope.stopProcess = function($event, operationId, $index) {
      $scope.$index = $index;
      ihmDemoFactory.stopProcess(operationId)
        .then(function(response) {
          // Update operation status
          $scope.search.response.data[$index].stepStatus = response.status;
          $scope.search.response.data[$index].globalStatus = response.headers('x-global-execution-status');
        },
        function(error) {
          // Display error message
          console.log(error);
          if (error.status === 401) {
            $scope.showAlert($event, "Erreur", "Action non autorisée");
          } else {
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