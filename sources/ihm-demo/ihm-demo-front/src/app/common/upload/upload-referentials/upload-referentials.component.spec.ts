import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { UploadReferentialsComponent } from './upload-referentials.component';
import { UploadService } from '../upload.service';
import { MessagesUtilsService } from '../../utils/messages-utils.service';

const UploadServiceStub = {
  clearIngest: () => null,
  downloadATR: () => null,
  checkIngestStatus: () => null,
  getUploadState: () => null,
  uploadFile: () => null,
};

const MessagesUtilsServiceStub = {
  getMessage: () => ''
};

describe('UploadReferentialsComponent', () => {
  let component: UploadReferentialsComponent;
  let fixture: ComponentFixture<UploadReferentialsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: UploadService, useValue: UploadServiceStub },
        { provide: MessagesUtilsService, useValue: MessagesUtilsServiceStub }
      ],
      declarations: [ UploadReferentialsComponent ],
      imports: [ RouterTestingModule ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(UploadReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
