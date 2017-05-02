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

describe('entryContractsDetailsController', function() {
    beforeEach(module('ihm.demo'));

    var scope, EntryContractsDetailsController, EntryContractResource;
    var contractId = 'aefqaaaaaagbcaacaapr4ak3uctdrnaaaaaq';

    var contractDetail = {
        "$hits":{
            "total":1,
            "offset":0,
            "limit":1000,
            "size":1
        },"$results":[{
            "_id":"aefqaaaaaagbcaacaapr4ak3uctdrnaaaaaq",
            "_tenant":0,
            "Name":"ArchivalAgreement0Test",
            "Description":"Contrat Vitam 0",
            "Status":"ACTIVE",
            "CreationDate":"2016-12-10T00:00",
            "LastUpdate":"2017-04-24T15:48:19.763",
            "ActivationDate":"2016-12-10T00:00",
            "DeactivationDate":"2016-12-10T00:00"
        }],"$context":{}
    };

    beforeEach(inject(function ($rootScope, $controller, _entryContractResource_) {
        scope = $rootScope.$new(false, null);
        EntryContractResource = _entryContractResource_;

        EntryContractsDetailsController = function() {
            return $controller('entryContractsDetailsController', {
                $scope: scope,
                $routeParams: {id: contractId},
                entryContractResource: EntryContractResource
            });
        };
    }));

    it('should init the detail on initialization', function() {
        spyOn(EntryContractResource, 'getDetails').and.callFake(function (id, callback) {
            callback({data: contractDetail});
        });

        EntryContractsDetailsController();

        expect(scope.contract._id).toBe(contractId)
        expect(scope.tmpVars.oldStatus).toBe('ACTIVE'); // $results.Status
        expect(scope.tmpVars.isActive).toBe(true); // true because Status is ACTIVE
    });

    it('should update Status when updateStatus is called', function() {
        spyOn(EntryContractResource, 'getDetails').and.callFake(function (id, callback) {
            callback({data: contractDetail});
        });

        EntryContractsDetailsController();

        expect(scope.tmpVars.oldStatus).toBe('ACTIVE'); // $results.Status
        expect(scope.tmpVars.isActive).toBe(true); // true because Status is ACTIVE
        scope.tmpVars.isActive = false;
        scope.updateStatus();
        expect(scope.tmpVars.oldStatus).toBe('ACTIVE'); // $results.Status
        expect(scope.contract.Status).toBe('INACTIVE'); // true because Status is ACTIVE
    });

});