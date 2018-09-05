import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesUpdateModeComponent } from './rules-update-mode.component';
import {ArchiveUnitHelper} from '../../../archive-unit.helper';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {KeysPipe} from '../../../../common/utils/pipes';
import {ComputeRulesUtilsService} from '../compute-rules-utils.service';
import {ArchiveUnitService} from '../../../archive-unit.service';
import {Observable} from 'rxjs';
import {VitamResponse} from '../../../../common/utils/response';
import {InheritedRule} from '../management-model';

describe('RulesUpdateModeComponent', () => {
  let component: RulesUpdateModeComponent;
  let fixture: ComponentFixture<RulesUpdateModeComponent>;

  const DefaultResponse = {
    $context: {},
    $hits: {},
    $results: [{'#object': '', '#operations': ['operationId']}],
    httpCode: 200
  };

  const ArchiveUnitServiceStub = {
    getDetails: (id) => Observable.of(DefaultResponse),
    getDetailsWithInheritedRules: () => Observable.of(DefaultResponse),
    updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
  };

  const ComputeRulesUtilsServiceStub = {
    getUpdateStructure: () => Observable.of({})
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RulesUpdateModeComponent, KeysPipe ],
      providers: [
        ArchiveUnitHelper,
        { provide: ComputeRulesUtilsService, useValue: ComputeRulesUtilsServiceStub },
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RulesUpdateModeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should add new rule', () => {
    // Given
    component.updateValues = {
      AppraisalRule: {
        Rules: [
          { Rule: 'Rule1', StartDate: '', EndDate: '', UnitId: 'U0', OriginatingAgency: 'OA', Paths: [[]] }
        ], Properties: []
      }
    };

    // When
    component.addRule('AppraisalRule');

    // Then
    expect(component.updateValues.AppraisalRule.Rules.length).toBe(2);
    expect(component.updateValues.AppraisalRule.Rules[1].newRule).toBeTruthy();
  });

  it('should fully delete newRule on delete', () => {
    // Given
    const rule = { Rule: 'Rule1', StartDate: '', EndDate: '', UnitId: 'U0', OriginatingAgency: 'OA', Paths: [[]], newRule: true };
    component.updateValues = {
      AppraisalRule: {
        Rules: [ rule ], Properties: []
      }
    };

    // When
    component.doOrUndoRemoveRule('AppraisalRule', 0, rule);

    // then
    expect(component.updateValues.AppraisalRule.Rules.length).toBe(0);
  });

  it('should strike/remove old rule on delete', () => {
    // Given
    const rule = { Rule: 'Rule1', StartDate: '', EndDate: '', UnitId: 'U0', OriginatingAgency: 'OA', Paths: [[]] };
    component.updateValues = {
      AppraisalRule: {
        Rules: [ rule ], Properties: []
      }
    };
    component.deletedRules = [];

    // When
    component.doOrUndoRemoveRule('AppraisalRule', 0, rule);

    // then
    expect(component.updateValues.AppraisalRule.Rules.length).toBe(1);
    expect(component.deletedRules.length).toBe(1);
    expect(component.deletedRules[0]).toBe('AppraisalRule-Rule1');
  });

  it('should reset striked/removed rule on delete', () => {
    // Given
    const rule = { Rule: 'Rule1', StartDate: '', EndDate: '', UnitId: 'U0', OriginatingAgency: 'OA', Paths: [[]] };
    component.updateValues = {
      AppraisalRule: {
        Rules: [ rule ], Properties: []
      }
    };
    component.deletedRules = ['AppraisalRule-Rule1'];

    // When
    component.doOrUndoRemoveRule('AppraisalRule', 0, rule);

    // then
    expect(component.updateValues.AppraisalRule.Rules.length).toBe(1);
    expect(component.deletedRules.length).toBe(0);
  });


});
