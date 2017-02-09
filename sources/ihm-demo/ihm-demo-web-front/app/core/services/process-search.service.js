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
angular.module('core')
  .service('processSearchService', function($timeout) {
    // TODO Change me in a directive that handle display of error by an unique way
    // TODO Change me in a directive that handle display of result array by an unique way
    var ProcessSearchService = {};

    var processSearch = function(searchParams) {
      if (!searchParams.isInitalized) {
        console.error('Params not initialized: ', searchParams.isInitalized, searchParams);
        return;
      }

      var handleSuccessCallback = function(response) {
        searchParams.clearResults();
        searchParams.errorScope.displayMessage = false;
        if (!searchParams.successCallback(response)) {
          errorCallback();
        }
      };

      var errorCallback = function(errorStructure) {
        // reinit scope values ?
        searchParams.clearResults();
        searchParams.errorScope.displayMessage = false;

        // TODO Do some general job before compute (Like return no connection / 404 or else for other problems ?)
        if (!!errorStructure && errorStructure.searchProcessError === true) {
          searchParams.errorScope.message = errorStructure.message;
        } else {
          searchParams.errorScope.message = searchParams.computeErrorMessage();
        }

        searchParams.errorScope.displayMessage = true;
        $timeout(function() {
          searchParams.errorScope.displayMessage = false;
        }, searchParams.displayMessageTime);
      };

      var options = searchParams.preProcessParams;
      if (searchParams.callbackPreProcess != null) {
        options = searchParams.callbackPreProcess(searchParams.preProcessParams);
      }

      if (!!options && options.searchProcessError === true) {
        errorCallback(options);
      } else if (!!options && !options.searchProcessSkip) {
        searchParams.searchFunction(options).then(handleSuccessCallback, errorCallback);
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
     * @param {Object} errorScope An object present in the controller scope that handle error information. This object will be updated by errorCallback.
     * @param {Object} errorScope.message Will contains the error message if an error occures.
     * @param {Object} errorScope.displayMessage Will contains true/false if the error message should be displayed
     * @param {Function} clearResults a callback function call any times the searchFunction is called.
     * @param {Boolean} [isAutoSearch] true if the searchFunction must be call at the initialization of the service (ie: and of this function).
     *  Default: false
     * @param {Object} [preProcessParams] Some custom values for callbackPreProcess parameters.
     * @param {Integer} [displayMessageTime] Override the timer of message display (in ms).
     *  Default: 5000
     * @returns {Function} The function that call the processSearch with initialized parameters.
     * If any preProcessParams update is needed, it can be given as first parameter of that function
     */
    ProcessSearchService.initAndServe = function(searchFunction, callbackPreProcess, successCallback, computeErrorMessage, errorScope, clearResults, isAutoSearch, preProcessParams, displayMessageTime) {
      var params = {};
      params.preProcessParams = preProcessParams;
      params.displayMessageTime = 5000;
      params.isInitalized = true;
      params.callbackPreProcess = null;
      params.successCallback = angular.noop;
      params.computeErrorMessage = null;
      params.clearResults = null;
      params.errorScope = null;
      params.searchFunction = null;

      var error = false;
      var autoSearch = false;

      if (!!displayMessageTime) {
        params.displayMessageTime = displayMessageTime;
      } else {
        console.info('No displayMessageTime provided, default to: ', params.displayMessageTime);
      }
      if (!!isAutoSearch) {
        autoSearch = isAutoSearch;
      } else {
        console.info('No autoSearch provided, default to: ', autoSearch);
      }
      if (!!callbackPreProcess) {
        params.callbackPreProcess = callbackPreProcess;
      } else {
        console.log('No pre process provided: ', callbackPreProcess);
      }
      if (!!successCallback) {
        params.successCallback = successCallback;
      } else {
        console.error('No success process provided: ', successCallback);
        error = true;
      }
      if (!!computeErrorMessage) {
        params.computeErrorMessage = computeErrorMessage;
      } else {
        console.error('No error message provided: ', computeErrorMessage);
        error = true;
      }
      if (!!clearResults) {
        params.clearResults = clearResults;
      } else {
        console.error('No clear results provided: ', clearResults);
        error = true;
      }
      if (!!errorScope) {
        params.errorScope = errorScope;
      } else {
        console.error('No error scope provided: ', errorScope);
        error = true;
      }
      if(!!searchFunction) {
        params.searchFunction = searchFunction;
      } else {
        console.error('No search function provided: ', searchFunction);
        error = true;
      }

      if (error) {
        params.isInitalized = false;
      }

      if (autoSearch) {
        processSearch(params);
      }

      return function(preProcessParams) {
        params.preProcessParams = preProcessParams;
        processSearch(params);
      };
    };

    return ProcessSearchService;
  });