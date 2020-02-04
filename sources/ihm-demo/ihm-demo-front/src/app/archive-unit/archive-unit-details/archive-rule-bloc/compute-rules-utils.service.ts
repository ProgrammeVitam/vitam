import { Injectable } from '@angular/core';
import {ArchiveUnitHelper} from '../../archive-unit.helper';
import {ManagementModel, RuleCategory} from './management-model';
import {ObjectsService} from '../../../common/utils/objects.service';
import {UpdateInfo, UpdatePropertiesModel} from './update-management-model';
import {DateService} from '../../../common/utils/date.service';

@Injectable()
export class ComputeRulesUtilsService {

  constructor(public archiveUnitHelper: ArchiveUnitHelper) { }

  computeEffectiveRules(management: any, preventRulesId: ManagementModel, archiveUnitProfile: string) {
    const computedManagmentInfo: ManagementModel = {
      NeedAuthorization: management.NeedAuthorization,
      ArchiveUnitProfile: archiveUnitProfile
    };
    const unitProperties: any = {};

    for (const category of this.archiveUnitHelper.rulesCategories) {
      const inheritedCategory = preventRulesId[category.rule];
      const managementCategory = management[category.rule];

      if (inheritedCategory && (inheritedCategory.Properties.length > 0 || inheritedCategory.Rules.length > 0)) {
        computedManagmentInfo[category.rule] = inheritedCategory;

        for (const property of inheritedCategory.Properties) {
          const unitPropertyKey = `${property.UnitId}-${category.rule}`;
          if (!unitProperties[unitPropertyKey]) {
            unitProperties[unitPropertyKey] = [];
          }
          unitProperties[unitPropertyKey].push(property);
        }

        if (managementCategory && managementCategory.Inheritance) {
          computedManagmentInfo[category.rule].Inheritance = managementCategory.Inheritance;
        } else {
          computedManagmentInfo[category.rule].Inheritance = {};
        }

      } else if (managementCategory && managementCategory.Inheritance) {
        computedManagmentInfo[category.rule] = {
          Rules: [],
          Properties: [],
          Inheritance: managementCategory.Inheritance
        }
      }
    }

    return {
      managementInfo: computedManagmentInfo,
      unitProperties: unitProperties
    };
  }

  getUpdateStructure(initialData: ManagementModel, unitId: string): {updateStructure: ManagementModel, localProperties: UpdatePropertiesModel, inheritedItems: ManagementModel} {
    const updateStructure: ManagementModel = ObjectsService.clone(initialData);
    const localProperties: UpdatePropertiesModel = new UpdatePropertiesModel(); // In order to simplify properties update
    const inheritedItems: ManagementModel = {}; // In order to simplify separate item display

    for (const category of this.archiveUnitHelper.rulesCategories) {
      // Init rule skeleton for empty categories
      if (!updateStructure[category.rule]) {
        updateStructure[category.rule] = {
          Rules: [],
          Properties: [],
          Inheritance: {
            PreventInheritance: false,
            PreventRulesId: []
          }
        };
        continue;
      }

      if (!updateStructure[category.rule].Inheritance) {
        updateStructure[category.rule].Inheritance = {
          PreventInheritance: false,
          PreventRulesId: []
        }
      } else {
        // Init rule skeleton for empty inheritance values
        if (!updateStructure[category.rule].Inheritance.PreventInheritance) {
          updateStructure[category.rule].Inheritance.PreventInheritance = false;
        }
        if (!updateStructure[category.rule].Inheritance.PreventRulesId) {
          updateStructure[category.rule].Inheritance.PreventRulesId = [];
        }
      }

      // Only update on local rules and not inherited ones. Inherited rules are keep for display
      for (let index = updateStructure[category.rule].Rules.length - 1; index >= 0; index--) {
        if (updateStructure[category.rule].Rules[index].UnitId !== unitId) {
          if (!inheritedItems[category.rule]) {
            inheritedItems[category.rule] = {Rules: [], Properties: []}
          }
          inheritedItems[category.rule].Rules.push(updateStructure[category.rule].Rules[index]);
          updateStructure[category.rule].Rules.splice(index, 1);
        } else if (updateStructure[category.rule].Rules[index].StartDate) {
            updateStructure[category.rule].Rules[index].editionStartDate = new Date(updateStructure[category.rule].Rules[index].StartDate);
        }
      }

      // Bind local properties and extract inherited properties
      for (let index = updateStructure[category.rule].Properties.length - 1; index >= 0; index--) {
        if (updateStructure[category.rule].Properties[index].UnitId !== unitId) {
          // Inherited Property
          if (!inheritedItems[category.rule]) {
            inheritedItems[category.rule] = {Rules: [], Properties: []}
          }
          inheritedItems[category.rule].Properties.push(updateStructure[category.rule].Properties[index]);
          updateStructure[category.rule].Properties.splice(index, 1);
        } else {
          // Local Property
          const property = updateStructure[category.rule].Properties[index];
          if (category.properties[property.PropertyName].kind === 'date') {
            localProperties[category.rule][property.PropertyName] = new Date(property.PropertyValue);
          } else {
            localProperties[category.rule][property.PropertyName] = property.PropertyValue;
          }
        }
      }
    }

    return {
      updateStructure: updateStructure,
      localProperties: localProperties,
      inheritedItems: inheritedItems
    };
  }

