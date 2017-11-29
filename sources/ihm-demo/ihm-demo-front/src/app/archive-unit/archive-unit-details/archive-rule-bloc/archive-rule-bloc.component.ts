import {Component, OnInit, Input, OnChanges} from '@angular/core';
import {DatePipe} from '@angular/common';
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
  public saveRunning = false;
  public displayOK = false;
  public displayKO = false;
  public messageToDisplay;


  frLocale = {
    dayNames: ["Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"],
    dayNamesShort: ["Dim.", "Lun.", "Mar.", "Mer.", "Jeu.", "Ven.", "Sam."],
    dayNamesMin: ["Di", "Lu", "Ma", "Me", "Je", "Ve", "Sa"],
    monthNames: ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Aout", "Septembre", "Octobre", "Novembre", "Décembre"],
    monthNamesShort: ["Jan", "Fév", "Mars", "Avr", "Mai", "Juin", "Juil", "Aou", "Sep", "Oct", "Nov", "Dec"],
    firstDayOfWeek: 1,
    today: "Aujourd'hui",
    clear: 'Vider'
  };

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

  getMgtRule(category, id) {
    if (!this.management[category]) {
      return null;
    }
    for (let rule of this.management[category].Rules) {
      if (rule.Rule == id) {
        return rule;
      }
    }
    return null;
  }

  checkUpdate(category, rule) {
    // FIXME Errors with StartDate ?
    let mgtRule = this.getMgtRule(category, rule.oldId);
    if (!this.management[category] || !mgtRule) {
      return false;
    }
    return rule.Rule !== mgtRule.Rule ||
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
      for (let i = 0, len = this.updatedFields[category].Rules.length; i < len; i++) {
        let rule = this.updatedFields[category].Rules[i];
        rule.StartDate = new DatePipe('fr-FR').transform(rule.StartDate, 'yyyy-MM-dd');
        if (!rule.inherited) {
          if (!rule.StartDate) delete rule.StartDate;
          if (rule.newRule) {
            // New Rule
            isCategoryUpdated = true;
            let addedRule = JSON.parse(JSON.stringify(rule));
            delete addedRule.newRule;
            newRules.push(addedRule);
            updateInfo.added++;
          } else if (rule.oldRule) {
            // Deleted rule
            isCategoryUpdated = true;
            updateInfo.deleted++;
          } else if (this.checkUpdate(category, rule)) {
            // Updated rule
            isCategoryUpdated = true;
            let updatedRule = JSON.parse(JSON.stringify(rule));
            delete updatedRule.oldId;
            delete updatedRule.EndDate;
            newRules.push(updatedRule);
            updateInfo.updated++;
          } else {
            // Non-Updated Old Rule
            let updatedRule = JSON.parse(JSON.stringify(rule));
            delete updatedRule.oldId;
            delete updatedRule.EndDate;
            newRules.push(updatedRule);
          }
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
    let rules = info.rules;
    if (rules.length > 0) {
      if (info.categories.length < 2) {
        this.messageToDisplay =  `Vous vous apprétez à modifier la catégorie ${info.categories} pour:<br />`
      } else {
        this.messageToDisplay =  `Vous vous apprétez à modifier les catégories ${info.categories} pour:<br />`
      }
      if (info.deleted < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Supprimer ${info.deleted} règle,<br />`;
      } else {
        this.messageToDisplay = this.messageToDisplay +  `- Supprimer ${info.deleted} règles,<br />`;
      }
      if (info.updated < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Modifier ${info.updated} règle,<br />`
      } else {
        this.messageToDisplay = this.messageToDisplay + `- Modifier ${info.updated} règles,<br />`
      }
      if (info.added < 2) {
        this.messageToDisplay = this.messageToDisplay + `- Ajouter ${info.added} règle`
      } else {
        this.messageToDisplay = this.messageToDisplay + `- Ajouter ${info.added} règles`
      }
      this.confirmationService.confirm({
        message: this.messageToDisplay,
        accept: () => {
          this.saveRunning = true;
          let request = [];
          request.push({'UpdatedRules': rules});
          this.archiveUnitService.updateMetadata(this.id, request).subscribe(
            (value) => {
              this.archiveUnitService.getDetails(this.id)
                .subscribe((data) => {
                  let response = data.$results[0];
                  this.inheritedRules = response.inheritedRule;
                  this.management = response['#management'];
                  this.id = response['#id'];
                  this.saveOriginal = JSON.stringify(this.management);
                  this.switchUpdateMode();
                  this.saveRunning = false;
                  this.displayOK = true;
                }, (error) => {
                  this.saveRunning = false;
                });
            },
            (error) => {
              this.saveRunning = false;
              this.displayKO = true;
            }
          );
        }
      });
    } else {
      this.switchUpdateMode();
    }

  }

  removeRule(category, index, rule) {
    if (rule.newRule) {
      this.updatedFields[category].Rules.splice(index, 1);
    } else if (rule.inherited) {
      let preventedIndex = this.updatedFields[category].Inheritance.PreventRulesId.indexOf(rule.Rule);
      if (preventedIndex == -1) {
        this.updatedFields[category].Inheritance.PreventRulesId.push(rule.Rule);
      } else {
        this.updatedFields[category].Inheritance.PreventRulesId.splice(preventedIndex, 1);
      }
    } else {
      rule.oldRule = !rule.oldRule;
    }
  }


  initUpdatedRules() {
    for (let category of this.rulesCategories) {
      if (this.management[category.rule]) {
        let rules = [];
        let ruleIds = [];
        for (let rule of this.management[category.rule].Rules) {
          let updatedRule = JSON.parse(JSON.stringify(rule));
          if (updatedRule.StartDate) {
            updatedRule.StartDate = new Date(updatedRule.StartDate);
          } else {
            updatedRule.StartDate = '';
          }
          updatedRule.oldId = updatedRule.Rule;
          let finalAction = this.management[category.rule].FinalAction;
          if (finalAction) {
            updatedRule.FinalAction = finalAction;
          }
          rules.push(updatedRule);
          ruleIds.push(updatedRule.Rule);
        }

        if (this.inheritedRules[category.rule]) {
          for (let ruleId in this.inheritedRules[category.rule]) {
            if (ruleIds.indexOf(ruleId) !== -1) {
              continue;
            }
            for (let id in this.inheritedRules[category.rule][ruleId]) {
              let inheritedRule = this.inheritedRules[category.rule][ruleId][id];

              let rule: any = {
                Rule: ruleId,
                StartDate: inheritedRule.StartDate ? new Date(inheritedRule.StartDate) : '',
                EndDate: inheritedRule.EndDate,
                inherited: true,
              };
              if (category.rule === 'StorageRule' || category.rule === 'AppraisalRule') {
                rule.FinalAction = inheritedRule.FinalAction;
              }
              rules.push(rule);
              break;
            }
          }
        }

        let inheritance = this.management[category.rule].Inheritance;

        this.updatedFields[category.rule] = {
          Rules: rules,
          Inheritance: inheritance ? JSON.parse(JSON.stringify(inheritance)) : {PreventRulesId: []}
        };
      } else {

        let rules = [];
        if (this.inheritedRules[category.rule]) {
          for (let ruleId in this.inheritedRules[category.rule]) {
            if (this.inheritedRules[category.rule].hasOwnProperty(ruleId)) {
              // add rule
              for (let id in this.inheritedRules[category.rule][ruleId]) {
                if (this.inheritedRules[category.rule][ruleId].hasOwnProperty(id)) {
                  let inheritedRule = this.inheritedRules[category.rule][ruleId][id];

                  let rule: any = {
                    Rule: ruleId,
                    StartDate: inheritedRule.StartDate ? new Date(inheritedRule.StartDate) : '',
                    EndDate: inheritedRule.EndDate,
                    inherited: true
                  };
                  if (category.rule === 'StorageRule' || category.rule === 'AppraisalRule') {
                    rule.FinalAction = inheritedRule.FinalAction;
                  }
                  rules.push(rule);
                  break;
                }
              }
            }
          }
        }

        this.updatedFields[category.rule] = {
          Rules: rules,
          Inheritance: {
            PreventRulesId: []
          }
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