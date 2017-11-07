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
            new FieldDefinition('ContractName', "Intitulé", 6, 8)
          ];
          this.searchForm = {"ContractID":"all","ContractName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '225px'})),
            ColumnDefinition.makeStaticColumn('#tenant', 'Tenant', undefined,
              () => ({'width': '63px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '62px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', 'Dernière modification', DateService.handleDate,
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
            new FieldDefinition('ContractName', "Intitulé", 6, 8)
          ];
          this.searchForm = {"ContractID":"all","ContractName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '225px'})),
            ColumnDefinition.makeStaticColumn('#tenant', 'Tenant', undefined,
              () => ({'width': '63px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '62px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', 'Dernière modification', DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/ingestContract';
          this.referentialIdentifier = 'Identifier';
          break;
        case "format":
          this.searchReferentialsService.setSearchAPI('admin/formats');
          this.breadcrumbName = "Formats";
          this.referentialData = [
            new FieldDefinition('FormatName', "Intitulé", 6, 8),
            FieldDefinition.createIdField('PUID', "PUID", 6, 8)
          ];
          this.searchForm = {"FormatName":"","PUID":"","orderby":{"field":"Name","sortType":"ASC"},"FORMAT":"all"};
          this.columns = [
            ColumnDefinition.makeStaticColumn('PUID', 'PUID', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Version', 'Version', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('MIMEType', 'MIME', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Extension', 'Extension(s)', undefined,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/format';
          this.referentialIdentifier = 'PUID';
          break;
        case "rule":
          this.searchReferentialsService.setSearchAPI('admin/rules');
          this.breadcrumbName = "Règles de gestion";
          let options = [
            {label : "Tous", value : "All"},
            {label : "Durée d'utilité aministrative", value : "AppraisalRule"},
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
            ColumnDefinition.makeSpecialValueColumn('Durée', SearchReferentialsComponent.appendUnitToRuleDuration,
                undefined, () => ({'width': '125px'})),
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
          this.breadcrumbName = "Profils d'archivage";
          this.referentialData = [
            new FieldDefinition('ProfileName', "Intitulé", 6, 8),
            FieldDefinition.createIdField('ProfileID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"ProfileID":"all","ProfileName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '60px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', 'Date de création', DateService.handleDate,
              () => ({'width': '100px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', 'Dernière modification', DateService.handleDate,
              () => ({'width': '100px'})),
            ColumnDefinition.makeIconColumn('Profil', ['fa-download'],
              SearchReferentialsComponent.downloadProfil, SearchReferentialsComponent.checkProfil,
              () => ({'width': '50px'}), this.searchReferentialsService)
          ];
          this.extraColumns = [];
          this.referentialPath = 'admin/profil';
          this.referentialIdentifier = 'Identifier';
          break;
        case "context":
          this.searchReferentialsService.setSearchAPI('contexts');
          this.breadcrumbName = "Contextes applicatifs";
          this.referentialData = [
            new FieldDefinition('ContextName', "Intitulé", 6, 8),
            FieldDefinition.createIdField('ContextID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"ContextID":"all","ContextName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
              () => ({'width': '325px'})),
            ColumnDefinition.makeStaticColumn('Identifier', 'Identifiant', undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('Status', 'Statut', SearchReferentialsComponent.handleStatus,
              () => ({'width': '125px'})),
            ColumnDefinition.makeSpecialIconColumn("Contrat d'accès",
              SearchReferentialsComponent.checkAccessContract, undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeSpecialIconColumn("Contrat d'entrée",
              SearchReferentialsComponent.checkIngestContract, undefined,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('CreationDate', "Date de création", DateService.handleDate,
              () => ({'width': '125px'})),
            ColumnDefinition.makeStaticColumn('LastUpdate', "Dernière modification", DateService.handleDate,
              () => ({'width': '125px'}))
          ];
          this.extraColumns = [
            ColumnDefinition.makeStaticColumn('#id', 'GUID', undefined,
              () => ({'width': '325px'}))
          ];
          this.referentialPath = 'admin/context';
          this.referentialIdentifier = 'Identifier';
          break;

        case "agencies":
          this.searchReferentialsService.setSearchAPI('agencies');
          this.breadcrumbName = "Services agents";
          this.referentialData = [
            new FieldDefinition('AgencyName', "Intitulé", 6, 8),
            FieldDefinition.createIdField('AgencyID', "Identifiant", 6, 8)
          ];
          this.searchForm = {"AgencyID":"all","AgencyName":"all","orderby":{"field":"Name","sortType":"ASC"}};
          this.columns = [
            ColumnDefinition.makeStaticColumn('Name', 'Intitulé', undefined,
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

  static appendUnitToRuleDuration(item): string {
    switch (item.RuleMeasurement.toUpperCase()) {
      case "YEAR":
        return item.RuleDuration <= 1 ? item.RuleDuration + ' année' : item.RuleDuration + ' années';
      case "MONTH":
        return item.RuleDuration + ' mois';
      case "DAY":
        return item.RuleDuration <= 1 ? item.RuleDuration + ' jour' : item.RuleDuration + ' jours';
      default :
        return item.RuleDuration;
    }
  }

  static checkAccessContract(item): string[] {
    if (item.Permissions instanceof Array) {
      for (let pem in item.Permissions) {
        if (item.Permissions[pem].AccessContracts &&  item.Permissions[pem].AccessContracts.length > 0) {
          return ['fa-check'];
        }
      }
    }
    return ['fa-close greyColor'];
  }

  static checkIngestContract(item): string[] {
    if (item.Permissions instanceof Array) {
      for (let pem in item.Permissions) {
        if (item.Permissions[pem].IngestContracts &&  item.Permissions[pem].IngestContracts.length > 0) {
          return ['fa-check'];
        }
      }
    }
    return ['fa-close greyColor'];
  }

  onNotifyPanelButton() {
    this.router.navigate(['admin/import/' + this.referentialType]);
  }

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }
}
