import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from "@angular/common/http/testing";
import { FunctionalTestsService } from './functional-tests.service';
import { ResourcesService } from '../../common/resources.service';
import { CookieService } from "angular2-cookie/core";
import { RouterTestingModule } from '@angular/router/testing';

describe('FunctionalTestsService', () => {
  let service: FunctionalTestsService;
  let backend: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule, HttpClientTestingModule ],
      providers: [
        ResourcesService,
        FunctionalTestsService,
        CookieService
      ]
    });

    backend = TestBed.get(HttpTestingController);
    service = TestBed.get(FunctionalTestsService);
  });

  it('getResult should call correct api + get response', () => {
    service.getResults().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/applicative-test',
      method: 'GET'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

  it('getResultDetail should call correct api + get response', () => {
    service.getResultDetail('fileName').subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/applicative-test/fileName',
      method: 'GET'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

  it('launchTests should call correct api + get response', () => {
    service.launchTests().subscribe(response => {
      expect(response).not.toBeNull();
    });

    let response = backend.expectOne({
      url: '/ihm-recette/v1/api/applicative-test',
      method: 'POST'
    });
    response.flush({
      "result": 1
    });

    backend.verify();
  });

});
