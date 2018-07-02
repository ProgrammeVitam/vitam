import { Component } from '@angular/core';
import { PageComponent } from '../../common/page/page-component';
import { TenantService } from '../../common/tenant.service';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { SelectItem } from 'primeng/api';
import {Contract} from '../../common/contract';
import { LinkAuService } from './link-au.service';
import { QueryDslService } from '../../tests/query-dsl/query-dsl.service';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Ajout et suppression d\'un parent', routerLink: 'admin/link-au'}
];

@Component({
  selector: 'vitam-link-au',
  templateUrl: './link-au.component.html',
  styleUrls: ['./link-au.component.css']
})
export class LinkAuComponent extends PageComponent {

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private queryDslService: QueryDslService,
              private linkAuService: LinkAuService, private tenantService: TenantService) {
    super('Ajout et suppression d\'un parent', breadcrumb, titleService, breadcrumbService);
  }

  tenant: string;
  contractsList: Array<SelectItem>;
  selectedContract: Contract;

  displayFormError: boolean = false;
  displaySuccess: boolean = false;
  displayError: boolean = false;

  linkOptions: SelectItem[] =  [
    {
      label: 'Ajouter un lien', value: 'ADD'
    }, {
      label: 'Supprimer un lien', value: 'DELETE'
    }
  ];

  request = {
    parentId: null,
    childId: null,
    action: null
  };

  pageOnInit() {
    return this.tenantService.getState().subscribe((value) => {
      this.tenant = value;
      if (this.tenant) {
        this.getContracts();
      }
    });
  }

  getContracts() {
    return this.queryDslService.getContracts().subscribe(
      (response) => {
        this.contractsList = response.map(
          (contract) => {
            return {label: contract.Name, value: contract}
          }
        );
      }
    )
  }

  launchOperation() {
    if (!this.request.parentId || !this.request.childId || !this.request.action || !this.selectedContract) {
      this.displayFormError = true;
      return;
    }

    this.linkAuService.updateLinks(this.request, this.selectedContract.Identifier).subscribe(
      () => {
        this.displaySuccess = true;
      },
      () => {
        this.displayError = true;
      }
    );
  }

}
