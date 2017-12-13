import { TestBed, inject } from '@angular/core/testing';

import { ResourcesService } from './resources.service';
import {CookieService} from 'angular2-cookie/core';
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { Router } from '@angular/router';

const RouterStub = {
  navigate: () => {}
};

describe('ResourcesService', () => {
  let resourcesService: ResourcesService;
  let backend: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ HttpClientTestingModule ],
      providers: [
        ResourcesService,
        CookieService,
        {provide: Router, useValue: RouterStub}
      ]
    });

    backend = TestBed.get(HttpTestingController);
    resourcesService = TestBed.get(ResourcesService);
  });

  it('should be created', inject([ResourcesService], (service: ResourcesService) => {
    expect(service).toBeTruthy();
  }));
});
