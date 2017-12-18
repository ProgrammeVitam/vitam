import { TestBed, inject } from '@angular/core/testing';

import { ErrorService } from './error.service';
import {DialogService} from "./dialog/dialog.service";
import {RouterTestingModule} from "@angular/router/testing";

describe('ErrorService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        ErrorService,
        DialogService
      ]
    });
  });

  it('should be created', inject([ErrorService], (service: ErrorService) => {
    expect(service).toBeTruthy();
  }));

  it( 'should handle 404 errors', inject([ErrorService], (service: ErrorService) => {
    let spy: any = spyOn(service.dialogService, 'displayMessage').and.callFake((arg1, arg2) => {});

    let error = {
      status: 500
    };
    service.handle404Error(error);
    expect(spy.calls.count()).toBe(0);

    error.status = 404;
    service.handle404Error(error);
    expect(spy.calls.count()).toBe(1);
    expect(spy.calls.allArgs()).toEqual([[
        'La page à laquelle vous souhaitez accéder n\'existe pas. Vous avez été redirigé vers la page d\'accueil.',
        'Page non disponible'
      ]]
    );
  }))
});
