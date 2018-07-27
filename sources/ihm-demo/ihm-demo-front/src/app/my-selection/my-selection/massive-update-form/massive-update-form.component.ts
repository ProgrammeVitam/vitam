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
    let ruleInformation = new RuleInformation();
    ruleInformation.Action = action;
    this.internalSavedRules[category].Rules.push(ruleInformation);
  }

  addNewMetadata(action: MetadataAction) {
    let metadataInformation = new MetadataInformation();
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

  removeRule(category, index) {
    this.internalSavedRules[category].Rules.splice(index, 1);
  }

  removeMetadata(index) {
    this.internalSavedMetadata.splice(index, 1);
  }

  checkAndPutInFormRuleUpdates(): boolean {
    let updates = {};
    let additions = {};
    let deletions = {};
    let nbErrors = 0;
    let nbUpdates = 0;

    for (let categoryKey in this.internalSavedRules) {
      let ruleCategory = this.internalSavedRules[categoryKey];

      // Check updates on FinalAction, ClassificationOwner and ClassificationLevel in the category
      if (ruleCategory.FinalAction != null) {
        updates[categoryKey] = { FinalAction: ruleCategory.FinalAction };
        nbUpdates++;
      }
      if (ruleCategory.ClassificationOwner != null) {
        updates[categoryKey] = { ClassificationOwner: ruleCategory.ClassificationOwner };
        nbUpdates++;
      }
      if (ruleCategory.ClassificationLevel != null) {
        updates[categoryKey] = { ClassificationLevel: ruleCategory.ClassificationLevel };
        nbUpdates++;
      }

      // Check for rules in category
      if (ruleCategory.Rules.length > 0) {
        for (let rule of ruleCategory.Rules) {
          // /UPDATE rules
          switch(rule.Action) {
            case RuleAction.UPDATE:
              if (!rule.OriginRule || (!rule.Rule && !rule.StartDate)) {
                console.log('Une règle ' + categoryKey + ' ajoutée ne renseigne pas d\'identifiant d\'origine ou ne renseigne pas de Date et d\'identifiant.');
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
                StartDate: rule.StartDate
              });
              nbUpdates++;
              break;
            case RuleAction.ADD:
              // Check for ADD rules (RuleID + StartDate shouldn't be empty
              if (!rule.Rule || !rule.StartDate) {
                console.log('Une règle ' + categoryKey + ' ajoutée ne renseigne pas de Date ou d\'identifiant.');
                nbErrors++;
                break;
              }
              if (!additions[categoryKey] || !additions[categoryKey].Rules) {
                additions[categoryKey] = {Rules: []};
              }
              additions[categoryKey].Rules.push({
                Rule: rule.Rule,
                StartDate: rule.StartDate
              });
              nbUpdates++;
              break;
            case RuleAction.DELETE:
              if (!rule.Rule) {
                console.log('Une règle ' + categoryKey + ' supprimée ne renseigne pas d\'identifiant.');
                nbErrors++;
                break;
              }
              if (!deletions[categoryKey] || !deletions[categoryKey].Rules) {
                deletions[categoryKey] = {Rules: []};
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
        adds: ObjectsService.objectToArray(additions),
        updates: ObjectsService.objectToArray(updates),
        deletes: ObjectsService.objectToArray(deletions)
      };
    } else {
      delete this.form.updateRules;
    }
    return true;
  }

  getMetadataUpdates(): boolean {
    let patterns = [];
    let updates = [];
    let deletions = [];
    let nbErrors = 0;
    let nbUpdates = 0;

    for (let field of this.internalSavedMetadata) {
      switch(field.Action) {
        case MetadataAction.PATTERN:
          if(!field.FieldName || !field.FieldValue || !field.FieldPattern) {
            console.log('Une métadonnée modifiée par pattern ne renseigne pas de nom, de pattern ou de valeur.');
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
            console.log('Une métadonnée modifiée ne renseigne pas de nom ou de valeur.');
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
            console.log('Une métadonnée supprimée ne renseigne pas de nom.');
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
        'Au moins une des métadonnées renseignée (ajout/modification/suppression) est incomplète',
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
    if (!this.checkAndPutInFormRuleUpdates()) couldUpdate = false;
    if (!this.getMetadataUpdates()) couldUpdate = false;

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
