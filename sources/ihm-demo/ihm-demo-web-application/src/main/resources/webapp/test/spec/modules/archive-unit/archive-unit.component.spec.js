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

describe('formArchive', function() {

  // Load the module that contains the `formArchive` component before each test
  beforeEach(module('archive.unit'));

  // Test the controller
  describe('ArchiveUnitController', function() {
    var $httpBackend, ctrl;

    beforeEach(inject(function($controller, _$httpBackend_) {
      $httpBackend = _$httpBackend_;
      $httpBackend.expectGET('archives/archiveUnit.json')
          .respond({_id: 'GUID', description: 'Description test', language: 'fr-FR'});

      // FIXME P0 Controller should have all required parametters.
      // FIXME In this case some parameters ($http for exemple) should be provided by a service layer.
      // TODO Fix this before fix test.
      /*ctrl = $controller('ArchiveUnitController', {
        $scope: scope,
        $routeParams: {archiveId: '1'}
      });*/
    }));

    it('should create ID and description in mainFields with `$http`', function() {
      /*jasmine.addCustomEqualityTester(angular.equals);

      expect(ctrl.archiveArray).toEqual([]);

      $httpBackend.flush();
      expect(ctrl.mainFields['ID']).toEqual({fieldName: 'ID', fieldValue:'GUID', isChild:false, typeF:'S'});
      expect(ctrl.mainFields['description']).toEqual({fieldName: 'description', fieldValue:'Description test', isChild:false, typeF:'S'});*/
    });

    it('should init other feilds as EndDate in mainFields', function() {
      /*jasmine.addCustomEqualityTester(angular.equals);

      expect(ctrl.archiveArray).toEqual([]);

      $httpBackend.flush();
      expect(ctrl.mainFields['EndDate']).toEqual({fieldName: 'EndDate', fieldValue:'', isChild:false, typeF:'S'});*/
    });


    it('should create other fields in archiveArray with $http', function() {
      /*jasmine.addCustomEqualityTester(angular.equals);

      expect(ctrl.archiveArray).toEqual([]);

      $httpBackend.flush();
      expect(ctrl.archiveArray).toEqual([{fieldName: 'language', fieldValue:'fr-FR', isChild:false, typeF:'S'}]);*/
    });
  });

});
