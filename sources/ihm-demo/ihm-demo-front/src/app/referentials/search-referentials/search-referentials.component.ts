import { Component, EventEmitter } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Title } from '@angular/platform-browser';

import { BreadcrumbService, BreadcrumbElement } from "../../common/breadcrumb.service";
import { VitamResponse } from "../../common/utils/response";
import { PageComponent } from "../../common/page/page-component";
import { Preresult } from '../../common/search/preresult';
import { FieldDefinition } from '../../common/search/field-definition';
import { DateService } from '../../common/utils/date.service';
import {ColumnDefinition} from '../../common/generic-table/column-definition';
import { ReferentialsService } from "./../referentials.service";

@Component({
  selector: 'vitam-search-referentials',
  templateUrl: './search-referentials.component.html',
  styleUrls: ['./search-referentials.component.css']
})
//TODO Revoir si on doit éclater le component
export class SearchReferentialsComponent  extends PageComponent {

  referentialType : string;
  breadcrumbName : string;
  referentialPath : string;
  referentialIdentifier : string;
  public response: VitamResponse;
  public searchForm: any = {};
  searchButtonLabel : string;

  referentialData  = [];
  public columns = [];
  public extraColumns = [];


  constructor(private activatedRoute: ActivatedRoute, private router : Router,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              private searchReferentialsService : ReferentialsService) {
    super('Recherche du référentiel', [], titleService, breadcrumbService);
    this.activatedRoute.params.subscribe( params => {
      this.referentialType = params['referentialType'];
      switch (this.referentialType)
      {
        case "accessContract":
          this.searchReferentialsService.setSearchAPI('accesscontracts');
          this.breadcrumbName = "Contrats d'accès";
          this.referentialData = [
            FieldDefinition.createIdField('ContractID', "Identifiant", 6, 8),
            new FieldDefinition('ContractName', "Nom du contrat", 6, 8)
          ];
          this.searchForm = {"ContractID":"all","ContractName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Nom', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '225px'})),
            ColumnDefinition.makeStaticColumn('_tenant', 'Tenant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/accessContract';
          this.referentialIdentifier = 'Identifier';
          break;
        case "ingestContract":
          this.searchReferentialsService.setSearchAPI('contracts');
          this.breadcrumbName = "Contrats d'entrée";
          this.referentialData = [
            FieldDefinition.createIdField('ContractID', "Identifiant", 6, 8),
            new FieldDefinition('ContractName', "Nom du contrat", 6, 8)
          ];
          this.searchForm = {"ContractID":"all","ContractName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Nom', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '225px'})),
            ColumnDefinition.makeStaticColumn('_tenant', 'Tenant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/ingestContract';
          this.referentialIdentifier = 'Identifier';
          break;
        case "format":
          this.searchReferentialsService.setSearchAPI('admin/formats');
          this.breadcrumbName = "Référentiel des formats";
          this.referentialData = [
            new FieldDefinition('FormatName', "Nom de format", 6, 8),
            FieldDefinition.createIdField('PUID', "PUID", 6, 8)
          ];
          this.searchForm = {"FormatName":"","PUID":"","orderby":{"field":"Name","sortType":"ASC"},"FORMAT":"all"};
          this.columns = [
            ColumnDefinition.makeStaticColumn('PUID', 'PUID', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Name', 'Nom de format', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Version', 'Version', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('MIMEType', 'MIME', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Extension', 'Extension', undefined,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/format';
          this.referentialIdentifier = 'PUID';
          break;
        case "rule":
          this.searchReferentialsService.setSearchAPI('admin/rules');
          this.breadcrumbName = "Référentiel des règles de gestion";
          let options = [
            {label : "Tous", value : "All"},
            {label : "Durée d'utilité Administrative", value : "AppraisalRule"},
            {label : "Délai de communicabilité", value : "AccessRule"},
            {label : "Durée d'utilité courante", value : "StorageRule"},
            {label : "Délai de diffusion", value : "DisseminationRule"},
            {label : "Durée de réutilisation", value : "ReuseRule"},
            {label : "Durée de classification", value : "ClassificationRule"}
          ];
          this.referentialData = [
            new FieldDefinition('RuleValue', "Intitulé", 6, 8),
            FieldDefinition.createSelectMultipleField('RuleType', "Type", options, 6, 8)
          ];
          this.searchForm = {"RuleValue":"","RuleType":"All","orderby":{"field":"RuleValue","sortType":"ASC"},"RULES":"all"};
          this.columns = [
            ColumnDefinition.makeStaticColumn('RuleValue', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('RuleType', 'Type', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('RuleDuration', 'Durée', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('RuleDescription', 'Description', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('RuleId', 'Identifiant', undefined,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/rule';
          this.referentialIdentifier = 'RuleId';
          break;
        case "profil":
          this.searchReferentialsService.setSearchAPI('profiles');
          this.breadcrumbName = "Profils";
          this.referentialData = [
            new FieldDefinition('ProfileName', "Nom du profil", 6, 8),
            FieldDefinition.createIdField('ProfileID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"ProfileID":"all","ProfileName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Nom', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Description', 'Description', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '125px'})),
            ColumnDefinition.makeIconColumn('Profil', ['fa-download'],
              SearchReferentialsComponent.downloadProfil, SearchReferentialsComponent.checkProfil,
              () => ({'width': '50px'}), this.searchReferentialsService)
          ];
          this.extraColumns = [
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', 'Dernière modification', DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('ActivationDate', "Date d'activation", DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('DeactivationDate', 'Date de désactivation', DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.referentialPath = 'admin/profil';
          this.referentialIdentifier = 'Identifier';
          break;
        case "context":
          this.searchReferentialsService.setSearchAPI('contexts');
          this.breadcrumbName = "Contextes";
          this.referentialData = [
            new FieldDefinition('ContextName', "Nom du contexte", 6, 8),
            FieldDefinition.createIdField('ContextID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"ContextID":"all","ContextName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Nom', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('ActivationDate', "Date d'activation", DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('DeactivationDate', "Date désactivation", DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', "Date de création", DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', "Dernière modification", DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [
            ColumnDefinition.makeStaticColumn('_id', 'GUID', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeSpecialValueColumn("Contrat d'accès",
              SearchReferentialsComponent.checkAccessContract, undefined,
              () => ({'width': '175px'})),
            ColumnDefinition.makeSpecialValueColumn("Contrat d'entrée",
              SearchReferentialsComponent.checkIngestContract, undefined,
              () => ({'width': '200px'}))
          ];
          this.referentialPath = 'admin/context';
          this.referentialIdentifier = 'Identifier';
          break;

        case "agencies":
          this.searchReferentialsService.setSearchAPI('agencies');
          this.breadcrumbName = "Service agent";
          this.referentialData = [
            new FieldDefinition('AgencyName', "Nom du service agent", 6, 8),
            FieldDefinition.createIdField('AgencyID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"AgencyID":"all","AgencyName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Nom', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Description', 'Description', undefined,
              () => ({'width': '225px'}))
          ];
          this.referentialPath = 'admin/agencies';
          this.referentialIdentifier = 'Identifier';
          break;

        default:
          this.router.navigate(['ingest/sip']);
      }
      if (this.referentialType != "accession-register") {
        this.searchButtonLabel =  'Accèder à l\'import des référentiels';
      }
      let newBreadcrumb = [
        {label: 'Administration', routerLink: ''},
        {label: this.breadcrumbName, routerLink: 'admin/search/' + this.referentialType}
      ];

      this.setBreadcrumb(newBreadcrumb);


      this.searchReferentialsService.getResults(this.searchForm).subscribe(
          data => {this.response = data;},
          error => console.log('Error - ', this.response)
      );
    });
  }

  pageOnInit() {
  }

  static downloadProfil(item, searchReferentialsService) {
    searchReferentialsService.downloadProfile(item.Identifier);
  }

  static checkProfil(item): boolean {
    return item.Path;
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    preResult.request = request;
    preResult.searchProcessSkip = false;
    preResult.success = true;
    return preResult;
  }

  onNotify(event) {
    this.response = event.response;
    this.searchForm = event.form;
  }

  public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
    service.getResults(form).subscribe(
      (response) => {
        responseEvent.emit({response: response, form: form});
      },
      (error) => responseEvent.emit({response: null, form: form})
    );
  }

  static handleStatus(status): string {
    return (status === 'ACTIVE' || status === true) ? 'Actif' : 'Inactif';
  }

  static checkAccessContract(item): string {
    if (item.Permissions instanceof Array) {
      for (let pem in item.Permissions) {
        if (item.Permissions[pem].AccessContracts &&  item.Permissions[pem].AccessContracts.length > 0) {
          return 'V';
        }
      }
    }
    return 'X';
  }

  static checkIngestContract(item): string {
    if (item.Permissions instanceof Array) {
      for (let pem in item.Permissions) {
        if (item.Permissions[pem].IngestContracts &&  item.Permissions[pem].IngestContracts.length > 0) {
          return 'V';
        }
      }
    }
    return 'X';
  }

  onNotifyPanelButton() {
    this.router.navigate(['admin/import/' + this.referentialType]);
  }
}
