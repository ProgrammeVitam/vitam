import {Component} from '@angular/core';
import {SelectItem} from 'primeng/api';
import {ResourcesService} from '../../common/resources.service';
import {IngestCleanupService} from './ingest-cleanup.service';
import {TenantService} from '../../common/tenant.service';
import { QueryDslService } from '../../tests/query-dsl/query-dsl.service';
import {Contract} from '../../common/contract';
import {PageComponent} from '../../common/page/page-component';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {Title} from '@angular/platform-browser';

class FileData {
  constructor(public file: string, public category: string, offerId: String) {
  }
}

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Ingest cleanup', routerLink: 'admin/testingest-cleanup'}
];

@Component({
  selector: 'ingest-cleanup',
  templateUrl: './ingest-cleanup.component.html',
  styleUrls: ['./ingest-cleanup.component.css']
})
export class IngestCleanupComponent extends PageComponent {
  error = false;

  tenant: string;
  operationId: string;
  contractsList: Array<SelectItem>;
  selectedContract: Contract;
  isChecked: string;
  ingestCleanupKo = false;
  ingestCleanupOk = false;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private queryDslService: QueryDslService, 
              private resourcesService: ResourcesService, private ingestCleanupService: IngestCleanupService, private tenantService: TenantService) {
    super('Lancement de nettoyage d\'ingest corrompu', breadcrumb, titleService, breadcrumbService);
  }

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
      },
      (error) => {
        this.ingestCleanupKo = true;

      }
    )
  }

  checkValue(event: any) {
    if(event == "Yes") {
      this.isChecked = "Yes";
     } else {
        this.isChecked = "No";
       }
   }

  cleanIngest() {


    if (!this.operationId || !this.tenant || !this.selectedContract || this.isChecked !== "Yes") {
      this.error = true;
      return;
    }

    this.ingestCleanupService.launch(this.operationId, this.selectedContract.Identifier).subscribe(
      (response) => {

        this.ingestCleanupOk = true;
      },
      (error) => {
        this.ingestCleanupKo = true;

      }
    );
  }
}
