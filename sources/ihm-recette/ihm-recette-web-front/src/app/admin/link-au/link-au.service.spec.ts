import { TestBed, inject } from '@angular/core/testing';

import { LinkAuService } from './link-au.service';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';

const ResourcesServiceStub = {
  post: (url, header?: Headers, body?: any) => {
    return Observable.of('OK')
  }
};

describe('LinkAuService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LinkAuService,
        {provide: ResourcesService, useValue: ResourcesServiceStub}
      ]
    });
  });

  it('should be created', inject([LinkAuService], (service: LinkAuService) => {
    expect(service).toBeTruthy();
  }));
});
