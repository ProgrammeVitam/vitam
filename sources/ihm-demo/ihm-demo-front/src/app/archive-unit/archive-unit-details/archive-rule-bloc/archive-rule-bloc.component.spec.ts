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
      AccessRule: {
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

  it('should compute the good update body for rule create', () => {
    component.updatedFields = {ReuseRule: {Rules:
          [{StartDate: new Date(), Rule: 'REU-00001', newRule: true}]
      }};

    let result = component.getUpdatedRules();

    expect(result.rules[0].ReuseRule.Rules.length).toBe(1);
    expect(result.rules[0].ReuseRule.Rules[0].Rule).toBe('REU-00001');
    expect(result.added).toBe(1);
    expect(result.updated).toBe(0);
    expect(result.deleted).toBe(0);
  });

  it('should send the good update body for rule update', () => {
    component.management = {AccessRule: {Rules: [
          {Rule: 'ACC-00001', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ]}};
    component.updatedFields = {AccessRule: {Rules:
          [{StartDate: new Date(), oldId: 'ACC-00001', Rule: 'ACC-00002'}]
      }};

    let result = component.getUpdatedRules();

    expect(result.rules[0].AccessRule.Rules.length).toBe(1);
    expect(result.rules[0].AccessRule.Rules[0].Rule).toBe('ACC-00002');
    expect(result.added).toBe(0);
    expect(result.updated).toBe(1);
    expect(result.deleted).toBe(0);
  });

  it('should keep old rule on update/create', () => {
    component.management = {AccessRule: {Rules: [
          {Rule: 'ACC-00001'},
          {Rule: 'ACC-00002', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ]}};
    component.updatedFields = {AccessRule: {Rules: [
          {Rule: 'ACC-00001'},
          {StartDate: new Date(), oldId: 'ACC-00002', Rule: 'ACC-00003'}
        ]}};

    let result = component.getUpdatedRules();

    expect(result.rules[0].AccessRule.Rules.length).toBe(2);
    expect(result.rules[0].AccessRule.Rules[0].Rule).toBe('ACC-00001');
    expect(result.rules[0].AccessRule.Rules[1].Rule).toBe('ACC-00003');
    expect(result.added).toBe(0);
    expect(result.updated).toBe(1);
    expect(result.deleted).toBe(0);

    component.updatedFields = {AccessRule: {Rules: [
          {Rule: 'ACC-00001'},
          {Rule: 'ACC-00002', StartDate: '2000-01-01', EndDate: '2025-01-01'},
          {StartDate: new Date(), Rule: 'ACC-00003', newRule: true}
        ]}};

    result = component.getUpdatedRules();

    expect(result.rules[0].AccessRule.Rules.length).toBe(3);
    expect(result.rules[0].AccessRule.Rules[0].Rule).toBe('ACC-00001');
    expect(result.rules[0].AccessRule.Rules[1].Rule).toBe('ACC-00002');
    expect(result.rules[0].AccessRule.Rules[2].Rule).toBe('ACC-00003');
    expect(result.added).toBe(1);
    expect(result.updated).toBe(0);
    expect(result.deleted).toBe(0);
  });

  it('should send the good update body with FinalAction', () => {
    component.management = {StorageRule: {
        Rules: [
          {Rule: 'STO-00001', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ], FinalAction: 'Copy'
      }, AppraisalRule: {
        Rules: [
          {Rule: 'APP-00001', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ], FinalAction: 'Keep'
      }};
    component.updatedFields = {
      StorageRule: {
        Rules: [
          {Rule: 'STO-00001', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ], FinalAction: 'Copy'
      }, AppraisalRule: {
        Rules: [
          {Rule: 'APP-00001', StartDate: '2000-01-01', EndDate: '2025-01-01'}
        ], FinalAction: 'Destroy'
      }
    };

    let result = component.getUpdatedRules();

    expect(result.rules[0].StorageRule).toBeUndefined();
    expect(result.rules[0].AppraisalRule.Rules.length).toBe(1);
    expect(result.rules[0].AppraisalRule.FinalAction).toBe('Destroy');
    expect(result.added).toBe(0);
    expect(result.updated).toBe(1);
    expect(result.deleted).toBe(0);
  });

});
