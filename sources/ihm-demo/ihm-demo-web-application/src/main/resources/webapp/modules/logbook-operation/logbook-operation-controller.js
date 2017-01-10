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
  .controller('logbookOperationController', function($scope, $mdDialog, $filter, $window, ihmDemoCLient,
																										 ITEM_PER_PAGE, $timeout, loadStaticValues,$translate){
    var ctrl = this;
    ctrl.client = ihmDemoCLient.getClient('logbook');
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 0;
    ctrl.searchOptions = {};
    ctrl.operationList = [];
    ctrl.resultPages = 0;

		function initFields(fields) {
			var result = [];
			var replaceQuoteFn = $filter('replaceDoubleQuote'); 
			for (var i = 0, len = fields.length; i<len; i++) {
				var fieldId = fields[i];
				result.push({
				 id: fieldId, label: replaceQuoteFn($translate.instant('operation.logbook.displayField.' + fieldId))
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

    function clearResults() {
      ctrl.operationList = [];
    }

    function displayError(message) {
      ctrl.fileNotFoundError = true;
      ctrl.errorMessage = message;
      ctrl.timer = $timeout(function() {
        ctrl.fileNotFoundError = false;
      }, 5000);
    }

    ctrl.getList = function(){
      clearResults();
      ctrl.fileNotFoundError = false;

      ctrl.searchOptions.EventType = ctrl.searchType;

      if(ctrl.searchOptions.EventType == "" || ctrl.searchOptions.EventType == undefined) {
        ctrl.searchOptions.EventType = "all";
      }

      ctrl.searchOptions.EventID = ctrl.searchID;

      if(ctrl.searchOptions.EventID == "" || ctrl.searchOptions.EventID == undefined) {
        ctrl.searchOptions.EventID = "all";
      }

      ctrl.searchOptions.orderby = "evDateTime";

      ctrl.client.all('operations').post(ctrl.searchOptions).then(function(response) {
        if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
          if (ctrl.searchType && ctrl.searchID) {
            displayError("Veuillez ne remplir qu'un seul champ");
          } else {
            displayError("Il n'y a aucun résultat pour votre recherche");
          }
          return;
        }
        ctrl.operationList = response.data.$results;
        ctrl.resultPages = Math.ceil(ctrl.operationList.length/ctrl.itemsPerPage);
        ctrl.currentPage = 1;
      }, function(response) {
        ctrl.searchOptions = {};
        ctrl.resultPages = 0;
        ctrl.currentPage = 0;
        if (ctrl.searchType && ctrl.searchID) {
          displayError("Veuillez ne remplir qu'un seul champ");
        } else {
          displayError("Il n'y a aucun résultat pour votre recherche");
        }

      });
    };

		ctrl.goToDetails = function(id) {
			$window.open('#!/admin/detailOperation/' + id)
		};

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
    }
  });


