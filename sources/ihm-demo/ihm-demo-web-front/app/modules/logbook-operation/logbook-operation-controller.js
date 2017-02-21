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

    $scope.search = {
      form: {
        EventID: '',
        EventType: defaultSearchType
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

    $scope.dynamicTable = {
      customFields: [],
      selectedObjects: []
    };

    $scope.startFormat = function(){
      var start="";

      if($scope.search.pagination.currentPage > 0 && $scope.search.pagination.currentPage <= $scope.search.pagination.resultPages){
        start= ($scope.search.pagination.currentPage-1)*$scope.search.pagination.itemsPerPage;
      }

      if($scope.search.pagination.currentPage>$scope.search.pagination.resultPages){
        start= ($scope.search.pagination.resultPages-1)*$scope.search.pagination.itemsPerPage;
      }
      return start;
    };

    $scope.goToDetails = function(id) {
      $window.open('#!/admin/detailOperation/' + id)
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
        $scope.dynamicTable.customFields = initFields(config.logbookOperationCustomFields);
      }, function onError(error) {

      });

    var clearResults = function() {
      $scope.search.pagination.resultPages = 0;
      $scope.search.pagination.currentPage = 0;
      $scope.search.response.totalResults = 0;
      $scope.search.response.data = [];
    };

    var preSearch = function() {
      var requestOptions = angular.copy($scope.search.form);
      clearResults();

      if (requestOptions.EventType === defaultSearchType || requestOptions.EventType == undefined) {
        requestOptions.EventType = "all";
      }

      if (requestOptions.EventID == "" || requestOptions.EventID == undefined) {
        requestOptions.EventID = "all";
      }

      requestOptions.orderby = "evDateTime";
      return requestOptions;
    };

    var successCallback = function(response) {
      if (!response.data.$hits || !response.data.$hits.total || response.data.$hits.total == 0) {
        return false;
      }
      $scope.search.response.data = response.data.$results;
      $scope.search.pagination.resultPages = Math.ceil($scope.search.response.data.length/$scope.search.pagination.itemsPerPage);
      $scope.search.response.totalResults = response.data.$hits.total;
      $scope.search.pagination.currentPage = 1;
      return true;
    };

    var computeErrorMessage = function() {
      if ($scope.search.form.EventType !== defaultSearchType && $scope.search.form.EventID) {
        return 'Veuillez ne remplir qu\'un seul champ';
      } else {
        return 'Il n\'y a aucun résultat pour votre recherche';
      }
    };

    var searchService = processSearchService.initAndServe(ihmDemoCLient.getClient('logbook').all('operations').post, preSearch, successCallback, computeErrorMessage, $scope.search, clearResults, true);
    $scope.getList = searchService.processSearch;
    $scope.reinitForm = searchService.processReinit;

  });


