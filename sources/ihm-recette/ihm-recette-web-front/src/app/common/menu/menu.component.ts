import {Component, HostListener, OnDestroy, OnInit} from '@angular/core';
import {Message} from 'primeng/primeng';
import {ResourcesService} from '../resources.service';
import {AuthenticationService} from '../../authentication/authentication.service';
import {Router} from '@angular/router';
import {TenantService} from "../tenant.service";
import {Subscription} from "rxjs/Subscription";

@Component({
  selector: 'vitam-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit, OnDestroy {

  isAuthenticated: boolean;
  msgs: Message[] = [];
  tenantChosen: string;
  tenants: Array<string>;
  tenantId = '';
  items = [];
  tenantSubscription: Subscription;

  constructor(private resourcesService: ResourcesService, private authenticationService: AuthenticationService,
              private tenantService: TenantService, private router: Router) {

  }

  ngOnDestroy() {
    this.tenantSubscription.unsubscribe();
  }

  ngOnInit() {

    const tenant = this.resourcesService.getTenant();
    if (tenant) {
      this.tenantChosen = tenant;
    }

    this.tenantSubscription = this.tenantService.getState().subscribe((value) => {
      this.tenantId = value;
      this.tenantChosen = value;
    });


    this.authenticationService.getState().subscribe((value) => {
      if (value) {
        this.items = [
          {
            id: 'admin',
            label: 'Admin',
            items: [
              {label: 'Administration des collections', routerLink: ['admin/collection']},
              {label: 'Recherche et Modification d\'un fichier', routerLink: ['admin/load-storage']},
                {label: 'Ajout et suppression d\'un parent', routerLink: ['admin/link-au']},
                {label: 'Test audit correctif', routerLink: ['admin/test-audit-correction']},
                {label: 'Configuration de la plateforme', routerLink: ['admin/configuration']},
                {label: 'Nettoyage d\'un ingest corrompu', routerLink: ['admin/ingest-cleanup']}
            ],
            size: 2
          },
          {
            id: 'test',
            label: 'Tests',
            items: [
              {label: 'Tests de performance', routerLink: ['tests/perf']},
              {label: 'Tests fonctionnels', routerLink: ['tests/functional-tests']},
              {label: 'Tests requêtes DSL', routerLink: ['tests/queryDSL']},
              {label: 'Visualisation du Graphe', routerLink: ['tests/dag-visualization']},
              {label: 'Test feature ', routerLink: ['tests/testFeature']}
            ],
            size: 2
          },
          {
            id: 'traceability',
            label: 'Sécurisation',
            items: [
              {label: 'Sécurisation des journaux', routerLink: ['traceability/logbook']}
            ],
            size: 2
          }
        ];
        this.isAuthenticated = true;
        this.resourcesService.getTenants().subscribe((tenants: Array<string>) => {
            this.tenants = tenants;
          },
          (error) => {
          });
      } else {
        this.items = [];
        this.isAuthenticated = false;
        this.router.navigate(["login"]);
      }
    });

  }

  setCurrentTenant() {
    if (!this.tenantId || this.tenantId === '-') {
      return;
    }
    this.tenantChosen = this.tenantId;
    this.resourcesService.setTenant(this.tenantId);
    this.tenantService.changeState(this.tenantId);

    this.msgs = [];
    this.msgs.push({severity: 'info', summary: 'Modification du tenant', detail: `Nouveau tenant : ${this.tenantId}`});
  }

  logOut() {
    this.authenticationService.logOut().subscribe();
    this.authenticationService.loggedOut();
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
