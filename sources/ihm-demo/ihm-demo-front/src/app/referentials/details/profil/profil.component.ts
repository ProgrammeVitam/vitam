import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { ObjectsService } from '../../../common/utils/objects.service';
import { PageComponent } from "../../../common/page/page-component";
import { Profil } from "./profil";

const PROFIL_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Nom',
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
              private searchReferentialsService : ReferentialsService) {
    super('Détail du profil', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: ' Profils', routerLink: 'admin/search/profil'},
        {label: this.id, routerLink: ''}
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
              this.getDetail();
            });
        });
    } else {
      this.searchReferentialsService.updateProfilById(this.id, this.updatedFields)
        .subscribe((data) => {
          this.getDetail();
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
