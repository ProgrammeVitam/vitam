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
  .controller('fileformatController',  function($scope, $mdDialog, ihmDemoCLient, ITEM_PER_PAGE, processSearchService, resultStartService) {
    $scope.startFormat = resultStartService.startFormat;

    $scope.search = {
      form: {
        FormatName: '',
        PUID: '',
        orderby: {
          field: 'Name',
          sortType: 'ASC'
        }
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

    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);
      requestOptions.FORMAT = "all";
      return requestOptions;
    };

    var successCallback = function() {
      $scope.search.response.data = $scope.search.response.data.sort(function (a, b) {
        return a.Name.trim().toLowerCase().localeCompare(b.Name.trim().toLowerCase());
      });
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var customPost = function(criteria, headers) {
      return ihmDemoCLient.getClient('admin').all('formats').customPOST(criteria, null, null, headers);
    };

    var searchService = processSearchService.initAndServe(customPost, preSearch, successCallback, computeErrorMessage, $scope.search, true);
    $scope.getFileFormats = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;
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
