import {TestBed, inject} from '@angular/core/testing';

import {MySelectionService} from './my-selection.service';
import {ArchiveUnitHelper} from '../archive-unit/archive-unit.helper';
import {BreadcrumbService} from '../common/breadcrumb.service';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ArchiveUnitService} from '../archive-unit/archive-unit.service';
import {Observable} from 'rxjs/Observable';
import {VitamResponse} from '../common/utils/response';
import {ResourcesService} from '../common/resources.service';

const DefaultResponse = {
  $context: {},
  $hits: {},
  $results: [{'#object': '', '#operations': ['operationId']}],
  httpCode: 200
};

const ResourcesServiceStub = {
  get: (url) => Observable.of(new VitamResponse()),
  post: (url, header, body) => Observable.of(new VitamResponse()),
  delete: (url) => Observable.of(new VitamResponse()),
};

const ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(DefaultResponse),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('MySelectionService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MySelectionService,
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: ResourcesService, useValue: ResourcesServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([MySelectionService], (service: MySelectionService) => {
    expect(service).toBeTruthy();
  }));
});
