import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from "@angular/core";

import {TraceabilityOperationDetailsComponent} from './traceability-operation-details.component';
import {BreadcrumbService} from "../../../common/breadcrumb.service";
import {TraceabilityOperationService} from "../traceability-operation.service";
import {ActivatedRoute} from "@angular/router";
import {Observable} from "rxjs/Observable";
import {ObjectsService} from "../../../common/utils/objects.service";
import {ArchiveUnitHelper} from "../../../archive-unit/archive-unit.helper";
import {LogbookService} from "../../../ingest/logbook.service";

const traceabilityDetails = {
  "httpCode": 200,
  "$hits": {"total": 1, "offset": 0, "limit": 10000, "size": 1, "scrollId": null},
  "$results": [{
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
    "evDetData": "{\"LogType\":\"OPERATION\",\"StartDate\":\"2017-11-08T11:53:14.105\",\"EndDate\":\"2017-11-08T12:10:03.240\",\"Hash\":\"PyfNxgyOFhqCaENi0DIQ4Dlvte/EPlu8QUFwBFGJj3hDv6wppOWEmrBqV1u+dFJrxpvmRIkFhuTAL954WHYtKw==\",\"TimeStampToken\":\"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\",\"NumberOfElements\":196,\"FileName\":\"0_LogbookOperation_20171108_121003.zip\",\"Size\":2109403,\"DigestAlgorithm\":\"SHA512\"}",
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
    }, {
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
      "evDetData": "{\"LogType\":\"OPERATION\",\"StartDate\":\"2017-11-08T11:53:14.105\",\"EndDate\":\"2017-11-08T12:10:03.240\",\"Hash\":\"PyfNxgyOFhqCaENi0DIQ4Dlvte/EPlu8QUFwBFGJj3hDv6wppOWEmrBqV1u+dFJrxpvmRIkFhuTAL954WHYtKw==\",\"TimeStampToken\":\"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\",\"NumberOfElements\":196,\"FileName\":\"0_LogbookOperation_20171108_121003.zip\",\"Size\":2109403,\"DigestAlgorithm\":\"SHA512\"}",
      "rightsStatementIdentifier": null,
      "agIdApp": null,
      "evIdAppSession": null,
      "evIdReq": "aecaaaaaacffskldaajwuak7toeyuxyaaaaq",
      "agIdExt": null,
      "obIdReq": null,
      "obIdIn": null
    }]
  }],
  "$context": {"$query": {}, "$filter": {}, "$projection": {}}
};

const timestampDetails = {
  genTime: 'Time',
  signerCertIssuer: 'CA,TEST'
};

const TraceabilityOperationServiceStub = {
  getDetails() {
    return Observable.of(traceabilityDetails);
  },

  extractTimeStampInformation() {
    return Observable.of(timestampDetails);
  },

  checkTraceabilityOperation() {
    return Observable.of(traceabilityDetails);
  }

};

const LogbookServiceStub = {};

describe('TraceabilityOperationDetailsComponent', () => {
  let component: TraceabilityOperationDetailsComponent;
  let fixture: ComponentFixture<TraceabilityOperationDetailsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [TraceabilityOperationDetailsComponent],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        ObjectsService,
        {provide: TraceabilityOperationService, useValue: TraceabilityOperationServiceStub},
        {provide: LogbookService, useValue: LogbookServiceStub},
        {provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})}}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TraceabilityOperationDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should extract correct data', () => {
    component.id='aecaaaaaacffskldaajwuak7toeyuxyaaaaq';
    component.extractData(traceabilityDetails.$results[0]);
    expect(component.item.logType).toBe('OPERATION');
    expect(component.item.startDate).toBe('2017-11-08T11:53:14.105');
    expect(component.item.endDate).toBe('2017-11-08T12:10:03.240');
    expect(component.item.numberOfElements).toBe(196);
    expect(component.item.digestAlgorithm).toBe('SHA512');
    expect(component.item.fileName).toBe('0_LogbookOperation_20171108_121003.zip');
    expect(component.item.fileSize).toBe(2109403);
    expect(component.item.evId).toBe('aecaaaaaacffskldaajwuak7toeyuxyaaaaq');
  });
});
