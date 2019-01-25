import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ArchiveUnitHelper} from "../../../archive-unit/archive-unit.helper";
import {RuleAction, RuleInformation, RulesInformation} from "./rules-information";
import {DateService} from "../../../common/utils/date.service";
import {MetadataAction, MetadataInformation} from './metadata-information';
import {DialogService} from '../../../common/dialog/dialog.service';
import {ObjectsService} from '../../../common/utils/objects.service';

@Component({
  selector: 'vitam-massive-update-form',
  templateUrl: './massive-update-form.component.html',
  styleUrls: ['./massive-update-form.component.css']
})
export class MassiveUpdateFormComponent implements OnInit {

  @Input() form: any;
  @Output() launchUpdate: EventEmitter<boolean> = new EventEmitter<boolean>();

  public internalSavedRules: RulesInformation = new RulesInformation();
  public internalSavedMetadata: MetadataInformation[] = [];
  public rulesCategories = this.archiveUnitHelper.rulesCategories;
  public finalActions = {
    StorageRule: this.archiveUnitHelper.storageFinalAction,
    AppraisalRule: this.archiveUnitHelper.appraisalFinalAction
  };
  frLocale = DateService.vitamFrLocale;
  RULE_ACTIONS = RuleAction;
  METADATA_ACITONS = MetadataAction;

  constructor(private archiveUnitHelper: ArchiveUnitHelper, private dialogService: DialogService) { }

  ngOnInit() {
  }

  addNewRule(action: RuleAction, category: string) {
    const ruleInformation = new RuleInformation();
    ruleInformation.Action = action;
    this.internalSavedRules[category].Rules.push(ruleInformation);
  }

  addNewMetadata(action: MetadataAction) {
    const metadataInformation = new MetadataInformation();
    metadataInformation.Action = action;
    this.internalSavedMetadata.push(metadataInformation);
  }

  getRuleActionLabel(action: RuleAction) {
    switch(action) {
      case RuleAction.ADD: return 'Ajout';
      case RuleAction.UPDATE: return 'Mise à jour';
      case RuleAction.DELETE: return 'Suppression';
    }
  }

  getRuleActionIcon(action: RuleAction) {
    switch (action) {
      case RuleAction.ADD: return 'fa fa-plus';
      case RuleAction.UPDATE: return 'fa fa-pencil';
      case RuleAction.DELETE: return 'fa fa-trash';
      default: return '';
    }
  }

  getMetadataActionLabel(action: MetadataAction) {
    switch(action) {
      case MetadataAction.PATTERN: return 'Modification de chaîne de caractères';
      case MetadataAction.UPDATE: return 'Mise à jour';
      case MetadataAction.DELETE: return 'Vider';
    }
  }

  getMetadataActionIcon(action: MetadataAction) {
    switch (action) {
      case MetadataAction.PATTERN: return 'fa fa-pencil';
      case MetadataAction.UPDATE: return 'fa fa-pencil';
      case MetadataAction.DELETE: return 'fa fa-times';
      default: return '';
    }
  }

  toogleStartDate(rule: RuleInformation, removeStartDate: boolean) {
    delete rule.StartDate;
    rule.DeleteStartDate = removeStartDate;
  }

  removeRule(category, index) {
    this.internalSavedRules[category].Rules.splice(index, 1);
  }

  removeMetadata(index) {
    this.internalSavedMetadata.splice(index, 1);
  }

  getPreventInheritance(preventInheritance) {
    if (preventInheritance === undefined || preventInheritance === null) {
        return 'Aucune modification';
    }

    return preventInheritance ? 'Bloquer l\'héritage' : 'Hériter des parents';
  }

  getReassessingAuthorizationLabel(reassessingAuthorization) {
    if (reassessingAuthorization === undefined || reassessingAuthorization === null) {
      return 'Aucune modification';
    }

    return reassessingAuthorization ? 'Validation humaine nécessaire' : 'Non soumis à validation';
  }

