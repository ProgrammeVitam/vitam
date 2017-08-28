import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { UploadSipComponent } from './upload-sip.component';
import { UploadService } from '../upload.service';

const UploadServiceStub = {
  uploadReferentials: (url, file) => Observable.of('OK')
};

describe('UploadSipComponent', () => {
  let component: UploadSipComponent;
  let fixture: ComponentFixture<UploadSipComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UploadService, useValue: UploadServiceStub }
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
