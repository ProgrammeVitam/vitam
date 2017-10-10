import { Component, OnInit, Input } from '@angular/core';
import { Message } from 'primeng/primeng';
import { ResourcesService } from '../resources.service';
import { AuthenticationService } from '../../authentication/authentication.service';
import {Router} from '@angular/router';


@Component({
  selector: 'vitam-menu',
  templateUrl: './menu.component.html',
  styleUrls: ['./menu.component.css']
})
export class MenuComponent implements OnInit {

  isAuthenticated: boolean;
  msgs: Message[] = [];
  tenantChosen: string;
  tenants: Array<string>;
  tenantId = '-';
  items = [];

  constructor(private resourcesService: ResourcesService, private authenticationService: AuthenticationService,
              private router: Router) {

  }

  ngOnInit() {
    const tenant = this.resourcesService.getTenant();
    if (tenant) {
      this.tenantChosen = tenant;
    }

    this.authenticationService.getState().subscribe((value) => {
      if (value) {
        this.items = [
          {
            label: 'Admin',
            items: [
              {label: 'Administration des collections', routerLink: ['admin/collection']}
            ]
          },
          {
            label: 'Tests',
            items: [
              {label: 'Tests de performance', routerLink: ['tests/perf']},
              {label: 'Tests fonctionnels', routerLink: ['tests/functional-tests']},
              {label: 'Tests requêtes DSL', routerLink: ['tests/queryDSL']}
            ]
          },
          {
            label: 'Sécurisation',
            items: [
              {label: 'Sécurisation du journal des opérations', routerLink: ['traceability/logbook']}
            ]
          }
        ];
        this.isAuthenticated = true;
        this.resourcesService.getTenants()
          .subscribe((tenants: Array<string>) => {console.log("ok "); this.tenants = tenants;},
          (error) => {console.log("ok ");});
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

    this.msgs = [];
    this.msgs.push({severity: 'info', summary: 'Modification du tenant', detail: `Nouveau tenant : ${this.tenantId}`});
  }

  logOut() {
    this.authenticationService.logOut().subscribe();
    this.authenticationService.loggedOut();
  }
}
