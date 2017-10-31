import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { plainToClass } from 'class-transformer';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { PageComponent } from "../../../common/page/page-component";
import { Rule } from "./rule";

const RULE_KEY_TRANSLATION = {
  RuleValue: 'Intitulé',
  RuleDescription : 'Description',
  RuleId : 'Identifiant',
  RuleType : 'Type',
  CreationDate : 'Date de creation',
  RuleMeasurement : 'Mesure',
  UpdateDate : 'Date de mise à jour',
  RuleDuration: "Durée"
};
const RULE_TYPE_TRANSLATION = {
  "AppraisalRule" : "Durée d'utilité administrative",
  "AccessRule" : "Délai de communicabilité",
  "StorageRule" : "Durée d'utilité courante",
  "DisseminationRule" : "Délai de diffusion",
  "ReuseRule" : "Durée de réutilisation",
  "ClassificationRule" : "Durée de classification",
};

const RULE_MEASUREMENT_TRANSLATION = {
  "Year" : "Année",
  "Month" : "Mois",
  "Day" : "Jour"
};


@Component({
  selector: 'vitam-rule',
  templateUrl: './rule.component.html',
  styleUrls: ['./rule.component.css']
})
export class RuleComponent extends PageComponent {

  rule : Rule;
  arrayOfKeys : string[];
  id: string;
  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Détail de la règle de gestion', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.searchReferentialsService.getRuleById(this.id).subscribe((value) => {
        this.rule = plainToClass(Rule, value.$results)[0];
        let keys = Object.keys(this.rule);
        let ruleType = this.rule.RuleType;
        this.rule.RuleType = RULE_TYPE_TRANSLATION[ruleType] || ruleType;
        let ruleMeasure = this.rule.RuleMeasurement;
        this.rule.RuleMeasurement = RULE_MEASUREMENT_TRANSLATION[ruleMeasure] || ruleMeasure;
        let rule = this.rule;
        this.arrayOfKeys = keys.filter(function(key) {
          return key != '_id' && !!rule[key] && rule[key].length > 0;
        });
      });
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Référentiel des règles de gestion', routerLink: 'admin/search/rule'},
        {label: this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.rule[key];
  }

  getKeyName(key: string) {
    return RULE_KEY_TRANSLATION[key] || key;
  }

}
