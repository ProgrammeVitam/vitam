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

describe('accessionRegisterDetailsController', function() {
  beforeEach(module('ihm.demo'));

  var scope, $q, $location, AccessionRegisterDetailController, AccessionRegisterService, AccessionRegisterResource;
  var accessionRegisterId = '001';

  var accessionRegisterSummary = {
    "_id": "aefaaaaaaaaam7mxaabdqakyue3nlzaaaaaq",
    "_tenant": 0,
    "OriginatingAgency": "FRAN_NP_005568",
    "TotalObjects": {
      "Total": 30,
      "Deleted": 0,
      "Remained": 30
    },
    "TotalObjectGroups": {
      "Total": 12,
      "Deleted": 0,
      "Remained": 12
    },
    "TotalUnits": {
      "Total": 36,
      "Deleted": 0,
      "Remained": 36
    },
    "ObjectSize": {
      "Total": 3427596,
      "Deleted": 0,
      "Remained": 3427596
    },
    "creationDate": "2016-11-26T15:17:55.300"
  };

  var accessionRegisterDetail = [{
    "_id": "aedqaaaaacaam7mxaadfaakyue3k5iaaaaaq",
    "_tenant": 0,
    "OriginatingAgency": "FRAN_NP_005568",
    "SubmissionAgency": "FRAN_NP_005061",
    "EndDate": "2016-11-26T16:17:55.529+01:00",
    "StartDate": "2016-11-26T16:17:55.530+01:00",
    "Status": "STORED_AND_COMPLETED",
    "TotalObjectGroups": {
      "total": 1,
      "deleted": 0,
      "remained": 1
    },
    "TotalUnits": {
      "total": 5,
      "deleted": 0,
      "remained": 5
    },
    "TotalObjects": {
      "total": 1,
      "deleted": 0,
      "remained": 1
    },
    "ObjectSize": {
      "total": 226224,
      "deleted": 0,
      "remained": 226224
    }
  }, {
    "_id": "aedqaaaaacaam7mxaadfaakyf2k16jaaaaaq",
    "_tenant": 0,
    "OriginatingAgency": "FRAN_NP_005568",
    "SubmissionAgency": "FRAN_NP_005062",
    "EndDate": "2016-11-26T16:17:55.529+01:00",
    "StartDate": "2016-11-26T16:17:55.530+01:00",
    "Status": "STORED_AND_COMPLETED",
    "TotalObjectGroups": {
      "total": 5,
      "deleted": 0,
      "remained": 5
    },
    "TotalUnits": {
      "total": 6,
      "deleted": 0,
      "remained": 6
    },
    "TotalObjects": {
      "total": 2,
      "deleted": 0,
      "remained": 2
    },
    "ObjectSize": {
      "total": 2260224,
      "deleted": 0,
      "remained": 2260224
    }
  }];

  beforeEach(inject(function ($rootScope, $controller, _$location_, _accessionRegisterService_,
                              _accessionRegisterResource_, _$q_) {
    $location = _$location_;
    $q = _$q_;
    scope = $rootScope.$new(false, null);
    AccessionRegisterService = _accessionRegisterService_;
    AccessionRegisterResource = _accessionRegisterResource_;

    AccessionRegisterDetailController = function() {
      return $controller('accessionRegisterDetailsController', {
        $scope: scope,
        $routeParams: {accessionRegisterId: accessionRegisterId},
        accessionRegisterService: AccessionRegisterService
      });
    };
  }));

  it('should call the callback in order to set operationArray and summaryData property', function () {
    spyOn(AccessionRegisterService, 'getDetails').and.callFake(function (id, callback) {
      callback(accessionRegisterDetail);
    });
    spyOn(AccessionRegisterService, 'getSummary').and.callFake(function (id, callback) {
      callback(accessionRegisterSummary);
    });

    AccessionRegisterDetailController();

    expect(scope.operationArray.length).toBe(2);
    expect(scope.operationArray).toBe(accessionRegisterDetail);
    expect(scope.summaryData).toBe(accessionRegisterSummary);
  });

  it('should init the controller with some parametters', function() {
    AccessionRegisterDetailController();

    expect(scope.accessionRegisterId).toBe(accessionRegisterId);
    expect(scope.isEditMode).toBe(false);
  })

});