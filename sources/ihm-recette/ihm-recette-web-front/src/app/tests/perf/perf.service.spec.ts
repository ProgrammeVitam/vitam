import { TestBed } from '@angular/core/testing';
import 'rxjs/add/observable/of';
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { PerfService } from './perf.service';
import { ResourcesService } from '../../common/resources.service';
import {CookieService} from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('PerfService', () => {
  let service: PerfService;
  let backend: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule, HttpClientTestingModule ],
      providers: [
        ResourcesService,
        PerfService,
        CookieService
      ]
    });

    backend = TestBed.get(HttpTestingController);
    service = TestBed.get(PerfService);
  });

  it('generateIngestStatReport should call correct api', () => {
    service.generateIngestStatReport().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/performances/reports',
      method: 'GET'
    });
    response.flush(['name1', 'name2']);

    backend.verify();
  });

  it('getAvailableSipForUpload should call correct api', () => {
    service.getAvailableSipForUpload().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/performances/sips',
      method: 'GET'
    });
    response.flush(['name1', 'name2']);

    backend.verify();
  });

  it('uploadSelected should call correct api', () => {
    service.uploadSelected({fileName : 'Test'}).subscribe();

    backend.expectOne({
      url: '/ihm-recette/v1/api/performances',
      method: 'POST'
    });

    backend.verify();
  });

});
