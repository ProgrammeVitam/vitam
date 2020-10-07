import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {Observable} from 'rxjs/Rx';

import {SearchReferentialsComponent} from './search-referentials.component';
import {BreadcrumbService} from '../../common/breadcrumb.service';
import {ReferentialsService} from '../referentials.service';
import {ArchiveUnitHelper} from '../../archive-unit/archive-unit.helper';
import {AuthenticationService} from '../../authentication/authentication.service';
import {LogbookService} from '../../ingest/logbook.service';

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({'$results': [{}]})
};

const AuthenticationServiceStub = {
  isTenantAdmin: () => {
    return true;
  }
};

const LogbookServiceStub = {
  getLastImportAgenciesOkOrStpImportRulesOperation: (body) => {
    return Observable.of({'$results': [{evId: 'responseId'}]})
  },
  downloadReferentialCSV: (objectId, type) => {
  }
};

describe('SearchReferentialsComponent', () => {
  let component: SearchReferentialsComponent;
  let fixture: ComponentFixture<SearchReferentialsComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([
        {path: 'ingest/sip', component: SearchReferentialsComponent}
      ])
      ],
      providers: [
        BreadcrumbService,
        ArchiveUnitHelper,
        {provide: ReferentialsService, useValue: ReferentialsServiceStub},
        {provide: AuthenticationService, useValue: AuthenticationServiceStub},
        {provide: LogbookService, useValue: LogbookServiceStub}
      ],
      declarations: [SearchReferentialsComponent],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SearchReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should check item status', () => {
    const item = {
      'status': 'ACTIVE'
    };
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Actif');

    item.status = 'true';
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Actif');

    item.status = 'INACTIVE';
    expect(SearchReferentialsComponent.handleStatus(item.status)).toEqual('Inactif');
  });

  it('should handle rule measurement values', () => {
    let itemWithRuleMeasurement = {
      'RuleDuration': '40',
      'RuleMeasurement': 'year'
    };
    expect(SearchReferentialsComponent.appendUnitToRuleDuration(itemWithRuleMeasurement)).toEqual('40 années');

    itemWithRuleMeasurement.RuleMeasurement = 'month';
    expect(SearchReferentialsComponent.appendUnitToRuleDuration(itemWithRuleMeasurement)).toEqual('40 mois');

    const itemWithoutRuleMeasurement = {
      'RuleDuration': 'UNLIMITED',
      'RuleMeasurement': 'year'
    };
    expect(SearchReferentialsComponent.appendUnitToRuleDuration(itemWithoutRuleMeasurement)).toEqual('illimitée');

    const itemWithoutRuleMeasurementAndDuration = {
    };
    expect(SearchReferentialsComponent.appendUnitToRuleDuration(itemWithoutRuleMeasurementAndDuration)).toEqual('indéterminée');
  });

  it('should do initial sort', () => {
    const data = [{'Name': 'mn'}, {'Name': ' ba'}, {'Name': '1z'}, {'Name': 'ac'}, {'Name': 'Ab'}];
    const sortedData = [{'Name': '1z'}, {'Name': 'Ab'}, {'Name': 'ac'}, {'Name': ' ba'}, {'Name': 'mn'}];

    SearchReferentialsComponent.doInitialSort(data, 'Name');

    for (let i = 0, len = data.length; i < len; i++) {
      expect(data[i]).toEqual(sortedData[i]);
    }
  });

  it('should correctly handle empty Names while in initial sort', () => {
    const data = [{id: 0}, {id: 4, 'Name': ' ba'}, {id: 1, 'Name': ''}, {id: 2, 'Name': null}, {id: 3, 'Name': 'Ab'}];

    SearchReferentialsComponent.doInitialSort(data, 'Name');

    for (let i = 0, len = data.length; i < len; i++) {
      expect(data[i].id).toEqual(i);
    }
  });

});
