import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import {Message, SelectItem} from 'primeng/primeng';

import { ResourcesService } from '../resources.service';
import { AuthenticationService } from '../../authentication/authentication.service';
import {AccessContractService} from "../access-contract.service";

@Component({
  selector: 'vitam-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {

  msgs: Message[] = [];
  isAuthenticated : boolean;
  tenantChosen: string;
  accessContracts: SelectItem[] = [];
  accessContract = '';
  items = [];

  constructor(private resourcesService: ResourcesService, private authenticationService : AuthenticationService,
              private router : Router, private accessContractService: AccessContractService) {

  }

  ngOnInit() {
    this.authenticationService.getLoginState().subscribe((value) => {
      if (value) {
        this.items = [
          {
            label: 'Entrée',
            icon: 'fa-sign-in',
            items: [
              {label: 'Transfert SIP',routerLink: ['ingest/sip']},
              {label: 'Suivi des opérations d\'entrée', routerLink: ['ingest/logbook']}
            ]
          },
          {
            label: 'Recherche',
            icon: 'fa-search',
            items: [
              {label: 'Recherche d\'archives', routerLink: ['search/archiveUnit']},
              {label: 'Registre des fonds', routerLink: ['admin/search/accession-register']}
            ]
          },
          {
            label: 'Administration',
            icon: 'fa-cogs',
            items: [
              {label: 'Référentiel des règles de gestion', routerLink: ['admin/search/rule']},
              {label: 'Référentiel des formats', routerLink: ['admin/search/format']},
              {label: 'Référentiel des profils', routerLink: ['admin/search/profil']},
              {label: 'Contrats d\'entrée', routerLink: ['admin/search/ingestContract']},
              {label: 'Contrats d\'accès', routerLink: ['admin/search/accessContract']},
              {label: 'Contextes applicatifs', routerLink: ['admin/search/context']},
              {label: 'Service agent', routerLink: ['admin/search/agencies']},
              {separator: true},
              {label: 'Journal des opérations', routerLink: ['admin/logbookOperation']},
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
              {label: 'Import des service agent', routerLink: ['admin/import/agencies']}
            ]
          },
          {
            label: 'Gestion des archives',
            icon: 'fa-area-chart',
            items: [
              {label: 'Audit', routerLink: ['admin/audits']}
            ]
          }

        ];
        this.isAuthenticated = true;
        this.tenantChosen = this.resourcesService.getTenant();
        if (this.accessContracts.length == 0) {
          this.resourcesService.getAccessContrats().subscribe((data) => {
            let accessContracts = this.accessContracts;
            data.$results.forEach(function(value) {
              accessContracts.push({label:value.Name, value:value.Name});
            });
            if (this.resourcesService.getAccessContract()) {
              let contractName = this.resourcesService.getAccessContract();
              this.accessContract = contractName;
            } else {
              this.accessContract = this.accessContracts[0].value;
            }
            this.updateContract();
          }, (error: HttpErrorResponse) => {

            if (error.error instanceof Error) {
              // A client-side or network error occurred. Handle it accordingly.
            } else {
              // The backend returned an unsuccessful response code.
              // The response body may contain clues as to what went wrong,
            }

            // Logout when cookie expired
            if (error.status == 0) {
              this.isAuthenticated = false;
              this.logOut();
              this.router.navigate(['login']);
            }
          });
        } else {
          if (this.resourcesService.getAccessContract()) {
            let contractName = this.resourcesService.getAccessContract();
            this.accessContract = contractName;
          } else {
            this.accessContract = this.accessContracts[0].value;
          }
          this.updateContract();
        }

      } else {
        this.items = [];
        this.isAuthenticated = false;
        this.router.navigate(['login']);
      }
    });
  }

  logOut() {
    this.authenticationService.logOut().subscribe();
    this.authenticationService.loggedOut();
  }

  updateContract() {
    this.resourcesService.setAccessContract(this.accessContract);
    this.accessContractService.update(this.accessContract);
  }
}
