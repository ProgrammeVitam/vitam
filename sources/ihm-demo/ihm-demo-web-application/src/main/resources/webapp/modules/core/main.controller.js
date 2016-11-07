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
  .controller('mainViewController', function($rootScope, $scope, $location, $translate, IHM_URLS, authVitamService,
                                             $window, Restangular, subject, usernamePasswordToken) {
    $scope.showMenuBar = true;
    $scope.credentials = usernamePasswordToken;
    $scope.session = {};

    $rootScope.$on('$routeChangeSuccess', function(event, next, current) {
      $scope.session.status = authVitamService.isConnect('userCredentials');
      if ($scope.session.status != 'logged') {
        $location.path('/login');
      } else if ($location.path() == '/login') {
        $location.path(IHM_URLS.IHM_DEFAULT_URL);
      }

    });

    $rootScope.$on('$routeChangeStart', function(event, next, current) {
      if ($location.path() != '/login') {
        authVitamService.url = $location.path();
      }
    });

    $scope.connectUser = function() {
      subject.login($scope.credentials)
        .then(
        function(res) {
          authVitamService.createCookie('role', res.role);
          authVitamService.createCookie('userCredentials', btoa(''+':'+''));
          $scope.session.status = 'logged';
          $scope.logginError = false;
          if (authVitamService.url && authVitamService.url != '') {
            $location.path(authVitamService.url);
            delete authVitamService.url;
          } else {
            $location.path(IHM_URLS.IHM_DEFAULT_URL);
            $translate.refresh();
          }
        },
        function(err) {
          $scope.logginError = true;
          $scope.sessionExpire(err);
        });
    };

    $scope.logoutUser = function() {
      subject.logout();
      $scope.session.status = 'notlogged';
      delete authVitamService.url;
      authVitamService.logout().then(function(res) {
        $location.path('/login');
      }, function(err) {
        $scope.sessionExpire(err);
        $location.path('/login');
      });
    };

    $scope.sessionExpire = function(err){
      if(err.status === 410){
        $scope.session.status = 'notlogged';
        $scope.statusLogin = 'Session Expire';
        authVitamService.logout();
      }
    };
  });
