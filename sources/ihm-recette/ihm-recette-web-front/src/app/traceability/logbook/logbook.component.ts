import { Component } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { PageComponent } from '../../common/page/page-component';
import { LogbookService } from './logbook.service';
import { Message } from 'primeng/primeng';
import { Subscription } from "rxjs/Subscription";

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
              private logbookService : LogbookService) {
    super('Sécurisation des opérations', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit(): Subscription {
    return null;
  }

  launchTraceability() {
    this.logbookService.launchTraceability()
      .subscribe(() => {
        this.messages = [];
        this.messages.push({severity: 'info', summary: 'Sécurisation', detail: 'Succès de l\'opération de sécurisation des journaux'});
      }, () => {
        this.messages = [];
        this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des journaux`});
      })
  }

  launchTraceabilityLFC() {
    this.logbookService.launchTraceabilityLFC()
      .subscribe(() => {
        this.messages = [];
        this.messages.push({severity: 'info', summary: 'Sécurisation', detail: 'Succès de l\'opération de sécurisation des cycles de vie'});
      }, () => {
        this.messages = [];
        this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des cycles de vie`});
      })
  }

  launchTraceabilityStorage() {
    this.logbookService.launchTraceabilityStorage()
      .subscribe(() => {
        this.messages = [];
        this.messages.push({severity: 'info', summary: 'Sécurisation', detail: `Succès de l'opération de sécurisation des offres`});
      }, () => {
        this.messages = [];
        this.messages.push({severity: 'error', summary: 'Sécurisation', detail: `Echec de l'opération de sécurisation des offres`});
      })
  }
}
