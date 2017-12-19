import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";

import { ContextComponent } from './context.component';
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DialogService } from "../../../common/dialog/dialog.service";
import {VitamResponse} from "../../../common/utils/response";
import {ErrorService} from "../../../common/error.service";

const context1 = {
  "#id":"aegqaaaaaahuuzbeabzqqak7aco5r5aaaaaq",
  "#tenant": '0',
  "Name":"admin-context",
  "Description": "Context de test",
  "Status":true,"EnableControl":false,
  "Permissions":[
    {"#tenant":0,"AccessContracts":[],"IngestContracts":[]}],
  "CreationDate":"2017-10-09T10:11:05.588",
  "LastUpdate":"2017-10-09T10:11:05.589",
  "ActivationDate":null,
  "DeactivationDate":null,
  "SecurityProfile":"admin-security-profile",
  "Identifier":"CT-000001"
};
const context2 = {
  "#id":"aegqaaaaaahuuzbeabzqqak7aco5r5aaaaaq",
  "#tenant": '0',
  "Name":"admin-context",
  "Description": "Context de test",
  "Status":true,"EnableControl":false,
  "Permissions":[
    {"#tenant":0,"AccessContracts":['ContratTNR'],"IngestContracts":[]}],
  "CreationDate":"2017-10-09T10:11:05.588",
  "LastUpdate":"2017-10-09T10:11:05.589",
  "ActivationDate":null,
  "DeactivationDate":null,
  "SecurityProfile":"admin-security-profile",
  "Identifier":"CT-000001"
};

const ReferentialsServiceStub = {
  getContextById: (id) => Observable.of({'$results': [context1]}),
  updateDocumentById: (id) => Observable.of(new VitamResponse()),
  getTenants : () => Observable.of([0])
};

const DialogServiceStub = {
  displayMessage: (message : string, header : string) => {}
};

describe('ContextComponent', () => {
  let component: ContextComponent;
  let fixture: ComponentFixture<ContextComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        BreadcrumbService,
        ErrorService,
        { provide: ReferentialsService, useValue: ReferentialsServiceStub },
        { provide: DialogService, useValue: DialogServiceStub }
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

  it('should not call update while no change is made', () => {
    let spy: any = spyOn(component.referentialsService, 'updateDocumentById').and.callThrough();

    component.context = context1;
    component.modifiedContext = component.context;
    component.updatedFields = {};

    component.saveUpdate();

    expect(spy.calls.count()).toBe(0);
  });

  it('should call update if permissions are changed', () => {
    let spy: any = spyOn(component.referentialsService, 'updateDocumentById').and.callThrough();

    component.context = context1;
    component.modifiedContext = context2;
    component.updatedFields = {};

    component.saveUpdate();

    expect(spy.calls.count()).toBe(1);

  });
});
