import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { ObjectsService } from '../../../common/utils/objects.service';
import { PageComponent } from "../../../common/page/page-component";
import { DialogService } from "../../../common/dialog/dialog.service";
import { AccessContract } from "./access-contract";


const ACCESS_CONTRACT_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Intitulé',
  Status : 'Statut',
  WritingPermission : 'Droit d\'écriture',
  Description : 'Description',
  DataObjectVersion : 'Usage',
  RootUnits : 'Noeuds de consultation',
  '#tenant' : 'Tenant',
  OriginatingAgencies : "Service producteur",
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
  id: string;
  update : boolean;
  isActif : boolean;
  updatedFields = {};

  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService, private dialogService : DialogService) {
    super('Détail du contrat d\'accès', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Référentiel des contrats d\'accès', routerLink: 'admin/search/accessContract'},
        {label: 'Détail du contrat d\'accès' + this.id, routerLink: ''}
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
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedContract =  ObjectsService.clone(this.contract);
    }
  }

  changeStatus() {
    if (this.isActif) {
      this.updatedFields['Status'] = 'ACTIVE';
    } else {
      this.updatedFields['Status'] = 'INACTIVE';
    }
  }

  changeBooleanValue(key : string) {
    this.updatedFields[key] = this.modifiedContract[key];
  }

  saveUpdate() {
    if (Object.keys(this.updatedFields).length == 0) {
      this.switchUpdateMode();
      return;
    }

    this.updatedFields['LastUpdate'] = new Date();
    this.searchReferentialsService.updateDocumentById('accesscontracts', this.id, this.updatedFields)
      .subscribe((data) => {
        if (data.httpCode >= 400) {
          this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
        } else {
          this.dialogService.displayMessage('La modification a bien été enregistrée', '');
        }
        this.getDetail();
        this.switchUpdateMode();
      }, (error) => {
        this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
      });
  }

  getDetail() {
    this.searchReferentialsService.getAccessContractById(this.id).subscribe((value) => {
      this.contract = plainToClass(AccessContract, value.$results)[0];
      this.modifiedContract =  ObjectsService.clone(this.contract);
      if (this.modifiedContract.Status === 'ACTIVE') {
        this.isActif = true;
      } else {
        this.isActif = false;
      }
    });
  }
}
