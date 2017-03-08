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

describe('searchPaginationController', function() {
  beforeEach(module('ihm.demo'));

  var SearchPaginationController, limitNbPages, itemPerPage, scope;

  beforeEach(inject(function($rootScope, $controller, LIMIT_NB_PAGES, ITEM_PER_PAGE) {

    scope = $rootScope.$new(false, null);

    scope.paginationScope = {
      currentPage: 0,
      itemsPerPage: 3,
      resultPages: 10
    };
    scope.searchFunction = function() {};

    SearchPaginationController = function() {

      limitNbPages = LIMIT_NB_PAGES;
      itemPerPage = ITEM_PER_PAGE;
      return $controller('searchPaginationController', {
        $scope: scope,
        LIMIT_NB_PAGES: limitNbPages,
        ITEM_PER_PAGE: itemPerPage
      });
    };
  }));

  it('should init startOffset and limit', function() {
    SearchPaginationController();

    expect(scope.paginationScope.startOffset).toBe(0);
    expect(scope.paginationScope.limit).toBe(limitNbPages*itemPerPage);
  });

  it('should not call searchFunction if pages are loaded', function(done) {
    scope.searchFunction = function() {
      fail();
    };

    SearchPaginationController();

    scope.getPage(1);
    scope.getPage(limitNbPages);

    setTimeout(function() {
      done();
    }, 1000);
  });

  it('should call searchFunction if pages aren\'t loaded', function() {
    var countSearchFunction = 0;
    scope.searchFunction = function() {
      countSearchFunction ++;

      if (countSearchFunction === 3) {
        fail();
      } else if (countSearchFunction === 2) {
        setTimeout(function() {
          done();
        }, 1000);
      }
    };

    SearchPaginationController();

    // Should call searchFunction
    scope.getPage(limitNbPages+1);
    // Should not call searchFunction
    scope.getPage(limitNbPages+2);
    // Should call searchFunction
    scope.getPage(1);
  });

  it('should handle errors on user inputs if asked page is < 1 or > nbPages', function() {
    SearchPaginationController();

    scope.getPage(-1);
    expect(scope.paginationScope.currentPage).toBe(1);

    scope.getPage(scope.paginationScope.resultPages+1);
    expect(scope.paginationScope.currentPage).toBe(scope.paginationScope.resultPages);
  });

});
