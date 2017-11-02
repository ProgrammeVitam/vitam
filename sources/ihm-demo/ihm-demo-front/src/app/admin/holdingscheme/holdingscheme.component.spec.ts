import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TabViewModule, RadioButtonModule, ProgressBarModule } from 'primeng/primeng';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { CookieService } from 'angular2-cookie/core';
import { FormsModule }   from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { UploadSipComponent } from '../../common/upload/upload-sip/upload-sip.component';
import { ResourcesService } from '../../common/resources.service';
import { UploadService } from '../../common/upload/upload.service';
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { HoldingschemeComponent } from './holdingscheme.component';
import { AuthenticationService } from '../../authentication/authentication.service';

const UploadServiceStub = {
  uploadFile: (username : string, password : string) => {
    return Observable.of('');
  },
  getUploadState : () => {
    return Observable.of(80);
  },
  clearIngest : () => {},
  downloadATR : () => {},
  checkIngestStatus: () => {
    return Observable.of({ status : 200 });
  }
};

describe('HoldingschemeComponent', () => {
  let component: HoldingschemeComponent;
  let fixture: ComponentFixture<HoldingschemeComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ HoldingschemeComponent, UploadSipComponent ],
      imports: [ TabViewModule, FormsModule, RadioButtonModule, ProgressBarModule,
        BrowserAnimationsModule, RouterTestingModule],
      providers: [
        BreadcrumbService,
        ResourcesService,
        CookieService,
        { provide: UploadService, useValue : UploadServiceStub },
        { provide: AuthenticationService, useValue : {isAdmin : () => {return true;}}}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(HoldingschemeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
