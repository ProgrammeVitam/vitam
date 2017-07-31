import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';

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

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Administration des collections', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {}

}
