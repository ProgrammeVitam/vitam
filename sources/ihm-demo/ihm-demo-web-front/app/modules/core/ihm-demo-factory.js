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

angular.module('core')
.constant('IHM_URLS', {
  'IHM_DEFAULT_URL':'/uploadSIP',
  'IHM_BASE_URL':'/ihm-demo/v1/api',
  'ADMIN_ROOT': 'admin',
  'TENANTS': 'tenants',
  'ACCESSION_REGISTER_SEARCH': 'accession-register',
  'ARCHIVE_ROOT': 'archiveunit',
  'ARCHIVE_TREE': 'tree/',
  'ARCHIVE_OBJECT': 'objects',
  'ARCHIVE_OBJECT_GROUP_DOWNLOAD_URL': '/archiveunit/objects/download/',
  'ARCHIVE_UPDATE_ROOT': 'archiveupdate',
  'ARCHIVE_UPDATE_UNITS': 'units/',
  'ARCHIVE_SEARCH_ROOT': 'archivesearch',
  'ARCHIVE_SEARCH_UNITS': 'units',
  'ARCHIVE_SEARCH_UNIT': 'unit',
  'ARCHIVE_DETAILS_CONFIG_FILE': 'modules/config/archive-details.json',
  "ARCHIVE_LIFECYCLE": 'unitlifecycles',
  "OBJECT_GROUP_LIFECYCLE": 'objectgrouplifecycles',
  'UNIT_LIFECYCLE_TYPE': 'unit',
  'OG_LIFECYCLE_TYPE': 'objectgroup',
  'CHECK_OPERATION_STATUS': 'check',
  'CLEAR_OPERATION_STATUS_HISTORY': 'clear'
})

/*ihmDemoCLient create a configured http client*/
.factory('ihmDemoFactory', ['$http','IHM_URLS', 'ihmDemoCLient', 'authVitamService',
  function($http, IHM_URLS, ihmDemoCLient, authVitamService) {

  var dataFactory = {};

  // Get the tenant lists Http Request (GET Method)
  dataFactory.getTenants = function (){
    return ihmDemoCLient.getClient(IHM_URLS.TENANTS).one('').get();
  };

  // Search Archive Units Http Request (POST method)
  dataFactory.searchArchiveUnits = function (criteria) {
    return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_SEARCH_ROOT).all(IHM_URLS.ARCHIVE_SEARCH_UNITS).post(criteria);
  };

  // Search Selected Archive Unit Details Http Request (GET Method)
  dataFactory.getArchiveUnitDetails = function (unitId){
    return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_SEARCH_ROOT).all(IHM_URLS.ARCHIVE_SEARCH_UNIT).get(unitId);
  };

  // Retrieve Archive unit details configuration (JSON file that contains fields to display and the labels used in interface)
  dataFactory.getArchiveUnitDetailsConfig = function(){
	// TODO Move ARCHIVE_DETAILS_CONFIG_FILE into static folder or his content into values.json or laguages_<lang>.json
    return $http.get(IHM_URLS.ARCHIVE_DETAILS_CONFIG_FILE);
  };

  // Save archive unit modifications
  dataFactory.saveArchiveUnit = function(unitId, modifiedFields){
    return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_UPDATE_ROOT).all(IHM_URLS.ARCHIVE_UPDATE_UNITS + unitId).put(modifiedFields);
  };

  //Get Object Group
  dataFactory.getArchiveObjectGroup = function(ogId){
    return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_ROOT).all(IHM_URLS.ARCHIVE_OBJECT).get(ogId);
  };

  //Get Object List
  dataFactory.getObjectAsInputStreamUrl = function(ogId, options){
	  return IHM_URLS.IHM_BASE_URL + IHM_URLS.ARCHIVE_OBJECT_GROUP_DOWNLOAD_URL + ogId +
      '?usage=' + encodeURIComponent(options.usage) +
      '&version=' + encodeURIComponent(options.version) +
      '&filename=' + encodeURIComponent(options.filename) +
      '&tenantId=' + (authVitamService.cookieValue(authVitamService.COOKIE_TENANT_ID) || 0);
  };

  // LifeCycle details
  dataFactory.getLifeCycleDetails = function(lifeCycleType, lifeCycleId) {
    if (IHM_URLS.UNIT_LIFECYCLE_TYPE == lifeCycleType) {
      return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_LIFECYCLE).all('').get(lifeCycleId);
    } else if (IHM_URLS.OG_LIFECYCLE_TYPE == lifeCycleType) {
      return ihmDemoCLient.getClient(IHM_URLS.OBJECT_GROUP_LIFECYCLE).all('').get(lifeCycleId);
    }
  };

  // Archive Tree
  dataFactory.getArchiveTree = function(unitId, allParents){
    return ihmDemoCLient.getClient(IHM_URLS.ARCHIVE_ROOT).all(IHM_URLS.ARCHIVE_TREE + unitId).post(allParents);
  };

  // Default Accession Register Search
  dataFactory.getAccessionRegisters = function(defaultCriteria){
    return ihmDemoCLient.getClient(IHM_URLS.ADMIN_ROOT).all(IHM_URLS.ACCESSION_REGISTER_SEARCH).post(defaultCriteria);
  };

  // Check operation status
  dataFactory.checkOperationStatus = function(operationId){
	// TODO How to set Timeout ?
    return ihmDemoCLient.getClient(IHM_URLS.CHECK_OPERATION_STATUS).all('').get(operationId);
  };

  // Check operation status
  dataFactory.cleanOperationStatus = function(operationId){
    return ihmDemoCLient.getClient(IHM_URLS.CLEAR_OPERATION_STATUS_HISTORY).all('').get(operationId);
  };

  return dataFactory;
}])

