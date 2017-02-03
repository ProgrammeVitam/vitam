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
  .filter('startFileRules', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .controller('filerulesController',  function($scope, $mdDialog, ihmDemoCLient, ITEM_PER_PAGE, processSearchService) {
    var ctrl = this;
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 0;
    ctrl.searchOptions = {};
    ctrl.fileRulesList = [];
    ctrl.client = ihmDemoCLient.getClient('admin');
    ctrl.fileNotFoundError = false;

    ctrl.deleteRuleValue = function () {
      delete ctrl.searchOptions.RuleValue;
      ctrl.getFileRules();
    };

    ctrl.startFormat = function(){
      var start="";

      if(ctrl.currentPage > 0 && ctrl.currentPage <= ctrl.resultPages){
        start= (ctrl.currentPage-1)*ctrl.itemsPerPage;
      }

      if(ctrl.currentPage>ctrl.resultPages){
        start= (ctrl.resultPages-1)*ctrl.itemsPerPage;
      }
      return start;
    };

    ctrl.openDialog = function($event, id) {
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

    var clearResults = function() {
      ctrl.fileRulesList = [];
      ctrl.currentPage = "";
      ctrl.resultPages = "";
      ctrl.results = 0;
    };

    var preSearch = function() {
      ctrl.searchOptions.RULES = "all";
      ctrl.searchOptions.orderby = "RuleValue";
      if( ctrl.RuleType)
      {
        ctrl.searchOptions.RuleType = ctrl.RuleType.toString();
      }
      return ctrl.searchOptions;
    };

    var successCallback = function(response) {
      if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
        return false;
      }
      ctrl.fileRulesList = response.data.$results.sort(function (a, b) {
        return a.RuleValue.toLowerCase().localeCompare(b.RuleValue.toLowerCase());
      });
      ctrl.resultPages = Math.ceil(ctrl.fileRulesList.length/ITEM_PER_PAGE);
      ctrl.currentPage = 1;
      ctrl.results = response.data.$hits.total;
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    $scope.error = {
      message: '',
      displayMessage: false
    };

    ctrl.getFileRules = processSearchService.initAndServe(ihmDemoCLient.getClient('admin').all('rules').post, preSearch, successCallback, computeErrorMessage, $scope.error, clearResults, true);

    ctrl.clearSearchOptions = function() {
      ctrl.searchOptions = {};
      ctrl.getFileRules();
    };
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
