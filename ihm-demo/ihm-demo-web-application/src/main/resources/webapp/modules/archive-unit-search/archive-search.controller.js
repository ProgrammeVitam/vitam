angular.module('archiveSearch').controller('ArchiveUnitSearchController', ['$scope','ihmDemoFactory', function($scope, ihmDemoFactory ) {

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
  $scope.displayArchiveUnitForm = function displayArchiveUnitForm(archiveId){
  }


  // Mock response
  $scope.showResult=true;
  $scope.totalResult = 10;
  $scope.archiveRes =  [ {"_id":"ID1", "Title":"Archive1", "Date":"2016-01-01"},
  {"_id":"ID2", "Title":"Archive2", "Date":"2016-01-01"},
  {"_id":"ID3", "Title":"Archive3", "Date":"2016-01-01"}];


}]);
