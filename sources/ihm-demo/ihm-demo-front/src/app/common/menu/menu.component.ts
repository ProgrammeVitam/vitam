import {Component, HostListener, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {HttpErrorResponse} from '@angular/common/http';
import {Message, SelectItem} from 'primeng/primeng';

import {ResourcesService} from '../resources.service';
import {AuthenticationService} from '../../authentication/authentication.service';
import {AccessContractService} from '../access-contract.service';

@Component({
  selector: 'vitam-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {

  isAuthenticated: boolean;
  tenantChosen: string;
  accessContracts: SelectItem[] = [];
  accessContract = '';
  items = [];

  constructor(private resourcesService: ResourcesService, private authenticationService: AuthenticationService,
              private router: Router, private accessContractService: AccessContractService) {
  }

  ngOnInit() {
    this.authenticationService.getLoginState().subscribe((value) => {
      if (value) {
        const ingestItems = [
          {id: 'sip', label: 'Transfert de SIP et plan de classement', routerLink: ['ingest/sip']},
          {id: 'jop', label: 'Suivi des opérations d\'entrée', routerLink: ['ingest/logbook']}
        ];
        const search = [
          {label: 'Recherche d\'archives', routerLink: ['search/archiveUnit']},
          {label: 'Recherche par service producteur', routerLink: ['admin/accessionRegister']}
        ];

        if (this.authenticationService.isAdmin()) {
          let importItems = [];
          if (this.authenticationService.isTenantAdmin()) {
            importItems = [
              {label: 'Import d\'un arbre de positionnement', routerLink: ['admin/holdingScheme']},
              {label: 'Import des contextes applicatifs', routerLink: ['admin/import/context']},
              {label: 'Import des contrats d\'entrée', routerLink: ['admin/import/ingestContract']},
              {label: 'Import des contrats d\'accès', routerLink: ['admin/import/accessContract']},
              {label: 'Import des documents type', routerLink: ['admin/import/archiveUnitProfile']},
              {label: 'Import des ontologies', routerLink: ['admin/import/ontology']},
              {label: 'Import des formats', routerLink: ['admin/import/format']},
              {label: 'Import des profils d\'archivage', routerLink: ['admin/import/profil']},
              {label: 'Import des règles de gestion', routerLink: ['admin/import/rule']},
              {label: 'Import des services agents', routerLink: ['admin/import/agencies']},
              {label: 'Import des griffons', routerLink: ['admin/import/griffins']},
              {label: 'Import des scénarios de préservation', routerLink: ['admin/import/scenarios']}
            ];
          } else {
            importItems = [
              {label: 'Import d\'un arbre de positionnement', routerLink: ['admin/holdingScheme']},
              {label: 'Import des contrats d\'entrée', routerLink: ['admin/import/ingestContract']},
              {label: 'Import des contrats d\'accès', routerLink: ['admin/import/accessContract']},
              {label: 'Import des documents type', routerLink: ['admin/import/archiveUnitProfile']},
              {label: 'Import des profils d\'archivage', routerLink: ['admin/import/profil']},
              {label: 'Import des règles de gestion', routerLink: ['admin/import/rule']},
              {label: 'Import des services agents', routerLink: ['admin/import/agencies']},
              {label: 'Import des scénarios de préservation', routerLink: ['admin/import/scenarios']}

            ];
          }

          this.items = [
            {
              id: 'ingest',
              label: 'Entrée',
              icon: 'fa-sign-in',
              items: ingestItems,
              size: 2
            },
            {
              id: 'search',
              label: 'Recherche',
              icon: 'fa-search',
              items: search,
              size: 3
            },
            {
              id: 'admin',
              label: 'Administration',
              icon: 'fa-cogs',
              items: [
                {
                  id: 'details',
                  label: 'Référentiels',
                  items: [
                    {label: 'Contextes applicatifs', routerLink: ['admin/search/context']},
                    {label: 'Contrats d\'entrée', routerLink: ['admin/search/ingestContract']},
                    {label: 'Contrats d\'accès', routerLink: ['admin/search/accessContract']},
                    {label: 'Documents type', routerLink: ['admin/search/archiveUnitProfile']},
                    {label: 'Ontologies', routerLink: ['admin/search/ontology']},
                    {label: 'Formats', routerLink: ['admin/search/format']},
                    {label: 'Profils d\'archivage', routerLink: ['admin/search/profil']},
                    {label: 'Règles de gestion', routerLink: ['admin/search/rule'], disabled: false},
                    {label: 'Services agents', routerLink: ['admin/search/agencies']},
                    {label: 'Griffons', routerLink: ['admin/search/griffins']},
                    {label: 'Scénarios de préservation', routerLink: ['admin/search/scenarios']}
                  ]
                },
                {
                  id: 'import',
                  label: 'Import des référentiels',
                  items: importItems
                },
                {
                  id: 'operation',
                  label: 'Opérations',
                  items: [
                    {label: 'Gestion des opérations', routerLink: ['admin/workflow']},
                    {label: 'Journal des opérations', routerLink: ['admin/logbookOperation']},
                    {label: 'Opérations de sécurisation', routerLink: ['admin/traceabilityOperation']}
                  ]
                }
              ],
              size: 3
            },
            {
              id: 'archives',
              label: 'Gestion des archives',
              icon: 'fa-area-chart',
              items: [
                {label: 'Audit', routerLink: ['admin/audits']},
                {label: 'Recherche Groupe Objets', routerLink: ['admin/objectsgroup']},
                {label: 'Résultats d\'élimination', routerLink: ['archiveManagement/eliminationSearch']}
              ],
              size: 4
            }
          ];
        } else {
          this.items = [
            {
              id: 'ingest',
              label: 'Entrée',
              icon: 'fa-sign-in',
              items: ingestItems,
              size: 2
            },
            {
              id: 'search',
              label: 'Recherche',
              icon: 'fa-search',
              items: search,
              size: 3
            },
            {
              id: 'admin',
              label: 'Administration',
              icon: 'fa-cogs',
              items: [
                {
                  id: 'details',
                  label: 'Référentiels',
                  items: [
                    {label: 'Contextes applicatifs', routerLink: ['admin/search/context']},
                    {label: 'Contrats d\'entrée', routerLink: ['admin/search/ingestContract']},
                    {label: 'Contrats d\'accès', routerLink: ['admin/search/accessContract']},
                    {label: 'Documents type', routerLink: ['admin/search/archiveUnitProfile']},
                    {label: 'Ontologies', routerLink: ['admin/search/ontology']},
                    {label: 'Formats', routerLink: ['admin/search/format']},
                    {label: 'Profils d\'archivage', routerLink: ['admin/search/profil']},
                    {label: 'Règles de gestion', routerLink: ['admin/search/rule'], disabled: false},
                    {label: 'Services agents', routerLink: ['admin/search/agencies']}
                  ]
                },
                {
                  id: 'operation',
                  label: 'Opérations',
                  items: [
                    {label: 'Journal des opérations', routerLink: ['admin/logbookOperation']}
                  ]
                }
              ],
              size: 3
            }
          ];
        }
        this.isAuthenticated = true;
        this.tenantChosen = this.resourcesService.getTenant();

        this.resourcesService.getAccessContrats().subscribe((data) => {
          this.accessContracts = [];
          const contractInCookie = this.resourcesService.getAccessContract();
          let hasContract = false;
          for (const contract of data.$results) {
            this.accessContracts.push({label: contract.Name, value: contract.Identifier});
            if (contract.Identifier === contractInCookie) {
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
          if (error.status === 0) {
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

  clickInside(event, items: any[]) {
    const lastItem = items[items.length - 1];

    // Handle navigation to other page (All menu should be hiden)
    if (lastItem.routerLink) {
      this.router.navigate(lastItem.routerLink);
      this.hideAll(this.items);
      return;
    }

    // Handle subMenu open
    event.data = {
      ids: items.map(item => item.id)
    };

    for (const item of items) {
      item.displayed = true;
    }
  }

  hideAll(items: any[]) {
    if (!items) {
      return;
    }

    for (const item of items) {
      item.displayed = false;
      this.hideAll(item.items);
    }
  }

  hideRecursively(items: any[], ids: string[]) {
    if (ids.length === 0 || !items) {
      return;
    }

    for (const item of items) {
      if (item.id !== ids[0]) {
        item.displayed = false;
        this.hideAll(item.items);
      } else {
        this.hideRecursively(item.items, ids.slice(1));
      }
    }
  }

  @HostListener('document:click', ['$event'])
  clickedOutside(event) {
    // This function is called on each click and will hide all submenu that the mouse is out of
    if (!event.data || !event.data.ids || event.data.ids.length === 0) {
      this.hideAll(this.items);
    } else {
      this.hideRecursively(this.items, event.data.ids);
    }
  }
}
