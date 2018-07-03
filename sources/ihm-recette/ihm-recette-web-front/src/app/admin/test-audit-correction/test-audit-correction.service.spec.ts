import { TestBed, inject } from '@angular/core/testing';

import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';
import {TestAuditCorrectionService} from "./test-audit-correction.service";

const cookies = {};
const ResourcesServiceStub = {
  get: (url, header?: Headers) => Observable.of('OK'),
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  getTenants: () => Observable.of(['0', '1', '2']),
  setTenant: (tenantId: string) => cookies['tenant'] = tenantId,
  getTenant: () => cookies['tenant']
};

describe('TestAuditCorrectionService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TestAuditCorrectionService,
        {
          provide: ResourcesService, useValue: ResourcesServiceStub
        }
      ]
    });
  });

  it('should be created', inject([TestAuditCorrectionService], (service: TestAuditCorrectionService) => {
    expect(service).toBeTruthy();
  }));
});
