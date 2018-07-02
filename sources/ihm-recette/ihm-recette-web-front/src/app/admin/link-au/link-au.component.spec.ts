import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LinkAuComponent } from './link-au.component';
import { QueryDslService } from '../../tests/query-dsl/query-dsl.service';
import { ResourcesService } from '../../common/resources.service';
import { TenantService } from '../../common/tenant.service';
import { BreadcrumbService } from '../../common/breadcrumb.service';
import { Observable } from 'rxjs/Observable';
import { LinkAuService } from './link-au.service';
import { NO_ERRORS_SCHEMA } from "@angular/core";

const cookies = {};

const ResourcesServiceStub = {
  post: () => Observable.of('OK'),
  getTenant: () => cookies['tenant']
};

const QueryDslServiceStub = {
  getContracts: () => Observable.of([
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
  ])
};
const TenantServiceStub = {
  getState: () => Observable.of('0')
};

const LinkAuServiceStub = {
  updateLinks: () => Observable.of('OK')
};

describe('LinkAuComponent', () => {
  let component: LinkAuComponent;
  let fixture: ComponentFixture<LinkAuComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LinkAuComponent ],
      providers: [
        BreadcrumbService,
        {provide: ResourcesService, useValue: ResourcesServiceStub},
        {provide: LinkAuService, useValue: LinkAuServiceStub},
        {provide: QueryDslService, useValue: QueryDslServiceStub},
        {provide: TenantService, useValue: TenantServiceStub}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LinkAuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
