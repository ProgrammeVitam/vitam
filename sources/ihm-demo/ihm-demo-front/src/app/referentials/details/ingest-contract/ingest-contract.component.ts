import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { PageComponent } from "../../../common/page/page-component";
import { IngestContract } from './ingest-contract';

const INGEST_CONTRACT_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Nom',
  Status : 'Statut',
  Description : 'Description',
  FilingParentId : 'Noeuds de rattachement',
  ArchiveProfiles : 'Profils d\'archivage',
  _tenant : 'Tenant',
};

@Component({
  selector: 'vitam-ingest-contract',
  templateUrl: './ingest-contract.component.html',
  styleUrls: ['./ingest-contract.component.css']
})

export class IngestContractComponent extends PageComponent {

  contract : IngestContract;
  modifiedContract : IngestContract;
  id: string;
  isActif :boolean;
  update : boolean;
  updatedFields = {};
  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Détail du contrat d\'entrée', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Contrats d\'entrée', routerLink: 'admin/search/ingestContract'},
        {label: this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.modifiedContract[key];
  }

  getKeyName(key: string) {
    return INGEST_CONTRACT_KEY_TRANSLATION[key] || key;
  }


  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
  }

  changeStatus() {
    if (this.isActif) {
      this.updatedFields['Status'] = 'ACTIVE';
    } else {
      this.updatedFields['Status'] = 'INACTIVE';
    }
  }


  saveUpdate() {
    console.log(this.updatedFields);
    if (!!this.updatedFields['isActive']) {

    }
    this.updatedFields['LastUpdate'] = new Date();
    this.searchReferentialsService.updateDocumentById('contracts', this.id, this.updatedFields)
      .subscribe((data) => {
        console.log(data);
        this.getDetail();
        this.switchUpdateMode();
      });
  }

  getDetail() {
    this.searchReferentialsService.getIngestContractById(this.id).subscribe((value) => {
      this.contract = plainToClass(IngestContract, value.$results)[0];
      let keys = Object.keys(this.contract);
      this.modifiedContract =  Object.assign({}, this.contract);
      let contract = this.contract;
      if (this.modifiedContract.Status === 'ACTIVE') {
        this.isActif = true;
      } else {
        this.isActif = false;
      }
    });
  }
}
