import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";

import { LogbookOperationEventsComponent } from './logbook-operation-events.component';
import {LogbookService} from "../../ingest/logbook.service";
import {Observable} from "rxjs";
import {Logbook} from "../utils/logbook";
import {LogbookHelperService} from "./logbook-helper.service";

const results = [{
  '#id':null,
  '#tenant':0,
  'agId':'{"Name":"vitam-iaas-app-02","Role":"functional-administration","ServerId":1617247585,"SiteId":1,"GlobalPlatformId":140852577}',
  'agIdApp':"CT-000001",
  'agIdExt':null,
  'evDateTime':"2017-10-20T01:55:21.439",
  'evDetData':null,
  'evId':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
  'evIdAppSession':null,
  'evIdProc':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
  'evIdReq':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
  'evParentId':null,
  'evType':"STP_IMPORT_ACCESS_CONTRACT",
  'evTypeProc':"MASTERDATA",
  'events': {
    'agId':'{"Name":"vitam-iaas-app-02","Role":"functional-administration","ServerId":1617247585,"SiteId":1,"GlobalPlatformId":140852577}',
    'agIdApp':null,
    'agIdExt':null,
    'evDateTime':"2017-10-20T01:55:21.451",
    'evDetData':'{↵  "accessContractCheck" : "The contract ContratTNR already exists in database,The contract contract_with_field_EveryDataObjectVersion already exists in database"↵}',
    'evId':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
    'evIdAppSession':null,
    'evIdProc':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
    'evIdReq':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
    'evParentId':null,
    'evType':"STP_IMPORT_ACCESS_CONTRACT",
    'evTypeProc':"MASTERDATA",
    'obId':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
    'obIdIn':null,
    'obIdReq':null,
    'outDetail':"STP_IMPORT_ACCESS_CONTRACT.KO",
    'outMessg':"Échec de l'import du contrat d'accès",
    'outcome':"KO",
    'rightsStatementIdentifier':null
  },
  'obId':"aecaaaaaacegkplbabgi4ak7g567bxyaaaaq",
  'obIdIn':null,
  'obIdReq':null,
  'outDetail':"STP_IMPORT_ACCESS_CONTRACT.STARTED",
  'outMessg':"Début de l'import du contrat d'accès",
  'outcome':"STARTED",
  'rightsStatementIdentifier':null
}];

const LogbookServiceStub = {
  getDetails: () => Observable.of({'$results': results})
};

describe('LogbookOperationEventsComponent', () => {
  let component: LogbookOperationEventsComponent;
  let fixture: ComponentFixture<LogbookOperationEventsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationEventsComponent ],
      providers: [
          LogbookHelperService,
          { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookOperationEventsComponent);
    component = fixture.componentInstance;
    component.results = {$results: [{events: []}]};
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
