import { TestBed } from '@angular/core/testing';
import {MockBackend} from "@angular/http/testing";
import 'rxjs/add/observable/of';

import { RequestMethod, BaseRequestOptions, ResponseOptions, Response, Http} from '@angular/http';
import { PerfService } from './perf.service';
import { ResourcesService } from '../../common/resources.service';
import {CookieService} from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('PerfService', () => {
  let service: PerfService;
  let backend: MockBackend;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        ResourcesService,
        PerfService, CookieService,
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
    service = TestBed.get(PerfService);
  });

  it('generateIngestStatReport should call correct api', (done) => {
    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/performances/reports');
      expect(connection.request.method).toEqual(RequestMethod.Get);
      done();
    });
    service.generateIngestStatReport();
  });

  it('generateIngestStatReport should get response', (done) => {
    let response = ['name1', 'name2'];

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.generateIngestStatReport().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });


  it('getAvailableSipForUpload should call correct api', (done) => {
    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/performances/sips');
      expect(connection.request.method).toEqual(RequestMethod.Get);
      done();
    });
    service.getAvailableSipForUpload();
  });

  it('getAvailableSipForUpload should get response', (done) => {
    let response = ['name1', 'name2'];

    backend.connections.subscribe(connection => {
      connection.mockRespond(new Response(<ResponseOptions>{
        body: JSON.stringify(response)
      }));
    });

    service.getAvailableSipForUpload().subscribe(response => {
      expect(response).not.toBeNull();
      done();
    });
  });

  it('uploadSelected should call correct api', (done) => {
    let body = {fileName : 'Test'};

    backend.connections.subscribe(connection => {
      expect(connection.request.url).toEqual('/ihm-recette/v1/api/performances');
      expect(connection.request.method).toEqual(RequestMethod.Post);
      expect(connection.request._body).toEqual(body);
      done();
    });
    service.uploadSelected(body);
  });

});
