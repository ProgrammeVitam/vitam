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

angular.module('upload.sip.perf')
 .filter('filterSize', function() {
  return function(bytes, precision) {
    if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
    if (typeof precision === 'undefined') precision = 1;
    var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
      number = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
  }})
  .controller('UploadSipPerfController', function($scope, uploadSipPerfResource){
    $scope.sipList = [];
    $scope.ingestOperationId = '';
    $scope.uploadLaunched = false;
    $scope.uploadFinished = false;
    $scope.uploadFailed = false;
    $scope.sipFound = true;

    $scope.getAvailableSipForUpload = function getAvailableSipForUpload(){
      uploadSipPerfResource.getAvailableSipForUpload()
      .then(function (response) {
        $scope.sipList = response.data;
        if ($scope.sipList.length > 0){
          $scope.sipFound = true;
        } else {
          $scope.sipFound = false;
        }
      }, function (error) {
          console.log('Get files available for upload failed with error : ' + error.message);
          $scope.sipFound = false;
      });
    };

    // Load SIP list
    $scope.getAvailableSipForUpload();

    // Upload SIP
    $scope.uploadSelectedSip = function uploadSelectedSip(fileName) {
      $scope.uploadLaunched = true;
      $scope.uploadFinished = false;
      $scope.uploadFailed = false;
      uploadSipPerfResource.uploadSelectedSip(fileName)
      .then(function (response) {
        $scope.ingestOperationId = response.data;
        $scope.uploadFinished = true;
        $scope.uploadLaunched = false;
        $scope.uploadFailed = false;

        // Generate report
        $scope.generateOperationStatistics($scope.ingestOperationId);
      }, function (error) {
          console.log('Upload failed with error : ' + error.message);
          $scope.uploadFinished = true;
          $scope.uploadLaunched = false;
          $scope.uploadFailed = true;
      });
    };

    // Generate Operation Statistics
    $scope.generateOperationStatistics = function generateOperationStatistics(operationId) {
      uploadSipPerfResource.generateIngestStatReport(operationId)
      .then(function (response) {
        var a = document.createElement("a");
        document.body.appendChild(a);
        var url = URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
        a.href = url;
        a.download = operationId + '.csv';
        a.click();
        setTimeout(function() {
          window.URL.revokeObjectURL(url);
        }, 100);
      }, function (error) {
          console.log('Report generation failed with error : ' + error.message);
      });
    };

  });
