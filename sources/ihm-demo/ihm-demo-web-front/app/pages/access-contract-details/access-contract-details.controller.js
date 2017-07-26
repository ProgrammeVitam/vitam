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

angular.module('ihm.demo')
    .controller('accessContractsDetailsController', function ($scope, $routeParams, accessContractResource, $mdDialog, authVitamService, $filter) {
        var id = $routeParams.id;
        var ACCESS_CONTRACTS_UPDATE_PERMISSION = 'accesscontracts:update';

    var self = this;

    self.isEditMode = true;

        $scope.tmpVars = {};
        $scope.updateStatus = function() {
            $scope.tmpVars.Status = $scope.tmpVars.isActive ? 'ACTIVE': 'INACTIVE';
        };


    $scope.updateWritePermission = function() {
      $scope.contract.Status = $scope.tmpVars.isActive? 'ACTIVE': 'INACTIVE';
    };

        var displayMessage = function(message, closeMessage) {
            if (!closeMessage) {
                closeMessage = 'Fermer';
            }
            $mdDialog.show($mdDialog.alert()
                .title(message)
                .ok(closeMessage));
        };

        var getDetails = function (id) {
            accessContractResource.getDetails(id, function (response) {
                if (response.data.length !== 0) {
                    $scope.contract = response.data.$results[0];
                    if (!$scope.contract.DataObjectVersion) {
                      $scope.contract.DataObjectVersion = [];
                    }
                    if (!$scope.contract.OriginatingAgencies) {
                      $scope.contract.OriginatingAgencies = [];
                    }
                    $scope.tmpVars = angular.copy($scope.contract);
                    $scope.tmpVars.isActive = $scope.contract.Status === 'ACTIVE';
                }
            });
        };
    $scope.editMode = false;
    $scope.changeEditMode = function() {
      $scope.editMode = !$scope.editMode;

      if ($scope.editMode == false) {
        $scope.tmpVars = angular.copy($scope.contract);
        $scope.tmpVars.isActive = $scope.contract.Status === 'ACTIVE';
      }
    };
        $scope.saveModifs = function() {

          $scope.editMode = false;
          if (angular.equals($scope.contract, $scope.tmpVars)) {
              displayMessage('Aucune modification effectuée');
              return;
          }

          var updateData = {
              LastUpdate: new Date()
          };

          for (var key in $scope.contract) {
            if (!angular.equals($scope.contract[key], $scope.tmpVars[key])) {
              var updateValue = $scope.tmpVars[key];
              if (key.toLowerCase().indexOf('date') >= 0) {
                updateValue = new Date(updateValue);
              }
              updateData[key] = updateValue;
            }
          }

          accessContractResource.update(id, updateData).then(function(response) {
            if (response.data.httpCode >= 300) {
              displayMessage('Erreur de modification. Aucune modification effectuée');
            } else {
              displayMessage('La modification a bien été enregistrée');
            }
            $scope.tmpVars.oldStatus = $scope.contract.Status;
            getDetails(id);
          }, function() {
              displayMessage('Aucune modification effectuée');
          });
        };
        
        getDetails(id);

        $scope.checkPermission = function() {
          return !authVitamService.hasPermission(ACCESS_CONTRACTS_UPDATE_PERMISSION);
        }

});


