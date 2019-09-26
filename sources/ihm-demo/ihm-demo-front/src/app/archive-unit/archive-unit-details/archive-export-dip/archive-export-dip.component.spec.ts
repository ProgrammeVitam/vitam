import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import 'rxjs/add/observable/of';

import { ArchiveExportDIPComponent } from './archive-export-dip.component';
import { ArchiveUnitHelper } from '../../archive-unit.helper';
import { ArchiveUnitService } from '../../archive-unit.service';
import { VitamResponse } from '../../../common/utils/response';

import { ReferentialHelper } from '../../../referentials/referential.helper';
import { plainToClass } from 'class-transformer';
import { AccessContract } from '../../../referentials/details/access-contract/access-contract';
import { ReferentialsService } from '../../../referentials/referentials.service';
import { ErrorService } from '../../../common/error.service';
import { AccessContractService } from '../../../common/access-contract.service';
import {DialogService} from '../../../common/dialog/dialog.service';
import { Router } from '@angular/router';

const ArchiveUnitServiceStub = {
  exportDIP: (body) => Observable.of(new VitamResponse()),
  downloadDIP: (id) => Observable.of(new VitamResponse()),
  downloadTransferSIP: (id) => Observable.of(new VitamResponse())
};

const ReferentialsServiceStub = {
  getResults: (id) => Observable.of({"$hits":{"total":1,"offset":0,"limit":125,"size":10000}, '$results': [{}]}),
  setSearchAPI: () => {},
  getAccessContractById: (id) => Observable.of({'$results': [{}]})
};

const RouterStub = {
  navigate: () => {}
};

describe('ArchiveExportDIPComponent', () => {
  let component: ArchiveExportDIPComponent;
  let fixture: ComponentFixture<ArchiveExportDIPComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveExportDIPComponent ],
      providers: [
        ErrorService,
        AccessContractService,
        DialogService,
        ReferentialHelper,
        { provide: Router, useValue: RouterStub },
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: ReferentialsService, useValue: ReferentialsServiceStub }
      ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveExportDIPComponent);
    component = fixture.componentInstance;
    component.id = 'auId';
    component.operation = 'operationId';
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should make the good request', () => {
    component.exportType = 'AU';
    let query = component.getQuery();
    expect(query.$query[0].$eq['#id']).toBe(component.id);

    component.exportType = 'INGEST';
    query = component.getQuery();

    expect(query.$query[0].$eq['#operations']).toBe(component.operation);

    component.exportType = 'FULL';
    query = component.getQuery();
    expect(query.$query[0].$or[0].$eq['#id']).toBe(component.id);
    expect(query.$query[0].$or[1].$in['#allunitups'][0]).toBe(component.id);;
  })
});
