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
  .filter('startFormat', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .controller('fileformatController',  function($scope, $mdDialog, ihmDemoCLient, ITEM_PER_PAGE, $timeout) {
    var ctrl = this;
    ctrl.itemsPerPage = ITEM_PER_PAGE;
    ctrl.currentPage = 1;
    ctrl.maxSize = 5;
    ctrl.searchOptions = {};
    ctrl.fileFormatList = [];
    ctrl.client = ihmDemoCLient.getClient('admin');
    ctrl.fileNotFoundError = false;

    /**
     * FIXME : Remove me ? Useless Function ?
     */
    function clearResults() {
      ctrl.fileFormatList = [];
      ctrl.currentPage = "";
      ctrl.resultPages = "";
      ctrl.fileNotFoundError = false;
    }

    function displayError(message) {
      ctrl.fileNotFoundError = true;
      ctrl.errorMessage = message;
      $timeout(function() {
        ctrl.fileNotFoundError = false;
      }, 5000);
    }

    ctrl.getFileFormats = function () {
      clearResults();
      ctrl.searchOptions.FORMAT = "all";
      ctrl.searchOptions.orderby = "Name";
      ctrl.client.all('formats').post(ctrl.searchOptions).then(function(response) {
        if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
          ctrl.results = 0;
          displayError("Il n'y a aucun résultat pour votre recherche");
          return;
        }
        ctrl.fileFormatList = response.data.$results.sort(function (a, b) {
          return a.Name.trim().toLowerCase().localeCompare(b.Name.trim().toLowerCase());
         });
         ctrl.currentPage = 1;
         ctrl.results = response.data.$hits.total;
         $scope.totalItems = ctrl.results;
        }, function(response) {
         displayError("Il n'y a aucun résultat pour votre recherche");
        });
    };

    ctrl.clearSearchOptions = function() {
      ctrl.searchOptions = {};
      clearResults();
      ctrl.client.all('formats').post({FORMAT: "all", orderby: "Name"}).then(function(response) {
        ctrl.fileFormatList = response.data.$results.sort(function (a, b) {
          return a.Name.trim().toLowerCase().localeCompare(b.Name.trim().toLowerCase());
         });
         ctrl.resultPages = Math.ceil(ctrl.fileFormatList.length/ITEM_PER_PAGE);
         ctrl.results = ctrl.fileFormatList.length;
        }, function(response) {
         displayError("Il n'y a aucun résultat pour votre recherche");
        });
    };

    ctrl.openDialog = function($event, id) {
      $mdDialog.show({
        controller: 'fileformatEntryController as entryCtrl',
        templateUrl: 'views/file-format-Entry.html',
        parent: angular.element(document.body),
        clickOutsideToClose:true,
        targetEvent: $event,
        locals : {
          formatId : id
        }

      })
    };

    ctrl.clearSearchOptions();

  })
  .controller('fileformatEntryController', function($scope, $mdDialog, formatId, ihmDemoCLient, idOperationService) {
    var self = this;

    self.getFormatEntry = function(id) {
      ihmDemoCLient.getClient('admin/formats').all(id).post({}).then(function(response) {
        updateEntry(response.data.$results[0]);
      });
    };

    self.getFormatEntryByPUID = function(puid) {
      ihmDemoCLient.getClient('admin').all('formats').post({FORMAT: "all", PUID: puid}).then(function(response) {
        updateEntry(response.data.$results[0]);
      });
    };

    function updateEntry(reponse) {
      self.detail = {
        PUID: reponse.PUID ? reponse.PUID.toString() : "",
        "Nom du fomat": reponse.Name ? reponse.Name.toString() : "",
        "MIME types": reponse.MIMEType ? reponse.MIMEType.toString() : "",
        "Extensions": reponse.Extension ? reponse.Extension.toString() : ""
      };

      self.listPriorityPUID = reponse.HasPriorityOverFileFormatID;
      self.pronomLink = reponse.PUID ? "http://www.nationalarchives.gov.uk/PRONOM/" + reponse.PUID.toString() : "";
    }

    self.getFormatEntry(formatId);
    self.close = function() {
      $mdDialog.cancel();
    };
  });