  checkAndPutInFormRuleUpdates(): boolean {
    const updates = {};
    const additions = {};
    const deletions = {};
    const updateMetadata: any = {};
    const deleteMetadata: any = {};
    let nbErrors = 0;
    let nbUpdates = 0;

    const categoryNames: string[] = this.rulesCategories.map(x => x.rule);

    if (this.internalSavedRules.RemoveArchiveUnitProfile) {
      deleteMetadata.ArchiveUnitProfile = '';
      nbUpdates++;
    } else if (!!this.internalSavedRules.ArchiveUnitProfile) {
      updateMetadata.ArchiveUnitProfile = this.internalSavedRules.ArchiveUnitProfile;
      nbUpdates++;
    }

    for (const categoryKey in this.internalSavedRules) {
      if (categoryNames.indexOf(categoryKey) === -1) {
        continue;
      }
      const ruleCategory = this.internalSavedRules[categoryKey];

      // Check updates on FinalAction, ClassificationOwner and ClassificationLevel in the category
      const categoryProperties: any = {};
      let propertiesUpdated = false;
      if (ruleCategory.FinalAction != null) {
        propertiesUpdated = true;
        categoryProperties.FinalAction = ruleCategory.FinalAction;
        nbUpdates++;
      }
      if (ruleCategory.ClassificationOwner != null && ruleCategory.ClassificationAudience !== '') {
        propertiesUpdated = true;
        categoryProperties.ClassificationOwner = ruleCategory.ClassificationOwner;
        nbUpdates++;
      }
      if (ruleCategory.ClassificationLevel != null && ruleCategory.ClassificationAudience !== '') {
        propertiesUpdated = true;
        categoryProperties.ClassificationLevel = ruleCategory.ClassificationLevel;
        nbUpdates++;
      }
      if (ruleCategory.ClassificationAudience != null && ruleCategory.ClassificationAudience !== '') {
        propertiesUpdated = true;
        categoryProperties.ClassificationAudience = ruleCategory.ClassificationAudience;
        nbUpdates++;
      } else if (ruleCategory.removeClassificationAudience) {
        if (!deletions[categoryKey]) {
          deletions[categoryKey] = { ClassificationAudience: '' }
        } else {
          deletions[categoryKey].ClassificationAudience = '';
        }
        nbUpdates++;
      }
      if (ruleCategory.ClassificationReassessingDate != null && ruleCategory.ClassificationAudience !== '') {
        propertiesUpdated = true;
        categoryProperties.ClassificationReassessingDate = ruleCategory.ClassificationReassessingDate;
        nbUpdates++;
      } else if (ruleCategory.removeClassificationReassessingDate) {
        if (!deletions[categoryKey]) {
          deletions[categoryKey] = { ClassificationReassessingDate: '' }
        } else {
          deletions[categoryKey].ClassificationReassessingDate = '';
        }
        nbUpdates++;
      }
      if (ruleCategory.NeedReassessingAuthorization != null) {
        propertiesUpdated = true;
        categoryProperties.NeedReassessingAuthorization = ruleCategory.NeedReassessingAuthorization;
        nbUpdates++;
      }

      // Check for Inheritance properties in category
      if (ruleCategory.PreventInheritance != null) {
        propertiesUpdated = true;
        categoryProperties.PreventInheritance = ruleCategory.PreventInheritance;
        nbUpdates++;
      }
      if (ruleCategory.PreventRuleIds != null && ruleCategory.PreventRuleIds.length > 0) {
        propertiesUpdated = true;
        categoryProperties.PreventRulesId = ruleCategory.PreventRuleIds;
        nbUpdates++;
      }
      if (ruleCategory.AllowRuleIds != null && ruleCategory.AllowRuleIds.length > 0) {
        propertiesUpdated = true;
        categoryProperties.PreventRulesId = ruleCategory.AllowRuleIds;
        nbUpdates++;
      }

      if (propertiesUpdated) {
        updates[categoryKey] = categoryProperties;
      }

      // Check for rules in category
      if (ruleCategory.Rules.length > 0) {
        for (const rule of ruleCategory.Rules) {
          // /UPDATE rules
          switch (rule.Action) {
            case RuleAction.UPDATE:
              if (!rule.OriginRule || (!rule.Rule && !rule.StartDate && !rule.DeleteStartDate)) {
                console.warn('Une règle ' + categoryKey + ' ajoutée ne renseigne pas d\'identifiant d\'origine ou ne modifie pas la règle');
                nbErrors++;
                break;
              }
              if (!updates[categoryKey]) {
                updates[categoryKey] = {Rules: []};
              } else if (!updates[categoryKey].Rules) {
                updates[categoryKey].Rules = [];
              }
              updates[categoryKey].Rules.push({
                OldRule: rule.OriginRule,
                Rule: rule.Rule,
                StartDate: rule.StartDate ? DateService.dateToString(rule.StartDate) : null,
                DeleteStartDate: rule.DeleteStartDate
              });
              nbUpdates++;
              break;
            case RuleAction.ADD:
              if (!rule.Rule) {
                console.warn('Une règle ' + categoryKey + ' ajoutée ne renseigne pas de Date ou d\'identifiant.');
                nbErrors++;
                break;
              }
              if (!additions[categoryKey] || !additions[categoryKey].Rules) {
                additions[categoryKey] = {Rules: []};
              }
              let newRule: any = {Rule: rule.Rule};
              if (!!rule.StartDate) {
                newRule.StartDate = DateService.dateToString(rule.StartDate);
              }
              additions[categoryKey].Rules.push(newRule);
              nbUpdates++;
              break;
            case RuleAction.DELETE:
              if (!rule.Rule) {
                console.warn('Une règle ' + categoryKey + ' supprimée ne renseigne pas d\'identifiant.');
                nbErrors++;
                break;
              }
              if (!deletions[categoryKey]) {
                deletions[categoryKey] = {Rules: []};
              } else if (!deletions[categoryKey].Rules) {
                deletions[categoryKey].Rules = [];
              }
              deletions[categoryKey].Rules.push({
                Rule: rule.Rule
              });
              nbUpdates++;
              break;
          }
        }
      }
    }

    if (nbErrors > 0) {
      this.dialogService.displayMessage(
        'Au moins une des règles renseignée (ajout/modification/suppression) est incomplète',
        'Erreur de saisie des modifications de règles'
      );
      delete this.form.updateRules;
      return false;
    }

    if (nbUpdates > 0) {
      this.form.updateRules = {
        add: ObjectsService.objectToArray(additions),
        update: ObjectsService.objectToArray(updates),
        delete: ObjectsService.objectToArray(deletions),
        addOrUpdateMetadata: updateMetadata,
        deleteMetadata: deleteMetadata
      };
    } else {
      delete this.form.updateRules;
    }
    return true;
  }

