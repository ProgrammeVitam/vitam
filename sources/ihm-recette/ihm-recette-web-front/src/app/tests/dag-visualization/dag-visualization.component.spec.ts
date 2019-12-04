import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { FormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DropdownModule, InputTextModule, SelectItem, ChipsModule } from 'primeng/primeng';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { ResourcesService } from '../../common/resources.service';
import { TenantService } from "../../common/tenant.service";
import { EggService } from "../../common/egg/egg.service";
import { QueryDslService } from '../query-dsl/query-dsl.service';
import { DagVisualizationComponent } from './dag-visualization.component';
import { VisModule, VisNetworkService } from 'ng2-vis';
import { HttpHeaders } from "@angular/common/http";

const ResourcesServiceStub = {
  post: (url, header?: HttpHeaders, body?: any) => Observable.of('OK'),
  getTenant: () => 0
};

const contract1 = {
  '_id': 'aefqaaaaaaecmnbgaay3wak5txnix2iaaaaq',
  '_tenant': 0,
  'Name': 'Access Contract 1',
  'Identifier': 'AC-000001',
  'Description': 'Contrat Acces 1',
  'Status': 'ACTIVE',
  'CreationDate': '2016-12-10T00:00',
  'LastUpdate': '2017-08-01T12:52:11.880',
  'ActivationDate': '2016-12-10T00:00',
  'DeactivationDate': '2016-12-10T00:00',
  'DataObjectVersion': null,
  'OriginatingAgencies': ['BBBBB', 'FRAAA', 'CCCCC'],
  'WritingPermission': false,
  'EveryOriginatingAgency': false,
  'EveryDataObjectVersion': false
};
const contract2 = {
  '_id': 'aefqaaaaaaecmnbgaay3wak5txnix2aaaaba',
  '_tenant': 0,
  'Name': 'Access Contract 2',
  'Identifier': 'AC-000002',
  'Description': 'Contrat Acces 2',
  'Status': 'ACTIVE',
  'CreationDate': '2016-12-10T00:00',
  'LastUpdate': '2017-08-01T12:52:11.880',
  'ActivationDate': '2016-12-10T00:00',
  'DeactivationDate': '2016-12-10T00:00',
  'DataObjectVersion': null,
  'OriginatingAgencies': ['BBBBB', 'FRAAA', 'CCCCC'],
  'WritingPermission': false,
  'EveryOriginatingAgency': false,
  'EveryDataObjectVersion': false
};
const QueryDslServiceStub = {
  getContracts: (api) => Observable.of([
    contract1,
    contract2
  ]),
  checkJson: (jsonRequest: string) => {
    return !!jsonRequest;
  },
  executeRequest: (query, contractId: string, requestedCollection: string,
    requestMethod: string, xAction: string, objectId: string) => {
    if (contractId) {
      return Observable.of({
        httpCode: 200,
        $result: {},
        $context: {}
      }
      )
    } else {
      return Observable.of({
        httpCode: 401,
        code: "020100",
        context: "External Access",
        state: "Input / Output",
        message: "Access external client error in selectUnits method.",
        description: "Access by Contract Exception"
      })
    }
  }
};
const TenantServiceStub = {
  getState: () => Observable.of('0')
};
const EggServiceStub = {
  getState: () => false
};

describe('DagVisualizationComponent', () => {
  let component: DagVisualizationComponent;
  let fixture: ComponentFixture<DagVisualizationComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [DagVisualizationComponent],
      providers: [
        QueryDslService,
        BreadcrumbService,
        VisNetworkService,
        { provide: ResourcesService, useValue: ResourcesServiceStub },
        { provide: QueryDslService, useValue: QueryDslServiceStub },
        { provide: TenantService, useValue: TenantServiceStub },
        { provide: EggService, useValue: EggServiceStub }
      ],
      imports: [
        InputTextModule,
        FormsModule,
        DropdownModule,
        BrowserAnimationsModule,
        VisModule,
        ChipsModule
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DagVisualizationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should have contracts and check second value', () => {
    let spy: any = spyOn(component.queryDslService, 'getContracts').and.callThrough();
    component.getContracts();
    const secondContract: SelectItem = component.contractsList[1];
    expect(secondContract.label).toEqual('Access Contract 2');
    expect(component.contractsList.length).toEqual(2);
  });

  it('should be used emptyJsonRequest and no display dag when no contract selected', () => {
    let spyQueryDslExecRequest: any = spyOn(component.queryDslService, 'executeRequest').and.callThrough();
    let spyDisplayDag: any = spyOn(component, 'displayDag').and.callThrough();
    component.sendRequest();
    let emptyJsonRequest = {
      $roots: [],
      $query: [{ $in: {} }],
      $projection: {}
    };
    expect(spyQueryDslExecRequest).toHaveBeenCalledWith(emptyJsonRequest, null, 'UNIT', 'GET', 'GET', null);
    expect(spyDisplayDag).toHaveBeenCalledTimes(0);
  });

  it('should be used jsonRequest with operationId and called display dag when contract selected', () => {
    let spyQueryDslExecRequest: any = spyOn(component.queryDslService, 'executeRequest').and.callThrough();
    let spyDisplayDag: any = spyOn(component, 'displayDag').and.callThrough();
    component.selectedContract = contract1;
    component.operationIds = ['opId'];
    component.sendRequest();
    let correctJsonRequest = {
      $roots: [],
      $query: [{ $in: {} }],
      $projection: {}
    };
    correctJsonRequest.$query[0].$in["#operations"] = ['opId'];
    expect(spyQueryDslExecRequest).toHaveBeenCalledWith(correctJsonRequest, contract1.Identifier, 'UNIT', 'GET', 'GET', null);
    expect(spyDisplayDag).toHaveBeenCalledTimes(1);
  });
});
