/*
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

describe('searchOperationController', function() {
  beforeEach(module('ihm.demo'));

  var scope, $q, $location, SearchOperationController, SearchOperationService;
  var searchOperationResult = {
    "_id": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
    "evId": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
    "evType": "LOGBOOK_OP_SECURISATION",
    "evDateTime": "2017-02-10T10:41:58.130",
    "evDetData": null,
    "evIdProc": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
    "evTypeProc": "TRACEABILITY",
    "outcome": "STARTED",
    "outDetail": "LOGBOOK_OP_SECURISATION.STARTED",
    "outMessg": "Début de la sécurisation des journaux",
    "agId": "{\"Name\":\"vitam-iaas-app-01\",\"Role\":\"logbook\",\"PlatformId\":425367}",
    "agIdApp": null,
    "evIdAppSession": null,
    "evIdReq": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
    "agIdSubm": null,
    "agIdOrig": null,
    "obId": null,
    "obIdReq": null,
    "obIdIn": null,
    "events": [{
        "evId": "aedqaaaaacaam7mxaah6gak2e6o2k4qaaaaq",
        "evType": "STP_OP_SECURISATION",
        "evDateTime": "2017-02-10T10:42:07.346",
        "evDetData": "{\"StartDate\":\"-999999999-01-01T00:00:00\",\"EndDate\":\"2017-02-10T10:41:58.173\",\"Hash\":\"hash\",\"TimeStampToken\":\"token\",\"NumberOfElements\":1828,\"FileName\":\"0_LogbookOperation_20170210_104158.zip\"}",
        "evIdProc": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
        "evTypeProc": "TRACEABILITY",
        "outcome": "OK",
        "outDetail": "STP_OP_SECURISATION.OK",
        "outMessg": "Succès du processus de sécurisation des journaux",
        "agId": {
          "Name": "vitam-iaas-app-01",
          "Role": "logbook",
          "PlatformId": 425367
        },
        "evIdReq": "aecaaaaaacaam7mxaah6gak2e6oycvyaaaaq",
        "obId": null,
        "obIdReq": null,
        "obIdIn": null
      }]
  };

  beforeEach(inject(function ($rootScope, $controller, _$location_, _searchOperationService_,
                              _$q_, _$window_, _downloadOperationService_,
                              _ihmDemoCLient_) {
    $location = _$location_;
    $q = _$q_;
    scope = $rootScope.$new(false, null);
    SearchOperationService = _searchOperationService_;

    SearchOperationController = function() {

      return $controller('searchOperationController', {
        $scope: scope,
        $window: _$window_,
        searchOperationService: SearchOperationService,
        ihmDemoCLient: _ihmDemoCLient_,
        downloadOperationService: _downloadOperationService_
      });
    };
  }));

  it('should call the callback in order to set result', function () {
    spyOn(SearchOperationService, 'getOperations').and.callFake(function (id, callback) {
      callback({data: {$results: [searchOperationResult], $hits: {total: 1}}});
    });

    SearchOperationController();
    scope.ctrl.getList();

    expect(scope.ctrl.operationList.length).toBe(1);
    expect(scope.ctrl.operationList[0].events[0].evDetData.FileName).toBe('0_LogbookOperation_20170210_104158.zip');
  });

  it('should init the controller with some parametters', function() {
    SearchOperationController();
    expect(scope.logType).toBe('--');
  })

});
