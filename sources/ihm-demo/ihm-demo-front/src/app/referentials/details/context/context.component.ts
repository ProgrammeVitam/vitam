import { Component, OnInit, Input } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { DateService } from '../../../common/utils/date.service';
import { PageComponent } from "../../../common/page/page-component";
import { Context } from "./context";

const CONTEXT_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  ActivationDate : 'Date d\'activation',
  DeactivationDate : 'Date de désactivation',
  Name : 'Nom',
  Status : 'Statut',
  Description : 'Description',
  _tenant : 'Tenant'
};

@Component({
  selector: 'vitam-context',
  templateUrl: './context.component.html',
  styleUrls: ['./context.component.css']
})

export class ContextComponent extends PageComponent {

  context : Context;
  arrayOfKeys : string[];
  id: string;
  update : boolean;
  updatedFields = {};
  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Détail du contexte', [], titleService, breadcrumbService);

  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.searchReferentialsService.getContextById(this.id).subscribe((value) => {
        this.context = plainToClass(Context, value.$results)[0];
        let keys = Object.keys(this.context);
        let context = this.context;
        this.arrayOfKeys = keys.filter(function(key) {
          return key != '_id' && !!context[key] && context[key].length > 0;
        });
      });
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Contextes', routerLink: 'admin/search/context'},
        {label: this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }


  getValue(key: string) {
    return this.context[key];
  }

  getKeyName(key: string) {
    return CONTEXT_KEY_TRANSLATION[key] || key;
  }

  translate(field : string) {
    if (field.indexOf('_tenant') >= 0) {
      return 'Tenant';
    }
    if (field.indexOf('AccessContracts') >= 0) {
      return 'Contrats d\'accès';
    }
    if (field.indexOf('IngestContracts') >= 0) {
      return 'Contrats d\'entrée';
    }
    return field;
  }

  isUpdatable(key : string) {
    if (['CreationDate', 'LastUpdate', 'Identifier', '_tenant', '_id'].indexOf(key) > -1) {
      return false;
    } else {
      return this.update;
    }
  }

  switchUpdateMode() {
    this.update = !this.update;
    if (!this.update) {
    }
  }



  saveUpdate() {
    console.log(this.updatedFields);
    this.updatedFields['LastUpdate'] = new Date();
    this.searchReferentialsService.updateDocumentById('contexts', this.id, this.updatedFields)
      .subscribe((data) => {
        console.log(data);
        return data;
      });
  }
}
