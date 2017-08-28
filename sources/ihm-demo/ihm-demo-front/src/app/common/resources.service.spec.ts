import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { ResourcesService } from './resources.service';
import {CookieService} from 'angular2-cookie/core';
import {MockBackend} from '@angular/http/testing';
import {BaseRequestOptions, Http, RequestOptions} from '@angular/http';

describe('ResourcesService', () => {
  let resourcesService: ResourcesService;
  let backend: MockBackend;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ResourcesService,
        CookieService,
        MockBackend,
        BaseRequestOptions,
        {
          provide: Http,
          useFactory: (mockBackend: MockBackend, defaultOptions: RequestOptions) => {
            return new Http(mockBackend, defaultOptions);
          },
          deps: [MockBackend, BaseRequestOptions]         }
        ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  beforeEach(inject([MockBackend, ResourcesService], (mockBackend, service: ResourcesService) => {
    backend = mockBackend;
    resourcesService = service;
  }));

  it('should be created', inject([ResourcesService], (service: ResourcesService) => {
    expect(service).toBeTruthy();
  }));
});
