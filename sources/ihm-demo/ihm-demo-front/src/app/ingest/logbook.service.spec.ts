import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { LogbookService } from './logbook.service';
import { VitamResponse } from "../common/utils/response";
import { ResourcesService } from "../common/resources.service";
import { IngestUtilsService } from "../common/utils/ingest-utils.service";
import { ArchiveUnitService } from "../archive-unit/archive-unit.service";

const IngestUtilsServiceStub = {
  downloadObject: (objectId, type) => Observable.of(new VitamResponse())
};
const ResourcesServiceStub = {
  get: (url) => Observable.of(new VitamResponse()),
  post: (url, header, body) => Observable.of(new VitamResponse()),
  delete: (url) => Observable.of(new VitamResponse()),
};

let ArchiveUnitServiceStub = {
  downloadDIP: (id) => Observable.of(new VitamResponse()),
  downloadTransferSIP: (id) => Observable.of(new VitamResponse())
};

describe('LogbookService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LogbookService,
        { provide: IngestUtilsService, useValue: IngestUtilsServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub },
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([LogbookService], (service: LogbookService) => {
    expect(service).toBeTruthy();
  }));
});
