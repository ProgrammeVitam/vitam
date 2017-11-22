
import { Component, EventEmitter } from '@angular/core';
import { ColumnDefinition } from '../../common/generic-table/column-definition';
import { LogbookService } from '../logbook.service';
import { IngestUtilsService } from '../../common/utils/ingest-utils.service';
import { FieldDefinition } from '../../common/search/field-definition';
import { Preresult } from '../../common/search/preresult';
import { VitamResponse } from "../../common/utils/response";
import { BreadcrumbService, BreadcrumbElement } from "../../common/breadcrumb.service";
import { Title } from '@angular/platform-browser';
import { PageComponent } from "../../common/page/page-component";
import { ArchiveUnitHelper } from "../../archive-unit/archive-unit.helper";
import { DateService } from "../../common/utils/date.service";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Entrée', routerLink: ''},
  {label: 'Suivi des opérations d\'entrée', routerLink: 'ingest/logbook'}
];

const operationType = [
  {label : "Tous", value : ""},
  {label : "Upload d'un SIP", value : "PROCESS_SIP_UNITARY"},
  {label : "Plan de classement", value : "FILINGSCHEME"}
];

@Component({
  selector: 'vitam-logbook',
  templateUrl: './logbook.component.html',
  styleUrls: ['./logbook.component.css']
})
export class LogbookComponent extends PageComponent {
  public getClass = (item) => 'clickableDiv';

  public response: VitamResponse;
  public searchForm: any = {};

  public logbookData = [
    FieldDefinition.createIdField('evId', 'Identifiant de la demande d\'entrée', 6, 8),
    FieldDefinition.createSelectMultipleField('evType', "Catégorie d\'opération", operationType, 6, 10),
    FieldDefinition.createDateField('IngestStartDate', 'Date de début', 6, 10),
    FieldDefinition.createDateField('IngestEndDate', 'Date de fin', 6, 10),
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
    ColumnDefinition.makeStaticColumn('obIdIn', 'Identifiant de la demande d\'entrée',
      undefined, () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Intitulé',

  (item) => (!!item.evDetData && JSON.parse(item.evDetData).EvDetailReq) ? JSON.parse(item.evDetData).EvDetailReq : '', undefined,
      () => ({'width': '200px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Statut',
      (item) => item.events[1], LogbookComponent.handleStatus,
      () => ({'width': '125px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Service versant',
      (item) => (!!item.evDetData && JSON.parse(item.evDetData).AgIfTrans) ? JSON.parse(item.evDetData).AgIfTrans : '', undefined,
      () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Contrat',
      (item) => (!!item.evDetData && JSON.parse(item.evDetData).ArchivalAgreement) ? JSON.parse(item.evDetData).ArchivalAgreement : '', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Début opération', DateService.handleDateWithTime,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Fin opération',
      (item) => item.events[1].evDateTime, DateService.handleDateWithTime,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeIconColumn('Bordereau', ['fa-download'],
      LogbookComponent.downloadManifest, LogbookComponent.displayManifestDownload,
      () => ({'width': '100px'}), this.ingestUtilsService, false),
    ColumnDefinition.makeIconColumn('AR', ['fa-download'],
      LogbookComponent.downloadReports, LogbookComponent.displayReportDownload,
      () => ({'width': '50px'}), this.ingestUtilsService, false)
  ];
  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evIdProc', 'Identifiant de l\'entrée', undefined,
      () => ({'width': '325px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Profil d\'archivage',
      (item) => !!item.ArchivalProfile ? item.ArchivalProfile : '', undefined,
      () => ({'width': '125px'}), false),
    ColumnDefinition.makeStaticColumn('EvDateTimeReq', 'Date', DateService.handleDate,
      () => ({'width': '100px'}), false),
    ColumnDefinition.makeStaticColumn('ServiceLevel', 'Niveau de service', undefined,
      () => ({'width': '125px'}), false),
    ColumnDefinition.makeStaticColumn('Signature', 'Signature', undefined,
      () => ({'width': '125px'}), false)
  ];

  constructor(public logbookService: LogbookService, private ingestUtilsService: IngestUtilsService,
              public titleService: Title, public breadcrumbService: BreadcrumbService, public archiveUnitHelper: ArchiveUnitHelper) {
    super('Suivi des opérations d\'entrée', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.searchForm = this.preSearchFunction({}).request;
    this.logbookService.getResults(this.searchForm, 0).subscribe(
      data => {
        this.response = data;
      },
      error => console.log('Error - ', this.response));
  }

  // TODO Move me in some utils class ?
  static handleStatus(event): string {
    let status = event.outcome.toUpperCase();
    if (event.evType === 'PROCESS_SIP_UNITARY' || event.evType === 'FILINGSCHEME') {
      switch (status) {
        case 'OK':
          return 'Succès';
        case 'STARTED':
          return 'En cours';
        case 'KO':
        case 'FATAL':
          return 'Erreur';
        default:
          return 'Avertissement';
      }
    } else {
      if (status === 'KO' || status === 'FATAL') {
        return 'Erreur';
      } else {
        return 'En cours';
      }
    }
  }

  static displayManifestDownload(item): boolean {
    return (item.events[1].outcome.toUpperCase() === 'OK' || item.events[1].outcome.toUpperCase() === 'WARNING')
        && (item.events[1].evType === 'PROCESS_SIP_UNITARY' || item.events[1].evType === 'FILINGSCHEME');
  }

  static displayReportDownload(item): boolean {
    return item.events[1].evType === 'PROCESS_SIP_UNITARY' || item.events[1].evType === 'FILINGSCHEME';
  }

  // TODO Move me in some utils class ?
  static downloadManifest(item, ingestUtilsService) {
    ingestUtilsService.downloadObject(item.evIdProc, 'manifests');
  }

  // TODO Move me in some utils class ?
  static downloadReports(item, ingestUtilsService) {
    ingestUtilsService.downloadObject(item.evIdProc, 'archivetransferreply');
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    request.INGEST = 'all';
    if (request.evId) {
      request = {
        'INGEST': 'all',
        'evId': request.evId
      };
    } else {
      for (let i in request) {
        if (!request[i] || request[i] === '') {
          delete request[i];
        }
      }
      if (request.evType) {
        if (typeof request.evType == 'object') {
          if (request.evType.length === 1) {
            request.evType = request.evType[0];
          } else {
            delete request.evType;
          }
        }
      }
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

}
