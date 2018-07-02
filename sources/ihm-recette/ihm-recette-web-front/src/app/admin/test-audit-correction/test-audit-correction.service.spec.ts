import { TestBed, inject } from '@angular/core/testing';

import { LoadStorageService } from './load-storage.service';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('LoadStorageService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LoadStorageService,
        {
          provide: ResourcesService, useValue: ResourcesServiceStub
        }
      ]
    });
  });

  it('should be created', inject([LoadStorageService], (service: LoadStorageService) => {
    expect(service).toBeTruthy();
  }));
});
