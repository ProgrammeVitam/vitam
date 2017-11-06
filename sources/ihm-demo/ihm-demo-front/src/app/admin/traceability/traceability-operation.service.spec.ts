import { TestBed, inject } from '@angular/core/testing';

import { TraceabilityOperationService } from './traceability-operation.service';
import {ResourcesService} from "../../common/resources.service";

const ResourcesServiceStub = {

};

describe('TraceabilityOperationService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TraceabilityOperationService,
        { provide: ResourcesService, useValue: ResourcesServiceStub }
      ]
    });
  });

  it('should be created', inject([TraceabilityOperationService], (service: TraceabilityOperationService) => {
    expect(service).toBeTruthy();
  }));
});
