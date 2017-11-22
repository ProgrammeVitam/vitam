import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {NO_ERRORS_SCHEMA} from "@angular/core";
import {Observable} from "rxjs/Rx";
import 'rxjs/add/observable/of';

import {Confirmation, ConfirmationService} from "primeng/primeng";

import {ArchiveRuleBlocComponent} from './archive-rule-bloc.component';
import {ArchiveUnitHelper} from "../../archive-unit.helper";
import {ArchiveUnitService} from "../../archive-unit.service";
import {VitamResponse} from "../../../common/utils/response";
import {KeysPipe} from '../../../common/utils/pipes';
import {arch} from "os";

let ArchiveUnitServiceStub = {
  getDetails: (id) => Observable.of(new VitamResponse()),
  getObjects: (id) => Observable.of(new VitamResponse()),
  getResults: (body, offset, limit) => Observable.of(new VitamResponse()),
  updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
};

let ConfirmationServiceStub = {
  confirm: (confirmation: Confirmation) => {
    this
  }
}

describe('ArchiveRuleBlocComponent', () => {
  let component: ArchiveRuleBlocComponent;
  let fixture: ComponentFixture<ArchiveRuleBlocComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        ArchiveUnitHelper,
        {provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub},
        {provide: ConfirmationService, useValue: ConfirmationServiceStub}
      ],
      declarations: [ArchiveRuleBlocComponent, KeysPipe],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ArchiveRuleBlocComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });


  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('should not display confirmation popup when no rules is updated in component', () => {
    let spy: any = spyOn(component.confirmationService, 'confirm').and.callThrough();
    spyOn(component, 'switchUpdateMode').and.callFake(() => {
    });
    spyOn(component, 'checkUpdate').and.returnValue(() => true);
    component.updatedFields = {};
    component.saveUpdate();
    expect(spy.calls.count()).toBe(0);

  });

  it('should display confirmation popup when rules is updated in component', () => {
    let spy: any = spyOn(component.confirmationService, 'confirm').and.callThrough();
    spyOn(component, 'switchUpdateMode').and.callFake(() => {
    });
    spyOn(component, 'checkUpdate').and.returnValue(() => true);
    component.updatedFields = {
      AccessRules: {
        Rules: [{
          Rule: "ACC-00003",
          StartDate: "Mon Nov 20 2017 01:00:00 GMT+0100 (CET)",
          EndDate: "2042-11-20",
          oldId: "ACC-00003"
        },
          {
            Rule: "ACC-00002",
            StartDate: "Tue Jan 01 2002 01:00:00 GMT+0100 (CET)",
            EndDate: "2027-01-01",
            inherited: true
          }]
      }
    };
    component.saveUpdate();
    expect(spy.calls.count()).toBe(1);

  });
});
