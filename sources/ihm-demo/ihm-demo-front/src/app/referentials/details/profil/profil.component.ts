import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
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
  _tenant : 'Tenant',
};
@Component({
  selector: 'vitam-profil',
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.css']
})
export class ProfilComponent extends PageComponent {

  profil : Profil;
  arrayOfKeys : string[];
  id: string;
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
      this.searchReferentialsService.getProfileById(this.id).subscribe((value) => {
        this.profil = plainToClass(Profil, value.$results)[0];
        let keys = Object.keys(this.profil);
        let profil = this.profil;
        this.arrayOfKeys = keys.filter(function(key) {
          return key != '_id' && !!profil[key] && profil[key].length > 0;
        });
      });
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
    if (!this.update) {
    }
  }

  uploadProfile() {
    console.log(this.updatedFields);
    this.updatedFields['LastUpdate'] = new Date();
    this.searchReferentialsService.updateDocumentById('contracts', this.id, this.updatedFields)
      .subscribe((data) => {
        console.log(data);
        return data;
      });
  }
}
