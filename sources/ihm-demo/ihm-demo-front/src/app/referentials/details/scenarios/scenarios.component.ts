import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService, BreadcrumbElement} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {PageComponent} from '../../../common/page/page-component';
import {ErrorService} from '../../../common/error.service';
import {Scenario} from './scenario';

@Component({
  selector: 'vitam-scenarios',
  templateUrl: './scenarios.component.html',
  styleUrls: ['./scenarios.component.css']
})
export class ScenariosComponent extends PageComponent {

  newBreadcrumb: BreadcrumbElement[];
  scenario: Scenario;
  hasUnit: boolean;
  id: string;
  panelHeader: string;

  constructor(private activatedRoute: ActivatedRoute, public router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService, private errorService: ErrorService) {
    super('Détail du Scénario', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
    });
  }
}
