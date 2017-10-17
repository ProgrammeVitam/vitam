import {Component, OnInit, Input, OnChanges} from '@angular/core';
import { DatePipe } from '@angular/common';
import {ArchiveUnitHelper} from "../../archive-unit.helper";
import {ArchiveUnitService} from "../../archive-unit.service";
import {ConfirmationService} from "primeng/primeng";

@Component({
  selector: 'vitam-archive-rule-bloc',
  templateUrl: './archive-rule-bloc.component.html',
  styleUrls: ['./archive-rule-bloc.component.css']
})
export class ArchiveRuleBlocComponent implements OnInit, OnChanges {
  @Input() inheritedRules;
  @Input() management;
  @Input() id;
  public rulesCategories = this.archiveUnitHelper.rulesCategories;
  public finalActions = {
    StorageRule: this.archiveUnitHelper.storageFinalAction,
    AppraisalRule: this.archiveUnitHelper.appraisalFinalAction
  };
  public titles = {};
  public displayDetails = {};
  public update = false;
  public updatedFields: any = {};
  public saveOriginal = '';

  constructor(public archiveUnitHelper: ArchiveUnitHelper, private archiveUnitService: ArchiveUnitService,
              public confirmationService: ConfirmationService) {

  }

  ngOnChanges() {
    this.saveOriginal = JSON.stringify(this.management);
    this.displayDetails = {};
  }

  ngOnInit() {
    this.saveOriginal = JSON.stringify(this.management);
  }

  isInherited(id) {
    return id === this.id ? 'Non' : 'Oui';
  }

  toogleDetails(category, rule, id) {
    this.displayDetails[rule + '.' + id] = !this.displayDetails[rule + '.' + id];
    if (this.inheritedRules[category][rule][id].path.length === 1 && (!this.titles[rule + '.' + id])) {
      this.archiveUnitService.getDetails(id)
        .subscribe(response => {
          this.titles[rule + '.' + id] = response.$results[0].Title;
        });
    }
  }

  switchUpdateMode() {
    this.update = !this.update;
    if (!this.update) {
      this.management = JSON.parse(this.saveOriginal);
      this.updatedFields = {};
    } else {
      this.initUpdatedRules();
    }
  }

  checkUpdate(category, index, rule) {
    // FIXME Errors with StartDate ?
    let mgtRule = this.management[category].Rules[index];
    return rule.ruleId !== mgtRule.Rule ||
    rule.FinalAction !== mgtRule.FinalAction ||
    rule.StartDate !== mgtRule.StartDate
  }

  getUpdatedRules() {
    // ruleCategory ~= this.updatedFields
    // updatedRules = the array that must be updated and pushed in request. updatedRules = [{'CategName': {'Rules': ..., 'Inheritance': ...}, {...}, ...];

    var updateInfo = {
      updated: 0,
      added: 0,
      deleted: 0,
      categories: [],
      rules: []
    };


    for (let category in this.updatedFields) {
      // categoryName = need looping over categories in this function and push in updatedRules[]
      var isCategoryUpdated = false;
      var newRules = [];
      for (let i=0, len = this.updatedFields[category].Rules.length; i < len; i++) {
        let rule = this.updatedFields[category].Rules[i];
        rule.StartDate = new DatePipe('fr-FR').transform(rule.StartDate, 'yyyy-MM-dd');
        if (!rule.StartDate) delete rule.StartDate;
        if (rule.newRule) {
          // New Rule
          isCategoryUpdated = true;
          let addedRule = JSON.parse(JSON.stringify(rule));
          delete addedRule.newRule;
          newRules.push(addedRule);
          updateInfo.added ++;
        } else if (rule.oldRule) {
          // Deleted rule
          isCategoryUpdated = true;
          updateInfo.deleted ++;
        } else if (this.checkUpdate(category, i, rule)) {
          // Updated rule
          isCategoryUpdated = true;
          newRules.push(rule);
          updateInfo.updated ++;
        } else {
          // Non-Updated Old Rule
          newRules.push(rule);
        }
      }
      if (isCategoryUpdated) {
        var setAction = {};
        setAction[category] = newRules;
        updateInfo.rules.push(setAction);
        updateInfo.categories.push(category);
      }
    }

    return updateInfo;
  }

  hasFinalActionEmptyRule(category) {
    if (category === 'StorageRule' || category === 'AccessRule') {
      if (this.management[category] && this.management[category].Rules) {
        for (let rule of this.management[category].Rules) {
          if (!rule.Rule && rule.FinalAction) {
            return true;
          }
        }
      }
    }
    return false;
  }

  getFinalActionRules(category) {
    let emptyRules = [];
    if (this.management[category] && this.management[category].Rules) {
      for (let rule of this.management[category].Rules) {
        if (!rule.Rule && rule.FinalAction) {
          emptyRules.push(rule);
        }
      }
    }
    return emptyRules;
  }

  saveUpdate() {
    let info = this.getUpdatedRules();
    this.confirmationService.confirm({
      message:
      `Vous vous apprétez à modifier les catégories ${info.categories} pour:<br />
      - Supprimer ${info.deleted} règles,<br />
      - Modifier ${info.updated} règles,<br />
      - Ajouter ${info.added} règles`,
      accept: () => {
        let request = [];
        let rules = info.rules;
        if (rules.length === 0) {
        } else {
          request.push({'UpdatedRules': rules});
          // TODO Back Request
        }
        this.switchUpdateMode();
      }
    });


  }

  removeRule(category, index) {
    if (this.updatedFields[category].Rules[index].newRule) {
      this.updatedFields[category].Rules.splice(index, 1);
    } else {
      this.updatedFields[category].Rules[index].oldRule = !this.updatedFields[category].Rules[index].oldRule;
    }
  }


  initUpdatedRules() {
    for(var category of this.rulesCategories) {
      if (this.management[category.rule]) {
        let rules = [];
        for (var rule of this.management[category.rule].Rules) {
          if (rule.StartDate) {
            rule.StartDate = new Date(rule.StartDate);
          } else {
            rule.StartDate = '';
          }
          rules.push(rule);
        }

        //let inheritance = this.management[category.rule].Inheritance;

        this.updatedFields[category.rule] = {
          Rules: rules/*,
           Inheritance: inheritance ? inheritance : {PreventRulesId:[]}*/
        };
      } else {
        this.updatedFields[category.rule] = {
          Rules: []/*,
           Inheritance:{
           PreventRulesId:[]
           }*/
        }
      }
    }
  }

  addRule(category) {
    this.updatedFields[category].Rules.push({
      Rule: '',
      StartDate: '',
      newRule: true
    });
  }

}