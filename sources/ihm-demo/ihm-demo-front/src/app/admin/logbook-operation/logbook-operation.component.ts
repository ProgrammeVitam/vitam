import { Component, EventEmitter } from '@angular/core';
import {FieldDefinition} from '../../common/search/field-definition';
import {VitamResponse} from "../../common/utils/response";
import {PageComponent} from "../../common/page/page-component";
import {BreadcrumbElement, BreadcrumbService} from "../../common/breadcrumb.service";
import {Title} from "@angular/platform-browser";
import {Preresult} from "../../common/search/preresult";
import {LogbookService} from "../../ingest/logbook.service";
import {ColumnDefinition} from "../../common/generic-table/column-definition";
import {ArchiveUnitHelper} from "../../archive-unit/archive-unit.helper";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Admin', routerLink: ''},
  {label: 'Journal des opérations', routerLink: 'admin/logbookOperation'}
];

@Component({
  selector: 'vitam-logbook-operation',
  templateUrl: './logbook-operation.component.html',
  styleUrls: ['./logbook-operation.component.css']
})
export class LogbookOperationComponent extends PageComponent {
  public response: VitamResponse;
  public searchForm: any = {};
  public options = [
    {label : "--", value : "--"},
    {label : "Audit", value : "audit"},
    {label : "Données de base", value : "masterdata"},
    {label : "Elimination", value : "elimination"},
    {label : "Entrée", value : "ingest"},
    {label : "Mise à jour", value : "update"},
    {label : "Préservation", value : "preservation"},
    {label : "Sécurisation", value : "traceability"},
    {label : "Vérification", value : "check"}
  ];
  public logbookData = [
    FieldDefinition.createIdField('evId', 'Identifiant', 6, 8),
    FieldDefinition.createSelectField('EventType', 'Catégorie d\'opération', '--', this.options, 6, 8)
  ];

  public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
    service.getResults(form, offset).subscribe(
        (response) => {
          responseEvent.emit({response: response, form: form});
        },
        (error) => console.log('Error: ', error)
    );
  }

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }

  public columns = [
    ColumnDefinition.makeStaticColumn('evTypeProc', 'Catégorie d\'opération', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evType', 'Opération', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date', this.archiveUnitHelper.handleDateWithTime,
        () => ({'width': '100px'})),
    ColumnDefinition.makeSpecialValueColumn('Statut',
        (item) => item.events[1] ? item.events[1].outcome.toUpperCase() : '', LogbookOperationComponent.handleStatus,
        () => ({'width': '125px'})),
    ColumnDefinition.makeStaticColumn('outMessg', 'Message', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'}))
  ];
  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evId', 'Identifiant de l\'opération', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('outDetail', 'Code technique', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agId', 'Identifiant de l\'agent interne', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('obId', 'Identifiant interne de l\'objet', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDetData', 'Informations complémentaires sur le résultat', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('rightsStatementIdentifier', 'Règles utilisées', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evIdReq', 'Identifiant de la requête', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agIdExt', 'Identifiants des agents externes', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('obIdIn', 'Identifiant externe du lot d\'objet', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('#tenant', 'Identifiant du tenant', undefined,
        () => ({'width': '125px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agIdApp', 'Identifiant application', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evIdAppSession', 'Identifiant transaction', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialIconColumn('Rapport', LogbookOperationComponent.handleReports,
        () => ({'width': '50px', 'overflow-wrap': 'break-word'}), LogbookOperationComponent.downloadReports, this.logbookService),
  ];

  constructor(public logbookService: LogbookService, public titleService: Title, public breadcrumbService: BreadcrumbService,
              public archiveUnitHelper: ArchiveUnitHelper) {
    super('Journal des opérations', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.searchForm = this.preSearchFunction({}).request;
    this.logbookService.getResults(this.searchForm, 0).subscribe(
        data => this.response = data,
        error => console.log('Error - ', this.response));
  }

  static handleReports(item): string[] {
    const evType = item.evTypeProc.toUpperCase();
    if (['AUDIT','EXPORT_DIP','INGEST'].indexOf(evType) > -1) {
      return ['fa-download']
    }else {
      return [];
    }
  }

  static handleStatus(status): string {
    switch (status) {
      case 'OK': return 'Succès';
      case 'STARTED': return 'En cours';
      case 'KO': case 'FATAL': return 'Erreur';
      default: return 'Avertissement';
    }
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    if (!request.evId) {
      request.evId = '';
      if (!request.EventType || request.EventType === '--') {
        request.EventType = 'all';
      }
      if (request.obIdIn === '') {
        delete request.obIdIn;
      }
    } else {
      delete request.EventType;
    }

    request.orderby = {
      field: 'evDateTime',
      sortType: 'DESC'
    };

    preResult.request = request;
    preResult.searchProcessSkip = false;
    preResult.success = true;
    return preResult;
  }

  onNotify(event) {
    this.response = event.response;
    this.searchForm = event.form;
  }

  static downloadReports(item, logbookService) {
    switch (item.evTypeProc.toUpperCase()) {
      case 'AUDIT':
        logbookService.downloadReport(item.evIdProc);
        break;
      case 'INGEST':
        logbookService.downloadObject(item.evIdProc);
        break;
      case 'EXPORT_DIP':
        logbookService.downloadDIP(item.evIdProc);
        break;
    }

  }

}
