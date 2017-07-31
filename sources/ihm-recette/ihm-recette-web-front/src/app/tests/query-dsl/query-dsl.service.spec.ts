import { TestBed, inject, ComponentFixture } from '@angular/core/testing';

import { QueryDslService } from './query-dsl.service';
import { Observable } from 'rxjs/Observable';
import { ResourcesService } from '../../common/resources.service';

import 'rxjs/add/observable/of';

const contractsResponse = [
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
]
const cookies = {};
const ResourcesServiceStub = {
  post: (url, header?: Headers, body?: any) => {
    return Observable.of({
        json : () => ({'$results': contractsResponse})
      })
  },
  getTenant: () => cookies['tenant'],
}

describe('QueryDslService', () => {
  let service: QueryDslService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        QueryDslService,
        {provide: ResourcesService, useValue: ResourcesServiceStub},
      ]
    });
  });

  beforeEach(inject([QueryDslService], (queryDslService: QueryDslService) => {
    service = queryDslService;
  }));

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should have contracts', () => {
    service.getContracts().subscribe(
      (response) => {
        expect(response.length).toEqual(2);
        expect(response[0].Identifier).toEqual('AC-000001')}
    );
  });

  it('should test checkjson function', () => {
    expect(service.checkJson('{}')).toBeTruthy();
    expect(service.checkJson('')).toBeFalsy();
  });
});