/*ihmDemoCLient create a configured restangular client*/
.factory('ihmDemoCLient', function(Restangular, IHM_URLS, $cookies) {
  var getClient = function(uri) {
    return Restangular.withConfig(function(RestangularConfigurer) {
      RestangularConfigurer.setBaseUrl(IHM_URLS.IHM_BASE_URL + '/' + uri);
      RestangularConfigurer.setFullResponse(true);
      function addMandatoryHeader(element, operation, route, url, headers, params, httpConfig) {
          headers['X-Tenant-Id'] = $cookies.get('tenantId');

          return {
            element: element,
            headers: headers,
            params: params,
            httpConfig: httpConfig
          };
        }

      RestangularConfigurer.addFullRequestInterceptor(addMandatoryHeader);
    });
  };

  return {
    getClient: getClient
  };

})

  /*idOperationService to find operation id in result*/
  .factory('idOperationService', function() {
    var getIdFromResult = function(result) {
      var id;
      result.events.forEach(function(element) {
        if (element.obIdIn !== null && element.obIdIn !== '') {
          id = element.obIdIn;
        }
      });
      return id;
    };


    return {
      getIdFromResult: getIdFromResult
    };

  })

  .factory('authVitamService', function($cookies, ihmDemoCLient) {

	var COOKIE_TENANT_ID = 'tenantId';
    var lastUrl = '';

    function createCookie(key, value) {
      $cookies.put(key, value);
    }

    function deleteCookie(key) {
      $cookies.remove(key);
    }

    function isConnect(key) {
      if ($cookies.get(key) === undefined) {
        return false;
      }
      return 'logged';
    }

    function cookieValue(key) {
      return $cookies.get(key);
    }

    function logout() {
      deleteCookie('userCredentials');
      deleteCookie('role');
      deleteCookie(COOKIE_TENANT_ID);
      return ihmDemoCLient.getClient('').one('logout').post();
    }

    return {
      url: lastUrl,
      cookieValue: cookieValue,
      deleteCookie: deleteCookie,
      createCookie: createCookie,
      isConnect: isConnect,
      logout: logout,
      COOKIE_TENANT_ID: COOKIE_TENANT_ID
    };
  })
  .factory('redirectInterceptor', function($q, $location, $cookies) {
    return  {
      'response':function(response){
        if (typeof response.data === 'string' && response.data.indexOf("PROGRAMME VITAM")>-1) {
          $location.path('/login');
          $cookies.remove('userCredentials');
          $cookies.remove('role');
          $cookies.remove('tenantId');
          return $q.reject(response);
        }else{
          return response;
        }
      }
    }

  })
  .factory('transferToIhmResult', function(){
    return {
        transferUnit : function(Result){
         Result.forEach(function(unit) {
            unit._id = unit["#id"];
            delete unit["#id"];
            unit._og = unit["#object"];
            delete unit["#object"];
            unit._ops = unit["#operations"];
            delete unit["#operations"];
            unit._tenant = unit["#tenant"];
            delete unit["#tenant"];
            unit._nbc = unit["#nbunits"];
            delete unit["#nbunits"];
            unit._up = unit["#unitups"];
            delete unit["#unitups"];
            unit._us = unit["#allunitups"];
            delete unit["#allunitups"];
            unit._min = unit["#min"];
            delete unit["#min"];
            unit._max = unit["#max"];
            delete unit["#max"];
	    unit._mgt = unit["#management"];
            delete unit["#management"];
        });
        return Result;
      }
    }
  });