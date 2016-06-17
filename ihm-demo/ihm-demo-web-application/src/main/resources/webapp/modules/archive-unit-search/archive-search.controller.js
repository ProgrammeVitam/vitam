angular.module('archiveSearch').controller('ArchiveUnitSearchController', ['$scope','ihmDemoFactory', function($scope, ihmDemoFactory) {

  // Archive Units Search Result
  $scope.archiveUnitsSearchResult;
  var criteriaSearch = {};
  $scope.getSearchResult = function getSearchResult(titleCriteria){
    if(titleCriteria!==''){

      // Build title criteria and default selection
      criteriaSearch.Title = titleCriteria;
      criteriaSearch._id = '';
      criteriaSearch.TransactedDate = '';

      ihmDemoFactory.searchArchiveUnits(criteriaSearch)
      .then(function (response) {
        $scope.status = 'Retrieved Archive Units!';
        $scope.archiveUnitsSearchResult = response.data.results;
        $scope.showResult=true;

        // Set Total result
        $scope.totalResult = response.data.hits.properties.total;

      }, function (error) {
        $scope.status = 'Error retrieving archive units! ' + error.message;
        $scope.showResult=false;
      });
    }
  }

  $scope.searchResults = [
   { id : 1,title : "Archive 1",   date: "2016-06-01" },
   { id : 4,title : "Archive 2",   date: "2018-05-05" },
   { id : 3,title : "Archive 3",   date: "2017-05-18" },
   { id : 2,title : "Archive 4",   date: "2016-05-24" },
   {  id : 6,title : "Archive 5",   date: "2016-02-31" },
   { id : 5,title : "Archive 6",   date: "2016-05-14" }
  ];
  $scope.propertyName = 'id';
  $scope.reverse = true;

  $scope.sortBy = function(propertyName) {
    $scope.reverse = ($scope.propertyName === propertyName) ? !$scope.reverse : false;
    $scope.propertyName = propertyName;
  };
}]);
