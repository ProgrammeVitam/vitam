import {TestBed, inject} from '@angular/core/testing';

import {WorkflowService} from './workflow.service';
import {ResourcesService} from "../common/resources.service";
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {Observable} from "rxjs/Observable";
import {VitamResponse} from "../common/utils/response";

describe('WorkflowService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
        providers: [WorkflowService,
          {provide: ResourcesService, useValue: ResourcesServiceStub}],
        schemas: [NO_ERRORS_SCHEMA]
      }
    );
  });

  const ResourcesServiceStub = {
    get: (url) => Observable.of(new VitamResponse()),
    post: (url, header, body) => Observable.of(new VitamResponse()),
    delete: (url) => Observable.of(new VitamResponse()),
  };

  it('should be created', inject([WorkflowService], (service: WorkflowService) => {
    expect(service).toBeTruthy();
  }));
});
