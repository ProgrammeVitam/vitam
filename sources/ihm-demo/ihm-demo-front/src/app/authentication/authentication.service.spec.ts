import { TestBed, inject } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import {CookieService} from 'angular2-cookie/core';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { HttpClient } from '@angular/common/http';

import { AuthenticationService } from './authentication.service';
import { ResourcesService } from '../common/resources.service';

describe('AuthenticationService', () => {
  let authenticationService: AuthenticationService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule],
      providers: [
        AuthenticationService,
        ResourcesService,
        CookieService,
        {
          provide: HttpClient,
          useValue: {} }
        ]
    });
  });

  beforeEach(inject([AuthenticationService], (mockBackend, service: AuthenticationService) => {
    authenticationService = service;
  }));

  it('should be created', inject([AuthenticationService], (service: AuthenticationService) => {
    expect(service).toBeTruthy();
  }));
});
