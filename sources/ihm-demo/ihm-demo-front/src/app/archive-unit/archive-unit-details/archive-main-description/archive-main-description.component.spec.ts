import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { Observable } from "rxjs/Rx";
import 'rxjs/add/observable/of';

import { ArchiveMainDescriptionComponent } from './archive-main-description.component';
import { ArchiveUnitHelper } from "../../archive-unit.helper";
import { ArchiveUnitService } from "../../archive-unit.service";
import { VitamResponse } from "../../../common/utils/response";
import { ActivatedRoute } from "@angular/router";
import { RouterTestingModule } from "@angular/router/testing";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of({$results: [{ test: 'newValue', '#management': {SubmissionAgency: 'id'} }]}),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

describe('ArchiveMainDescriptionComponent', () => {
  let component: ArchiveMainDescriptionComponent;
  let fixture: ComponentFixture<ArchiveMainDescriptionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [ RouterTestingModule ],
      providers: [
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub },
        { provide: ActivatedRoute, useValue: {params: Observable.of({id: 1})} }
      ],
      declarations: [ ArchiveMainDescriptionComponent ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveMainDescriptionComponent);
    component = fixture.componentInstance;
    component.archiveUnit = {
      test: 'initialValue',
      '#management': {SubmissionAgency: 'id'}
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
    component.dataToDisplay.test = 'updated';
    component.switchUpdateMode();
    expect(component.dataToDisplay.test).toBe('initialValue');
    expect(component.update).toBeFalsy();
  });

  it('should update data on update success', () => {
    expect(component.saveRunning).toBeFalsy();
    component.updatedFields = {test: 'newValue'};
    component.saveUpdate();
    expect(component.dataToDisplay.test).toBe('newValue');
  });

  it('should emit the title if it was successfully updated', (done) => {
    component.titleUpdate.subscribe(
      (newTitle) => {
        expect(newTitle).toBe('NewTitle');
        done();
      }
    );

    component.updatedFields = {Title: 'NewTitle'};
    component.saveUpdate();
  });

  it('should not emit the title if it was failure on Title update', () => {
    let fail = this.fail;
    component.titleUpdate.subscribe(
      () => {
        fail('titleUpdate should not emit message');
      }
    );

    spyOn(component.archiveUnitService, 'updateMetadata').and.callFake(
      () => {
        return Observable.throw('Error while update');
      });

    component.updatedFields = {Title: 'NewTitle'};
    component.saveUpdate();
  });

  it('shouln\'t emit message if update do not concern title', () => {
    let fail = this.fail;
    component.titleUpdate.subscribe(
      () => {
        fail('titleUpdate should not emit message');
      }
    );

    component.updatedFields = {Description: 'NewDesc'};
    component.saveUpdate();
  });
});
