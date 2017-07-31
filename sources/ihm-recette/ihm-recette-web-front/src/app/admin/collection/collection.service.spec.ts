import { TestBed, inject } from '@angular/core/testing';

import { CollectionService } from './collection.service';
import {Observable} from 'rxjs/Observable';
import {ResourcesService} from '../../common/resources.service';

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  delete: (url) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('CollectionService', () => {
  let collectionService: CollectionService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CollectionService,
        {
          provide: ResourcesService, useValue: ResourcesServiceStub
        }
      ]
    });
  });

  beforeEach(inject([CollectionService], (service: CollectionService) => {
    collectionService = service;
  }));

  it('should be created', inject([CollectionService], (service: CollectionService) => {
    expect(service).toBeTruthy();
  }));
});
