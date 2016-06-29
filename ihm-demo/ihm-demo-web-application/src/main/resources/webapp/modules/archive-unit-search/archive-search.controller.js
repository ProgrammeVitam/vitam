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

angular.module('archiveSearch')
 .constant('ARCHIVE_SEARCH_MODULE_CONST', {
   'ARCHIVE_FORM_ALREADY_OPENED' : 'Le formulaire de l\'Archive Unit sélectionnée est déjà ouvert.'
 })
 .controller('ArchiveUnitSearchController', ['$scope','ihmDemoFactory','$window', '$mdToast', 'ARCHIVE_SEARCH_MODULE_CONST',
          function($scope, ihmDemoFactory, $window, $mdToast, ARCHIVE_SEARCH_MODULE_CONST) {

  // Archive Units Search Result
  $scope.archiveUnitsSearchResult;
  var criteriaSearch = {};
  $scope.getSearchResult = function getSearchResult(titleCriteria){
    if(titleCriteria!==''){

      // Build title criteria and default selection
      criteriaSearch.Title = titleCriteria;
      criteriaSearch.projection_transactdate = "TransactedDate";
      criteriaSearch.projection_id = "_id";
      criteriaSearch.projection_title = "Title";
      criteriaSearch.orderby = "TransactedDate";

      ihmDemoFactory.searchArchiveUnits(criteriaSearch)
      .then(function (response) {
        $scope.status = 'Retrieved Archive Units!';
        $scope.archiveUnitsSearchResult = response.data.$result;
        $scope.showResult=true;
        $scope.error=false;

        // Set Total result
        $scope.totalResult = response.data.$hits.total;
      }, function (error) {
        $scope.status = 'Error retrieving archive units! ' + error.message;
        $scope.error=true;
        $scope.showResult=false;
      });
    }
  }

  // Display Selected Archive unit form

  //******** Toast diplayed only if the archive unit is already opened ********* //
  var last = {
      bottom: false,
      top: true,
      left: false,
      right: true
    };
  $scope.toastPosition = angular.extend({},last);
  $scope.getToastPosition = function() {
    sanitizePosition();
    return Object.keys($scope.toastPosition)
      .filter(function(pos) { return $scope.toastPosition[pos]; })
      .join(' ');
  };
  function sanitizePosition() {
    var current = $scope.toastPosition;
    if ( current.bottom && last.top ) current.top = false;
    if ( current.top && last.bottom ) current.bottom = false;
    if ( current.right && last.left ) current.left = false;
    if ( current.left && last.right ) current.right = false;
    last = angular.extend({},current);
  }
  $scope.showSimpleToast = function() {
    var pinTo = $scope.getToastPosition();
    $mdToast.show(
      $mdToast.simple()
        .textContent(ARCHIVE_SEARCH_MODULE_CONST.ARCHIVE_FORM_ALREADY_OPENED)
        .position(pinTo )
        .hideDelay(3000)
    );
  };
  // **************************************************************************** //


  $scope.openedArchiveId=[];
  $scope.openedArchiveWindowRef=[];
  $scope.displayArchiveUnitForm = function displayArchiveUnitForm(archiveId){

    var archiveUnitwindow;
    var archiveUnitwindowIndex = $scope.openedArchiveId.indexOf(archiveId);

    if(archiveUnitwindowIndex !== -1){
      archiveUnitwindow = $scope.openedArchiveWindowRef[archiveUnitwindowIndex];

      // Show Toast to indicate that the archive unit form is already opened
      $scope.showSimpleToast();

    }else{
      archiveUnitwindow = $window.open('#!/archiveunit/' + archiveId);

      // add a close listener to the window so that the
      archiveUnitwindow.onbeforeunload = function(){
        $scope.openedArchiveId.splice(archiveUnitwindowIndex, 1);
      }
      $scope.openedArchiveId.push(archiveId);
      $scope.openedArchiveWindowRef.push(archiveUnitwindow);
    }
  }

  // ******************* Mock response *************** //
  $scope.showResult=true;
  $scope.totalResult = 10;
  $scope.archiveRes =  [ {"_id":"1", "Title":"Archive1", "Date":"2016-01-01"},
  {"_id":"1", "Title":"Archive2", "Date":"2016-01-01"},
  {"_id":"2", "Title":"Archive3", "Date":"2016-01-01"}];
  // ************************************************ //


}]);