  private checkUpdate(categoryName, rule, management) {
    if (!management[categoryName]) {
      return false;
    }

    let mgtRule = null;
    for (const ruleInMgt of management[categoryName].Rules) {
      if (ruleInMgt.Rule === rule.Rule) {
        mgtRule = ruleInMgt;
      }
    }
    if (mgtRule === null) { return true }
    return rule.Rule !== mgtRule.Rule || rule.StartDate !== mgtRule.StartDate
  }

  getUpdateInformation(updateStructure: ManagementModel, management: any, deletedRules: string[], properties: UpdatePropertiesModel): UpdateInfo {
    const updateInfo: UpdateInfo = {
      updated: 0,
      added: 0,
      deleted: 0,
      categories: [],
      rules: [],
    };

    if (updateStructure.NeedAuthorization !== management.NeedAuthorization && (!!updateStructure.NeedAuthorization || !!management.NeedAuthorization)) {
      updateInfo.NeedAuthorization = updateStructure.NeedAuthorization;
      updateInfo.updated++;
    }

    if (updateStructure.ArchiveUnitProfile !== management.ArchiveUnitProfile && (!!updateStructure.ArchiveUnitProfile || !!management.ArchiveUnitProfile)) {
      updateInfo.ArchiveUnitProfile = updateStructure.ArchiveUnitProfile;
      updateInfo.updated++;
    }

    for (const propertyOrCategoryName in updateStructure) {
      if (!updateStructure.hasOwnProperty(propertyOrCategoryName)) { continue; }

      const category: any = updateStructure[propertyOrCategoryName];

      // It it's not a category, no need do to anything
      // FIXME: Check how to put `category instanceof RuleCategory` instead
      if (typeof category === 'string' || typeof category === 'boolean') { continue; }

      let isCategoryUpdated = false;
      const newCategory: any = {
        Rules: []
      };

      // Handle Rules
      for (const rule of category.Rules) {
        rule.StartDate = DateService.dateToString(rule.editionStartDate);
        delete rule.editionStartDate;
        if (!rule.StartDate) { delete rule.StartDate; }
        if (rule.newRule) {
          // New Rule
          isCategoryUpdated = true;
          const addedRule = ObjectsService.clone(rule);
          delete addedRule.newRule;
          newCategory.Rules.push(addedRule);

          updateInfo.added++;
        } else if (deletedRules.indexOf(`${propertyOrCategoryName}-${rule.Rule}`) !== -1) {
          // Deleted rule
          isCategoryUpdated = true;
          updateInfo.deleted++;
        } else if (this.checkUpdate(propertyOrCategoryName, rule, management)) {
          // Updated rule
          isCategoryUpdated = true;
          const updatedRule = ObjectsService.clone(rule);
          delete updatedRule.EndDate;
          delete updatedRule.Paths;
          delete updatedRule.OriginatingAgency;
          delete updatedRule.UnitId;
          newCategory.Rules.push(updatedRule);
          updateInfo.updated++;
        } else {
          // Non-Updated Old Rule
          const updatedRule = ObjectsService.clone(rule);
          delete updatedRule.newRule;
          delete updatedRule.EndDate;
          delete updatedRule.editionStartDate;
          delete updatedRule.Paths;
          delete updatedRule.OriginatingAgency;
          delete updatedRule.UnitId;
          newCategory.Rules.push(updatedRule);
        }
      }

      // Handle Inheritance values
      if (category.Inheritance) {
        newCategory.Inheritance = {};

        const preventInheritance = category.Inheritance.PreventInheritance;
        let preventRulesId = ObjectsService.clone(category.Inheritance.PreventRulesId);

        if (preventInheritance === true) {
          preventRulesId = [];
        }

        const mgtCategory = management[propertyOrCategoryName];
        let mgtHasInheritance = true;
        if (!mgtCategory || !mgtCategory.Inheritance) {
          mgtHasInheritance = false;
        }

        if ((mgtHasInheritance && !ObjectsService.isSameArray(preventRulesId, mgtCategory.Inheritance.PreventRulesId))
          || (!mgtHasInheritance && preventRulesId && preventRulesId.length > 0)) {
          if (preventRulesId) {
            preventRulesId.sort();
          }
          if (mgtCategory && mgtCategory.Inheritance && mgtCategory.Inheritance.preventRulesId) {
            mgtCategory.Inheritance.PreventRulesId.sort();
          }
          newCategory.Inheritance.PreventRulesId = preventRulesId;
          isCategoryUpdated = true;
          updateInfo.updated++;
        } else {
          if (preventRulesId) {
            preventRulesId.sort();
          }
          if (mgtCategory && mgtCategory.Inheritance && mgtCategory.Inheritance.preventRulesId) {
            mgtCategory.Inheritance.PreventRulesId.sort();
          }
          newCategory.Inheritance.PreventRulesId = preventRulesId;
        }

        if ((mgtHasInheritance && preventInheritance !== mgtCategory.Inheritance.PreventInheritance && (!!preventInheritance || !!mgtCategory.Inheritance.PreventInheritance))
          || (!mgtHasInheritance && preventInheritance)) {
          newCategory.Inheritance.PreventInheritance = preventInheritance;
          isCategoryUpdated = true;
          updateInfo.updated++;
        } else {
          newCategory.Inheritance.PreventInheritance = preventInheritance;
        }
      }

      // Handle Properties
      if (properties[propertyOrCategoryName]) {
        for (const propertyName in properties[propertyOrCategoryName]) {
          if (!properties[propertyOrCategoryName].hasOwnProperty(propertyName)) { continue; }

          let property = properties[propertyOrCategoryName][propertyName];
          if (propertyName === 'ClassificationReassessingDate') {
            property = DateService.dateToString(property);
          }

          if ((management[propertyOrCategoryName] && property !== management[propertyOrCategoryName][propertyName] && (!!property || !!management[propertyOrCategoryName][propertyName]))
            || (!management[propertyOrCategoryName] && property)) {
            newCategory[propertyName] = property;
            isCategoryUpdated = true;
            updateInfo.updated++;
          } else if (propertyName === 'FinalAction') {
            newCategory[propertyName] = property;
          }
        }
      }

      if (isCategoryUpdated) {
        const setAction = {};
        setAction[propertyOrCategoryName] = newCategory;
        updateInfo.rules.push(setAction);
        updateInfo.categories.push(propertyOrCategoryName);
      }
    }

    return updateInfo;
  }

}
