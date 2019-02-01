import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {BaseInheritedItem, InheritedRule, ManagementModel} from '../management-model';
import { ArchiveUnitHelper } from '../../../archive-unit.helper';
import { ComputeRulesUtilsService } from '../compute-rules-utils.service';
import { DateService } from '../../../../common/utils/date.service';
import {UpdateInfo, UpdatePropertiesModel} from '../update-management-model';
import {ConfirmationService} from 'primeng/api';
import {ArchiveUnitService} from '../../../archive-unit.service';

@Component({
  selector: 'vitam-rules-update-mode',
  templateUrl: './rules-update-mode.component.html',
  styleUrls: ['./rules-update-mode.component.css']
})
export class RulesUpdateModeComponent implements OnInit, OnChanges{
  @Input() computedData: ManagementModel;
  @Input() management: any;
  @Input() unitId: string;
  @Output() switchUpdate = new EventEmitter<any>();

  MANAGEMENT_RULES_CATEGORIES = this.archiveUnitHelper.rulesCategories;
  FR_LOCALE = DateService.vitamFrLocale;

  updateValues: ManagementModel;
  localProperties: UpdatePropertiesModel;
  inheritedItems: ManagementModel;
  deletedRules: string[] = [];

  saveRunning = false;
  displayOK = false;
  displayKO = false;
  errorMessage = '';

  confirmation: any = {};

  constructor(public archiveUnitHelper: ArchiveUnitHelper, public computeRulesUtilsService: ComputeRulesUtilsService,
              public archiveUnitService: ArchiveUnitService) { }

  ngOnChanges(changes: SimpleChanges): void {
    if(changes.computedData){
      this.init();
    }
  }

  ngOnInit() {
    this.init();
  }

  init(){
    const updateStructure = this.computeRulesUtilsService.getUpdateStructure(this.computedData, this.unitId);
    this.updateValues = updateStructure.updateStructure;
    this.localProperties = updateStructure.localProperties;
    this.inheritedItems = updateStructure.inheritedItems;
  }

  addRule(category: string) {
    this.updateValues[category].Rules.push({
      Rule: '',
      StartDate: '',
      newRule: true
    });
  }

  doOrUndoRemoveRule(category: string, index: number, rule: InheritedRule) {
    const uniqueRuleIdentifier = `${category}-${rule.Rule}`;
    const deletedRuleIndex = this.deletedRules.indexOf(uniqueRuleIdentifier);
    if (rule.newRule) {
      this.updateValues[category].Rules.splice(index, 1);
    } else if (deletedRuleIndex !== -1) {
      this.deletedRules.splice(deletedRuleIndex, 1);
    } else {
      this.deletedRules.push(uniqueRuleIdentifier);
    }
  }

  disableUpdateMode() {
    this.switchUpdate.emit(false);
  }

  computeConfirmationMessageInfo(info: UpdateInfo) {
    const rulesCategoriesFr =
      info.categories.map(
        title => {
          const ruleCategory = this.MANAGEMENT_RULES_CATEGORIES.filter(x => x.rule === title);
          return ruleCategory.length === 1 ? ruleCategory[0].label : '';
        });

    this.confirmation = {
      display: true,
      rulesCategoriesFr: rulesCategoriesFr,
      nbCategories: info.categories.length,
      deleted: info.deleted,
      added: info.added,
      updated: info.updated,
      rules: info.rules,
      NeedAuthorization: info.NeedAuthorization,
      ArchiveUnitProfile: info.ArchiveUnitProfile
    }
  }

  buildErrorMessage(error): string {

    switch (error.description) {
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_EXIST':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_EXIST':
        return 'La règle ajoutée n\'existe pas dans le référentiel.';
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_CATEGORY':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_CATEGORY':
        return 'La règle de gestion ajoutée n\'est pas de la bonne catégorie.';
      case 'ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_START_DATE':
      case 'ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_START_DATE':
        return 'La date de départ de la règle de gestion est supérieure ou égale à 9000.';
      default:
        if (error.code === '020102') {
          return 'Droits insufisant pour modifier les métadonnées de gestion';
        }
        return 'Echec lors de la mise à jour des règles.';
    }
  }

  doSave(rules, needAuthorization, archiveUnitProfile) {
    this.confirmation.display = false;
    this.saveRunning = true;
    const request = [];

    request.push({'UpdatedRules': rules});
    if (needAuthorization !== undefined) {
      request.push({fieldId: '#management.NeedAuthorization', newFieldValue: needAuthorization});
    }
    if (archiveUnitProfile !== undefined) {
      request.push({fieldId: 'ArchiveUnitProfile', newFieldValue: archiveUnitProfile});
    }

    this.archiveUnitService.updateMetadata(this.unitId, request).subscribe(
      () => {
        this.archiveUnitService.getDetailsWithInheritedRules(this.unitId)
          .subscribe((data) => {
            this.switchUpdate.emit(data.$results[0]);
            this.saveRunning = false;
            this.displayOK = true;
          }, () => {
            this.saveRunning = false;
          });
      },
      (error) => {
        this.saveRunning = false;
        this.errorMessage = this.buildErrorMessage(error.error);
        this.displayKO = true;
      }
    );
  }

  saveUpdate() {
    const info: UpdateInfo =
      this.computeRulesUtilsService.getUpdateInformation(this.updateValues, this.management, this.deletedRules, this.localProperties);

    const rules = info.rules;
    if (rules.length > 0 || info.NeedAuthorization !== undefined || info.ArchiveUnitProfile !== undefined) {
      this.computeConfirmationMessageInfo(info)
    } else {
      this.disableUpdateMode();
    }
  }

}
