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
    'IHM_DEFAULT_URL':'/adminHome',
    'IHM_BASE_URL':'/ihm-recette/v1/api'
  })

  /*ihmDemoCLient create a configured restangular client*/
  .factory('ihmDemoCLient', function(Restangular, IHM_URLS) {
    var getClient = function(uri) {
      return Restangular.withConfig(function(RestangularConfigurer) {
        RestangularConfigurer.setBaseUrl(IHM_URLS.IHM_BASE_URL + '/' + uri);
        RestangularConfigurer.setFullResponse(true);
      });
    };

    return {
      getClient: getClient
    };

  })

  .factory('authVitamService', function($cookies, ihmDemoCLient) {

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

    function login() {
      return ihmDemoCLient.getClient('').all('login').post();
    }

    function logout() {
      deleteCookie('userCredentials');
      deleteCookie('role');
      return ihmDemoCLient.getClient('').one('logout').post();
    }

    return {
      url: lastUrl,
      cookieValue: cookieValue,
      deleteCookie: deleteCookie,
      createCookie: createCookie,
      isConnect: isConnect,
      login: login,
      logout: logout
    };
  })
  .factory('redirectInterceptor', function($q, $location, $cookies) {
    return  {
      'response':function(response){
        if (typeof response.data === 'string' && response.data.indexOf("PROGRAMME VITAM")>-1) {
          $location.path('/login');
          $cookies.remove('userCredentials');
          $cookies.remove('role');
          return $q.reject(response);
        }else{
          return response;
        }
      }
    }

  });

