import { Component, OnInit } from '@angular/core';
import { Message } from 'primeng/primeng';
import { ResourcesService } from '../resources.service';


@Component({
  selector: 'vitam-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {

  msgs: Message[] = [];
  tenantChosen: string;
  tenants: Array<string>;
  tenantId = '-';
  items = [
    {
      label: 'Entrée',
      items: [
        {label: 'Transfert SIP', routerLink: ['ingest/sip']},
        {label: 'Transfert du plan de classement', routerLink: ['ingest/holdingScheme']},
        {label: 'Suivi des opérations d\'entrée', routerLink: ['ingest/logbook']}
      ]
    },
    {
      label: 'Recherche',
      items: [
        {label: 'Recherche d\'archives', routerLink: ['search/archiveUnit']},
        {label: 'Registre des fonds', routerLink: ['search/accessionRegister']}
      ]
    },
    {
      label: 'Administration',
      items: [
        {label: 'Référentiel des règles de gestion', routerLink: ['admin/rule']},
        {label: 'Référentiel des formats', routerLink: ['admin/format']},
        {label: 'Référentiel des profiles', routerLink: ['admin/profil']},
        {label: 'Contrats d\'entrée', routerLink: ['admin/ingestContract']},
        {label: 'Contrats d\'accès', routerLink: ['admin/accessContract']},
        {label: 'Contextes applicatifs', routerLink: ['admin/context']},
        {separator: true},
        {label: 'Journal des opérations', routerLink: ['admin/logbook']},
        {label: 'Gestion des opérations', routerLink: ['admin/workflow']},
        {label: 'Opérations de sécurisation', routerLink: ['admin/traceability']},
        {separator: true},
        {label: 'Import de l\'arbre de positionnement', routerLink: ['admin/import/fillingScheme']},
        {label: 'Import du référentiel des règles de gestion', routerLink: ['admin/import/rule']},
        {label: 'Import du référentiel des formats', routerLink: ['admin/import/format']},
        {label: 'Import des contrats d\'entrée', routerLink: ['admin/import/ingestContract']},
        {label: 'Import des contrats d\'accès', routerLink: ['admin/import/accessContract']},
        {label: 'Import des profils', routerLink: ['admin/import/profil']},
        {label: 'Import des contextes', routerLink: ['admin/import/context']},
      ]
    },
    {
      label: 'Gestion des archives',
      routerLink: ['management/archiveUnit']
    }
  ];

  constructor(private resourcesService: ResourcesService) {

  }

  ngOnInit() {
    const tenant = this.resourcesService.getTenant();
    if (tenant) {
      this.tenantChosen = tenant;
    }

    this.resourcesService.getTenants()
      .subscribe((tenants: Array<string>) => {this.tenants = tenants;});
  }

  setCurrentTenant() {
    if (!this.tenantId || this.tenantId === '-') {
      return;
    }
    this.tenantChosen = this.tenantId;
    this.resourcesService.setTenant(this.tenantId);

    this.msgs = [];
    this.msgs.push({severity: 'info', summary: 'Modification du tenant', detail: `Nouveau tenant : ${this.tenantId}`});
  }

}
