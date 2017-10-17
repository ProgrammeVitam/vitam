import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ArchiveUnitService } from './archive-unit.service';
import { VitamResponse } from "../common/utils/response";
import { ResourcesService } from "../common/resources.service";

const ResourcesServiceStub = {
  get: (url) => Observable.of(new VitamResponse()),
  post: (url, header, body) => Observable.of(new VitamResponse()),
  delete: (url) => Observable.of(new VitamResponse()),
};

describe('ArchiveUnitService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitService,
        { provide: ResourcesService, useValue: ResourcesServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([ArchiveUnitService], (service: ArchiveUnitService) => {
    expect(service).toBeTruthy();
  }));
});
