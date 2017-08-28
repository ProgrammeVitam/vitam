import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from '../../common/breadcrumb.service';
import {PageComponent} from "../../common/page/page-component";

@Component({
  selector: 'vitam-sip',
  templateUrl: './sip.component.html',
  styleUrls: ['./sip.component.css']
})
export class SipComponent  extends PageComponent {

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Transfert SIP', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    let newBreadcrumb = [
      {label: 'Entrée', routerLink: ''},
      {label: 'Transfert SIP', routerLink: 'ingest/sip'}
    ];

    this.setBreadcrumb(newBreadcrumb);
  }
}
