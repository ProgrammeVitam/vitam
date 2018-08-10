import {AccessionRegisterSymbolicComponent} from './accession-register-symbolic.component';
import {TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {BreadcrumbService} from '../../../common/breadcrumb.service';
import {ErrorService} from '../../../common/error.service';
import {ReferentialsService} from '../../referentials.service';
import {LogbookService} from '../../../ingest/logbook.service';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs';
import {DialogService} from '../../../common/dialog/dialog.service';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import * as chartjs from 'chart.js';

describe('AccessionRegisterSymbolicComponent', () => {
  it('should run the component', () => {
    // Given
    configureTest();

    // When
    const component = TestBed.createComponent(AccessionRegisterSymbolicComponent).componentInstance;

    // Then
    expect(component).toBeTruthy();
  });

  it('should get accession register symbolic by date', () => {
    // Given
    const accessionRegisterSymbolic = {ObjectGroup: 1, ArchiveUnit: 1, BinaryObject: 1, BinaryObjectSize: 1, CreationDate: new Date().toISOString()};

    const referentialService = {getAccessionRegisterSymbolicByDate: () => Observable.of({$results: [accessionRegisterSymbolic]})};
    configureTest(referentialService);

    const component = TestBed.createComponent(AccessionRegisterSymbolicComponent).componentInstance;

    // When
    component.getAccessionRegisterSymbolic();

    // Then
    expect(component.ObjectGroup.data).toContain({y: accessionRegisterSymbolic.ObjectGroup, x: new Date(accessionRegisterSymbolic.CreationDate)});
    expect(component.ArchiveUnit.data).toContain({y: accessionRegisterSymbolic.ArchiveUnit, x: new Date(accessionRegisterSymbolic.CreationDate)});
    expect(component.BinaryObject.data).toContain({y: accessionRegisterSymbolic.BinaryObject, x: new Date(accessionRegisterSymbolic.CreationDate)});
    expect(component.BinaryObjectSize.data).toContain({y: accessionRegisterSymbolic.BinaryObjectSize, x: new Date(accessionRegisterSymbolic.CreationDate)});
  });

  it('should get accession register symbolic ordered by date', () => {
    // Given
    const accessionRegisterSymbolic1 = {ObjectGroup: 1, ArchiveUnit: 1, BinaryObject: 1, BinaryObjectSize: 1, CreationDate: '2018-10-04T07:39:33.842Z'};
    const accessionRegisterSymbolic2 = {ObjectGroup: 2, ArchiveUnit: 2, BinaryObject: 2, BinaryObjectSize: 2, CreationDate: '2018-10-04T10:39:33.842Z'};

    const referentialService = {getAccessionRegisterSymbolicByDate: () => Observable.of({$results: [accessionRegisterSymbolic2, accessionRegisterSymbolic1]})};
    configureTest(referentialService);

    const component = TestBed.createComponent(AccessionRegisterSymbolicComponent).componentInstance;

    // When
    component.getAccessionRegisterSymbolic();

    // Then
    expect(component.ObjectGroup.data[0]).toEqual({y: accessionRegisterSymbolic1.ObjectGroup, x: new Date(accessionRegisterSymbolic1.CreationDate)});
    expect(component.ObjectGroup.data[1]).toEqual({y: accessionRegisterSymbolic2.ObjectGroup, x: new Date(accessionRegisterSymbolic2.CreationDate)});
  });

  it('should create four new chart according to start date and end date specified', () => {
    // Given
    const referentialService = {getAccessionRegisterSymbolicByDate: () => Observable.of({$results: []})};
    let chartCreatedCount = 0;
    const chart = function () {
      chartCreatedCount++;
    };
    configureTest(referentialService, chart);

    const component = TestBed.createComponent(AccessionRegisterSymbolicComponent).componentInstance;

    // When
    component.getAccessionRegisterSymbolic();

    // Then
    expect(chartCreatedCount).toBe(4);
  });
});

async function configureTest(referentialService = {}, Chart = function () {}) {
  chartjs.Chart = Chart;
  TestBed.configureTestingModule({
    imports: [RouterTestingModule],
    providers: [
      BreadcrumbService,
      ErrorService,
      {provide: ReferentialsService, useValue: referentialService},
      {provide: LogbookService, useValue: {}},
      {provide: ActivatedRoute, useValue: {params: Observable.of({id: 'Mock_Service'})}},
      {provide: DialogService, useValue: {}}
    ],
    declarations: [AccessionRegisterSymbolicComponent],
    schemas: [NO_ERRORS_SCHEMA]
  }).compileComponents();
}