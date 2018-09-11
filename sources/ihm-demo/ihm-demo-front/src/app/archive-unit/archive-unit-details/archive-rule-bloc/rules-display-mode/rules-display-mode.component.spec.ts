import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { RulesDisplayModeComponent } from './rules-display-mode.component';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ComputeRulesUtilsService} from '../compute-rules-utils.service';
import {ArchiveUnitHelper} from '../../../archive-unit.helper';
import {NgArrayPipesModule} from 'ngx-pipes';
import {ArchiveUnitService} from '../../../archive-unit.service';
import {Observable} from 'rxjs';
import {VitamResponse} from '../../../../common/utils/response';
import {BaseInheritedItem, InheritanceProperties, InheritedRule, PropertyModel} from '../management-model';

describe('RulesDisplayModeComponent', () => {
  let component: RulesDisplayModeComponent;
  let fixture: ComponentFixture<RulesDisplayModeComponent>;

  const DefaultResponse = {
    $context: {},
    $hits: {},
    $results: [
      {
        '#object': '',
        '#operations': ['operationId'],
        'Title': 'Titre'
      }],
    httpCode: 200
  };

  const ArchiveUnitServiceStub = {
    getDetails: (id) => Observable.of(DefaultResponse),
    getDetailsWithInheritedRules: () => Observable.of(DefaultResponse),
    updateMetadata: (id, updateRequest) => Observable.of(new VitamResponse())
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ RulesDisplayModeComponent ],
      imports: [ NgArrayPipesModule ],
      providers: [
        ComputeRulesUtilsService,
        ArchiveUnitHelper,
        { provide: ArchiveUnitService, useValue: ArchiveUnitServiceStub }
      ],
      schemas: [ NO_ERRORS_SCHEMA ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(RulesDisplayModeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should test inheritance of an item', () => {
    // Given
    component.unitId = 'Unit0';
    const inheritedRule: InheritedRule = {Rule: '', StartDate: '', EndDate: '', UnitId: 'Unit1', Paths: [[]], OriginatingAgency: ''};
    const localRule: InheritedRule = {Rule: '', StartDate: '', EndDate: '', UnitId: 'Unit0', Paths: [[]], OriginatingAgency: ''};
    const inheritedProperty: PropertyModel = {PropertyName: '', PropertyValue: '', UnitId: 'Unit1', Paths: [[]], OriginatingAgency: ''};
    const localProperty: PropertyModel = {PropertyName: '', PropertyValue: '', UnitId: 'Unit0', Paths: [[]], OriginatingAgency: ''};

    // Then
    expect(component.isInherited(inheritedRule)).toBe('Oui');
    expect(component.isInherited(inheritedProperty)).toBe('Oui');
    expect(component.isInherited(localRule)).toBe('Non');
    expect(component.isInherited(localProperty)).toBe('Non');
  });

  it('should toggle rule details and call for title', () => {
    // Given
    component.displayDetails = {};
    const rule = {Rule: 'Rule1', StartDate: '', EndDate: '', UnitId: 'Unit1', Paths: [[]], OriginatingAgency: ''};
    const uniqueRuleIdentifier = rule.Rule + '-' + rule.UnitId;

    // When
    component.toggleRuleDetails(rule);

    // Then
    expect(component.displayDetails[uniqueRuleIdentifier]).toBeTruthy();
  });
});
