import { TestBed, inject } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { ResourcesService } from './resources.service';
import {CookieService} from 'angular2-cookie/core';
import { HttpClient } from '@angular/common/http';

describe('ResourcesService', () => {
  let resourcesService: ResourcesService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ResourcesService,
        CookieService,
        {
          provide: HttpClient,
          useValue: {} }
        ],
      schemas: [NO_ERRORS_SCHEMA]
    });
  });

  beforeEach(inject([ResourcesService], (mockBackend, service: ResourcesService) => {
    resourcesService = service;
  }));

  it('should be created', inject([ResourcesService], (service: ResourcesService) => {
    expect(service).toBeTruthy();
  }));
});
