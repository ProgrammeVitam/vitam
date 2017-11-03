import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { ObjectsService } from '../../../common/utils/objects.service';
import { DialogService } from "../../../common/dialog/dialog.service";
import { PageComponent } from "../../../common/page/page-component";
import { Profil } from "./profil";

const PROFIL_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Intitulé',
  Status : 'Statut',
  Description : 'Description',
  Path : 'Fichier',
  '#tenant' : 'Tenant',
};
@Component({
  selector: 'vitam-profil',
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.css']
})
export class ProfilComponent extends PageComponent {

  profil : Profil;
  file : File;
  modifiedProfil : Profil;
  arrayOfKeys : string[];
  id: string;
  isActif : boolean;
  update: boolean;
  updatedFields = {};

  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService, private dialogService : DialogService) {
    super('Détail du profil d\'archivage', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Référentiel des profils d\'archivage', routerLink: 'admin/search/profil'},
        {label: 'Détail du profil d\'archivage' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.profil[key];
  }

  getKeyName(key: string) {
    return PROFIL_KEY_TRANSLATION[key] || key;
  }

  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedProfil =  ObjectsService.clone(this.profil);
    }
  }

  saveUpdate() {
    if (Object.keys(this.updatedFields).length == 0 && this.file == null) {
      this.switchUpdateMode();
      return;
    }

    this.updatedFields['LastUpdate'] = new Date();
    if (this.file != null) {
      let updatedFields = this.updatedFields;
      this.searchReferentialsService.uploadProfile(this.id, this.file)
        .subscribe((data) => {
          this.searchReferentialsService.updateProfilById(this.id, updatedFields)
            .subscribe((data) => {
              if (data.httpCode >= 400) {
                this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
              } else {
                this.dialogService.displayMessage('La modification a bien été enregistrée', '');
              }
              this.getDetail();
            }, (error) => {
              this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
            });
        }, (error) => {
          this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
        });
    } else {
      this.searchReferentialsService.updateProfilById(this.id, this.updatedFields)
        .subscribe((data) => {
          if (data.httpCode >= 400) {
            this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
          } else {
            this.dialogService.displayMessage('La modification a bien été enregistrée', '');
          }
          this.getDetail();
        }, (error) => {
          this.dialogService.displayMessage('Erreur de modification. Aucune modification effectuée', '');
        });
    }

    this.switchUpdateMode();
  }

  getDetail() {
    this.searchReferentialsService.getProfileById(this.id).subscribe((value) => {
      this.profil = plainToClass(Profil, value.$results)[0];
      this.modifiedProfil =  ObjectsService.clone(this.profil);
      if (this.modifiedProfil.Status === 'ACTIVE') {
        this.isActif = true;
      } else {
        this.isActif = false;
      }
    });
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
}
