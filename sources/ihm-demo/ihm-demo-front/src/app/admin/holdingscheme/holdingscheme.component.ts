import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from "../../common/page/page-component";

@Component({
  selector: 'vitam-holdingscheme',
  templateUrl: './holdingscheme.component.html',
  styleUrls: ['./holdingscheme.component.css']
})
export class HoldingschemeComponent   extends PageComponent {

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Import de l\'arbre de positionnement', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    let newBreadcrumb = [
      {label: 'Administration', routerLink: ''},
      {label: 'Import de l\'arbre de positionnement', routerLink: ''}
    ];

    this.setBreadcrumb(newBreadcrumb);
  }
}
