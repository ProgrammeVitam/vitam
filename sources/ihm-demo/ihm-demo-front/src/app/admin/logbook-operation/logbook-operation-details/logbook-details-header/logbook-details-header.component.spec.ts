import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookDetailsHeaderComponent } from './logbook-details-header.component';
import { LogbookService } from "../../../../ingest/logbook.service";
import { Observable } from "rxjs";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { LogbookHelperService } from "../../../../common/logbook-operation-events/logbook-helper.service";
import { Logbook } from "../../../../common/utils/logbook";

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
  'events': [
    {
      "evId": "aecaaaaaacgwwmrhaba2uak7kpci5niaaaaq",
      "evParentId": null,
      "evType": "STP_IMPORT_ACCESS_CONTRACT",
      "evDateTime": "2017-10-25T13:41:51.422",
      "evDetData": "{\n  \"accessContractCheck\" : \"The contract ContratTNR already exists in database,The contract contract_with_field_EveryDataObjectVersion already exists in database\"\n}",
      "evIdProc": "aecaaaaaacgwwmrhaba2uak7kpci5niaaaaq",
      "evTypeProc": "MASTERDATA",
      "outcome": "KO",
      "outDetail": "STP_IMPORT_ACCESS_CONTRACT.KO",
      "outMessg": "Échec de l'import du contrat d'accès",
      "agId": "{\"Name\":\"vitam-iaas-app-02\",\"Role\":\"functional-administration\",\"ServerId\":1298870823,\"SiteId\":1,\"GlobalPlatformId\":225128999}",
      "evIdReq": "aecaaaaaacgwwmrhaba2uak7kpci5niaaaaq",
      "obId": "aecaaaaaacgwwmrhaba2uak7kpci5niaaaaq",
      'rightsStatementIdentifier':null
    }
  ],
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

describe('LogbookDetailsHeaderComponent', () => {
  let component: LogbookDetailsHeaderComponent;
  let fixture: ComponentFixture<LogbookDetailsHeaderComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookDetailsHeaderComponent ],
      providers: [
        LogbookHelperService,
        { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookDetailsHeaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
