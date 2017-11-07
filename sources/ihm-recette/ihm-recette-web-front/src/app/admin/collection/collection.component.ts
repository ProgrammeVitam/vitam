import {Component} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {PageComponent} from '../../common/page/page-component';
import {ResourcesService} from "../../common/resources.service";
import {TenantService} from "../../common/tenant.service";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Purge des collections', routerLink: 'admin/collection'}
];

@Component({
  selector: 'vitam-collection',
  templateUrl: './collection.component.html',
  styleUrls: ['./collection.component.css']
})
export class CollectionComponent extends PageComponent {

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, resourcesService: ResourcesService, tenantService: TenantService) {
    super('Administration des collections', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
  }

}
