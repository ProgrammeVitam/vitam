import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { HttpHeaders } from '@angular/common/http';
import { SelectItem } from 'primeng/api';
import { Subscription } from "rxjs/Subscription";

import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { ResourcesService } from '../../common/resources.service';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Configuration de la plateforme', routerLink: 'admin/configuration'}
];

@Component({
  selector: 'vitam-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.css']
})
export class ConfigurationComponent extends PageComponent {
  strategyList: Array<SelectItem>;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private resourcesService: ResourcesService) {
    super('Configuration de la plateforme', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit(): Subscription {
    this.getStrategies();
    return null;
  }

  getStrategies() {
    return this.resourcesService.get("strategies",new HttpHeaders().set('X-Tenant-Id', "1")).subscribe(
      (response) => {
        this.strategyList = response.map(
          (strategy) => {
            return {label: strategy.id, value: strategy}
          }
        );
      }
    )
  }

}
