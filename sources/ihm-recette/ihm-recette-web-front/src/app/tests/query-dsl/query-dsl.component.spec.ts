import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { QueryDSLComponent } from './query-dsl.component';
import {QueryDslService} from './query-dsl.service';
import {DropdownModule, InputTextModule, SelectItem} from 'primeng/primeng';
import {FormsModule} from '@angular/forms';
import {BreadcrumbService} from '../../common/breadcrumb.service';
import {ResourcesService} from '../../common/resources.service';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/observable/of';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {Response, ResponseOptions, Headers} from '@angular/http';
import {TenantService} from "../../common/tenant.service";


const cookies = {};
const ResourcesServiceStub = {
  post: (url, header?: Headers, body?: any) => Observable.of('OK'),
  getTenant: () => cookies['tenant']
};
const QueryDslServiceStub = {
  getContracts: (api) => Observable.of([
    {
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
    },
    {
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
    }
  ]),
  checkJson: (jsonRequest: string) => {
      return !!jsonRequest;
  },
  executeRequest: (query, contractId: string, requestedCollection: string,
                   requestMethod: string, xAction: string, objectId: string) => {
    if (contractId) {
      return Observable.of(
        new Response(
          new ResponseOptions({body: JSON.stringify(
            {_body: '{httpCode: 200, $result: {}, $context: {}}', status: 200, ok: true, statusText: 'OK'})})
        )
      )
    } else {
      return Observable.of(
        new Response(
          new  ResponseOptions({body: JSON.stringify(
            {_body: '<html></html>', status: 400, ok: false, statusText: 'Bad Request'})})
        )
      )
    }
  }
};
const TenantServiceStub = {
  getState: () => Observable.of('0')
};

describe('QueryDSLComponent', () => {
  let component: QueryDSLComponent;
  let fixture: ComponentFixture<QueryDSLComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ QueryDSLComponent ],
      providers: [
        QueryDslService,
        BreadcrumbService,
        {provide: ResourcesService, useValue: ResourcesServiceStub},
        {provide: QueryDslService, useValue: QueryDslServiceStub},
        {provide: TenantService, useValue: TenantServiceStub}
      ],
      imports: [
        InputTextModule,
        FormsModule,
        DropdownModule,
        BrowserAnimationsModule
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(QueryDSLComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should have contracts and check first value', () => {
    component.getContracts();
    const firstContract: SelectItem = component.contractsList[0];
    expect(firstContract.label).toEqual('Access Contract 1');
    expect(component.contractsList.length).toEqual(2);
  });

  it('should check component checkjson function', () => {
    component.jsonRequest = '{}';
    component.checkJson();
    expect(component.validRequest.valid).toEqual('valide');

    component.jsonRequest = '';
    component.checkJson();
    expect(component.validRequest.valid).toEqual('non valide');
  });

  it('should return different responses when executing requests', () => {
    component.selectedContract = {
      '_id': 'aefqaaaaaaecmnbgaay3wak5txnix2iaaaaq',
      '_tenant': 0,
      'Name': 'Access Contract 1',
      'Identifier': 'AC-000001',
      'Description': 'Contrat Acces 1',
      'Status': 'ACTIVE',
      'CreationDate': '2016-12-10T00:00',
      'LastUpdate': '2017-08-01T12:52:11.880',
      'ActivationDate': '2016-12-10T00:00',
      'DeactivationDate': '2016-12-10T00:00'
    };
    component.sendRequest();
    let response: any = component.requestResponse;
    let parsedResponse = JSON.parse(response);
    expect(parsedResponse.ok).toBeTruthy();

    component.selectedContract.Name = null;
    component.sendRequest();
    response = component.requestResponse;
    parsedResponse = JSON.parse(response);
    expect(parsedResponse.ok).toBeFalsy();
  });
});
