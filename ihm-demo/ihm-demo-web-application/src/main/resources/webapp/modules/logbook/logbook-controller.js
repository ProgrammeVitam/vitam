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
  .constant('ITEM_PER_PAGE', 10)
  .filter('startFrom', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .controller('logbookController', function($scope, $mdDialog, ihmDemoCLient, ITEM_PER_PAGE) {
    var ctrl = this;
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 0;
    ctrl.startDate = new Date();
    ctrl.endDate = new Date();
    ctrl.searchOptions = {};
    ctrl.fileFormatList = [];
    ctrl.client = ihmDemoCLient.getClient('logbook');

    ctrl.getFileFormats = function() {
      ctrl.fileFormatList = [];
      ctrl.searchOptions.INGEST = "all";
      ctrl.searchOptions.orderby = "evDateTime";
      ctrl.client.all('operations').post(ctrl.searchOptions).then(function(response) {
        ctrl.fileFormatList = response.data.result;
        ctrl.fileFormatList.map(function(item) {
          item.obIdIn = ctrl.searchOptions.obIdIn;
        });
        ctrl.resultPages = Math.ceil(ctrl.fileFormatList.length/10);
        ctrl.currentPage = 1;
        ctrl.searchOptions = {};
      }, function(response) {
        ctrl.searchOptions = {};
        alert('Request error, code: ' + response.status);
      });
    };

    ctrl.clearSearchOptions = function() {
      ctrl.searchOptions = {};
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

  })
  .controller('logbookEntryController', function($scope, $mdDialog, operationId, ihmDemoCLient, idOperationService) {
    var self = this;

    ihmDemoCLient.getClient('logbook/operations').all(operationId).post({}).then(function(response) {
      self.detail = response.data.result;
      self.detailId = idOperationService.getIdFromResult(self.detail);
    });

    self.close = function() {
      $mdDialog.cancel();
    };
  });
