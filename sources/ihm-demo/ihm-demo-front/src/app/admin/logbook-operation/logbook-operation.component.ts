import {Component, EventEmitter} from '@angular/core';
import {FieldDefinition} from '../../common/search/field-definition';
import {VitamResponse} from '../../common/utils/response';
import {PageComponent} from '../../common/page/page-component';
import {BreadcrumbElement, BreadcrumbService} from '../../common/breadcrumb.service';
import {Title} from '@angular/platform-browser';
import {Preresult} from '../../common/preresult';
import {LogbookService} from '../../ingest/logbook.service';
import {ColumnDefinition} from '../../common/generic-table/column-definition';
import {DateService} from '../../common/utils/date.service';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
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
    {label: '--', value: ''},
    {label: 'Audit', value: 'audit'},
    {label: 'Données de base', value: 'masterdata'},
    {label: 'Elimination', value: 'elimination'},
    {label: 'Entrée', value: 'ingest'},
    {label: 'Export DIP', value: 'export_dip'},
    {label: 'Mise à jour', value: 'update'},
    // {label: 'Mise à jour de format autorisé', value: 'formatUpdate'},
    {label: 'Préservation', value: 'preservation'},
    {label: 'Réorganisation', value: 'reclassification'},
    {label: 'Sauvegarde écritures', value: 'storage_backup'},
    {label: 'Sécurisation', value: 'traceability'},
    {label: 'Vérification', value: 'check'}
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
      (error) => console.error('Error: ', error)
    );
  }

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }

  public columns = [
    ColumnDefinition.makeStaticColumn('evTypeProc', 'Catégorie d\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evType', 'Opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date', DateService.handleDateWithTime,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Statut',
      LogbookOperationComponent.getOperationStatus, LogbookOperationComponent.handleStatus,
      () => ({'width': '125px'}), false),
    ColumnDefinition.makeStaticColumn('outMessg', 'Message', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false)
  ];
  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evId', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('outDetail', 'Code technique', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agId', 'Identifiant de l\'agent interne', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('obId', 'Identifiant interne de l\'objet', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evDetData', 'Informations complémentaires sur le résultat', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('rightsStatementIdentifier', 'Règles utilisées', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evIdReq', 'Identifiant de la requête', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agIdExt', 'Identifiants des agents externes', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('obIdIn', 'Identifiant externe du lot d\'objet', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('#tenant', 'Identifiant du tenant', undefined,
      () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agIdApp', 'Identifiant application', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evIdAppSession', 'Identifiant transaction', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialIconColumn('Rapport', LogbookOperationComponent.handleReports,
      () => ({
        'width': '50px',
        'overflow-wrap': 'break-word'
      }), LogbookOperationComponent.downloadReports, this.logbookService, false),
    ColumnDefinition.makeSpecialIconColumn('Fichier d\'origine', LogbookOperationComponent.handleDownloadReferentialCSV,
      () => ({
        'width': '50px',
        'overflow-wrap': 'break-word'
      }), LogbookOperationComponent.downloadReferentialCSV, this.logbookService, false)
  ];

  constructor(public logbookService: LogbookService, public titleService: Title, public breadcrumbService: BreadcrumbService) {
    super('Journal des opérations', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.searchForm = this.preSearchFunction({}).request;
    this.logbookService.getResults(this.searchForm, 0).subscribe(
      data => this.response = data,
      error => console.log('Error - ', this.response));
  }

  static handleDownloadReferentialCSV(item): string[] {
    if ((item.evType.toUpperCase() == 'IMPORT_AGENCIES' || item.evType.toUpperCase() == 'STP_IMPORT_RULES')
      && LogbookOperationComponent.getOperationStatus(item) === 'OK') {
      return ['fa-download']
    } else {
      return [];
    }
  }

  static handleReports(item): string[] {
    const evType = item.evTypeProc.toUpperCase();
    if (['AUDIT', 'EXPORT_DIP', 'INGEST', 'MASS_UPDATE'].indexOf(evType) > -1 || item.evType.toUpperCase() === 'STP_IMPORT_RULES'
      || item.evType.toUpperCase() === 'IMPORT_AGENCIES' || item.evType.toUpperCase() === 'HOLDINGSCHEME'
      || item.evType.toUpperCase() === 'IMPORT_ONTOLOGY' || item.evType.toUpperCase() === 'STP_REFERENTIAL_FORMAT_IMPORT'
      || item.evType.toUpperCase() === 'DATA_MIGRATION' || item.evType.toUpperCase() === 'ELIMINATION_ACTION'
      || item.evType.toUpperCase() === 'IMPORT_PRESERVATION_SCENARIO' || item.evType.toUpperCase() === 'IMPORT_GRIFFIN' || item.evType.toUpperCase() === 'STP_IMPORT_GRIFFIN'
      || item.evType.toUpperCase() === 'PRESERVATION' || item.evType.toUpperCase() === 'STP_IMPORT_ACCESS_CONTRACT' ) {

      if (LogbookOperationComponent.isOperationInProgress(item)) {
        return ['fa-clock-o'];
      } else {
        return ['fa-download'];
      }
    } else {
      return [];
    }
  }


  static handleStatus(status): string {
    switch (status) {
      case 'OK':
        return 'Succès';
      case 'STARTED':
        return 'En cours';
      case 'En cours':
        return 'En cours';
      case 'KO':
      case 'FATAL':
        return 'Erreur';
      default:
        return 'Avertissement';
    }
  }

  static handleValue(item): string {
    if (!item.events) {
      return ''
    }
    const lastItem = item.events.length - 1;
    return item.events[lastItem] ? item.events[lastItem].outcome.toUpperCase() : ''
  }

  static getOperationStatus(item): string {
    const eventsLength = item.events.length;

    if (eventsLength > 0) {
      if (item.evType === item.events[eventsLength - 1].evType) {
        return item.events[eventsLength - 1].outcome;
      } else {
        return 'En cours';
      }
    } else {
      return 'KO';
    }
  }

  public preSearchFunction(request): Preresult {
    const preResult = new Preresult();
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

    if (LogbookOperationComponent.isOperationInProgress(item)) {
      return;
    }

    switch (item.evTypeProc.toUpperCase()) {
      case 'AUDIT':
        if(item.evType === "EXPORT_PROBATIVE_VALUE") {
          logbookService.downloadReport(item.evIdProc);
          break;
        }
        if(item.evType === "EVIDENCE_AUDIT") {
          logbookService.downloadReport(item.evIdProc);
          break;
        }
        break;
      case 'DATA_MIGRATION':
      case 'ELIMINATION':
      case 'PRESERVATION':
        logbookService.downloadBatchReport(item.evIdProc);
        break;
      case 'MASS_UPDATE':
        logbookService.downloadMassUpdateReport(item.evIdProc);
        break;
      case 'INGEST':
        logbookService.downloadObject(item.evIdProc);
        break;
      case 'EXPORT_DIP':
        logbookService.downloadDIP(item.evIdProc);
        break;
      case 'MASTERDATA':
        if (item.evType.toUpperCase() === 'STP_IMPORT_RULES' || item.evType.toUpperCase() === 'IMPORT_AGENCIES' || item.evType.toUpperCase() === 'IMPORT_ONTOLOGY') {
          logbookService.downloadReport(item.evIdProc);
          break;
        } else if (item.evType.toUpperCase() === 'HOLDINGSCHEME') {
          logbookService.downloadObject(item.evIdProc);
          break;
        } else if (item.evType === 'STP_REFERENTIAL_FORMAT_IMPORT') {
          logbookService.downloadReport(item.evIdProc);
          break;
        } else if (item.evType === 'IMPORT_GRIFFIN') {
            logbookService.downloadReport(item.evIdProc);
            break;
        } else if (item.evType === 'STP_IMPORT_GRIFFIN') {
            logbookService.downloadReport(item.evIdProc);
            break;
        } else if (item.evType === 'IMPORT_PRESERVATION_SCENARIO') {
            logbookService.downloadReport(item.evIdProc);
            break;
        } else if (item.evType === 'STP_IMPORT_ACCESS_CONTRACT') {
          logbookService.downloadReport(item.evIdProc);
          break;
    }
  }
  }
  static isOperationInProgress(item): boolean {
    const status = LogbookOperationComponent.getOperationStatus(item);
    switch (status) {
      case 'STARTED':
      case 'En cours':
        return true;
      default:
        return false;
    }
  }

  static downloadReferentialCSV(item, logbookService) {
    if (item.evType.toUpperCase() === 'STP_IMPORT_RULES') {
      logbookService.downloadReferentialCSV(item.evIdProc, 'rules');
    } else if (item.evType.toUpperCase() === 'IMPORT_AGENCIES') {
      logbookService.downloadReferentialCSV(item.evIdProc, 'agencies');
    }
  }


}
