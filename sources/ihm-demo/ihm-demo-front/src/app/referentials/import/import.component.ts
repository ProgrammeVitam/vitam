import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { SelectItem } from 'primeng/primeng';

import { BreadcrumbService } from '../../common/breadcrumb.service';
import {PageComponent} from "../../common/page/page-component";
import { AuthenticationService } from '../../authentication/authentication.service';

@Component({
  selector: 'vitam-import',
  templateUrl: './import.component.html',
  styleUrls: ['./import.component.css']
})
export class ImportComponent  extends PageComponent {

  referentialType: string;
  referentialTypes : SelectItem[] = [
    {label:"Contextes applicatifs", value:'context'},
    {label:"Contrats d'accès", value:'accessContract'},
    {label:"Contrats d'entrée", value:'ingestContract'},
    {label:"Contrats de gestion", value:'managementContract'},
    {label:"Formats", value:'format'},
    {label:"Griffons", value:'griffins'},
    {label:"Ontologies", value:'ontology'},
    {label:"Profils d'archivage", value:'profil'},
    {label:"Profils d'unités archivistiques", value:'archiveUnitProfile'},
    {label:"Règles de gestion", value:'rule'},
    {label:"Scénarios de préservation", value:'scenarios'},
    {label:"Services agents", value:'agencies'}
  ];
  extensions : string[];
  uploadAPI : string;
  breadcrumbName: string;
  importSucessMsg: string;
  importErrorMsg: string;

  constructor(private activatedRoute: ActivatedRoute, private router : Router, private authenticationService : AuthenticationService,
              public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Import des référentiels', [], titleService, breadcrumbService);
    this.activatedRoute.params.subscribe( params => {
      this.referentialType = params['referentialType'];
      if (!this.authenticationService.isTenantAdmin()) {
          this.referentialTypes = this.referentialTypes.filter(
              item => item.value !== 'format' && item.value !== 'context'  && item.value !== 'ontology'&& item.value !== 'griffins'
          );
      }
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
        case "managementContract":
          this.extensions = ["json"];
          this.uploadAPI = 'managementcontracts';
          this.importSucessMsg = "Les contrats de gestion ont bien été importés";
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des contrats de gestion";
          break;
        case "archiveUnitProfile":
          this.extensions = ["json"];
          this.uploadAPI = 'archiveunitprofiles';
          this.importSucessMsg = 'Les profils d\'unités archivistiques ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des profils d\'unités archivistiques";
          break;
        case "format":
          this.extensions = ["xml"];
          this.uploadAPI = 'format/upload';
          this.importSucessMsg = 'Les formats ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des formats";
          break;
        case "rule":
          this.extensions = ["csv"];
          this.uploadAPI = 'rules/upload';
          this.importSucessMsg = 'Les règles de gestion ont bien été importées';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des règles de gestion";
          break;
        case "profil":
          this.extensions = ["json"];
          this.uploadAPI = 'profiles';
          this.importSucessMsg = 'Les profils d\'archivage ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des profils d'archivage";
          break;
        case "context":
          this.extensions = ["json"];
          this.uploadAPI = 'contexts';
          this.importSucessMsg = 'Les contextes applicatifs ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des contextes applicatifs";
          break;
        case "agencies":
          this.extensions = ["csv"];
          this.uploadAPI = 'agencies';
          this.importSucessMsg = 'Les services agents ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des services agents";
          break;
        case "ontology":
          this.extensions = ["json"];
          this.uploadAPI = 'ontologies';
          this.importSucessMsg = 'Les ontologies ont bien été importées';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des ontologies";
          break;
        case "griffins":
          this.extensions = ["json"];
          this.uploadAPI = 'griffins';
          this.importSucessMsg = 'Les griffons ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des griffons";
          break;
        case "scenarios":
          this.extensions = ["json"];
          this.uploadAPI = 'scenarios';
          this.importSucessMsg = 'Les scénarios de préservation ont bien été importés';
          this.importErrorMsg = "Echec de l'import du fichier.";
          this.breadcrumbName = "Import des scénarios de préservation ";
          break;
        default:
          this.router.navigate(['ingest/sip']);
      }

      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: this.breadcrumbName, routerLink: 'admin/import/' + this.referentialType}
      ];
      this.setBreadcrumb(newBreadcrumb);

    });

  }

  navigateToPage(action : string) {
    this.router.navigate(['admin/' + action + '/' + this.referentialType]);
  }


  pageOnInit() { }

}
