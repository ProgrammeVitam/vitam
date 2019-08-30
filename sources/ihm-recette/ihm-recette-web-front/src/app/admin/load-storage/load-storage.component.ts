import { Component } from '@angular/core';
import {HttpHeaders} from '@angular/common/http';
import {SelectItem} from 'primeng/api';
import {ResourcesService} from '../../common/resources.service';
import {LoadStorageService} from './load-storage.service';
import {FileSystemFileEntry} from 'ngx-file-drop';
import {TenantService} from '../../common/tenant.service';
import {PageComponent} from '../../common/page/page-component';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {Title} from '@angular/platform-browser';

class FileData {
  constructor(public file: string, public category: string, strategyId:String, offerId:String ) { }
}

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Recherche et modification d\'un fichier', routerLink: 'admin/load-storage'}
];

@Component({
  selector: 'vitam-load-storage',
  templateUrl: './load-storage.component.html',
  styleUrls: ['./load-storage.component.css']
})
export class LoadStorageComponent extends PageComponent {
  error = false;
  fileName: string;
  strategyId: string;
  offerId: string;
  category: string;
  tenant: string = this.resourcesService.getTenant();
  savedData: FileData;
  dataState = 'KO';

  fileUpload: File;
  readOrderRequestError: any;
  displayErrorInitImport = false;
  displayGetError = false;
  displayAsyncGetMessage = false;
  displayExportStatusOK = false;
  displayExportStatusInprogressOrKo = false;
  readOrderRequest: any;
  displayDeleteError =false;
  displayMessageDelete = false ;
  displayErrorImport = false;
  displaySuccessImport = false;

  selectedStrategy: any = {};

  strategyList: Array<SelectItem>;
  offerList: Array<SelectItem>;
  categoryOptions: SelectItem[] = [
    { label: 'Unités Archivistiques', value: 'UNIT' },
    { label: 'Objet binaires', value: 'OBJECT' },
    { label: 'Groupes d\'objets techniques', value: 'OBJECTGROUP' }
  ];

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, private resourcesService: ResourcesService,
              private loadStorageService: LoadStorageService, private tenantService: TenantService) {
    super('Recherche et modification d\'un fichier', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    return this.tenantService.getState().subscribe((value) => {
      this.tenant = value;
      if (this.tenant) {
        this.getStrategies();
      }
      this.dataState = 'KO';
      delete this.savedData;
    });
  }

  getStrategies() {
    return this.resourcesService.get("strategies",new HttpHeaders().set('X-Tenant-Id', this.tenant)).subscribe(
      (response) => {
        this.strategyList = response.map(
          (strategy) => {
            return {label: strategy.id, value: strategy}
          }
        );
      }
    )
  }

  selectStrategy(){
    this.strategyId = this.selectedStrategy.id;
    this.offerList = this.selectedStrategy.offers.map(
      (offer) => {
        return {label: offer.id, value: offer.id}
      });
  }

  getObject() {

    if (!this.fileName || !this.category || !this.tenant || !this.strategyId || !this.offerId) {
      this.error = true;
      return;
    }

    this.dataState = 'RUNNING';

    this.savedData = new FileData(this.fileName, this.category, this.strategyId, this.offerId);

    this.loadStorageService.download(this.fileName, this.category, this.strategyId, this.offerId).subscribe(
      (response) => {
        const a = document.createElement('a');
        document.body.appendChild(a);
        a.href = URL.createObjectURL(response.body)
        a.download = this.fileName
        a.click();
        this.dataState = 'OK';
      }, () => {
        delete this.savedData;
        this.dataState = 'KO';
        this.displayGetError = true;
      }
    );
  }

  exportObject() {

    if (!this.fileName || !this.category || !this.tenant || !this.strategyId || !this.offerId) {
      this.error = true;
      return;
    }

    this.dataState = 'RUNNING';

    this.savedData = new FileData(this.fileName, this.category, this.strategyId, this.offerId);

    this.loadStorageService.createReadOrderRequest(this.fileName, this.category, this.strategyId, this.offerId).subscribe(
      (response) => {
        this.readOrderRequest = response.$results[0];
        this.displayAsyncGetMessage = true;
        this.dataState = 'OK';
      }, (error) => {
        delete this.savedData;
        this.readOrderRequestError = error.error;
        this.dataState = 'KO';
        this.displayGetError = true;
      }
    );
  }

  checkExportStatus() {

    if (!this.fileName || !this.tenant || !this.strategyId ) {
      this.error = true;
      return;
    }

    this.dataState = 'RUNNING';

    this.loadStorageService.getReadOrderRequest(this.fileName, this.strategyId, this.offerId).subscribe(
     (response) => {
        this.readOrderRequest = response.$results[0];
        this.displayExportStatusOK = true;
        this.dataState = 'OK';
      }, (error) => {
        delete this.savedData;
        this.readOrderRequestError = error.error;
        this.dataState = 'KO';
        this.displayExportStatusInprogressOrKo = true;
      }
    );
  }

  deleteObject() {

    if (!this.fileName || !this.category || !this.tenant || !this.strategyId || !this.offerId) {
      this.error = true;
      return;
    }

    this.loadStorageService.delete(this.fileName, this.category, this.strategyId, this.offerId).subscribe(
      (response) => {

          this.displayMessageDelete = true;

      }, () => {
        delete this.savedData;
        this.displayDeleteError = true;
      }
    );
  }

  onFileDrop(event) {
    for (const droppedFile of event.files) {

      if (droppedFile.fileEntry.isFile) {
        const fileEntry = droppedFile.fileEntry as FileSystemFileEntry;
        fileEntry.file((file: File) => {
          this.fileUpload = file;
        });
      }
    }

  }

  onChange(files) {
    this.fileUpload = files[0];
  }

  uploadFile() {

    this.loadStorageService.uploadFile(this.fileUpload, this.fileName,this.fileUpload.size, this.category, this.strategyId, this.offerId).subscribe(
      (response) => {
        delete this.fileUpload;
        this.displaySuccessImport = true;
      }, () => {
        delete this.fileUpload;
        this.displayErrorImport = true;
      }
    );

  }

}
