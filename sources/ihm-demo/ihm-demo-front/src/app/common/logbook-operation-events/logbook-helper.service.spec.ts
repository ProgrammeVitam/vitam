import { TestBed, inject } from '@angular/core/testing';

import { LogbookHelperService } from './logbook-helper.service';

describe('LogbookHelperService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LogbookHelperService]
    });
  });

  it('should be created', inject([LogbookHelperService], (service: LogbookHelperService) => {
    expect(service).toBeTruthy();
  }));
});
