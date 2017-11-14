import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
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
  updatedFields: any = {};
  saveRunning = false;

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
        {label: 'Contrats d\'accès', routerLink: 'admin/search/accessContract'},
        {label: 'Détail du contrat d\'accès ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
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
    if (Object.keys(this.updatedFields).length == 0 || this.updatedFields.Name === '') {
      this.switchUpdateMode();
      this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
      return;
    }

    this.saveRunning = true;
    this.updatedFields['LastUpdate'] = new Date();
    this.searchReferentialsService.updateDocumentById('accesscontracts', this.id, this.updatedFields)
      .subscribe((data) => {
        this.searchReferentialsService.getAccessContractById(this.id).subscribe((value) => {
          this.initData(value);
          this.switchUpdateMode();
          this.saveRunning = false;
          if (data.httpCode >= 400) {
            this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
          } else {
            this.dialogService.displayMessage('Les modifications ont bien été enregistrées.', '');
          }
        }, (error) => {
          this.saveRunning = false;
          this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
        });        
      }, (error) => {
        this.saveRunning = false;
        this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
      });
  }

  getDetail() {
    this.searchReferentialsService.getAccessContractById(this.id).subscribe((value) => {
      this.initData(value);
    });
  }

  initData(value) {
    this.contract = plainToClass(AccessContract, value.$results)[0];
    this.modifiedContract =  ObjectsService.clone(this.contract);
    this.isActif = this.modifiedContract.Status === 'ACTIVE' ? true : false;
  }
}
