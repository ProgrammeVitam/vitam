import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService, BreadcrumbElement} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {PageComponent} from '../../../common/page/page-component';
import {ErrorService} from '../../../common/error.service';
import {Scenario} from './scenario';
import {ArchiveUnitHelper} from '../../../archive-unit/archive-unit.helper';
import {ReferentialHelper} from '../../referential.helper';

@Component({
  selector: 'vitam-scenarios',
  templateUrl: './scenarios.component.html',
  styleUrls: ['./scenarios.component.css']
})
export class ScenariosComponent extends PageComponent {

  newBreadcrumb: BreadcrumbElement[];
  scenario: Scenario;
  id: string;
  panelHeader: string;
  keyToLabel :  (x: string) => string;

  constructor(private activatedRoute: ActivatedRoute, public router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService, private errorService: ErrorService, private referentialHelper : ReferentialHelper) {
    super('Détail du Scénario', [], titleService, breadcrumbService);
    const translations = this.referentialHelper.getScenarioTranslations();
    this.keyToLabel = (field: string) => {
      const value = translations[field];
      if (translations[field]) {
        return value;
      } else {
        return field.split('.').pop();
      }
    };
  }



  pageOnInit() {
    this.activatedRoute.params.subscribe(
      params => {
        this.id = params['id'];
        this.searchReferentialsService.getScenarioById(this.id).subscribe((value) => {
          this.scenario = plainToClass(Scenario, value.$results)[0];
          this.panelHeader = 'Détail du scénario';
        }, (error) => {
          this.errorService.handle404Error(error);
        });
        let newBreadcrumb = [
          {label: 'Administration', routerLink: ''},
          {label: 'Scenarios', routerLink: 'admin/search/scenarios'},
          {label: 'Détail du scenario ' + this.id, routerLink: ''}
        ];

        this.setBreadcrumb(newBreadcrumb);
      });
  }
}
