import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from '../../../common/breadcrumb.service';
import { ReferentialsService } from '../../referentials.service';
import { ObjectsService } from '../../../common/utils/objects.service';
import { PageComponent } from '../../../common/page/page-component';
import { DialogService } from '../../../common/dialog/dialog.service';
import { ManagementContract, ManagementContractStorage } from './management-contract';
import { ErrorService } from '../../../common/error.service';

@Component({
  selector: 'vitam-management-contract',
  templateUrl: './management-contract.component.html',
  styleUrls: ['./management-contract.component.css']
})
export class ManagementContractComponent extends PageComponent {

  contract: ManagementContract;
  modifiedContract: ManagementContract;
  id: string;
  update: boolean;
  isActif: boolean;
  updatedFields: any = {};
  saveRunning = false;

  constructor(private activatedRoute: ActivatedRoute, private router: Router,
    public titleService: Title, public breadcrumbService: BreadcrumbService,
    private searchReferentialsService: ReferentialsService, private dialogService: DialogService,
    private errorService: ErrorService) {
    super('Détail du contrat de gestion', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        { label: 'Administration', routerLink: '' },
        { label: 'Contrats de gestion', routerLink: 'admin/search/managementContract' },
        { label: 'Détail du contrat de gestion ' + this.id, routerLink: '' }
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedContract = ObjectsService.clone(this.contract);
      this.isActif = this.modifiedContract.Status === 'ACTIVE';
    }
  }

  changeStatus() {
    if (this.isActif) {
      this.updatedFields.Status = 'ACTIVE';
    } else {
      this.updatedFields.Status = 'INACTIVE';
    }

  }
  updateFieldStorage() {

    if (!this.updatedFields.hasOwnProperty('UnitStrategy') &&
      !this.updatedFields.hasOwnProperty('ObjectGroupStrategy') &&
      !this.updatedFields.hasOwnProperty('ObjectStrategy')) {
        return;
    }

    this.updatedFields.Storage = this.modifiedContract.Storage;
    if (this.updatedFields.hasOwnProperty('UnitStrategy')) {
      this.updatedFields.Storage.UnitStrategy = this.updatedFields.UnitStrategy;
      delete this.updatedFields.UnitStrategy;
    }

    if (this.updatedFields.hasOwnProperty('ObjectGroupStrategy')) {
      this.updatedFields.Storage.ObjectGroupStrategy = this.updatedFields.ObjectGroupStrategy;
      delete this.updatedFields.ObjectGroupStrategy;
    }
    if (this.updatedFields.hasOwnProperty('ObjectStrategy')) {
      this.updatedFields.Storage.ObjectStrategy = this.updatedFields.ObjectStrategy;
      delete this.updatedFields.ObjectStrategy;
    }

    if(this.updatedFields.Storage.UnitStrategy === '' || this.updatedFields.Storage.UnitStrategy === null){
      delete this.updatedFields.Storage.UnitStrategy;
    }
    if(this.updatedFields.Storage.ObjectGroupStrategy === '' || this.updatedFields.Storage.ObjectGroupStrategy === null){
      delete this.updatedFields.Storage.ObjectGroupStrategy;
    }
    if(this.updatedFields.Storage.ObjectStrategy === '' || this.updatedFields.Storage.ObjectStrategy === null){
      delete this.updatedFields.Storage.ObjectStrategy;
    }

  }

  saveUpdate() {
    if (Object.keys(this.updatedFields).length == 0 || this.updatedFields.Name === '') {
      this.switchUpdateMode();
      this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
      return;
    }

    this.saveRunning = true;
    this.updateFieldStorage();
    this.updatedFields.LastUpdate = new Date();
    this.searchReferentialsService.updateDocumentById('managementcontracts', this.id, this.updatedFields)
      .subscribe((data) => {
        this.searchReferentialsService.getManagementContractById(this.id).subscribe((value) => {
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
    this.searchReferentialsService.getManagementContractById(this.id).subscribe(
      (value) => {
        this.initData(value);
      }, (error) => {
        this.errorService.handle404Error(error);
      }
    );
  }

  initData(value) {
    this.contract = plainToClass(ManagementContract, value.$results)[0];
    this.modifiedContract = ObjectsService.clone(this.contract);
    if (this.modifiedContract.Storage === null || !this.modifiedContract.Storage ) {
      this.modifiedContract.Storage = new ManagementContractStorage();
    }
    this.isActif = this.modifiedContract.Status === 'ACTIVE';
  }
}
