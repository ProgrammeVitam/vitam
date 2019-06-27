import {Component} from '@angular/core';
import {SelectItem} from 'primeng/api';
import {ResourcesService} from '../../common/resources.service';
import {TestAuditCorrectionService} from './test-audit-correction.service';
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
  {label: 'Correction d\'audit', routerLink: 'admin/test-audit-correction'}
];

@Component({
  selector: 'vitam-test-audit-correction',
  templateUrl: './test-audit-correction.component.html',
  styleUrls: ['./test-audit-correction.component.css']
})
export class TestAuditCorrectionComponent extends PageComponent {
  error = false;

  tenant: string;
  operationId: string;
  contractsList: Array<SelectItem>;
  selectedContract: Contract;

  auditKo = false;
  auditOk = false;

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private queryDslService: QueryDslService, 
              private resourcesService: ResourcesService, private testAuditCorrectionService: TestAuditCorrectionService, private tenantService: TenantService) {
    super('Lancement audit correctifs', breadcrumb, titleService, breadcrumbService);
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
        this.auditKo = true;

      }
    )
  }

  getObject() {

    if (!this.operationId || !this.tenant || !this.selectedContract) {
      this.error = true;
      return;
    }

    this.testAuditCorrectionService.launch(this.operationId, this.selectedContract.Identifier).subscribe(
      (response) => {

        this.auditOk = true;
      },
      (error) => {
        this.auditKo = true;

      }
    );
  }

}
