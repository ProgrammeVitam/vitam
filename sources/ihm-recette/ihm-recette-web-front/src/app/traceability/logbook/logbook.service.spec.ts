import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { MockBackend } from "@angular/http/testing";
import { Injectable, ReflectiveInjector } from '@angular/core';
import { RequestMethod, BaseRequestOptions, ResponseOptions,
  ConnectionBackend, Http, Response, RequestOptions } from '@angular/http';
import { LogbookService } from './logbook.service';
import { ResourcesService } from '../../common/resources.service';
import { CookieService } from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('LogbookService', () => {
  let service: LogbookService;
  let backend: MockBackend;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        ResourcesService,
        LogbookService, CookieService,
        MockBackend,
        BaseRequestOptions,
        {
          provide: Http,
          useFactory: (backend, options) => new Http(backend, options),
          deps: [MockBackend, BaseRequestOptions]
        }
      ]
    });

    backend = TestBed.get(MockBackend);
    service = TestBed.get(LogbookService);
  });

  it('launchTraceability should call correct api', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/operations/traceability');
      expect(connection.request.method).toEqual(RequestMethod.Post);
      done();
    });
    service.launchTraceability();
  });

  it('launchTraceability should get response', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.launchTraceability().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });
  
  it('launchTraceabilityLFC should call correct api', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/lifecycles/traceability');
      expect(connection.request.method).toEqual(RequestMethod.Post);
      done();
    });
    service.launchTraceabilityLFC();
  });

  it('launchTraceabilityLFC should get response', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.launchTraceabilityLFC().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });

});
