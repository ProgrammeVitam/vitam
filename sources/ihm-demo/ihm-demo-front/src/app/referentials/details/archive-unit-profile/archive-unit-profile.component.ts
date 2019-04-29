import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { ObjectsService } from '../../../common/utils/objects.service';
import { PageComponent } from "../../../common/page/page-component";
import { DialogService } from "../../../common/dialog/dialog.service";
import { ArchiveUnitProfile } from "./archive-unit-profile";
import { ErrorService } from "../../../common/error.service";

const AU_PROFILE_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Intitulé',
  Status : 'Statut',
  Description : 'Description',
  ControlSchema : 'Schéma de contrôle',
  '#tenant' : 'Tenant',
};
@Component({
  selector: 'vitam-archive-unit-profile',
  templateUrl: './archive-unit-profile.component.html',
  styleUrls: ['./archive-unit-profile.component.css']
})
export class ArchiveUnitProfileComponent extends PageComponent {

  archiveUnitProfile : ArchiveUnitProfile;
  file : File;
  modifiedArchiveUnitProfile : ArchiveUnitProfile;
  id: string;
  isActif : boolean;
  update: boolean;
  updatedFields: any = {};
  saveRunning = false;
  validRequest: any;

  constructor(private activatedRoute: ActivatedRoute, private router : Router, private errorService: ErrorService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService, private dialogService : DialogService) {
    super('Détail du profil d\'unités archivistiques ', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Profils d\'unités archivistiques', routerLink: 'admin/search/archiveUnitProfile'},
        {label: 'Détail du profil d\'unités archivistiques ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  getValue(key: string) {
    return this.archiveUnitProfile[key];
  }

  getKeyName(key: string) {
    return AU_PROFILE_KEY_TRANSLATION[key] || key;
  }

  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedArchiveUnitProfile =  ObjectsService.clone(this.archiveUnitProfile);
    }
  }

  saveUpdate() {
    if ((Object.keys(this.updatedFields).length == 0 && this.file == null) || this.updatedFields.Name === '') {
      this.switchUpdateMode();
      this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée.', '');
      return;
    }

    this.saveRunning = true;
    this.updatedFields.LastUpdate = new Date();
    if (this.file != null) {
      let updatedFields = this.updatedFields;
      this.searchReferentialsService.uploadArchiveUnitProfile(this.id, this.file)
        .subscribe((data) => {
          this.searchReferentialsService.updateArchiveUnitProfileById(this.id, updatedFields)
            .subscribe((data) => {
              this.searchReferentialsService.getArchiveUnitProfileById(this.id).subscribe((value) => {
                this.initData(value);
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
        }, (error) => {
          this.saveRunning = false;
          this.dialogService.displayMessage('Le profil d\'unités archivistiques est peut-être au mauvais format', 'Echec de l\'import du fichier');
        });
    } else {
      this.searchReferentialsService.updateArchiveUnitProfileById(this.id, this.updatedFields)
        .subscribe((data) => {
          this.searchReferentialsService.getArchiveUnitProfileById(this.id).subscribe((value) => {
            this.initData(value);
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

    this.switchUpdateMode();
  }

  getDetail() {
    this.searchReferentialsService.getArchiveUnitProfileById(this.id).subscribe(
      (value) => {
        this.initData(value);
      }, (error) => {
        this.errorService.handle404Error(error);
      }
    );
  }
    
  initJson(jsonQuery: string) {
    try {
      JSON.parse(jsonQuery);
      jsonQuery = JSON.stringify(JSON.parse(jsonQuery), null, 2);
    } catch (e) {
        return jsonQuery;  
    }    
    return jsonQuery;
  }
    
  public checkJson() {
    try {
      if (this.updatedFields.ControlSchema !== undefined) {
        JSON.parse(this.updatedFields.ControlSchema);
      } else {
        JSON.parse(this.modifiedArchiveUnitProfile.ControlSchema);
      }        
      this.validRequest = {valid: 'valide', css: 'font-color-green'};
      return true;
    } catch (e) {
      this.validRequest = {valid: 'non valide', css: 'font-color-red'};
      return false;
    }
  }

  changeStatus() {
    if (this.isActif) {
      this.updatedFields['Status'] = 'ACTIVE';
    } else {
      this.updatedFields['Status'] = 'INACTIVE';
    }
  }

  onChange(file) {
    this.file = file[0];
  }

  initData(value) {
    this.archiveUnitProfile = plainToClass(ArchiveUnitProfile, value.$results)[0];
    this.modifiedArchiveUnitProfile =  ObjectsService.clone(this.archiveUnitProfile);
    this.isActif = this.modifiedArchiveUnitProfile.Status === 'ACTIVE' ? true : false;
    this.modifiedArchiveUnitProfile.ControlSchema = this.initJson(this.modifiedArchiveUnitProfile.ControlSchema);
  }
}
