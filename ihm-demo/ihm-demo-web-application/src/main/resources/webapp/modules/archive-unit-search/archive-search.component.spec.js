'use strict';

describe('ArchiveUnitSearchController', function() {
beforeEach(module('archiveSearch'));

var $controller;

beforeEach(inject(function(_$controller_){
  // The injector unwraps the underscores (_) from around the parameter names when matching
  $controller = _$controller_;
}));

describe('$scope.searchResults', function() {
  it('should sort searchResults by some property, when clicking on the column header', function() {
    var $scope = {};
    var controller = $controller('OrderSearchResultController', { $scope: $scope });
    $scope.sortBy('id');
    expect($scope.reverse).toEqual(false);
  });
});
});
