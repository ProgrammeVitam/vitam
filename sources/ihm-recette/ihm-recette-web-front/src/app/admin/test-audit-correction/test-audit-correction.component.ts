import { Component } from '@angular/core';
import {SelectItem} from 'primeng/api';
import {ResourcesService} from '../../common/resources.service';
import {TestAuditCorrectionService} from './test-audit-correction.service';
import {FileSystemFileEntry} from 'ngx-file-drop';
import {TenantService} from '../../common/tenant.service';
import {PageComponent} from '../../common/page/page-component';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {Title} from '@angular/platform-browser';

class FileData {
  constructor(public file: string, public category: string,offerId:String ) { }
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

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private resourcesService: ResourcesService,
              private testAuditCorrectionService: TestAuditCorrectionService, private tenantService: TenantService) {
    super('Lancement audit correctifs', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    return this.tenantService.getState().subscribe((value) => {
      this.tenant = value;
    });
  }

  getObject() {

    if (!this.operationId || !this.tenant) {
      this.error = true;
      return;
    }

    this.testAuditCorrectionService.launch(this.operationId).subscribe(
      (response) => {
// TODO
          console.log(response);
      }, (error) => {
// TODO
            console.error(error);
      }
    );
  }

}
