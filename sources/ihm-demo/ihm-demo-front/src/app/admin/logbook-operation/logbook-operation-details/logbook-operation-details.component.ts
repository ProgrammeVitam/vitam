import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PageComponent } from "../../../common/page/page-component";
import { BreadcrumbElement, BreadcrumbService } from "../../../common/breadcrumb.service";
import { Title } from "@angular/platform-browser";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Admin', routerLink: ''},
  {label: 'Journal des opérations', routerLink: 'admin/logbookOperation'},
  {label: 'Détail d\'une opération', routerLink: 'admin/logbookOperation/:id'}
];

@Component({
  selector: 'vitam-logbook-operation-details',
  templateUrl: './logbook-operation-details.component.html',
  styleUrls: ['./logbook-operation-details.component.css']
})
export class LogbookOperationDetailsComponent extends PageComponent {
  id: string;

  constructor(private route: ActivatedRoute,
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Détail d`une opération', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.params.subscribe( params => {
      this.id = params['id'];
      this.breadcrumb[this.breadcrumb.length - 1] = {
        label: 'Détail d\'une opération (' + this.id + ')',
        routerLink: 'admin/logbookOperation/' + this.id
      };
      this.breadcrumbService.changeState(this.breadcrumb);
    });
  }

}
