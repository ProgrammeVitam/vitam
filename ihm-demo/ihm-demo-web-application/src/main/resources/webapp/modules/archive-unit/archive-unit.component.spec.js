'use strict';

describe('formArchive', function() {

  // Load the module that contains the `formArchive` component before each test
  beforeEach(module('archive.unit'));

  // Test the controller
  describe('ArchiveUnitController', function() {
    var $httpBackend, ctrl;

    beforeEach(inject(function($componentController, _$httpBackend_, $routeParams) {
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('archives/archiveUnit.json')
          .respond({_id: 'GUID', description: 'Description test'});

      ctrl = $componentController('formArchive', {
         $routeParams: {archiveId: '1'}
      });
    }));

    it('should create an array of 2 objects of type fieldSet with `$http`', function() {
      jasmine.addCustomEqualityTester(angular.equals);

      expect(ctrl.archiveArray).toEqual([]);

      $httpBackend.flush();
      expect(ctrl.archiveArray).toEqual([{fieldName: 'ID', fieldValue:'GUID', isChild:false, typeF:'S'},
        {fieldName: 'description', fieldValue:'Description test', isChild:false, typeF:'S'}]);
    });
  });

});
