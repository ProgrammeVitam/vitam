import { Component } from '@angular/core';
import { Title} from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';
import { QueryDslService } from './query-dsl.service';
import { Contract } from '../../common/contract';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ResourcesService } from '../../common/resources.service';
import { PageComponent } from '../../common/page/page-component';
import {TenantService} from "../../common/tenant.service";

const defaultMethods = [
  {label: 'Rechercher', value: 'GET'},
  {label: 'Mettre à jour', value: 'PUT'}
];
const operationsMethods = [
  {label: 'Rechercher', value: 'GET'},
  {label: 'Action - SUIVANT', value: 'NEXT'},
  {label: 'Action - PAUSE', value: 'PAUSE'},
  {label: 'Action - REPRENDRE', value: 'RESUME'},
  {label: 'Action - STOP', value: 'STOP'},
];

const defaultCollections = [
  {label: 'Unit', value: 'UNIT'},
  {label: 'Groupe d\'objets', value: 'OBJECTGROUP'},
  {label: 'Journal CV unit', value: 'UNITLIFECYCLES'},
  {label: 'Journal CV GOT', value: 'OBJECTGROUPLIFECYCLES'},
  {label: 'Journal des opérations', value: 'LOGBOOK'},
  {label: 'Registre des fonds', value: 'ACCESSION_REGISTERS'},
  {label: 'Profil', value: 'PROFILE'},
  {label: 'Contrat d\'accès', value: 'ACCESS_CONTRACTS'},
  {label: 'Contrat d\'entrée', value: 'ENTRY_CONTRACTS'},
  {label: 'Contexte', value: 'CONTEXTS'},
  {label: 'Règle de gestion', value: 'RULES'},
  {label: 'Format', value: 'FORMATS'},
  {label: 'Opération', value: 'OPERATIONS'},
  {label: 'Workflow', value: 'WORKFLOWS'},
];

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Tests', routerLink: ''},
  {label: 'Requêtes DSL', routerLink: 'tests/queryDSL'}
];

@Component({
  selector: 'vitam-query-dsl',
  templateUrl: './query-dsl.component.html',
  styleUrls: ['./query-dsl.component.css']
})

export class QueryDSLComponent extends PageComponent {
  collections: SelectItem[] = defaultCollections;
  methods: SelectItem[] = defaultMethods;
  selectedContract: Contract;
  selectedCollection: string;
  selectedMethod: string;
  objectId: string;
  selectedAction: string;
  jsonRequest: string;
  requestResponse: {};
  validRequest: any;
  contractsList: Array<SelectItem>;

  constructor(public breadcrumbService: BreadcrumbService, private queryDslService: QueryDslService,
              public titleService: Title, private resourcesService: ResourcesService,
              public tenantService: TenantService) {
    super('Tests des requêtes DSL', breadcrumb, titleService, breadcrumbService)
  }

  pageOnInit() {
    this.getContracts();
    this.tenantService.getState().subscribe(
      () => this.getContracts()
    );
  }

  getTenant() {
    return this.resourcesService.getTenant();
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

  checkJson() {
    if (this.queryDslService.checkJson(this.jsonRequest)) {
      this.jsonRequest = JSON.stringify(JSON.parse(this.jsonRequest), null, 2);
      this.validRequest = {valid: 'valide', css: 'font-color-green'};
    } else {
      this.validRequest = {valid: 'non valide', css: 'font-color-red'};
    }
  }

  sendRequest() {
    this.checkJson();
    if (this.selectedCollection === 'OPERATIONS' && this.selectedMethod !== 'GET') {
      this.selectedAction = this.selectedMethod;
      if (this.selectedAction === 'STOP') {
        this.selectedMethod = 'DELETE';
      } else {
        this.selectedMethod = 'PUT';
      }
    }
    this.queryDslService.executeRequest(this.jsonRequest, this.selectedContract.Name,
      this.selectedCollection, this.selectedMethod, this.selectedAction, this.objectId).subscribe(
      (response) => this.requestResponse = response.json(),
      (error) => this.requestResponse = error._body
    );
  }

  updateMethods() {
    this.methods =
      this.selectedCollection === 'OPERATIONS' ? operationsMethods : defaultMethods;
  }

}
