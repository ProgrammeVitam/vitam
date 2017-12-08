import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LogbookOperationComponent } from './logbook-operation.component';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { BreadcrumbService } from "../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../archive-unit/archive-unit.helper";
import { LogbookService } from "../../ingest/logbook.service";
import { VitamResponse } from "../../common/utils/response";
import { Observable } from "rxjs";

const LogbookServiceStub = {
  getResults: () => Observable.of(new VitamResponse())
};

const item = {
    "evId": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
    "evParentId": null,
    "evType": "STP_OP_SECURISATION",
    "evDateTime": "2017-11-08T12:10:03.240",
    "evIdProc": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
    "evTypeProc": "TRACEABILITY",
    "outcome": "STARTED",
    "outDetail": "STP_OP_SECURISATION.STARTED",
    "outMessg": "Début du processus de sécurisation des journaux",
    "agId": "{\"Name\":\"vitam-iaas-app-04\",\"Role\":\"logbook\",\"ServerId\":1381575011,\"SiteId\":1,\"GlobalPlatformId\":173615459}",
    "obId": null,
    "evDetData": "{\"LogType\":\"OPERATION\",\"StartDate\":\"207-11-08T11:53:14.105\",\"EndDate\":\"2017-11-08T12:10:03.240\",\"Hash\":\"mockHash\",\"TimeStampToken\":\"mockTimeStampToken\",\"NumberOfElement\":196,\"FileName\":\"0_LogbookOperation_20171108_121003.zip\",\"Size\":2109403,\"DigestAlgorithm\":\"SHA512\"}",
    "rightsStatementIdentifier": null,
    "agIdApp": null,
    "evIdAppSession": null,
    "evIdReq": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
    "agIdExt": null,
    "obIdReq": null,
    "obIdIn": null,
    "#id": null,
    "#tenant": null,
    "events": [{
        "evId": "aedqaaaaacffskldaajwuak7toeyzfqaaaaq",
        "evParentId": null,
        "evType": "OP_SECURISATION_TIMESTAMP",
        "evDateTime": "2017-11-08T12:10:03.798",
        "evIdProc": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
        "evTypeProc": "TRACEABILITY",
        "outcome": "OK",
        "outDetail": "OP_SECURISATION_TIMESTAMP.OK",
        "outMessg": "Succès de la création du tampon d'horodatage de l'ensemble des journaux",
        "agId": "{\"Name\":\"vitam-iaas-app-04\",\"Role\":\"logbook\",\"ServerId\":1381575011,\"SiteId\":1,\"GlobalPlatformId\":173615459}",
        "obId": null,
        "evDetData": null,
        "rightsStatementIdentifier": null,
        "agIdApp": null,
        "evIdAppSession": null,
        "evIdReq": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
        "agIdExt": null,
        "obIdReq": null,
        "obIdIn": null
    }, {
        "evId": "aedqaaaaacffskldaajwuak7toey6jiaaaaq",
        "evParentId": null,
        "evType": "OP_SECURISATION_STORAGE",
        "evDateTime": "2017-11-08T12:10:04.453",
        "evIdProc": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
        "evTypeProc": "TRACEABILITY",
        "outcome": "OK",
        "outDetail": "OP_SECURISATION_STORAGE.OK",
        "outMessg": "Succès de l'enregistrement des journaux sur les offres de stockage",
        "agId": "{\"Name\":\"vitam-iaas-app-04\",\"Role\":\"logbook\",\"ServerId\":1381575011,\"SiteId\":1,\"GlobalPlatformId\":173615459}",
        "obId": null,
        "evDetData": null,
        "rightsStatementIdentifier": null,
        "agIdApp": null,
        "evIdAppSession": null,
        "evIdReq": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
        "agIdExt": null,
        "obIdReq": null,
        "obIdIn": null
    }]
};

describe('LogbookOperationComponent', () => {
  let component: LogbookOperationComponent;
  let fixture: ComponentFixture<LogbookOperationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LogbookOperationComponent ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: LogbookService, useValue: LogbookServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LogbookOperationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should return operation status', () => {
    expect(LogbookOperationComponent.getOperationStatus(item)).toEqual('En cours');
    item.events.push({
          "evId": "aedqaaaaacffskldaajwuak7toey6pyaaaaq",
          "evParentId": null,
          "evType": "STP_OP_SECURISATION",
          "evDateTime": "2017-11-08T12:10:04.479",
          "evIdProc": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
          "evTypeProc": "TRACEABILITY",
          "outcome": "OK",
          "outDetail": "STP_OP_SECURISATION.OK",
          "outMessg": "Succès du processus de sécurisation des journaux",
          "agId": "{\"Name\":\"vitam-iaas-app-04\",\"Role\":\"logbook\",\"ServerId\":1381575011,\"SiteId\":1,\"GlobalPlatformId\":173615459}",
          "obId": null,
          "evDetData": "{\"LogType\":\"OPERATION\",\"StartDate\":\"2017-11-08T11:53:14.105\",\"EndDate\":\"2017-11-08T12:10:03.240\",\"Hash\":\"mockHash\",\"TimeStampToken\":\"mockTimeStampToken\",\"NumberOfElement\":196,\"FileName\":\"0_LogbookOperation_20171108_121003.zip\",\"Size\":2109403,\"DigestAlgorithm\":\"SHA512\"}",
          "rightsStatementIdentifier": null,
          "agIdApp": null,
          "evIdAppSession": null,
          "evIdReq": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
          "agIdExt": null,
          "obIdReq": null,
          "obIdIn": null
    });
    expect(LogbookOperationComponent.getOperationStatus(item)).toEqual('OK');
  });
});
