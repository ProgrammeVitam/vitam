import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ContextComponent } from './context.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { VitamResponse } from "../../../common/utils/response";
import { DialogService } from "../../../common/dialog/dialog.service";

const ReferentialsServiceStub = {
  getContextById: (id) => Observable.of({'$results': [{
    "_id":"aegqaaaaaahuuzbeabzqqak7aco5r5aaaaaq",
    "Name":"admin-context",
    "Status":true,"EnableControl":false,
    "Permissions":[
      {"_tenant":0,"AccessContracts":[],"IngestContracts":[]}],
    "CreationDate":"2017-10-09T10:11:05.588",
    "LastUpdate":"2017-10-09T10:11:05.589",
    "ActivationDate":null,
    "DeactivationDate":null,
    "SecurityProfile":"admin-security-profile",
    "Identifier":"CT-000001"}]}),
  getTenants : () => Observable.of([0])
};

describe('ContextComponent', () => {
  let component: ContextComponent;
  let fixture: ComponentFixture<ContextComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: {} }
      ],
      declarations: [ ContextComponent ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ContextComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
