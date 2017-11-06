import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { ArchiveUnitService } from '../../../archive-unit/archive-unit.service';
import { PageComponent } from "../../../common/page/page-component";
import { Agency } from './agency';

@Component({
  selector: 'vitam-agencies',
  templateUrl: './agencies.component.html',
  styleUrls: ['./agencies.component.css']
})
export class AgenciesComponent extends PageComponent {

  agency : Agency;
  hasUnit : boolean;
  id: string;
  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Détail du service agent', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Référentiel des services agents', routerLink: 'admin/search/agencies'},
        {label: 'Détail du service agent ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  getDetail() {
    this.searchReferentialsService.getAgenciesById(this.id).subscribe((value) => {
      this.agency = plainToClass(Agency, value.$results)[0];
      this.searchReferentialsService.getFundRegisterById(this.agency.Identifier).subscribe((value) => {
        if (value.$hits.total == 1) {
          this.hasUnit = true;
        }
      }, error => this.hasUnit = false)
    });
  }

  goToSummaryRegisterPage() {
    this.router.navigate(['admin/accessionRegister/' + this.id]);
  }

}
