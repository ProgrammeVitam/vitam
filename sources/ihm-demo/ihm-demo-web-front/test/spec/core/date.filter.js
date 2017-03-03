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

describe('Filter : vitamFormatDate', function () {
  beforeEach(module('ihm.demo'));
  var $filter;
  // on sauvegarde la timeZONE
  var timezoneProto = Date.prototype.getTimezoneOffset;
  beforeEach(inject(function (_$filter_) {
    $filter = _$filter_;
    // on surcharge la timeZone
    Date.prototype.getTimezoneOffset = function () {
      // GMT - 3
      return -180;
    };
  }));
  afterEach(function () {
    // on restaure la timeZone
    Date.prototype.getTimezoneOffset = timezoneProto;
  });
  it('should be able to transform utc to local date', function () {
    var a = "2017-02-01T10:19:22.160";
    expect($filter('vitamFormatDate')(a)).toBe('01-02-2017 11:19');
    expect($filter('vitamFormatDate')(a)).not.toBe('01-02-2017 10:19');
    expect($filter('vitamFormatDate')('notAdate')).toBe('');
    expect($filter('vitamFormatDate')('01-02-2017 10:19')).not.toBe('01-02-2017 10:19');
    expect($filter('vitamFormatDate')('')).toBe('');
    expect($filter('vitamFormatDate')(undefined)).toBe('');
    expect($filter('vitamFormatDate')(null)).toBe('');
    expect($filter('vitamFormatDate')('2017-02-01')).toBe('01-02-2017 00:00');
  });
});
