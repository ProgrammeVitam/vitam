import { TestBed, inject } from '@angular/core/testing';

import { ComputeRulesUtilsService } from './compute-rules-utils.service';
import {NO_ERRORS_SCHEMA} from '@angular/core';
import {ArchiveUnitHelper} from '../../archive-unit.helper';
import {ManagementModel} from './management-model';
import {UpdateInfo, UpdatePropertiesModel} from './update-management-model';

describe('ComputeRulesUtilsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ComputeRulesUtilsService,
        ArchiveUnitHelper
      ],
      schemas: [ NO_ERRORS_SCHEMA ]
    });
  });

  it('should be created', inject([ComputeRulesUtilsService], (service: ComputeRulesUtilsService) => {
    expect(service).toBeTruthy();
  }));

  it( 'should merge management into inheritedInfo', inject([ComputeRulesUtilsService], (service: ComputeRulesUtilsService) => {
    // Given
    const archiveUnitProfile = 'Profile';
    const management = {NeedAuthorization: true, AccessRule: {Inheritance: {PreventRulesId: ['preventedId']}}};
    const inheritedRules = {
      AccessRule: {
        Rules: [{ Rule: 'Rule', StartDate: 'StartDate', EndDate: 'EndDate', UnitId: 'id', OriginatingAgency: 'OA', Paths: [[]]}],
        Properties: []
      },
      AppraisalRule: {
        Rules: [],
        Properties: [{ PropertyName: 'FinalAction', PropertyValue: 'Keep', UnitId: 'id', OriginatingAgency: 'OA', Paths: [[]]}]
      }
    };

    // When
    const response = service.computeEffectiveRules(management, inheritedRules, archiveUnitProfile);
    const managementInfo = response.managementInfo;
    const properties = response.unitProperties;

    // Then
    expect(managementInfo.NeedAuthorization).toBeTruthy('Should merge NeedAuthorization from management');
    expect(managementInfo.ArchiveUnitProfile).toBe('Profile', 'Should merge ArchiveUnitProfile from management');
    expect(managementInfo.AccessRule.Inheritance.PreventRulesId.length).toBe(1, 'Should merge preventInheritance');
    expect(managementInfo.AccessRule.Inheritance.PreventRulesId[0]).toBe('preventedId', 'Should merge preventInheritance');
    expect(managementInfo.AccessRule.Rules.length).toBe(1, 'Should merge rules');
    expect(managementInfo.AccessRule.Rules[0].Rule).toBe('Rule', 'Should merge rules');

    expect(properties['id-AppraisalRule'].length).toBe(1, 'Should extract properties');
    expect(properties['id-AppraisalRule'][0].PropertyValue).toBe('Keep', 'Should extract properties');
  }));

  it('should get a structure adapted for update', inject([ComputeRulesUtilsService], (service: ComputeRulesUtilsService) => {
    // Given
    const unitId = 'unitId0';
    const initialData: ManagementModel = {
      AppraisalRule: {
        Rules: [],
        Properties: [{ PropertyName: 'FinalAction', PropertyValue: 'Keep', UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]}]
      },
      ClassificationRule: {
        Rules: [],
        Properties: [
          { PropertyName: 'ClassificationReassessingDate', PropertyValue: '1990-10-01', UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]}
        ]
      },
      DisseminationRule: {
        Rules: [{ Rule: 'Rule1', StartDate: '2015-01-01', EndDate: '2015-01-01', UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']] }],
        Properties: [],
        Inheritance: {PreventInheritance: true}
      },
      StorageRule: {
        Rules: [
          { Rule: 'Rule1', StartDate: '2015-01-01', EndDate: '2015-01-01', UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]},
          { Rule: 'Rule2', StartDate: '2015-01-01', EndDate: '2015-01-01', UnitId: 'unitId1', OriginatingAgency: 'OA', Paths: [['unitId0', 'unitId1']]}
        ],
        Properties: [
          { PropertyName: 'FinalAction', PropertyValue: 'Copy', UnitId: 'unitId1', OriginatingAgency: 'OA', Paths: [['unitId0', 'unitId1']]}
        ]
      },
      ArchiveUnitProfile: 'AUP-00001',
      NeedAuthorization: true
    };

    // When
    const response = service.getUpdateStructure(initialData, unitId);
    const updated = response.updateStructure;
    const properties = response.localProperties;
    const inheritance = response.inheritedItems;

    // Then
    expect(updated.ArchiveUnitProfile).toBe('AUP-00001', 'should merge ArchiveUnitProfile');
    expect(updated.NeedAuthorization).toBe(true, 'should merge NeedAuthorization');
    expect(updated.AccessRule).toBeDefined('should define not given categories');
    expect(updated.AppraisalRule.Inheritance).toBeDefined('should defined not given inheritance');
    expect(updated.DisseminationRule.Rules.length).toBe(1, 'Local rules should remain in the structure');
    expect(updated.DisseminationRule.Rules[0].editionStartDate.getTime())
      .toBe(new Date(updated.DisseminationRule.Rules[0].StartDate).getTime(),
        'Rule StartDate should be transformed to Date into editionStartDate');
    expect(updated.StorageRule.Rules.length).toBe(1, 'should remove inherited rules');
    expect(updated.StorageRule.Rules[0].Rule).toBe('Rule1', 'should only remain local rules');

    expect(properties.AppraisalRule.FinalAction).toBe('Keep', 'local properties should be copy in localProperties');
    expect(properties.ClassificationRule.ClassificationReassessingDate.getTime())
      .toBe(new Date('1990-10-01').getTime(), 'ClassificationReassessingDate should be transform to date');
    expect(properties.StorageRule.FinalAction).toBeUndefined('inherited properties should not be copy into localProperties');

    expect(inheritance.StorageRule.Rules.length).toBe(1, 'inherited rules should be moved into inheritedItems');
    expect(inheritance.StorageRule.Rules[0].Rule).toBe('Rule2', 'inherited rules should be moved into inheritedItems');
    expect(inheritance.StorageRule.Properties.length).toBe(1, 'inherited proeprties should be moved into inheritedItems');
    expect(inheritance.StorageRule.Properties[0].PropertyValue).toBe('Copy', 'inherited proeprties should be moved into inheritedItems');
  }));

  it('should prepare the update request body', inject([ComputeRulesUtilsService], (service: ComputeRulesUtilsService) => {
    // Given
    const updatedFields: ManagementModel = {
      AccessRule: {
        Rules: [
          {
            Rule: 'Rule1', StartDate: '2015-01-01', EndDate: '2015-01-01', editionStartDate: new Date('2016-01-01'),
            UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]
          }
        ], Properties: [], Inheritance: {}
      },
      AppraisalRule: {
        Rules: [],
        Properties: [],
        Inheritance: {
          PreventInheritance: true,
          PreventRulesId: ['Rule5']
        }
      },
      DisseminationRule: {
        Rules: [
          {
            Rule: 'Rule2', StartDate: '', EndDate: '', editionStartDate: new Date('1990-10-01'), newRule: true,
            UnitId: 'unitId0', OriginatingAgency: '', Paths: [[]]
          }
        ], Properties: [], Inheritance: {}
      },
      ReuseRule: {
        Rules: [
          {
            Rule: 'Rule3', StartDate: '2015-01-01', EndDate: '2015-01-01',
            UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]
          }, {
            Rule: 'Rule4', StartDate: '2015-01-01', EndDate: '2015-01-01', editionStartDate: new Date('2015-01-01'),
            UnitId: 'unitId0', OriginatingAgency: 'OA', Paths: [['unitId0']]
          }
        ],
        Properties: [],
        Inheritance: {
          PreventRulesId: ['Rule7', 'Rule6']
        }
      },
      NeedAuthorization: true,
      ArchiveUnitProfile: ''
    };
    const management: any = {
      AccessRule: {
        Rules: [
          { Rule: 'Rule1', StartDate: '2015-01-01', EndDate: '2015-01-01' }
        ]
      },
      AppraisalRule: {
        FinalAction: 'Keep',
        Inheritance: { PreventRulesId: ['Rule5'] }
      },
      ReuseRule: {
        Rules: [
          { Rule: 'Rule3', StartDate: '2015-01-01', EndDate: '2015-01-01' },
          { Rule: 'Rule4', StartDate: '2015-01-01', EndDate: '2015-01-01' }
        ],
        Inheritance: {
          PreventRulesId: ['Rule6', 'Rule7']
        }
      },
      StorageRule: {
        Rules: [], Inheritance: {},
        FinalAction: 'Copy'
      }
    };
    const deletedRules: string[] = ['ReuseRule-Rule3'];
    const properties: UpdatePropertiesModel = {
      AppraisalRule: { FinalAction: 'Destroy' },
      ClassificationRule: {},
      StorageRule: { FinalAction: 'Copy' }
    };

    // When
    const response: UpdateInfo = service.getUpdateInformation(updatedFields, management, deletedRules, properties);
    const rules = response.rules;

    // Then
    expect(response.ArchiveUnitProfile).toBeUndefined('Should not update ArchiveUnitProfile from undefined to empty');
    expect(response.NeedAuthorization).toBeTruthy('Should update NeedAuthorization from undefined to true');
    expect(rules.length).toBe(4, 'Should have 4 remains rules at the end');
    expect(response.updated).toBe(5, 'Should have update 4 rules and 1 property');
    expect(response.added).toBe(1, 'Should add 1 rule');
    expect(response.deleted).toBe(1, 'Should delete 1 rule');
    expect(response.categories.length).toBe(4, 'Should update rules and properties in 4 categories');
  }));

});
