import { TestBed, inject } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import {CookieService} from 'angular2-cookie/core';
import {MockBackend} from '@angular/http/testing';
import {BaseRequestOptions, Http, RequestOptions} from '@angular/http';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { AuthenticationService } from './authentication.service';
import { ResourcesService } from '../common/resources.service';

describe('AuthenticationService', () => {
  let authenticationService: AuthenticationService;
  let backend: MockBackend;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule],
      providers: [
        AuthenticationService,
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
        ]
    });
  });

  beforeEach(inject([MockBackend, AuthenticationService], (mockBackend, service: AuthenticationService) => {
    backend = mockBackend;
    authenticationService = service;
  }));

  it('should be created', inject([AuthenticationService], (service: AuthenticationService) => {
    expect(service).toBeTruthy();
  }));
});
