import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService, BreadcrumbElement} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {PageComponent} from '../../../common/page/page-component';
import {Agency} from './agency';
import {ErrorService} from '../../../common/error.service';
import {ArchiveUnitService} from '../../../archive-unit/archive-unit.service';

@Component({
  selector: 'vitam-agencies',
  templateUrl: './agencies.component.html',
  styleUrls: ['./agencies.component.css']
})
export class AgenciesComponent extends PageComponent {

  newBreadcrumb: BreadcrumbElement[];
  agency: Agency;
  hasUnit: boolean;
  id: string;
  panelHeader: string;

  constructor(private activatedRoute: ActivatedRoute, public router: Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService, private errorService: ErrorService) {
    super('Détail du service agent', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.getDetail();
      this.updateBreadcrumb(params['type']);
    });
  }

  updateBreadcrumb(type: string) {
    if (type === 'accessionRegister') {
      this.newBreadcrumb = [
        {label: 'Recherche', routerLink: ''},
        {label: 'Recherche par service producteur', routerLink: 'admin/accessionRegister'},
        {label: 'Détail du service producteur ' + this.id, routerLink: ''}
      ];
      this.panelHeader = 'Détail du service producteur';
    } else {
      this.newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Services agents', routerLink: 'admin/search/agencies'},
        {label: 'Détail du service agent ' + this.id, routerLink: ''}
      ];
      this.panelHeader = 'Détail du service agent';
    }
    this.setBreadcrumb(this.newBreadcrumb);
  }

  getDetail() {
    this.searchReferentialsService.getAgenciesById(this.id).subscribe(
      (value) => {
        this.agency = plainToClass(Agency, value.$results)[0];
        this.searchReferentialsService.getFundRegisterById(this.agency.Identifier, 1).subscribe((value) => {
            if (value.$hits.total == 1) {
              this.hasUnit = true;
            }
          }, () => {
            this.hasUnit = false;
          }
        )
      }, (error) => {
        this.errorService.handle404Error(error);
      });
  }

  goToSummaryRegisterPage() {
    if (this.router.url.indexOf('/agencies/accessionRegister/') > -1) {
      this.router.navigate(['admin/accessionRegister/accessionRegister/' + encodeURIComponent(this.id)]);
    } else {
      this.router.navigate(['admin/accessionRegister/all/' + encodeURIComponent(this.id)]);
    }
  }
}
