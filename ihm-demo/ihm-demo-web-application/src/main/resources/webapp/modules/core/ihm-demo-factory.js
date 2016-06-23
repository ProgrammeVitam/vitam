angular.module('core')
  .constant('IHM_BASE_URL', '/vitam/ihm-demo/api')
  /*ihmDemoCLient create a configured http client*/
     .factory('ihmDemoFactory', ['$http','IHM_BASE_URL', function($http, IHM_BASE_URL) {

    var dataFactory = {};

    dataFactory.searchArchiveUnits = function (criteria) {
        return $http.post(IHM_BASE_URL + '/archivesearch/units', criteria);
    };

    return dataFactory;
}])

  /*ihmDemoCLient create a configured restangular client*/
  .factory('ihmDemoCLient', function(Restangular, IHM_BASE_URL) {
    var getClient = function(uri) {
      return Restangular.withConfig(function(RestangularConfigurer) {
        RestangularConfigurer.setBaseUrl(IHM_BASE_URL + '/' + uri);
        RestangularConfigurer.setFullResponse(true);
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

  });
