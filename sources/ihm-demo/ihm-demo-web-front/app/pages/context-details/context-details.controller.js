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
    .controller('contextsDetailsController', function ($scope, $routeParams, contextResource, $mdDialog, authVitamService) {
      var id = $routeParams.id;

      $scope.editMode = false;
      $scope.changeEditMode = function() {
        $scope.editMode = !$scope.editMode;

        if ($scope.editMode == false) {
          $scope.tmpVars = angular.copy($scope.contract);
          $scope.tmpVars.isActive = $scope.contract.Status === 'ACTIVE';
          getAvalableTenants();
        }
      };

      var getAvalableTenants = function() {
        $scope.avalableTenants = angular.copy($scope.tenants);
        if ($scope.context.Permissions) {
          for (var i = 0; i < $scope.context.Permissions.length; i++) {
            var tenantToRemove = $scope.context.Permissions[i]._tenant;
            $scope.avalableTenants.splice($scope.avalableTenants.indexOf(tenantToRemove), 1);
          }
        }
      };
      var getDetails = function (id) {
          contextResource.getDetails(id, function (response) {
            if (response.data.length !== 0) {
              $scope.context = response.data.$results[0];
              $scope.tmpVars = angular.copy($scope.context);
              getAvalableTenants();
              updateContextStatus();
            }
          });
        };

      var updateContextStatus = function() {
        $scope.contextStatus = $scope.tmpVars.Status ? 'Actif' : 'Inactif';
      };

      $scope.updateStatus = function() {
        updateContextStatus();
      };

      $scope.addTenant = function() {
        if (!$scope.tmpVars.Permissions) {
          $scope.tmpVars.Permissions = [];
        }
        var newTenant = parseInt($scope.selectedTenant);
        $scope.tmpVars.Permissions.push({
          _tenant : newTenant,
          AccessContracts : [],
          IngestContracts: []
        });
        $scope.avalableTenants.splice($scope.avalableTenants.indexOf(newTenant), 1);
      };

      $scope.removeTenant = function(tenantId) {
        if (!$scope.tmpVars.Permissions) {
          $scope.tmpVars.Permissions = [];
        }
        $scope.tmpVars.Permissions.forEach(function(value, index) {
          if (tenantId == value._tenant) {
            $scope.tmpVars.Permissions.splice(index, 1);
          }
        });
        $scope.avalableTenants.push(tenantId);
        $scope.avalableTenants.sort();
      };

      $scope.changeEditMode = function() {
        $scope.editMode = !$scope.editMode;
        if ($scope.editMode == false) {
          $scope.tmpVars = angular.copy($scope.context);
          updateContextStatus();
        }
      };
      var displayMessage = function(message, closeMessage) {
        if (!closeMessage) {
          closeMessage = 'Fermer';
        }
        $mdDialog.show($mdDialog.alert()
          .title(message)
          .ok(closeMessage));
      };
      $scope.saveModifs = function() {
        $scope.editMode = false;
        if (angular.equals($scope.context, $scope.tmpVars)) {
          displayMessage('Aucune modification effectuée');
          return;
        }

        var updateData = {
          LastUpdate: new Date()
        };

        for (var key in $scope.context) {
          console.log($scope.tmpVars[key]);
          if (!angular.equals($scope.context[key], $scope.tmpVars[key])) {
            var updateValue = $scope.tmpVars[key];
            if (key.toLowerCase().indexOf('date') >= 0) {
              updateValue = new Date(updateValue);
            }
            updateData[key] = updateValue;
          }
        }

        contextResource.update(id, updateData).then(function(response) {
          if (response.data.httpCode >= 300) {
            displayMessage('Erreur de modification. Aucune modification effectuée');
          } else {
            displayMessage('La modification a bien été enregistrée');
          }
          getDetails(id);
        }, function() {
          displayMessage('Aucune modification effectuée');
        });
      };
      getDetails(id);

    });