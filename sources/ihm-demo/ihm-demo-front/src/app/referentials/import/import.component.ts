import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';

import { BreadcrumbService } from '../../common/breadcrumb.service';
import {PageComponent} from "../../common/page/page-component";

@Component({
  selector: 'vitam-import',
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.css']
})
export class ImportComponent  extends PageComponent {

  referentialType: string;
  referentialTypes : SelectItem[] = [
    {label:"Contrats d'accès", value:'accessContract'},
    {label:"Contrats d'entrée", value:'ingestContract'},
    {label:"Formats", value:'format'},
    {label:"Règles de gestion", value:'rule'},
    {label:"Profils", value:'profil'},
    {label:"Contextes", value:'context'},
    {label:"Service Agent", value:'agencies'}
  ];
  extensions : string[];
  uploadAPI : string;
  breadcrumbName: string;
  importSucessMsg: string;
  importErrorMsg: string;

  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Import du référentiel', [], titleService, breadcrumbService);
    this.activatedRoute.params.subscribe( params => {
      this.referentialType = params['referentialType'];
      switch (this.referentialType)
      {
        case "accessContract":
          this.extensions = ["json"];
          this.uploadAPI = 'accesscontracts';
          this.importSucessMsg = "Les contrats d'accès ont bien été importés";
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des contrats d'accès";
          break;
        case "ingestContract":
          this.extensions = ["json"];
          this.uploadAPI = 'contracts';
          this.importSucessMsg = "Les contrats d'entrée ont bien été importés";
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des contrats d'entrée";
          break;
        case "format":
          this.extensions = ["xml"];
          this.uploadAPI = 'format/upload';
          this.importSucessMsg = 'Le réferentiel de formats est bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import du référentiel des formats";
          break;
        case "rule":
          this.extensions = ["csv"];
          this.uploadAPI = 'rules/upload';
          this.importSucessMsg = 'Le référentiel des règles de gestion est bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import du référentiel des règles de gestion";
          break;
        case "profil":
          this.extensions = ["json"];
          this.uploadAPI = 'profiles';
          this.importSucessMsg = 'Les profils ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des profils";
          break;
        case "context":
          this.extensions = ["json"];
          this.uploadAPI = 'contexts';
          this.importSucessMsg = 'Les contextes ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des contextes";
          break;
        case "agencies":
          this.extensions = ["csv"];
          this.uploadAPI = 'agencies';
          this.importSucessMsg = 'Les services agents ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des services agents";
          break;
        default:
          this.router.navigate(['ingest/sip']);
      }

      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: this.breadcrumbName, routerLink: 'import/' + this.referentialType}
      ];
      this.setBreadcrumb(newBreadcrumb);

    });

  }

  navigateToPage(action : string) {
    this.router.navigate(['admin/' + action + '/' + this.referentialType]);
  }


  pageOnInit() { }

}
