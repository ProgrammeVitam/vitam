'use strict';
angular
    .module('ihm.demo')
    .factory('HttpRequestErrorInterceptor', function($rootScope, $q) {
        return {
            responseError: function(error) {
                // Consider only Server errors
                if (500 <= error.status && error.status < 600) {
                    var message = {
                        requestServerError : true,
                        requestServerTitle : 'Erreur Serveur (' + error.status + ')',
                        requestServerRequestId : ''+error.headers('x-request-id'),
                        requestServerURL : error.config.url
                    };
                    $rootScope.$emit('httpRequestError', message);
                }

                return $q.reject(error);
            }
        };

    });


