import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { LogbookService } from './logbook.service';
import { ResourcesService } from '../../common/resources.service';
import { CookieService } from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('LogbookService', () => {
  let service: LogbookService;
  let backend: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule, HttpClientTestingModule ],
      providers: [
        ResourcesService,
        LogbookService,
        CookieService
      ]
    });

    backend = TestBed.get(HttpTestingController);
    service = TestBed.get(LogbookService);
  });

  it('launchTraceability should call correct api + get response', () => {
    service.launchTraceability().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/operations/traceability',
      method: 'POST'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

  it('launchTraceabilityUnitLfc should call correct api', () => {
    service.launchTraceabilityUnitLfc().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/lifecycles/units/traceability',
      method: 'POST'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

  it('launchTraceabilityObjectGroupLfc should call correct api', () => {
    service.launchTraceabilityObjectGroupLfc().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/lifecycles/objectgroups/traceability',
      method: 'POST'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

});
