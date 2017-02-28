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
  .constant('ITEM_PER_PAGE', 25)
  .constant('MAX_REQUEST_ITEM_NUMBER', 125)
  // FIXME : Same filter than logbook-operation-controller. Do a generic filter in core/filters!
  .filter('startFrom', function() {
    return function (input, start) {
      start = +start; //parse to int
      return input.slice(start);
    }
  })
  .filter('replaceDoubleQuote', function() {
      return function (input) {
        if (!!input) {
          return input.replace(/\'\'/g, '\'');
    	}
        return input;
      }
    })
  .controller('logbookController', function($scope, $window, ihmDemoCLient, ITEM_PER_PAGE, MAX_REQUEST_ITEM_NUMBER, processSearchService, resultStartService) {
    var header = {'X-Limit': MAX_REQUEST_ITEM_NUMBER};

    $scope.startFormat = resultStartService.startFormat;
    $scope.search = {
      form: {
        obIdIn: ''
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

    $scope.downloadObject = function(objectId, type) {
      ihmDemoCLient.getClient('ingests').one(objectId).one(type).get().then(function(response) {
        var a = document.createElement("a");
        document.body.appendChild(a);

        var url = URL.createObjectURL(new Blob([response.data], { type: 'application/xml' }));
        a.href = url;

        if(response.headers('content-disposition')!== undefined && response.headers('content-disposition')!== null) {
          a.download = response.headers('content-disposition').split('filename=')[1];
          a.click();
        }
      }, function() {
        $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
          .clickOutsideToClose(true).title('Téléchargement erreur').textContent('Non disponible en téléchargement')
          .ariaLabel('Alert Dialog Demo').ok('OK'));
      });
    };

    // FIXME : Same method than logbook-operation-controller. Put it in generic service in core/services
    $scope.goToDetails = function(id) {
      $window.open('#!/admin/logbookOperations/' + id);
    };

    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);

      requestOptions.INGEST = "all";
      requestOptions.orderby = "evDateTime";
      if(requestOptions.obIdIn === ""){
        delete requestOptions.obIdIn;
      }
      return [requestOptions, header];
    };

    var successCallback = function(response) {
      if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
        return false;
      }
      $scope.search.response.data = response.data.$results;
      $scope.search.response.data.map(function(item) {
        item.obIdIn = $scope.search.form.obIdIn;
      });
      $scope.search.pagination.currentPage = 1;
      $scope.search.response.totalResult = response.data.$hits.total;
      // FIXME What about X-REQUEST-ID in header ?
      // header['X-REQUEST-ID'] = response.headers('X-REQUEST-ID');
      $scope.search.pagination.resultPages = Math.ceil($scope.search.response.totalResult/$scope.search.pagination.itemsPerPage);
      return true;
    };

    var computeErrorMessage = function() {
      return 'Il n\'y a aucun résultat pour votre recherche';
    };

    var searchFunction = function(result) {
      return ihmDemoCLient.getClient('logbook').all('operations').customPOST(result[0], null, null, result[1]);
    };

    var searchService = processSearchService.initAndServe(searchFunction, preSearch, successCallback, computeErrorMessage, $scope.search, true);
    $scope.getLogbooks = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;
    $scope.onInputChange = searchService.onInputChange;

  });