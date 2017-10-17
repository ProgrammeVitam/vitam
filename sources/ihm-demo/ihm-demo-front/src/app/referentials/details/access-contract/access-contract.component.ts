import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { PageComponent } from "../../../common/page/page-component";
import { AccessContract } from "./access-contract";


const ACCESS_CONTRACT_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Nom',
  Status : 'Statut',
  WritingPermission : 'Droit d\'écriture',
  Description : 'Description',
  DataObjectVersion : 'Usage',
  RootUnits : 'Noeuds de consultation',
  _tenant : 'Tenant',
  OriginatingAgencies : "Service Producteur",
  EveryOriginatingAgency : 'Tous les services producteurs',
  EveryDataObjectVersion : 'Tous les usages'
};

@Component({
  selector: 'vitam-access-contract',
  templateUrl: './access-contract.component.html',
  styleUrls: ['./access-contract.component.css']
})

export class AccessContractComponent  extends PageComponent {

  contract : AccessContract;
  modifiedContract : AccessContract;
  arrayOfKeys : string[];
  id: string;
  update : boolean;
  isActif : boolean
  updatedFields = {};

  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Détail du contrat d\'accès', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.searchReferentialsService.getAccessContractById(this.id).subscribe((value) => {
        this.contract =  plainToClass(AccessContract, value.$results)[0];
        let keys = Object.keys(this.contract);
        let contract = this.contract;
        this.modifiedContract =  Object.assign({}, this.contract);
        if (this.contract.Status === 'ACTIVE') {
          this.isActif = true;
        } else {
          this.isActif = false;
        }
        this.arrayOfKeys = keys.filter(function(key) {
          return key != '_id';
        });
      });
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Contrats d\'accès', routerLink: 'admin/search/accessContract'},
        {label: this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.contract[key];
  }

  getKeyName(key: string) {
    return ACCESS_CONTRACT_KEY_TRANSLATION[key] || key;
  }

  switchUpdateMode() {
    this.update = !this.update;
    if (!this.update) {
    }
  }

  isUpdatable(key : string) {
    if (['CreationDate', 'LastUpdate', 'Identifier', '_tenant', '_id'].indexOf(key) > -1) {
      return false;
    } else {
      return this.update;
    }
  }


  changeStatus() {
    if (this.isActif) {
      this.updatedFields['Status'] = 'ACTIVE';
    } else {
      this.updatedFields['Status'] = 'INACTIVE';
    }
  }

  saveUpdate() {
    this.updatedFields['LastUpdate'] = new Date();
    console.log(this.updatedFields);
    this.searchReferentialsService.updateDocumentById('accesscontracts', this.id, this.updatedFields)
      .subscribe((data) => {
        console.log(data);
        return data;
      });
  }
}
