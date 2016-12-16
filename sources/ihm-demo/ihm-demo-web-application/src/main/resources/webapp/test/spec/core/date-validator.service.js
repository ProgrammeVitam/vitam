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

describe('dateValidator', function() {
  beforeEach(module('ihm.demo'));

  var DateValidatorService;
  beforeEach(inject(function(_dateValidator_) {
    DateValidatorService = _dateValidator_;
  }));

  var dateNotEnoughParts = '10/05';
  var dateTooMuchParts = '10/05/2016/1';
  var dateNotANumber = '10/05/ko';
  var dateNotANumber2 = '10/05/2k16';

  var dateDay0 = '00/05/2016';
  var dateMounth0 = '10/00/2016';
  var dateYear0 = '10/05/0000';
  var dateDayOutOfBound = '32/01/2016';
  var dateMounthOutOfBound = '32/13/2016';
  var dateFebuary = '30/02/2016';
  var dateNovember = '31/11/2016';
  var dateFeb2015 = '29/02/2015';

  var goodDate = '01/10/1990';

  it('should return false if its not a date structure', function() {
    expect(DateValidatorService.validateDate(dateNotEnoughParts)).toEqual(false);
    expect(DateValidatorService.validateDate(dateTooMuchParts)).toEqual(false);
    expect(DateValidatorService.validateDate(dateNotANumber)).toEqual(false);

    // FIXME P0 This test should return false, initial service must be corrected !
    expect(DateValidatorService.validateDate(dateNotANumber2)).toEqual(true);
  });

  it('should return false if parts are out of bound', function() {
    expect(DateValidatorService.validateDate(dateDay0)).toEqual(false);
    expect(DateValidatorService.validateDate(dateDayOutOfBound)).toEqual(false);
    expect(DateValidatorService.validateDate(dateMounth0)).toEqual(false);
    expect(DateValidatorService.validateDate(dateMounthOutOfBound)).toEqual(false);
    expect(DateValidatorService.validateDate(dateYear0)).toEqual(false);
    expect(DateValidatorService.validateDate(dateFebuary)).toEqual(false);

    // FIXME P0 These tests should return false, because of specific mounth/years
    expect(DateValidatorService.validateDate(dateNovember)).toEqual(true);
    expect(DateValidatorService.validateDate(dateFeb2015)).toEqual(true);
  });

  it('should return true if the response structure is good', function() {
    expect(DateValidatorService.validateDate(goodDate)).toEqual(true);
  })
});