  getMetadataUpdates(): boolean {
    const patterns = [];
    const updates = [];
    const deletions = [];
    let nbErrors = 0;
    let nbUpdates = 0;

    for (const field of this.internalSavedMetadata) {

      if (field.FieldName === 'ArchiveUnitProfile') {
        nbErrors++;
        console.warn('Impossible de modifier ArchiveUnitProfile. Utiliser le bloc de métadonnées de gestion');
        break;
      }

      switch (field.Action) {
        case MetadataAction.PATTERN:
          if (!field.FieldName || !field.FieldValue || !field.FieldPattern) {
            console.warn('Une métadonnée modifiée par pattern ne renseigne pas de nom, de pattern ou de valeur.');
            nbErrors++;
            break;
          }
          patterns.push({
            FieldName: field.FieldName,
            FieldValue: field.FieldValue,
            FieldPattern: field.FieldPattern
          });
          nbUpdates++;
          break;
        case MetadataAction.UPDATE:
          if (!field.FieldName || !field.FieldValue) {
            console.warn('Une métadonnée modifiée ne renseigne pas de nom ou de valeur.');
            nbErrors++;
            break;
          }
          updates.push({
            FieldName: field.FieldName,
            FieldValue: field.FieldValue
          });
          nbUpdates++;
          break;
        case MetadataAction.DELETE:
          if (!field.FieldName) {
            console.warn('Une métadonnée supprimée ne renseigne pas de nom.');
            nbErrors++;
            break;
          }
          deletions.push({
            FieldName: field.FieldName
          });
          nbUpdates++;
          break;
      }
    }

    if (nbErrors > 0) {
      this.dialogService.displayMessage(
        'Au moins une des métadonnées renseignée (ajout/modification/suppression) est incomplète ou non supportée',
        'Erreur de saisie des modifications de métadonnées'
      );
      delete this.form.updateMetadata;
      return false;
    }

    if (nbUpdates > 0) {
      this.form.updateMetadata = {
        updates: updates,
        patterns: patterns,
        deletions: deletions
      };
    } else {
      delete this.form.updateMetadata;
    }
    return true;
  }

  getUpdates(isOnSelection) {
    let couldUpdate = true;
    if (!this.checkAndPutInFormRuleUpdates()) { couldUpdate = false; }
    if (!this.getMetadataUpdates()) { couldUpdate = false; }

    if (!couldUpdate) {
      return;
    }

    if (!this.form.updateRules && !this.form.updateMetadata) {
      this.dialogService.displayMessage(
        'Aucune modifications n\'a été saisie, la mise à jour de masse n\'a pas été lancée',
        'Aucune modifications'
      );
      return;
    }

    this.launchUpdate.emit(isOnSelection);
  }

}
