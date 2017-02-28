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

angular.module('ihm.demo')
  .controller('filerulesController',  function($scope, $mdDialog, ihmDemoCLient, ITEM_PER_PAGE, processSearchService, resultStartService) {
    $scope.startFormat = resultStartService.startFormat;

    $scope.search = {
      form: {
        RuleValue: '',
        RuleType: ['All']
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

    $scope.openDialog = function($event, id) {
      $mdDialog.show({
        controller: 'filerulesEntryController as entryRulesCtrl',
        templateUrl: 'views/file-rules-entry.html',
        parent: angular.element(document.body),
        clickOutsideToClose:true,
        targetEvent: $event,
        locals : {
          RuleValue : id
        }

      })
    };

    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);

      requestOptions.RULES = "all";
      requestOptions.orderby = "RuleValue";
      if( requestOptions.RuleType)
      {
        requestOptions.RuleType = requestOptions.RuleType.toString();
      }
      return requestOptions;
    };

    var successCallback = function(response) {
      if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
        return false;
      }
      $scope.search.response.data = response.data.$results.sort(function (a, b) {
        return a.RuleValue.toLowerCase().localeCompare(b.RuleValue.toLowerCase());
      });
      $scope.search.pagination.resultPages = Math.ceil($scope.search.response.data.length/ITEM_PER_PAGE);
      $scope.search.pagination.currentPage = 1;
      $scope.search.response.totalResult = response.data.$hits.total;
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var searchService = processSearchService.initAndServe(ihmDemoCLient.getClient('admin').all('rules').post, preSearch, successCallback, computeErrorMessage, $scope.search, true);
    $scope.getFileRules = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;

  })
  .controller('filerulesEntryController', function($scope, $mdDialog, RuleValue, ihmDemoCLient, idOperationService, $filter) {
    var self = this;

    self.getRulesEntry = function(id) {
      ihmDemoCLient.getClient('admin/rules').all(id).post({}).then(function(response) {
        updateEntry(response.data.$results[0]);
      });
    };

    self.getRulesEntryByRuleValue = function(ruleId) {
      ihmDemoCLient.getClient('admin').all('rules').post({RULES: "all", RuleValue: RuleValue}).then(function(response) {
        updateEntry(response.data.$results[0]);
      });
    };

    function updateEntry(reponse) {
      self.detail = {
        "Intitulé":    reponse.RuleValue       ? reponse.RuleValue.toString() : "",
        "Identifiant": reponse.RuleId          ? reponse.RuleId.toString() : "",
        "Description": reponse.RuleDescription ? reponse.RuleDescription.toString() : "",
        "Durée":       reponse.RuleDuration    ? reponse.RuleDuration.toString() : "",
        "Type":        reponse.RuleType        ? reponse.RuleType.toString() : "",
        "Mesure":      reponse.RuleMeasurement ? reponse.RuleMeasurement.toString() : "",
        "Date de creation":      reponse.CreationDate ? $filter('vitamFormatDate')(reponse.CreationDate.toString()) : "",
        "Date de Modification":      reponse.UpdateDate ? $filter('vitamFormatDate')(reponse.UpdateDate.toString()) : ""
      };
    }

    self.getRulesEntry(RuleValue);
    self.close = function() {
      $mdDialog.cancel();
    };
  });
