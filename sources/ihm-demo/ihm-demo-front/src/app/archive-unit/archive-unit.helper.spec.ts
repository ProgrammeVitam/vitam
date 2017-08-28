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
});
