import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { ArchiveUnitHelper } from './archive-unit.helper';

describe('ArchiveUnitHelper', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ArchiveUnitHelper],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    expect(service).toBeTruthy();
  }));

  it('should translate the given keys', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    const translations = service.getTranslationConstants();
    expect(translations.DescriptionLevel).toBe('Niveau de description');
    expect(translations.Keyword['@@']).toBe('Mot-clés');
    expect(translations.WritingGroup.Writer['@@']).toBe('Rédacteur');
    expect(translations.AudienceGroup.Addressee.BirthPlace.Country).toBe('Pays');
  }));

  it('should check the type', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    expect(service.isTextArea('Description')).toBeTruthy();
    expect(service.isSelection('DescriptionLevel')).toBeTruthy();
  }));

  it('should return the good date', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    let unitData = {
      DescriptionLevel: 'RecordGrp',
      StartDate: '2011-02-01',
      EndDate: '2012-02-01',
      ReceivedDate: '2012-03-01',
      CreatedDate: '2011-01-01'
    };
    expect(service.getStartDate(unitData)).toBe('2011-02-01');
    expect(service.getEndDate(unitData)).toBe('2012-02-01');

    unitData.DescriptionLevel = 'Item';
    expect(service.getStartDate(unitData)).toBe('2011-01-01');
    expect(service.getEndDate(unitData)).toBe('2012-03-01');
  }));

  it('should translate the type', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    expect(service.transformType('FILING_UNIT')).toBe('Plan de classement');
    expect(service.transformType('INGEST')).toBe('Standard');
    expect(service.transformType('UNKNOW_TYPE')).toBe('UNKNOW_TYPE');
  }));

  it('should exclude some fields', inject([ArchiveUnitHelper], (service: ArchiveUnitHelper) => {
    expect(service.mustExcludeFields('#id')).toBeTruthy();
    expect(service.mustExcludeFields('Description')).toBeTruthy();
    expect(service.mustExcludeFields('_unitType')).toBeTruthy();
    expect(service.mustExcludeFields('#Management.SomeValue')).toBeTruthy();
    expect(service.mustExcludeFields('OtherField')).toBeFalsy();
  }));

});
