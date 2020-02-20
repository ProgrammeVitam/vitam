import {TestBed, inject} from '@angular/core/testing';

import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';
import {IngestCleanupService} from './ingest-cleanup.service';

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('IngestCleanupService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [IngestCleanupService,
        {
          provide: ResourcesService, useValue: ResourcesServiceStub
        }
      ]
    });
  });

  it('should be created', inject([IngestCleanupService], (service: IngestCleanupService) => {
    expect(service).toBeTruthy();
  }));
});
