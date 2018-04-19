import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { plainToClass } from 'class-transformer';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { ReferentialsService } from "../../referentials.service";
import { ObjectsService } from '../../../common/utils/objects.service';
import { PageComponent } from "../../../common/page/page-component";
import { DialogService } from "../../../common/dialog/dialog.service";
import { Ontology } from "./ontology";
import { ErrorService } from "../../../common/error.service";

const ONTOLOGY_KEY_TRANSLATION = {
  Identifier: 'Identifiant',
  CreationDate : 'Date de création',
  LastUpdate : 'Date de mise à jour',
  SedaField : 'Intitulé SEDA',
  ApiField : 'Intitulé externe',
  Type : 'Statut',
  Description : 'Description'
};
@Component({
  selector: 'vitam-ontology',
  templateUrl: './ontology.component.html',
  styleUrls: ['./ontology.component.css']
})
export class OntologyComponent extends PageComponent {

  ontology : Ontology;
  file : File;
  modifiedOntology : Ontology;
  id: string;
  isActif : boolean;
  update: boolean;
  updatedFields: any = {};
  saveRunning = false;
  validRequest: any;

  constructor(private activatedRoute: ActivatedRoute, private router : Router, private errorService: ErrorService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService, private dialogService : DialogService) {
    super('Détail de l\'ontologie ', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.activatedRoute.params.subscribe( params => {
      this.id = params['id'];
      this.getDetail();
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: 'Ontologies', routerLink: 'admin/search/Ontology'},
        {label: 'Détail de l\'ontologie ' + this.id, routerLink: ''}
      ];

      this.setBreadcrumb(newBreadcrumb);
    });
  }

  getValue(key: string) {
    return this.ontology[key];
  }

  getKeyName(key: string) {
    return ONTOLOGY_KEY_TRANSLATION[key] || key;
  }

  switchUpdateMode() {
    this.update = !this.update;
    this.updatedFields = {};
    if (!this.update) {
      this.modifiedOntology =  ObjectsService.clone(this.ontology);
    }
  }

  saveUpdate() {

  }

  getDetail() {
    this.searchReferentialsService.getOntologyById(this.id).subscribe(
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
    




  onChange(file) {
    this.file = file[0];
  }

  initData(value) {
    this.ontology = plainToClass(Ontology, value.$results)[0];
    this.modifiedOntology =  ObjectsService.clone(this.ontology);
  }
}
