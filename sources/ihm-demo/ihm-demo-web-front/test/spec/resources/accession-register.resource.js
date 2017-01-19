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

describe('accessionRegisterResource', function() {
  beforeEach(module('ihm.demo'));

  var AccessionRegisterResource;
  var $http;
  beforeEach(inject(function (_accessionRegisterResource_, _$http_) {
    AccessionRegisterResource = _accessionRegisterResource_;
    $http = _$http_;
  }));

  it('should call the good API point for accession register detail', function() {
    // Init a spy on $http.get
    spyOn($http, 'post');

    var criteria = {OriginatingAgency: '001'};
    AccessionRegisterResource.getDetails('001', criteria);
    expect($http.post).toHaveBeenCalledWith('/ihm-demo/v1/api/admin/accession-register/001/accession-register-detail/', criteria);
    expect($http.post).toHaveBeenCalledTimes(1);
  });

  it('should call the good API point for accession register summary', function() {
    // Init a spy on $http.get
    spyOn($http, 'post');

    var criteria = {OriginatingAgency: '002'};
    AccessionRegisterResource.getSummary(criteria);
    expect($http.post).toHaveBeenCalledWith('/ihm-demo/v1/api/admin/accession-register/', criteria);
    expect($http.post).toHaveBeenCalledTimes(1);
  });

});
