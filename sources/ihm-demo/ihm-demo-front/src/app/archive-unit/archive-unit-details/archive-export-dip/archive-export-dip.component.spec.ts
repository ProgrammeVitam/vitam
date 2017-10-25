import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveExportDIPComponent } from './archive-export-dip.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";

let ArchiveUnitServiceStub = {
  exportDIP: (body) => Observable.of(new VitamResponse()),
  downloadDIP: (id) => Observable.of(new VitamResponse())
};

describe('ArchiveExportDIPComponent', () => {
  let component: ArchiveExportDIPComponent;
  let fixture: ComponentFixture<ArchiveExportDIPComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ArchiveExportDIPComponent ],
      providers: [
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
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
    expect(query.$query[0].$depth).toBe(20);

    component.exportType = 'FULL';
    query = component.getQuery();
    expect(query.$query[0].$or[0].$eq['#id']).toBe(component.id);
    expect(query.$query[0].$or[1].$in['#allunitups'][0]).toBe(component.id);;
    expect(query.$query[0].$depth).toBe(0);
  })
});
