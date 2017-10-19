import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';

import { ReferentialsService } from "../../referentials/referentials.service";
import { AuditService } from "./audit.service";
import { PageComponent } from "../../common/page/page-component";
import { BreadcrumbService, BreadcrumbElement } from "../../common/breadcrumb.service";

@Component({
  selector: 'vitam-audit',
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.css']
})
export class AuditComponent extends PageComponent {

  registers : SelectItem[] = [];
  auditTypes : SelectItem[] = [
    {label:"Service Producteur", value:'originatingagency'},
    {label:"Tenant", value:'tenant'}
  ];
  auditType : string;
  objectId : string;
  tenantCurrent : string;
  auditExistence : boolean;
  disableAuditExistence : boolean;
  auditIntegrity : boolean;
  auditAction : string;

  constructor(public titleService: Title, private searchReferentialsService : ReferentialsService,
              public breadcrumbService: BreadcrumbService, private auditService : AuditService) {
    super('Recherche du référentiel', [], titleService, breadcrumbService);
    let newBreadcrumb = [
      {label: 'Administration', routerLink: ''},
      {label: 'Audit', routerLink: ''}
    ];

    this.setBreadcrumb(newBreadcrumb);
  }

  pageOnInit() {
    this.searchReferentialsService.setSearchAPI('admin/accession-register');
    this.searchReferentialsService.getResults({}).subscribe(
        data => {
          for (let result of data.$results) {
            console.log(result);
            this.registers.push({label: result['OriginatingAgency'], value:result['OriginatingAgency']})
          }
          this.auditType = 'originatingagency';
          if (this.registers.length > 0) {
            this.objectId = this.registers[0].value;
          }
        },
        error => console.log('Error - ', error)
    );
    this.tenantCurrent = this.searchReferentialsService.getTenantCurrent();
  }

  launchAudit() {
    this.auditService.launchAudit({
      auditActions : this.auditAction,
      auditType : this.auditType,
      objectId : (this.auditType === 'tenant') ? this.tenantCurrent : this.objectId
    }).subscribe(
      (response) => {
        console.log(response)
      },
      (error) =>
        console.log(error)
    );
  }

  validAuditAction() {
    if (this.auditIntegrity === true) {
      this.auditAction = "AUDIT_FILE_INTEGRITY";
      this.auditExistence = true;
      this.disableAuditExistence = true;
    } else if (this.auditExistence === true) {
      this.auditAction = "AUDIT_FILE_EXISTING";
      this.disableAuditExistence = false;
    } else {
      this.auditAction = null;
      this.disableAuditExistence = false;
    }
  }
}
