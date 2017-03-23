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

// Define controller for soap-ui
angular.module('functional.test')
  .controller('functionalTestController', function($scope/*, $interval*/, $window, functionalTestService) {
    $scope.step  = 'Name ';
    $scope.version  = "";
    $scope.dataVersion = "";
    $scope.error = false;
    $scope.pending = false;
    $scope.finished = false;


    function getSuccess(array) {
      var success = 0;
      for(var i = 0, len = array.length; i < len; i++) {
        var item = array[i];
        if (item.Ok) {
          success++;
        }
      }
      return success;
    }

    var launchSuccessCallback = function(response) {
        if (response.status == 202) {
            $scope.pending = true;
        }
    };

    var launchErrorCallback = function(response) {
      $scope.finished = false;
      $scope.error = true;
    };

    $scope.launchTests = function() {
      $scope.finished = false;
      $scope.pending = false;
      $scope.error = false;
      functionalTestService.launch(launchSuccessCallback, launchErrorCallback);
    };

    $scope.sync = functionalTestService.sync;

    functionalTestService.listReports(function onSuccess(result) {
      $scope.report = {
        results: result.data
      };
    });

    $scope.goToDetails = function(name) {
      $window.open('#!/applicativeTest/' + name)
    };
  });
