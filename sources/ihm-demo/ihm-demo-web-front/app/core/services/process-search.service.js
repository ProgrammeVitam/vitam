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

/**
 * Service that handle all the search pages of IHM-DEMO,
 * The getParams initialization function should be call before all using of this service.
 * The parametters returned by getParams should be given to all function call of this service.
 */

angular.module("core").constant("_", window._);

angular.module('core')
  .service('processSearchService', function(_, responseValidator, ITEM_PER_PAGE, LIMIT_NB_PAGES) {
    // TODO Remove the given searchScope as argument and create an inner scope for directive (Where form should be paste)
    // TODO Change me in a directive that handle display of error by an unique way
    // TODO Change me in a directive that handle display of pagination by an unique way
    // TODO Change me in a directive that handle display of result array by an unique way
    var ProcessSearchService = {};

    function checkParam(params, param, type, varName, mandatory, defaultValue) {
      if (!!param) {
        if (typeof param === type) {
          return param;
        } else {
          params.initError += '-' + varName + 'Type';
          return null;
        }
      } else if (mandatory) {
        params.initError += '-' + varName;
        return null;
      } else {
        return defaultValue;
      }
    }

    function clearResults(searchParams) {
      if (!!searchParams.clearResults) {
        searchParams.clearResults();
      } else {
        searchParams.searchScope.response.data = [];
        searchParams.searchScope.response.totalResult = 0;
        searchParams.searchScope.pagination.currentPage = 0;
        searchParams.searchScope.pagination.resultPages = 0;
      }
    }

    function prepareHeaders(paginationVars) {
      headers = {'X-Limit': ITEM_PER_PAGE*LIMIT_NB_PAGES, 'X-Offset': 0};

      if (!!paginationVars.limit) {
        headers['X-Limit'] = paginationVars.limit;
      }

      if (!!paginationVars.startOffset) {
        headers['X-Offset'] = paginationVars.startOffset;
      }

      return headers;
    }

    var isInError = function(searchParams) {
      if (!searchParams.isInitalized) {
        throw new Error('Params not initialized ' + searchParams.initError);
      }
    };


    var processSearch = function(searchParams) {
      isInError(searchParams);

      var handleSuccessCallback = function(response) {
        var triggerError = false;

        searchParams.searchScope.error.displayMessage = false;
        if (!responseValidator.validateReceivedResponse(response) || response.data.$hits.total === 0) {
          triggerError = true;
        } else {
          searchParams.searchScope.response.data = response.data.$results;
          searchParams.searchScope.response.totalResult = response.data.$hits.total;
          searchParams.searchScope.pagination.resultPages = Math.ceil(searchParams.searchScope.response.totalResult / searchParams.searchScope.pagination.itemsPerPage);
          searchParams.searchScope.pagination.currentPage = Math.floor(searchParams.searchScope.pagination.startOffset / searchParams.searchScope.pagination.itemsPerPage) + 1;
        }

        if (triggerError || !searchParams.successCallback(response)) {
          clearResults(searchParams);
          errorCallback();
        }
      };

      var errorCallback = function(errorStructure) {
        // reinit scope values ?
        clearResults(searchParams);
        searchParams.searchScope.error.displayMessage = false;

        // TODO Do some general job before compute (Like check no connection / 404 or else for other problems ?)
        if (!!errorStructure && errorStructure.searchProcessError === true) {
          searchParams.searchScope.error.message = errorStructure.message;
        } else {
          searchParams.searchScope.error.message = searchParams.computeErrorMessage();
        }
        searchParams.searchScope.error.displayMessage = true;

        if (searchParams.displayMessageTime !== 0) {
          setTimeout(function () {
            searchParams.searchScope.error.displayMessage = false;
            searchParams.searchScope.error.message = '';
          }, searchParams.displayMessageTime);
        }
      };

      var options = searchParams.preProcessParams;
      if (!!searchParams.callbackPreProcess) {
        options = searchParams.callbackPreProcess(searchParams.preProcessParams);
      }

      if (!!options && options.searchProcessError === true) {
        errorCallback(options);
      } else if (!!options && !options.searchProcessSkip) {
        var headers = prepareHeaders(searchParams.searchScope.pagination);
        var promise = searchParams.searchFunction(options, headers);
        promise.then(handleSuccessCallback, errorCallback);
      }
    };

    var reinitForm = function(searchParams) {
      searchParams.searchScope.form = angular.copy(searchParams.initialForm);
      searchParams.searchScope.error.displayMessage = false;
      searchParams.searchScope.error.message = '';
      searchParams.searchScope.pagination.startOffset = 0;
      if (searchParams.autoSearch) {
        processSearch(searchParams);
      } else {
        clearResults(searchParams);
      }
    };

    var checkForm = function(searchParams) {
      if (_.isEqual(searchParams.searchScope.form, searchParams.initialForm)) {
        reinitForm(searchParams);
      }
    };

    /**
     * Initialize the service by checking and prepare the parameters. Do the first call if 'isAutoSearch' is true and return a function to trigger another search.
     *
     * @param {Function} searchFunction The service or resource or http call to get the results
     * @param {Function} callbackPreProcess A function that should be call before the searchFunction.
     *  Should return the parameters called by searchFunction.
     *  Could return a structure with {searchProcessError: true, message: ''} to throw a specific error message without call searchFunction.
     *  Could return a structure with {searchProcessSkip: true} to skip the search without specific error.
     * @param {Function} successCallback The success callback of the searchFunction.
     *  Should return false if any result is wrong and should trigger errorCallback
     *  Should return true if there is no error
     * @param {Function} computeErrorMessage A function that return the error message
     * @param {Object} searchScope An object updated by the search service and that update some data for display
     * @param {Object} searchScope.form [LATER]An object to handle form inputs. Usefull to reset input values or check update on them.
     * @param {Object} searchScope.pagination [LATER]An object to handle pagination values.
     * @param {Number} searchScope.pagination.currentPage [LATER]The current page number
     * @param {Number} searchScope.pagination.resultPages [LATER]The result page
     * @param {Object} searchScope.error An object to handle error values. Fill by the service to display / update / hide error message.
     * @param {String} searchScope.error.message Will contains the error message if an error occures.
     * @param {Boolean} searchScope.error.displayMessage Will contains true/false if the error message should be displayed.
     * @param {Object} searchScope.response [LATER]An object to handle response. Fill by the service to put searchFunction returns values.
     * @param {Array} searchScope.response.data [LATER]Will contains the list of items returned by the searchFunction
     * @param {Object} searchScope.response.hints [LATER]Will contains response structure for ES / number of results / ...
     * @param {Number} searchScope.response.totalResult [LATER]Will contains the total number of result of the search
     * @param {Boolean} [isAutoSearch] true if the searchFunction must be call at the initialization of the service (ie: end of this function).
     *  Default: false
     * @param {Object} [preProcessParams] Some custom values for callbackPreProcess parameters.
     * @param {Function} [clearResults] a callback function to override the clear which is call any times the result may change.
     * @param {Integer} [displayMessageTime] Override the timer of message display (in ms).
     *  Default: 0 (infinite)
     * @returns {Object} A set of function to use service.
     *  processSearch: The function that call the processSearch with initialized parameters
     *  reinitForm: The function that reinit forms (initial value of searchScope.form) and recall (if needed) the initial search
     *  onInputChange: Should be called by the controller when an input is updated. this function check if the form is empty and refresh the initial search if needed.
     * If any preProcessParams update is needed, it can be given as first parameter of that function
     */
    ProcessSearchService.initAndServe = function(searchFunction, callbackPreProcess, successCallback, computeErrorMessage, searchScope, isAutoSearch, preProcessParams, clearResults, displayMessageTime) {
      var params = {};
      params.displayMessageTime = 0;
      params.isInitalized = true;
      params.initError = '';
      params.callbackPreProcess = null;
      params.preProcessParams = null;
      params.successCallback = angular.noop;
      params.autoSearch = false;

      params.autoSearch = checkParam(params, isAutoSearch, 'boolean', 'isAutoSearch', false, params.autoSearch);
      params.callbackPreProcess = checkParam(params, callbackPreProcess, 'function', 'callbackPreProcess', false, params.callbackPreProcess);
      params.clearResults = checkParam(params, clearResults, 'function', 'clearResults', false, null);
      params.displayMessageTime = checkParam(params, displayMessageTime, 'number', 'displayMessageTime', false, params.displayMessageTime);
      params.preProcessParams = checkParam(params, preProcessParams, 'object', 'preProcessParams', false, params.preProcessParams);
      params.successCallback = checkParam(params, successCallback, 'function', 'successCallback', true);
      params.computeErrorMessage = checkParam(params, computeErrorMessage, 'function', 'computeErrorMessage', true);
      params.searchScope = checkParam(params, searchScope, 'object', 'searchScope', true);
      params.searchFunction = checkParam(params, searchFunction, 'function', 'searchFunction', true);
      if (params.initError.indexOf('searchScope') === -1) {
        params.initialForm = checkParam(params, angular.copy(searchScope.form), 'object', 'searchForm', true);
      }

      if (params.initError !== '') {
        params.isInitalized = false;
      }

      isInError(params);

      if (params.autoSearch) {
        processSearch(params);
      }

      return {
        processSearch: function(preProcessParams) {
          params.preProcessParams = preProcessParams;
          processSearch(params);
        },
        processReinit: function() {
          reinitForm(params);
        },
        onInputChange: function() {
          checkForm(params);
        }
      };
    };

    return ProcessSearchService;
  });
