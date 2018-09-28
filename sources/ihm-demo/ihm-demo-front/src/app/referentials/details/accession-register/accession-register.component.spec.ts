import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {Observable} from 'rxjs/Rx';

import {BreadcrumbService} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {LogbookService} from '../../../ingest/logbook.service';
import {AccessionRegisterComponent} from './accession-register.component';
import {ActivatedRoute} from '@angular/router';
import {ErrorService} from '../../../common/error.service';
import {DialogService} from '../../../common/dialog/dialog.service';
import {AccessionRegisterDetail} from './accession-register';

const ReferentialsServiceStub = {
  getFundRegisterDetailById: () => Observable.of({
    'httpCode': 200,
    '$hits': {'total': 2, 'offset': 0, 'limit': 50, 'size': 2},
    '$results': [{
      'objectSize': {'ingested': 12, 'deleted': 0, 'remained': 12},
      '#id': 'aehaaaaaaae2tauiaamukalgccsvjfyaaaaq',
      '#tenant': 0,
      '#version': 0,
      'OriginatingAgency': 'FRAN_NP_009913',
      'SubmissionAgency': 'FRAN_NP_009913',
      'ArchivalAgreement': 'ArchivalAgreement0',
      'EndDate': '2018-09-25T12:12:42.519',
      'StartDate': '2018-09-25T12:12:42.519',
      'LastUpdate': '2018-09-25T12:12:42.519',
      'Status': 'STORED_AND_COMPLETED',
      'TotalObjectGroups': {'ingested': 2, 'deleted': 0, 'remained': 2},
      'TotalUnits': {'ingested': 3, 'deleted': 0, 'remained': 3},
      'TotalObjects': {'ingested': 2, 'deleted': 0, 'remained': 2},
      'ObjectSize': {'ingested': 12, 'deleted': 0, 'remained': 12},
      'Opc': 'aeeaaaaaace2tauiaann4algccsr5ciaaaaq',
      'Opi': 'aeeaaaaaace2tauiaann4algccsr5ciaaaaq',
      'OpType': 'INGEST',
      'Events': [{
        'Opc': 'aeeaaaaaace2tauiaann4algccsr5ciaaaaq',
        'OpType': 'INGEST',
        'Gots': 2,
        'Units': 3,
        'Objects': 2,
        'ObjSize': 12,
        'CreationDate': '2018-09-25T14:12:42.519'
      }],
      'OperationIds': ['aeeaaaaaace2tauiaann4algccsr5ciaaaaq']
    }, {
      'objectSize': {'ingested': 12, 'deleted': 0, 'remained': 12},
      '#id': 'aehaaaaaaae2tauiaamukalgbn4etkaaaaaq',
      '#tenant': 0,
      '#version': 0,
      'OriginatingAgency': 'FRAN_NP_009913',
      'SubmissionAgency': 'FRAN_NP_009913',
      'ArchivalAgreement': 'ArchivalAgreement0',
      'EndDate': '2018-09-24T12:05:24.520',
      'StartDate': '2018-09-24T12:05:24.520',
      'LastUpdate': '2018-09-24T12:05:24.520',
      'Status': 'STORED_AND_COMPLETED',
      'TotalObjectGroups': {'ingested': 2, 'deleted': 0, 'remained': 2},
      'TotalUnits': {'ingested': 3, 'deleted': 0, 'remained': 3},
      'TotalObjects': {'ingested': 2, 'deleted': 0, 'remained': 2},
      'ObjectSize': {'ingested': 12, 'deleted': 0, 'remained': 12},
      'Opc': 'aeeaaaaaace2tauiaann4algbn4bcpiaaaaq',
      'Opi': 'aeeaaaaaace2tauiaann4algbn4bcpiaaaaq',
      'OpType': 'INGEST',
      'Events': [{
        'Opc': 'aeeaaaaaace2tauiaann4algbn4bcpiaaaaq',
        'OpType': 'INGEST',
        'Gots': 2,
        'Units': 3,
        'Objects': 2,
        'ObjSize': 12,
        'CreationDate': '2018-09-24T14:05:24.520'
      }],
      'OperationIds': ['aeeaaaaaace2tauiaann4algbn4bcpiaaaaq']
    }],
    '$facetResults': [],
    '$context': {
      '$query': {'$and': [{'$eq': {'OriginatingAgency': 'FRAN_NP_009913'}}]},
      '$filter': {'$orderby': {'EndDate': -1}},
      '$projection': {}
    }
  }),
  getAccessionRegisterSymbolic: () => Observable.of({'$results': [{}]}),
  getFundRegisterById: () => Observable.of({
    '$hits': {'total': 1, 'offset': 0, 'limit': 125, 'size': 10000},
    '$results': [{}]
  })
};

const LogbookServiceStub = {
  getResults: () => Observable.of({'$results': [{}]}),
  getDetails: () => Observable.of({'$results': [{}]}),
};

describe('AccessionRegisterComponent', () => {
  let component: AccessionRegisterComponent;
  let fixture: ComponentFixture<AccessionRegisterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [
        BreadcrumbService,
        ErrorService,
        {provide: ReferentialsService, useValue: ReferentialsServiceStub},
        {provide: LogbookService, useValue: LogbookServiceStub},
        {provide: ActivatedRoute, useValue: {params: Observable.of({id: 'Mock_Service'})}},
        {provide: DialogService, useValue: {}}
      ],
      declarations: [AccessionRegisterComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AccessionRegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should adapt breadcrumb regarding provided url', () => {
    component.updateBreadcrumb('all');
    expect(component.newBreadcrumb.length).toBe(4);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 2].label)
      .toBe('Détail du service agent Mock_Service');
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
      .toBe('Détail du fonds Mock_Service');

    component.updateBreadcrumb('accessionRegister');
    expect(component.newBreadcrumb.length).toBe(4);
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 2].label)
      .toBe('Détail du service producteur Mock_Service');
    expect(component.newBreadcrumb[component.newBreadcrumb.length - 1].label)
      .toBe('Détail du fonds Mock_Service');
  });

  it('should show optional col if selected', () => {
    // Given
    component.extraSelectedCols.push('batman');

    // When
    const showCol = component.showOptionalCol('batman');

    // Then
    expect(showCol).toBeTruthy();
  });

  it('should call pageOnInit on start up', () => {
    // Given
    let callDetail = false;
    component.getDetail = () => callDetail = true;

    let callUpdate = false;
    component.updateBreadcrumb = () => callUpdate = true;

    let callPaginate = false;
    component.paginate = () => callPaginate = true;

    // When
    component.pageOnInit();

    // Then
    expect(callDetail).toBeTruthy();
    expect(callPaginate).toBeTruthy();
    expect(callUpdate).toBeTruthy();
  });
});
