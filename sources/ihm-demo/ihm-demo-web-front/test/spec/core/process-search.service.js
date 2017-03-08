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

describe('processSearchService', function() {
  beforeEach(module('ihm.demo'));

  var ProcessSearchService, scope, $rootScope, $q, $httpBackend;
  var itemsPerPage, nbPages;
  beforeEach(inject(function ($injector) {
    ProcessSearchService = $injector.get('processSearchService');
    itemsPerPage = $injector.get('ITEM_PER_PAGE');
    nbPages = $injector.get('LIMIT_NB_PAGES');

    $rootScope = $injector.get('$rootScope');
    scope = $rootScope.$new(false, null);
    $q = $injector.get('$q');
    $httpBackend = $injector.get('$httpBackend');

    $httpBackend.whenGET(/\.json$/).respond({'test':'test'});

    scope.search = {
      form: {
        test: ''
      }, pagination: {
        startOffset: 0,
        currentPage: 0,
        resultPages: 0,
        itemsPerPage: itemsPerPage
      }, error: {
        message: '',
        displayMessage: false
      }, response: {
        data: [],
        hints: {},
        totalResult: 0
      }
    };

    scope.state = {};
    jasmine.DEFAULT_TIMEOUT_INTERVAL = 15000;
  }));

  // Mock structure
  var mock = {
    sF: function searchFunction(params) {
      // TODO Should use params in order to throw different result

      var deferred = $q.defer();
      var result;
      if (!!params && params.error) {
        result = 'Error';
        scope.state.error = 'sF';
        deferred.reject(result);
      } else {
        result = {
          data: {
            $hits: {},
            $results: []
          }
        };
        deferred.resolve(result);
      }

      setTimeout(function() {
        $rootScope.$digest();
      }, 500);
      return deferred.promise;
    },
    cPP: function callbackPreProcess(params) {
      if (!!params) {
        if (params.throwError) {
          return {searchProcessError: true, message: 'Error thrown by cPP'};
        }
        if (params.throwSkip) {
          return {searchProcessSkip: true};
        }
        return params;
      }
      // Should return one or most values
      return {};
    },
    sC: function successCallback() {
      // Should return false in almost 1 case
      return true;
    },
    cEM: function computeErrorMessage() {
      if (scope.state.error === 'sF') {
        return 'error in searchFunction';
      }
      return 'Error Message';
    }
  };

  // expect take a function as argument that call the required function
  function callInitWithArgs(sF, cPP, sC, cEM, searchScope, cR, iAS, pPP, dMT) {
    return function() {
      ProcessSearchService.initAndServe(sF, cPP, sC, cEM, searchScope, cR, iAS, pPP, dMT);
    };
  }

  it('should raise error if mandatory parameter is missing', function() {
    // Check missing mandatory parameters
    expect(callInitWithArgs(null, mock.cPP, mock.sC, mock.cEM, scope.search)).toThrow(new Error('Params not initialized -searchFunction'));
    expect(callInitWithArgs(mock.sF, mock.cPP, null, mock.cEM, scope.search)).toThrow(new Error('Params not initialized -successCallback'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, null, scope.search)).toThrow(new Error('Params not initialized -computeErrorMessage'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, null)).toThrow(new Error('Params not initialized -searchScope'));

    // Check all missing mandatory parameters
    expect(callInitWithArgs()).toThrow(new Error('Params not initialized -successCallback-computeErrorMessage-searchScope-searchFunction'));
    expect(callInitWithArgs(null, mock.cPP, null, null, null)).toThrow(new Error('Params not initialized -successCallback-computeErrorMessage-searchScope-searchFunction'));

    // Should not raise exception when mandatory parameters are there
    ProcessSearchService.initAndServe(mock.sF, null, mock.sC, mock.cEM, scope.search);
    ProcessSearchService.initAndServe(mock.sF, null, mock.sC, mock.cEM, scope.search, null, null, null, null);
  });

  it('should raise error if parameters are wrong', function() {
    var wrongParam = 'SomeString';
    expect(callInitWithArgs(wrongParam, mock.cPP, mock.sC, mock.cEM, scope.search)).toThrow(new Error('Params not initialized -searchFunctionType'));
    expect(callInitWithArgs(mock.sF, wrongParam, mock.sC, mock.cEM, scope.search)).toThrow(new Error('Params not initialized -callbackPreProcessType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, wrongParam, mock.cEM, scope.search)).toThrow(new Error('Params not initialized -successCallbackType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, wrongParam, scope.search)).toThrow(new Error('Params not initialized -computeErrorMessageType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, wrongParam)).toThrow(new Error('Params not initialized -searchScopeType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, wrongParam)).toThrow(new Error('Params not initialized -isAutoSearchType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, false, wrongParam)).toThrow(new Error('Params not initialized -preProcessParamsType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, false, {}, wrongParam)).toThrow(new Error('Params not initialized -clearResultsType'));
    expect(callInitWithArgs(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, false, {}, function() {}, wrongParam)).toThrow(new Error('Params not initialized -displayMessageTimeType'));
  });

  it('should do a first call if isAutoSearch is true, else shouldn\'t', function() {
    // setup
    var preProcessParams = {
      throwError: true
    };

    // act 1st case
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, false, preProcessParams);
    // assert
    expect(scope.search.error.displayMessage).toBeFalsy();
    expect(scope.search.error.message).toBe('');

    // act 2nd case
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);
    // assert
    expect(scope.search.error.displayMessage).toBeTruthy();
    expect(scope.search.error.message).toBe('Error thrown by cPP');
  });

  it('should use preProcessParams if any', function() {
    // setup
    var preProcessParams = {isHere: true};
    spyOn(mock, 'sF').and.callThrough();

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);

    // assert
    expect(mock.sF).toHaveBeenCalledWith(preProcessParams, {'X-Limit': nbPages*itemsPerPage, 'X-Offset': 0});
  });

  it('should call the error callback without call search function nor computeErrorMessage', function() {
    // setup
    var preProcessParams = {
      throwError: true
    };
    spyOn(mock, 'sF').and.callThrough();
    spyOn(mock, 'cEM').and.callThrough();

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);

    // assert
    expect(mock.sF).toHaveBeenCalledTimes(0);
    expect(mock.cEM).toHaveBeenCalledTimes(0);
    expect(scope.search.error.displayMessage).toBeTruthy();
    expect(scope.search.error.message).toBe('Error thrown by cPP');
  });

  it('should skip the search function', function() {
    // setup
    var preProcessParams = {
      throwSkip: true
    };
    spyOn(mock, 'sF').and.callThrough();

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);

    // assert
    expect(mock.sF).toHaveBeenCalledTimes(0);
  });

  it('should call the success callback on success', function(done) {
    // setup
    spyOn(mock, 'sC').and.callThrough();

    function doneAfterSuccessCallback(params) {
      var result = mock.sC(params);

      // assert
      expect(mock.sC).toHaveBeenCalledTimes(1);
      done();
      return result;
    }

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, doneAfterSuccessCallback, mock.cEM, scope.search, true);
  });

  it('should update error scope and call compute error message on error', function(done) {
    // setup
    var preProcessParams = {
      error: true
    };
    spyOn(mock, 'cEM').and.callThrough();

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);

    // assert
    scope.$watch('search.error', function(newValue) {
      expect(newValue.message).toBe('error in searchFunction');
      expect(newValue.displayMessage).toBe(true);
      expect(mock.cEM).toHaveBeenCalledTimes(1);
      done();
    });

    scope.$digest();
  });

  it('should display error message for given displayMessageTime ms', function(done) {
    // setup
    var preProcessParams = {
      throwError: true
    };

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams, null, 1000);

    // assert
    scope.$watch('search.error', function() {
      expect(scope.search.error.message).toBe('Error thrown by cPP');
      expect(scope.search.error.displayMessage).toBe(true);

      setTimeout(function() {
        $rootScope.$digest();
        expect(scope.search.error.message).toBe('');
        expect(scope.search.error.displayMessage).toBe(false);
        done();
      }, 2000);
    });

    scope.$digest();
  });

  it('should display error message for infinite time if no value given', function(done) {
    // setup
    var preProcessParams = {
      throwError: true
    };

    // act
    ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search, true, preProcessParams);

    // assert
    scope.$watch('search.error', function() {
      expect(scope.search.error.message).toBe('Error thrown by cPP');
      expect(scope.search.error.displayMessage).toBe(true);

      setTimeout(function() {
        $rootScope.$digest();
        expect(scope.search.error.message).toBe('Error thrown by cPP');
        expect(scope.search.error.displayMessage).toBe(true);
        done();
      }, 6000);
    });

    scope.$digest();
  });

  it('should reset form if reset function is call', function() {
    var searchService = ProcessSearchService.initAndServe(mock.sF, mock.cPP, mock.sC, mock.cEM, scope.search);
    scope.search.form.test = 'Value';
    expect(scope.search.form.test).toBe('Value');

    searchService.processReinit();
    expect(scope.search.form.test).toBe('');
  });

  it('should recall search on reset if autoSeatch is true', function(done) {
    // setup
    var count = 0;
    spyOn(mock, 'sC').and.callThrough();

    function doneAfterSuccessCallback(params) {
      var result = mock.sC(params);

      // assert
      if (count === 0) {
        expect(mock.sC).toHaveBeenCalledTimes(1);
      } else {
        expect(mock.sC).toHaveBeenCalledTimes(2);
        done();
      }
      count++;
      return result;
    }

    // act
    var searchService = ProcessSearchService.initAndServe(mock.sF, mock.cPP, doneAfterSuccessCallback, mock.cEM, scope.search, true);
    searchService.processReinit();
  });

  it('should not do anything if autoSearch = false on onInputChange call', function(done) {
    // setup
    var count = 0;
    spyOn(mock, 'sC').and.callThrough();

    function doneAfterSuccessCallback(params) {
      var result = mock.sC(params);

      // assert
      if (count === 0) {
        // Should be called only once
        expect(mock.sC).toHaveBeenCalledTimes(1);
        setTimeout(function() {
          done();
        }, 2000);
      } else {
        // Should fail if called more that once
        fail();
      }
      count++;
      return result;
    }

    var searchService = ProcessSearchService.initAndServe(mock.sF, mock.cPP, doneAfterSuccessCallback, mock.cEM, scope.search, false);
    scope.search.form.test = 'updated';
    searchService.onInputChange();
    searchService.processSearch();
  });

  it('should recall search onInputChange call', function(done) {
    // setup
    var count = 0;
    spyOn(mock, 'sC').and.callThrough();

    function doneAfterSuccessCallback(params) {
      var result = mock.sC(params);

      // assert
      if (count === 0) {
        // First call, nothing to check
      } else if (count === 1) {
        // Should be called only twice (init call + 2nd onInputChange)
        expect(mock.sC).toHaveBeenCalledTimes(2);
        setTimeout(function() {
          done();
        }, 2000);
      } else {
        // Should fail if called more that twice
        fail();
      }
      count++;
      return result;
    }

    var searchService = ProcessSearchService.initAndServe(mock.sF, mock.cPP, doneAfterSuccessCallback, mock.cEM, scope.search, true);
    scope.search.form.test = 'updated';
    // search should not be called there
    searchService.onInputChange();
    scope.search.form.test = '';
    searchService.onInputChange();
  });

});