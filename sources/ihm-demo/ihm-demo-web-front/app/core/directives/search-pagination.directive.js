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

/*
 Used to handle pagination in search pages

 It takes some mandatory parameters:

 */

'use strict';

angular.module('ihm.demo')
  .constant('ITEM_PER_PAGE', 25)
  .constant('LIMIT_NB_PAGES', 5)
  .controller('searchPaginationController', function($scope, LIMIT_NB_PAGES, ITEM_PER_PAGE) {

    if (!$scope.pageNumber) {
      $scope.paginationScope.startOffset = 0;
      $scope.pageNumber = 0;
    }

    if (!$scope.paginationScope.limit) {
      $scope.paginationScope.limit = LIMIT_NB_PAGES * ITEM_PER_PAGE;
    }

    /**
     * get a page to be displayed in results array
     *
     * @param pageNumber the page to be displayed
     */
    $scope.getPage = function(pageNumber) {

      // Check limits (1 - nbPages)
      if ($scope.paginationScope.resultPages != 0 && (pageNumber < 1)) {
        pageNumber = 1;
      }
      if (pageNumber > $scope.paginationScope.resultPages) {
        pageNumber = $scope.paginationScope.resultPages;
        $scope.pageNumber = $scope.paginationScope.resultPages;
      }

      // Get loaded pages (min - max)
      var minPage = $scope.paginationScope.startOffset / $scope.paginationScope.itemsPerPage + 1;
      var maxPage = ($scope.paginationScope.startOffset + $scope.paginationScope.limit) / $scope.paginationScope.itemsPerPage;

      // Check if another load is needed
      if (pageNumber < minPage || maxPage < pageNumber) {
        $scope.paginationScope.startOffset = (pageNumber-1) * $scope.paginationScope.itemsPerPage;
        $scope.searchFunction();
      }

        $scope.paginationScope.currentPage = pageNumber;
    }

  })
  .directive('searchPagination', function()  {
    return {
      require : "ngModel",
      restrict: 'E',
      scope: {
        paginationScope: '=',
        searchFunction: '='
      },
      controller: 'searchPaginationController',
      templateUrl: 'core/directives/search-pagination.directive.html'
    };
  });
