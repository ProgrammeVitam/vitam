import {Component} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {plainToClass} from 'class-transformer';
import {Title} from '@angular/platform-browser';

import {BreadcrumbService} from '../../../common/breadcrumb.service';
import {ReferentialsService} from '../../referentials.service';
import {ObjectsService} from '../../../common/utils/objects.service';
import {PageComponent} from '../../../common/page/page-component';
import {DialogService} from '../../../common/dialog/dialog.service';
import {IngestContract} from './ingest-contract';
import {ErrorService} from '../../../common/error.service';
import {ReferentialHelper} from '../../referential.helper';

@Component({
  selector: 'vitam-ingest-contract',
  templateUrl: './ingest-contract.component.html',
  styleUrls: ['./ingest-contract.component.css']
})

export class IngestContractComponent extends PageComponent {

  contract: IngestContract;
  modifiedContract: IngestContract;
  id: string;
  isActif: boolean;
  isCheckParent: boolean;
  isMasterMandatory: boolean;
  isFormatUnidentifiedAuthorized: boolean;
  linkDeclaredInManifest: string;
  update: boolean;
  updatedFields: any = {};
  saveRunning = false;

  public selectionOptions: any[] = [
    {label: 'Autoriser', value: 'AUTHORIZED'},
    {label: 'Obligatoire', value: 'REQUIRED'},
    {label: 'Interdit', value: 'UNAUTHORIZED'}];

  constructor(private activatedRoute: ActivatedRoute, private router: Router, private errorService: ErrorService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService: ReferentialsService, private dialogService: DialogService) {
    super('Détail du contrat d\'entrée ', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe(params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Contrats d\'entrée', routerLink: 'admin/search/ingestContract'},
        {label: 'Détail du contrat d\'entrée ' + this.id, routerLink: ''}
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

  displayCheckParentLink(test: string): string {
    return this.selectionOptions.find((item) => item.value == test).label;
  }

  changeStatus() {
    if (this.isActif) {
      this.updatedFields.Status = 'ACTIVE';
    } else {
      this.updatedFields.Status = 'INACTIVE';
    }
  }

  changeMasterMandatory() {
    this.updatedFields.MasterMandatory = this.modifiedContract.MasterMandatory;
  }

  changeBooleanValue(key: string) {
    this.updatedFields[key] = this.modifiedContract[key];
    if (key === 'EveryDataObjectVersion') {
      if (this.updatedFields[key] === true) {
        ObjectsService.pushAllWithoutDuplication(this.modifiedContract.DataObjectVersion, ReferentialHelper.optionLists.DataObjectVersion);
        this.updatedFields.DataObjectVersion = this.modifiedContract.DataObjectVersion;
      } else {
        ObjectsService.pushAllWithoutDuplication(this.modifiedContract.DataObjectVersion, this.contract.DataObjectVersion);
        delete this.updatedFields.DataObjectVersion;
      }
    }
  }

  changeFormatUnidentifiedAuthorized() {
    this.updatedFields.FormatUnidentifiedAuthorized = this.modifiedContract.FormatUnidentifiedAuthorized;
  }

  valueChange(key: string) {
    this.updatedFields[key] = this.modifiedContract[key];
  }

  checkParentIdChange(values: string[]){
    ObjectsService.pushAllWithoutDuplication(this.modifiedContract.CheckParentId, this.contract.CheckParentId);
    this.updatedFields.CheckParentId = this.modifiedContract.CheckParentId;
  }

  saveUpdate() {
    if (Object.keys(this.updatedFields).length == 0 || this.updatedFields.Name === '') {
      this.switchUpdateMode();
      this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
      return;
    }

    this.saveRunning = true;
    this.updatedFields.LastUpdate = new Date();
    this.searchReferentialsService.updateDocumentById('contracts', this.id, this.updatedFields)
      .subscribe((data) => {
        this.searchReferentialsService.getIngestContractById(this.id).subscribe((value) => {
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
    this.searchReferentialsService.getIngestContractById(this.id).subscribe(
      (value) => {
        this.initData(value);
      }, (error) => {
        this.errorService.handle404Error(error);
      }
    );
  }

  initData(value) {
    this.contract = plainToClass(IngestContract, value.$results)[0];
    if (this.contract.DataObjectVersion === undefined) {
      this.contract.DataObjectVersion = [];
    }
    if (this.contract.CheckParentId === undefined) {
      this.contract.CheckParentId = [];
    }
    this.modifiedContract = ObjectsService.clone(this.contract);
    this.isActif = this.modifiedContract.Status === 'ACTIVE';
  }
}
