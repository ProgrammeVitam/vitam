import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { AccessContractService } from './access-contract.service';

describe('AccessContractService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AccessContractService],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  it('should be created', inject([AccessContractService], (service: AccessContractService) => {
    expect(service).toBeTruthy();
  }));
});
