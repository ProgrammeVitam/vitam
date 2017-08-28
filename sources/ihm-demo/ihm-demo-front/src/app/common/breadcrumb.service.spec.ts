import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { BreadcrumbService } from './breadcrumb.service';

describe('BreadcrumbService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [BreadcrumbService],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([BreadcrumbService], (service: BreadcrumbService) => {
    expect(service).toBeTruthy();
  }));
});
