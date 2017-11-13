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
        //FIXME change menu model or wait primeng fix the bug of visible https://github.com/primefaces/primeng/issues/3072
        if (this.authenticationService.isAdmin()) {
          this.items = [
            {
              label: 'Entrée',
              icon: 'fa-sign-in',
              items: [
                {label: 'Transfert de SIP et plan de classement',routerLink: ['ingest/sip']},
                {label: 'Suivi des opérations d\'entrée', routerLink: ['ingest/logbook']}
              ]
            },
            {
              label: 'Recherche',
              icon: 'fa-search',
              items: [
                {label: 'Recherche d\'archives', routerLink: ['search/archiveUnit']},
                {label: 'Recherche par service producteur', routerLink: ['admin/accessionRegister']}
              ]
            },
            {
              label: 'Administration',
              icon: 'fa-cogs',
              items: [
                {
                  label: 'Référentiels',
                  items: [
                    {label: 'Contextes applicatifs', routerLink: ['admin/search/context']},
                    {label: 'Contrats d\'entrée', routerLink: ['admin/search/ingestContract']},
                    {label: 'Contrats d\'accès', routerLink: ['admin/search/accessContract']},
                    {label: 'Formats', routerLink: ['admin/search/format']},
                    {label: 'Profils d\'archivage', routerLink: ['admin/search/profil']},
                    {label: 'Règles de gestion', routerLink: ['admin/search/rule'], disabled : false},
                    {label: 'Services agents', routerLink: ['admin/search/agencies']}
                  ]
                },
                {
                  label: 'Import des référentiels',
                  items: [
                    {label: 'Import d\'un arbre de positionnement', routerLink: ['admin/holdingScheme']},
                    {label: 'Import des contextes applicatifs', routerLink: ['admin/import/context']},
                    {label: 'Import des contrats d\'entrée', routerLink: ['admin/import/ingestContract']},
                    {label: 'Import des contrats d\'accès', routerLink: ['admin/import/accessContract']},
                    {label: 'Import des formats', routerLink: ['admin/import/format']},
                    {label: 'Import des profils d\'archivage', routerLink: ['admin/import/profil']},
                    {label: 'Import des règles de gestion', routerLink: ['admin/import/rule']},
                    {label: 'Import des services agents', routerLink: ['admin/import/agencies']}
                  ]
                },
                {
                  label: 'Opérations',
                  items: [
                    {label: 'Gestion des opérations', routerLink: ['admin/workflow']},
                    {label: 'Journal des opérations', routerLink: ['admin/logbookOperation']},
                    {label: 'Opérations de sécurisation', routerLink: ['admin/traceabilityOperation']}
                  ]
                }
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
        } else {
          this.items = [
            {
              label: 'Entrée',
              icon: 'fa-sign-in',
              items: [
                {label: 'Transfert de SIP et plan de classement',routerLink: ['ingest/sip']},
                {label: 'Suivi des opérations d\'entrée', routerLink: ['ingest/logbook']}
              ]
            },
            {
              label: 'Recherche',
              icon: 'fa-search',
              items: [
                {label: 'Recherche d\'archives', routerLink: ['search/archiveUnit']},
                {label: 'Recherche par services producteurs', routerLink: ['admin/accessionRegister']}
              ]
            },
            {
              label: 'Administration',
              icon: 'fa-cogs',
              items: [
                {
                  label: 'Référentiels',
                  items: [
                    {label: 'Contextes applicatifs', routerLink: ['admin/search/context']},
                    {label: 'Contrats d\'entrée', routerLink: ['admin/search/ingestContract']},
                    {label: 'Contrats d\'accès', routerLink: ['admin/search/accessContract']},
                    {label: 'Formats', routerLink: ['admin/search/format']},
                    {label: 'Profils d\'archivage', routerLink: ['admin/search/profil']},
                    {label: 'Règles de gestion', routerLink: ['admin/search/rule'], disabled : false},
                    {label: 'Services agents', routerLink: ['admin/search/agencies']}
                  ]
                },
                {
                  label: 'Opérations',
                  items: [
                    {label: 'Journal des opérations', routerLink: ['admin/logbookOperation']}
                  ]
                }
              ]
            }
          ];
        }
        this.isAuthenticated = true;
        this.tenantChosen = this.resourcesService.getTenant();

        this.resourcesService.getAccessContrats().subscribe((data) => {
          this.accessContracts = [];
          let contractInCookie = this.resourcesService.getAccessContract();
          let hasContract = false;
          for (let contract of data.$results) {
            this.accessContracts.push({label:contract.Name, value:contract.Name});
            if (contract.Name === contractInCookie) {
              hasContract = true
            }
          }
          if (contractInCookie && hasContract) {
            this.accessContract = contractInCookie;
          } else if (this.accessContracts.length > 0) {
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
