import { Component } from '@angular/core';
import { PageComponent } from "../../../common/page/page-component";
import { BreadcrumbElement, BreadcrumbService } from "../../../common/breadcrumb.service";
import { ActivatedRoute } from "@angular/router";
import { Title } from "@angular/platform-browser";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Ingest', routerLink: ''},
  {label: 'Suivi des opérations d\'entrée', routerLink: 'ingest/logbook'},
  {label: 'Détail d\'une opération d\'entrée', routerLink: 'ingest/logbook/:id'}
];

@Component({
  selector: 'vitam-logbook-details',
  templateUrl: './logbook-details.component.html',
  styleUrls: ['./logbook-details.component.css']
})
export class LogbookDetailsComponent extends PageComponent {
  id: string;

  constructor(private route: ActivatedRoute,
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Détail d\'une opération d\'entrée', breadcrumb, titleService, breadcrumbService);
  }
  pageOnInit() {
    this.route.params.subscribe( params => {
      this.id = params['id'];
      this.breadcrumb[this.breadcrumb.length - 1] = {
        label: 'Détail d\'une opération d\'entrée (' + this.id + ')',
        routerLink: 'ingest/logbookOperation/' + this.id
      };
      this.breadcrumbService.changeState(this.breadcrumb);
    });
  }

}
