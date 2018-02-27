import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveExtraDescriptionComponent } from './archive-extra-description.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of({$results: [{ test: 'newValue', '#management': {SubmissionAgency: 'id'} }]}),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveExtraDescriptionComponent', () => {
  let component: ArchiveExtraDescriptionComponent;
  let fixture: ComponentFixture<ArchiveExtraDescriptionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      declarations: [ ArchiveExtraDescriptionComponent ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
        .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveExtraDescriptionComponent);
    component = fixture.componentInstance;
    component.archiveUnit = {
      test: 'initialValue'
    };
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should display update when swhitch update is called', () => {
    expect(component.update).toBeFalsy();
    component.switchUpdateMode();
    expect(component.update).toBeTruthy();
  });

  it('should rollback update when cancel update is called', () => {
    component.switchUpdateMode();
    expect(component.update).toBeTruthy();
    component.fields[0].value = 'updatedValue';
    component.switchUpdateMode();
    expect(component.archiveUnit[component.fields[0].value]).toBe('initialValue');
    expect(component.update).toBeFalsy();
  });

  it('should update data on update success', () => {
    expect(component.saveRunning).toBeFalsy();
    component.updatedFields = {test: 'newValue'};
    component.saveUpdate();
    expect(component.archiveUnit[component.fields[0].value]).toBe('newValue');
  });

  it('should reset updated fields after update success', () => {
    expect(component.saveRunning).toBeFalsy();
    component.updatedFields = {test: 'newValue'};
    component.saveUpdate();
    expect(component.archiveUnit[component.fields[0].value]).toBe('newValue');
    expect(Object.keys(component.updatedFields).length).toBe(0);
  });
});
