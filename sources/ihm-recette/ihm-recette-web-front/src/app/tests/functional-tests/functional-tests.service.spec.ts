import { TestBed } from '@angular/core/testing';
import {MockBackend} from "@angular/http/testing";
import { RequestMethod, BaseRequestOptions, ResponseOptions, Response, Http} from '@angular/http';
import { FunctionalTestsService } from './functional-tests.service';
import { ResourcesService } from '../../common/resources.service';
import {CookieService} from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('FunctionalTestsService', () => {
  let service: FunctionalTestsService;
  let backend: MockBackend;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        ResourcesService,
        FunctionalTestsService, CookieService,
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
    service = TestBed.get(FunctionalTestsService);
  });

  it('getResult should call correct api', (done) => {
    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/applicative-test');
      expect(connection.request.method).toEqual(RequestMethod.Get);
      done();
    });
    service.getResults();
  });

  it('getResult should get response', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.getResults().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });


  it('getResultDetail should call correct api', (done) => {
    const fileName = 'fileName';

    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/applicative-test/fileName');
      expect(connection.request.method).toEqual(RequestMethod.Get);
      done();
    });
    service.getResultDetail(fileName);
  });

  it('getResultDetail should get response', (done) => {
    let response = {
      "result": 1
    };
    const fileName = 'fileName';

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.getResultDetail('fileName').subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });


  it('launchTests should call correct api', (done) => {
    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/applicative-test');
      expect(connection.request.method).toEqual(RequestMethod.Post);
      done();
    });
    service.launchTests();
  });

  it('launchTests should get response', (done) => {
    let response = {
      "result": 1
    };

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.launchTests().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });

});
