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
  .constant("UPLOAD_CONSTANTS", {
    "VITAM_URL": "/ihm-demo/v1/api/ingest/continue",
    "ACCEPTED_STATUS": 206,
    "NO_CONTENT_STATUS": 204,
    "KO_STATUS": 400,
    "FATAL_STATUS": 500
  })
  .controller('uploadController', function($scope, FileUploader, $mdDialog, $route, $cookies, $location, UPLOAD_CONSTANTS,
    $interval, ihmDemoFactory, $http, $timeout) {

    // *************************************** // modal dialog //************************************* //
    $scope.showAlert = function($event, dialogTitle, message) {
      $mdDialog.show($mdDialog.alert().parent(angular.element(document.querySelector('#popupContainer')))
          .clickOutsideToClose(true)
          .title(dialogTitle)
          .textContent(message)
          .ariaLabel('Alert Dialog Demo')
          .ok('OK')
          .targetEvent($event)
      );
    };
    // **************************************************************************** //

    function downloadATR(response, headers){
      var a = document.createElement("a");
      document.body.appendChild(a);

      // item_715
      var url = URL.createObjectURL(new Blob([response.data], { type: 'application/xml' }));
      a.href = url;
      a.download = response.headers('content-disposition').split('filename=')[1];
      a.click();
    }


    // *************************************** TIMER *********************************************** //
    function clearHistoryAfterUpload(operationIdServerAppLevel) {
      ihmDemoFactory.cleanOperationStatus(operationIdServerAppLevel)
      .then(function (response) {
        console.log("clean succeeded");
      }, function(error) {
        console.log("clean failed");
      });
    };

    $scope.stopPromise = undefined;
    $scope.check = function(fileItem, operationIdServerAppLevel) {
       if ( angular.isDefined($scope.stopPromise) ) return;

       $scope.stopPromise = $interval(function() {
         // Every 100ms check if the operation is finished
         ihmDemoFactory.checkOperationStatus(operationIdServerAppLevel)
         .then(function (response) {
           if(response.status === UPLOAD_CONSTANTS.FATAL_STATUS) {
             // stop check
             $scope.stopCheck();
             fileItem.isProcessing = false;
             fileItem.isSuccess = false;
             fileItem.isWarning = false;
             fileItem.isError = true;

             clearHistoryAfterUpload(operationIdServerAppLevel);
             $scope.disableSelect = false;
           } else if (response.status !== UPLOAD_CONSTANTS.NO_CONTENT_STATUS) {
             // Finished operation
             var receivedOperationId = response.headers('X-REQUEST-ID');
             $scope.stopCheck();

             $scope.ingestOperationId = receivedOperationId;
             $scope.uploadFinished = true;
             $scope.uploadLaunched = false;
             $scope.uploadFailed = false;
             fileItem.isProcessing = false;

             if(response.status === UPLOAD_CONSTANTS.ACCEPTED_STATUS){
               fileItem.isWarning = true;
             } else {
               fileItem.isWarning = false;
             }
             fileItem.isSuccess = !fileItem.isWarning;
             downloadATR(response, response.headers);
             clearHistoryAfterUpload(operationIdServerAppLevel);
             $scope.disableSelect = false;
           }
         }, function (error) {
             console.log('Upload failed with error : ' + error.status);

             if(error.status !== -1){
               $scope.stopCheck();
               $scope.uploadFinished = true;
               $scope.uploadLaunched = false;
               $scope.uploadFailed = true;

               // Refresh upload status icon
               fileItem.isProcessing = false;
               fileItem.isSuccess = false;
               fileItem.isWarning = false;
               fileItem.isError = true;
               downloadATR(error, error.headers);
               clearHistoryAfterUpload(operationIdServerAppLevel);
               $scope.disableSelect = false;
             }
         });
       }, 5000); // 5000 ms
    };

     $scope.stopCheck = function() {
       if (angular.isDefined($scope.stopPromise)) {
         $interval.cancel($scope.stopPromise);
         $scope.stopPromise = undefined;
       }
     };

    $scope.$on('$destroy', function() {
     // Make sure that the interval is destroyed too
     $scope.stopCheck();
    });

    //************************************************************************************************ //
    $scope.fileItem = {};
    $scope.fileItem.isProcessing = false;
    $scope.fileItem.isSuccess = false;
    $scope.fileItem.isError = false;
    $scope.fileItem.isWarning = false;

    $scope.startUpload = function(params){
      // item_715
      // Start pooling after receiving the first operationId
      var operationIdServerAppLevel = params['x-request-id'];
      $scope.fileItem.isProcessing = true;
      $scope.fileItem.isSuccess = false;
      $scope.fileItem.isError = false;
      $scope.fileItem.isWarning = false;

      $scope.check($scope.fileItem, operationIdServerAppLevel);
    }

    $scope.getSize = function(bytes, precision) {
      if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) return '-';
      if (typeof precision === 'undefined') precision = 1;
      var units = ['bytes', 'kB', 'MB', 'GB', 'TB', 'PB'],
        number = Math.floor(Math.log(bytes) / Math.log(1024));
      return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) +  ' ' + units[number];
    };

    // $scope.checkExtension = function($event,filename) {
    //   var validFormats = ['zip', 'tar'];
    //   var ext = filename.substr(filename.lastIndexOf('.')+1);
    //   var index=validFormats.indexOf(ext);
    //   if( index== -1){
    //     var formats = ['tar.gz','tar.bz2'];
    //     var spName=filename.split(".");
    //     var ext1 = spName.pop();
    //     var ext2 = spName.pop();
    //     var extn = ext2+'.'+ext1;
    //     if(formats.indexOf(extn)== -1){
    //       $scope.showAlert($event, 'Erreur : '+ filename,' Format du SIP incorrect. Sélectionner un fichier au format .zip, .tar, .tar.gz ou .tar.bz2')
    //       console.info('Format du SIP incorrect. Sélectionner un fichier au format .zip, .tar, .tar.gz ou .tar.bz2');
    //       $route.reload();
    //     }
    //   }
    // };

    $scope.disableUpload = true;
    $scope.disableSelect = false;
    $scope.ctrl = $scope;
  });
