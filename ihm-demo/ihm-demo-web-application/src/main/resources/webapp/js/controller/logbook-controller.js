'use strict';

angular.module('ihm.demo')
  .constant("VITAM_URL", "https://int.env.programmevitam.fr/ihm-demo/servlet/upload")
  .filter('startFrom', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .controller('logbookController', function($scope, VITAM_URL, $mdDialog, ihmDemoCLient) {
    var ctrl = this;
    ctrl.currentPage = 0;
    ctrl.startDate = new Date();
    ctrl.endDate = new Date();
    ctrl.searchOptions = {};
    ctrl.logbookResults = [];
    ctrl.client = ihmDemoCLient.getClient('logbook');

    ctrl.getMockResult = function() {
      ctrl.logbookResults = [];
      ctrl.searchOptions.INGEST = "all";
      ctrl.client.all('operations').post(ctrl.searchOptions).then(function(response) {
        ctrl.logbookResults = response.data.result;
        ctrl.logbookResults.map(function(item) {
          item.obIdIn = ctrl.searchOptions.obIdIn;
        });
        ctrl.resultPages = Math.ceil(ctrl.logbookResults.length/10);
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
  .controller('logbookEntryController', function($scope, VITAM_URL, $mdDialog, operationId, ihmDemoCLient, idOperationService) {
    var self = this;

    ihmDemoCLient.getClient('logbook/operations').all(operationId).post({}).then(function(response) {
      self.detail = response.data.result;
      self.detailId = idOperationService.getIdFromResult(self.detail);
    });

    self.close = function() {
      $mdDialog.cancel();
    };
  });