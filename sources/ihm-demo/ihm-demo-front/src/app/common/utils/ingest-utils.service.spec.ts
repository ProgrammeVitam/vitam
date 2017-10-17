import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { IngestUtilsService } from './ingest-utils.service';
import { IngestService } from '../../ingest/ingest.service';

const IngestServiceStub = {
  getObject: (objectId, type) => {return {text: 'OK', headers: []}}
};

describe('IngestUtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        IngestUtilsService,
        { provide: IngestService, useValue: IngestServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([IngestUtilsService], (service: IngestUtilsService) => {
    expect(service).toBeTruthy();
  }));
});
