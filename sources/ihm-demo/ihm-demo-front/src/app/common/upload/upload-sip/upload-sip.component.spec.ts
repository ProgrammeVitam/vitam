import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { UploadSipComponent } from './upload-sip.component';
import { UploadService } from '../upload.service';
import { AuthenticationService } from '../../../authentication/authentication.service';

const UploadServiceStub = {
  uploadReferentials: (url, file) => Observable.of('OK')
};

describe('UploadSipComponent', () => {
  let component: UploadSipComponent;
  let fixture: ComponentFixture<UploadSipComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UploadService, useValue: UploadServiceStub },
        { provide: AuthenticationService, useValue : {isAdmin : () => {return true;}}}
      ],
      declarations: [ UploadSipComponent ],
      imports: [ RouterTestingModule ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UploadSipComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
