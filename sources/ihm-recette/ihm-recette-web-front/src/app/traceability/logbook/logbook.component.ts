import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { LogbookService } from './logbook.service';
import {Message} from 'primeng/primeng';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Sécurisation', routerLink: ''},
  {label: 'Journaux des opérations', routerLink: 'traceability/logbook'}
];

@Component({
  selector: 'vitam-logbook',
  templateUrl: './logbook.component.html',
  styleUrls: ['./logbook.component.css']
})
export class LogbookComponent extends PageComponent {

  messages: Message[] = [];

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService,
              private logbookservice : LogbookService) {
    super('Sécurisation des opérations', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {

  }

  launchTraceability() {
    this.logbookservice.launchTraceability()
      .subscribe(response => {
        this.messages = [];
        if (response.status == 200) {
          this.messages.push({severity: 'info', summary: 'Sécurisation', detail: 'Succès de l\'opération de sécurisation des journaux'});
        } else {
          this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des journaux`});
        }
      }, error => {
        this.messages = [];
        this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des journaux`});
      })
  }
  
  launchTraceabilityLFC() {
    this.logbookservice.launchTraceabilityLFC()
      .subscribe(response => {
        this.messages = [];
        if (response.status == 200) {
          this.messages.push({severity: 'info', summary: 'Sécurisation', detail: 'Succès de l\'opération de sécurisation des cycles de vie'});
        } else {
          this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des cycles de vie`});
        }
      }, error => {
        this.messages = [];
        this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des cycles de vie`});
      })
  }
}
