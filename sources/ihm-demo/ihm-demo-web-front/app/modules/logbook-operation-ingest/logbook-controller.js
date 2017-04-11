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
  .filter('replaceDoubleQuote', function() {
    return function (input) {
      if (!!input) {
        return input.replace(/\'\'/g, '\'');
      }
      return input;
    }
  })
  .controller('logbookController', function($scope, $window, $filter, ihmDemoCLient, ITEM_PER_PAGE,
                                            processSearchService, resultStartService, loadStaticValues) {

    $scope.startFormat = resultStartService.startFormat;

    // Vars for dynamic table
    $scope.selectedObjects = [];
    $scope.customFields = [];
    // $scope.columnsToDisplay = [];

    $scope.search = {
      form: {
        obIdIn: '',
        orderby: {
          field: 'evDateTime',
          sortType: 'DESC'
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

    function initFields(fields) {
      var result = [];
      for (var i = 0, len = fields.length; i < len; i++) {
        var fieldId = fields[i];
        result.push({id: fieldId, label: 'ingest.logbook.displayField.' + fieldId});
      }
      return result;
    }

    loadStaticValues.loadFromFile().then(
      function onSuccess(response) {
        var config = response.data;
        $scope.customFields = initFields(config.logbookIngestCustomFields);
      }, function onError(error) {

      });

    $scope.downloadObject = function (objectId, type) {
      ihmDemoCLient.getClient('ingests').one(objectId).one(type).get().then(function (response) {
        var a = document.createElement("a");
        document.body.appendChild(a);

        var url = URL.createObjectURL(new Blob([response.data], {type: 'application/xml'}));
        a.href = url;

        if (response.headers('content-disposition') !== undefined && response.headers('content-disposition') !== null) {
          a.download = response.headers('content-disposition').split('filename=')[1];
          a.click();
        }
      }, function () {
        $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
          .clickOutsideToClose(true).title('Téléchargement erreur').textContent('Non disponible en téléchargement')
          .ariaLabel('Alert Dialog Demo').ok('OK'));
      });
    };

    // FIXME : Same method than logbook-operation-controller. Put it in generic service in core/services
    $scope.goToDetails = function (id) {
      $window.open('#!/admin/logbookOperations/' + id);
    };

    var preSearch = function () {
      var requestOptions = angular.copy($scope.search.form);

      requestOptions.INGEST = "all";
      if (requestOptions.obIdIn === "") {
        delete requestOptions.obIdIn;
      }
      return requestOptions;
    };

    var successCallback = function (response) {
      $scope.search.response.data.map(function (item) {
        item.obIdIn = $scope.search.form.obIdIn;

        if (!!item.evDetData) {
          if (typeof item.evDetData === 'string') {
            try {
                item.evDetData = JSON.parse(item.evDetData);
            } catch (e) {
              console.error("Parsing error while get evDetData:", e);
            }
          }

          // handle Date Format
          if (!!item.evDetData.EvDateTimeReq) {
            item.evDetData.EvDateTimeReq = $filter('vitamFormatDate')(item.evDetData.EvDateTimeReq);
          }

          // Handle multi lang comment
          var displayableComment = '';
          var count = 0;
          angular.forEach(item.evDetData, function(value, key) {
            if (key.indexOf('EvDetailReq') == 0) {
              count ++;
              // If its the 2nd, the first one should be enter quotes to be array format
              if (count === 2) {
                displayableComment = '"' + displayableComment + '"';
              }

              // If its the 2nd or more, the new value should be enter quotes to be array format
              if (count > 1) {
                value = '"' + value + '"';
              }

              if(key.indexOf('_') == 11) {
                // Its a lang spe comment
                displayableComment += (displayableComment.length===0? '': ', ') + value;
              } else {
                // It's the default comment, put it in first
                displayableComment = value + (displayableComment.length===0? '': ', ') + displayableComment;
              }

            }
          });

          // If multiple comments, put in an array.
          if (count > 1) {
            item.evDetData.EvDetailReq = '[' + displayableComment + ']';
          } else {
            item.evDetData.EvDetailReq = displayableComment;
          }
        }
      });
      return true;
    };

    var computeErrorMessage = function () {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var callCustomPost = function (criteria, headers) {
      return ihmDemoCLient.getClient('logbook').all('operations').customPOST(criteria, null, null, headers);
    };

    var searchService = processSearchService.initAndServe(callCustomPost, preSearch, successCallback, computeErrorMessage, $scope.search, true);
    $scope.getLogbooks = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;

    $scope.getMessageIdentifier = function (logOperation) {
      if (logOperation.events[0].obIdIn === null) {
        return logOperation.events[1].obIdIn;
      }
      return logOperation.events[0].obIdIn;
    };

  });
