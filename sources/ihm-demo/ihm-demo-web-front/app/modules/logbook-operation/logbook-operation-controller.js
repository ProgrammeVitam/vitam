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
    .filter('startFrom', function() {
        return function (input, start) {
            start = +start; //parse to int
            return input.slice(start);
        }
    })
  .controller('logbookOperationController', function($scope, $mdDialog, $filter, $window, ihmDemoCLient, ITEM_PER_PAGE, loadStaticValues,$translate, processSearchService){
    var defaultSearchType = "--";
    var ctrl = this;
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 0;
    ctrl.searchOptions = {};
    ctrl.operationList = [];
    ctrl.resultPages = 0;
    ctrl.searchType = defaultSearchType;
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
		function initFields(fields) {
			var result = [];
			for (var i = 0, len = fields.length; i<len; i++) {
				var fieldId = fields[i];
				result.push({
				 id: fieldId, label: 'operation.logbook.displayField.' + fieldId
				 });
				}
			return result;
		}

		loadStaticValues.loadFromFile().then(
			function onSuccess(response) {
				var config = response.data;
				ctrl.customFields = initFields(config.logbookOperationCustomFields);
			}, function onError(error) {

			});

		ctrl.selectedObjects = [];

		ctrl.goToDetails = function(id) {
			$window.open('#!/admin/detailOperation/' + id)
		};

    // FIXME P0: Useless Function ? When and why is it created ?
    ctrl.openDialog = function($event, id) {
      $mdDialog.show({
        controller: 'logbookEntryController as entryCtrl',
        templateUrl: 'views/logbookEntry.html',
        parent: angular.element(document.body),
        clickOutsideToClose:true,
        targetEvent: $event,
        locals : {
          operationId : id
        }
      })
    };

    var clearResults = function() {
      ctrl.searchOptions = {};
      ctrl.resultPages = 0;
      ctrl.currentPage = 0;
      ctrl.results = 0;
      ctrl.operationList = [];
    };

    var preSearch = function() {
      clearResults();
      ctrl.fileNotFoundError = false;

      ctrl.searchOptions.EventType = ctrl.searchType;

      if (ctrl.searchOptions.EventType === defaultSearchType || ctrl.searchOptions.EventType == undefined) {
        ctrl.searchOptions.EventType = "all";
      }

      ctrl.searchOptions.EventID = ctrl.searchID;

      if (ctrl.searchOptions.EventID == "" || ctrl.searchOptions.EventID == undefined) {
        ctrl.searchOptions.EventID = "all";
      }

      ctrl.searchOptions.orderby = "evDateTime";
      return ctrl.searchOptions;
    };

    var successCallback = function(response) {
      if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
        return false;
      }
      ctrl.operationList = response.data.$results;
      ctrl.resultPages = Math.ceil(ctrl.operationList.length/ctrl.itemsPerPage);
      ctrl.results = response.data.$hits.total;
      ctrl.currentPage = 1;
      return true;
    };

    var computeErrorMessage = function() {
      if (ctrl.searchType !== defaultSearchType && ctrl.searchID) {
        return 'Veuillez ne remplir qu\'un seul champ';
      } else {
        return 'Il n\'y a aucun résultat pour votre recherche';
      }
    };

    $scope.error = {
      message: '',
      displayMessage: false
    };

    ctrl.getList = processSearchService.initAndServe(ihmDemoCLient.getClient('logbook').all('operations').post, preSearch, successCallback, computeErrorMessage, $scope.error, clearResults, true);
  });


