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
    .controller('mainViewController', function ($rootScope, $scope, $location, $translate, IHM_URLS, authVitamService,
                                                $window, Restangular, subject, usernamePasswordToken, ihmDemoFactory,
                                                $timeout) {
        $scope.showMenuBar = true;
        $scope.credentials = usernamePasswordToken;
        $scope.session = {};
        $scope.tenants = ['0', '1'];
        if (!!authVitamService.cookieValue('userCredentials')) {
          ihmDemoFactory.getAccessContracts({ContractName : "all", Status : "ACTIVE"}).then(function (repsonse) {
            if (repsonse.status == 200 && repsonse.data['$results'] && repsonse.data['$results'].length > 0) {
              $scope.contracts = repsonse.data['$results'];
              authVitamService.setContract($scope.contracts);
            }
          }, function (error) {
            console.log('Error while get contrat. Set default list : ', error);
          });
        }

        if (!!authVitamService.cookieValue("X-Access-Contract-Id")){
            $scope.accessContratId = authVitamService.cookieValue("X-Access-Contract-Id");
        }

        $window.addEventListener('storage', function(event) {
            if (event.key === 'reset-timeout') {
                $rootScope.restartTimeout();
                localStorage.removeItem('reset-timeout');
            }
        });

        if (localStorage.getItem('user')) {
            $rootScope.user = JSON.parse(localStorage.getItem('user'));
        }
        console.log($rootScope.user);
        $scope.hideError = function() {
            $rootScope.requestServerError = false;
        };

        $rootScope.$on('httpRequestError', showHttpError);

        function showHttpError($event, message){
            $scope.requestServerError = message.requestServerError;
            if ($scope.requestServerError) {
                $('#nvlEntree').modal('show');
            }
            $scope.requestServerTitle = message.requestServerTitle;
            $scope.requestServerRequestId = message.requestServerRequestId;
            $scope.requestServerURL = message.requestServerURL;

        }

        ihmDemoFactory.getTenants().then(function (repsonse) {
            if (repsonse.data.length !== 0) {
                $scope.tenants = repsonse.data;
            }
            $scope.tenantId = '' + $scope.tenants[0];
        }, function (error) {
            console.log('Error while get tenant. Set default list : ', error);
            $scope.tenantId = '' + $scope.tenants[0];
        });

        $rootScope.hasPermission = authVitamService.hasPermission;

        $rootScope.$on('$routeChangeSuccess', function (event, next, current) {
            $scope.session.status = authVitamService.isConnect('userCredentials');
            if (!angular.isUndefined(next.$$route) && !angular.isUndefined(next.$$route.title)) {
                $rootScope.title = next.$$route.title;
                //Checks lifecycle type (unit or GOT) to set right page title
                if (!angular.isUndefined(next.params.type) && next.$$route.template.indexOf('lifecycle') > -1) {
                    if (next.params.type === 'unit') {
                        $rootScope.title += 'de l\'unité archivistique';
                    }
                    if (next.params.type === 'objectgroup') {
                        $rootScope.title += 'du groupe d\'objet';
                    }
                }
            } else {
                $rootScope.title = 'VITAM';
            }
            if ($scope.session.status != 'logged') {
                $location.path('/login');
            } else if ($location.path() == '/login') {
                $location.path(authVitamService.hasPermission('ingest:create') ? IHM_URLS.IHM_DEFAULT_URL : IHM_URLS.IHM_DEFAULT_URL_FOR_GUEST);
            } else {
                var permission = next.$$route.permission;
                if (!authVitamService.hasPermission(permission)) {
                    $location.path(authVitamService.hasPermission('ingest:create') ? IHM_URLS.IHM_DEFAULT_URL : IHM_URLS.IHM_DEFAULT_URL_FOR_GUEST);
                }

            }

        });

        $rootScope.$on('$routeChangeStart', function (event, next, current) {
            if ($location.path() != '/login') {
                authVitamService.url = $location.path();
            }
        });

        $scope.changeContract = function(accessContratId) {
          authVitamService.createCookie("X-Access-Contract-Id", accessContratId);
          $window.location.reload();
        };

        $scope.connectUser = function (tenantId) {
            subject.login($scope.credentials)
                .then(
                    function (res) {
                        authVitamService.createCookie('role', res.role);
                        authVitamService.createCookie('userCredentials', btoa('' + ':' + ''));
                        authVitamService.createCookie(authVitamService.COOKIE_TENANT_ID, tenantId || 0);
                        $scope.session.status = 'logged';
                        $scope.logginError = false;

                        var user = {
                            userName: res.userName,
                            tenantId: tenantId,
                            permissions: res.permissions,
                            sessionTimeout: res.sessionTimeout
                        };
                        authVitamService.login(user);

                        $rootScope.user = user;
                        if (authVitamService.url && authVitamService.url != '') {
                            $location.path(authVitamService.url);
                            delete authVitamService.url;
                        } else {
                            $location.path(authVitamService.hasPermission('ingest:create') ? IHM_URLS.IHM_DEFAULT_URL : IHM_URLS.IHM_DEFAULT_URL_FOR_GUEST);
                            $translate.refresh();
                        }

                        ihmDemoFactory.getAccessContracts({ContractName : "all", Status : "ACTIVE"}).then(function (repsonse) {

                            if (repsonse.status == 200 && repsonse.data['$results'] && repsonse.data['$results'].length > 0) {
                                $scope.contracts = repsonse.data['$results'];
                                authVitamService.setContract($scope.contracts);
                                $scope.accessContratId = $scope.contracts[0].Name;
                                authVitamService.createCookie("X-Access-Contract-Id", $scope.accessContratId);
                            }
                        }, function (error) {
                            console.log('Error while get tenant. Set default list : ', error);
                        });
                    },
                    function (err) {
                        $scope.logginError = true;
                        $scope.sessionExpire(err);
                    });
        };

        $scope.logoutUser = function () {
            subject.logout();
            $scope.session.status = 'notlogged';

            delete $rootScope.user;


            delete authVitamService.url;
            $timeout.cancel($rootScope.restartTimeoutPromise);
            authVitamService.logout().then(function (res) {
                $location.path('/login');
            }, function (err) {
                $scope.sessionExpire(err);
                $location.path('/login');
            });
        };

        $scope.sessionExpire = function (err) {
            if (err.status === 410) {
                $scope.session.status = 'notlogged';
                $scope.statusLogin = 'Session Expire';
                authVitamService.logout();
            }
        };

        $rootScope.restartTimeout = function() {
          var timeoutTime = $rootScope.user.sessionTimeout;
          $timeout.cancel($rootScope.restartTimeoutPromise);
          $rootScope.restartTimeoutPromise = $timeout($scope.logoutUser, timeoutTime);
        };
    });
