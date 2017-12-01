import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { LifecycleComponent } from './lifecycle.component';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { LogbookService } from "../../../ingest/logbook.service";
import { Observable } from "rxjs/Rx";
import { VitamResponse } from "../../../common/utils/response";
import { ActivatedRoute } from "@angular/router";

const response =
  {
    "httpCode": 200,
    "$hits": {
      "total": 1,
      "offset": 0,
      "limit": 10000,
      "size": 1,
      "scrollId": null
    },
    "$results": [{
      "evId": "aedqaaaaachxyyj7aaaviak7s5u6f5aaaabq",
      "evParentId": null,
      "evType": "LFC.LFC_CREATION",
      "evDateTime": "2017-11-07T16:56:59.892",
      "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
      "evTypeProc": "INGEST",
      "outcome": "OK",
      "outDetail": "LFC.LFC_CREATION.OK",
      "outMessg": "Succès de la création du journal du cycle de vie",
      "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
      "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
      "evDetData": null,
      "rightsStatementIdentifier": null,
      "_id": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
      "_tenant": 0,
      "_v": 0,
      "events": [{
        "evId": "aedqaaaaachxyyj7aaaviak7s5u6f5iaaaaq",
        "evParentId": null,
        "evType": "LFC.CHECK_MANIFEST",
        "evDateTime": "2017-11-07T16:56:59.893",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.CHECK_MANIFEST.OK",
        "outMessg": "Succès de la vérification de la cohérence du bordereau de transfert",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": "{ }",
        "rightsStatementIdentifier": null
      }, {
        "evId": "aedqaaaaachxyyj7aaaviak7s5u6f5iaaaba",
        "evParentId": "aedqaaaaachxyyj7aaaviak7s5u6f5iaaaaq",
        "evType": "LFC.CHECK_MANIFEST.LFC_CREATION",
        "evDateTime": "2017-11-07T16:56:59.893",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.CHECK_MANIFEST.LFC_CREATION.OK",
        "outMessg": "Succès de la création du journal du cycle de vie",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": null,
        "rightsStatementIdentifier": null
      }, {
        "evId": "aedqaaaaachxyyj7aaaviak7s5u6yuaaaaaq",
        "evParentId": null,
        "evType": "LFC.CHECK_UNIT_SCHEMA",
        "evDateTime": "2017-11-07T16:57:02.363",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.CHECK_UNIT_SCHEMA.OK",
        "outMessg": "Succès de la vérification globale de l'unité archivistique",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": "{}",
        "rightsStatementIdentifier": null
      }, {
        "evId": "aedqaaaaachxyyj7aaaviak7s5u6ziqaaaaq",
        "evParentId": null,
        "evType": "LFC.UNITS_RULES_COMPUTE",
        "evDateTime": "2017-11-07T16:57:02.375",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.UNITS_RULES_COMPUTE.OK",
        "outMessg": "Succès du calcul des dates d'échéance",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": "{}",
        "rightsStatementIdentifier": null
      }, {
        "evId": "aedqaaaaachxyyj7aaaviak7s5u7lpiaaaaq",
        "evParentId": null,
        "evType": "LFC.UNIT_METADATA_INDEXATION",
        "evDateTime": "2017-11-07T16:57:04.734",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.UNIT_METADATA_INDEXATION.OK",
        "outMessg": "Succès de l'indexation des métadonnées de l'unité archivistique",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": "{}",
        "rightsStatementIdentifier": null
      }, {
        "evId": "aedqaaaaachxyyj7aaaviak7s5u77aqaaaaq",
        "evParentId": null,
        "evType": "LFC.UNIT_METADATA_STORAGE",
        "evDateTime": "2017-11-07T16:57:07.288",
        "evIdProc": "aedqaaaaacheyplmaac5wak7s5u5yuyaaaaq",
        "evTypeProc": "INGEST",
        "outcome": "OK",
        "outDetail": "LFC.UNIT_METADATA_STORAGE.OK",
        "outMessg": "Succès de l'écriture des métadonnées de l'unité archivistique sur les offres de stockage",
        "agId": "{\"Name\":\"880f5ac32d9a\",\"Role\":\"worker\",\"ServerId\":1065115967,\"SiteId\":1,\"GlobalPlatformId\":259809599}",
        "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq",
        "evDetData": "{\"FileName\":\"aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq.json\",\"Algorithm\":\"SHA-512\",\"MessageDigest\":\"b1e084b252ce35bdc1c36b240ba425e9c3b903a86e4df447dac5dd260e10ac977ac51ae2e140e5100566722809b61c9649060fb2128d1d6478530d5193fb553b\",\"Offers\":\"localhost\"}",
        "rightsStatementIdentifier": null
      }]
    }],
    "$context": {
      "$query": {
        "$eq": {
          "obId": "aeaqaaaaaahxyyj7aaaviak7s5u6eeqaaaaq"
        }
      },
      "$filter": {},
      "$projection": {}
    }
  };

const LogbookServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse),
  getLifecycleLogbook: (id) => Observable.of(response)
};
let mockResults = [{
  "Feature" : "initialisation",
  "Description" : "Sc\u00E9nario 0 import des regles des getions et formats",
  "Errors" : [ ],
  "Ok" : true
}];
const ActivatedRouteStub = {
  'snapshot': {
    'url': 'search/archiveUnit/aebaaaaaaahxyyj7aaaviak7s5u6dvqaaaaq/objectgrouplifecycle'
  },
  paramMap: Observable.of({
    get : (id) => Observable.of("aebaaaaaaahxyyj7aaaviak7s5u6dvqaaaaq"),
    switchMap: () => Observable.of({
      Reports : mockResults,
      NumberOfTestOK : 1,
      NumberOfTestKO : 0
    })
  })
};

describe('LifecycleComponent', () => {
  let component: LifecycleComponent;
  let fixture: ComponentFixture<LifecycleComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      declarations: [ LifecycleComponent ],
      providers : [
        BreadcrumbService,
        ArchiveUnitHelper,
        { provide: LogbookService, useValue: LogbookServiceStub },
        { provide: ActivatedRoute, useValue: ActivatedRouteStub }
      ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LifecycleComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should adapt panel header & lifecycleType variable regarding lifecycle type', () => {
    component.setObjectVariables('unitlifecycle');
    expect(component.panelHeader)
        .toBe('Journal du cycle de vie de l\'unité archivistique');
    expect(component.lifecycleType).toEqual('UNIT');

    component.setObjectVariables('objectgrouplifecycle');
    expect(component.panelHeader)
        .toBe('Journal du cycle de vie du groupe d\'objets techniques');
    expect(component.lifecycleType).toEqual('OBJECTGROUP');
  });
});
