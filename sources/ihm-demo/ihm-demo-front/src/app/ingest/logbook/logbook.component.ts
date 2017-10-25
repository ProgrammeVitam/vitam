import {Component, EventEmitter} from '@angular/core';
import {DatePipe} from '@angular/common';
import {ColumnDefinition} from '../../common/generic-table/column-definition';
import {LogbookService} from '../logbook.service';
import { IngestUtilsService } from '../../common/utils/ingest-utils.service';
import {FieldDefinition} from '../../common/search/field-definition';
import {Preresult} from '../../common/search/preresult';
import {VitamResponse} from "../../common/utils/response";
import {BreadcrumbService, BreadcrumbElement} from "../../common/breadcrumb.service";
import {Title} from '@angular/platform-browser';
import {PageComponent} from "../../common/page/page-component";
import {ArchiveUnitHelper} from "../../archive-unit/archive-unit.helper";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Entrée', routerLink: ''},
  {label: 'Suivi des opérations d\'entrée', routerLink: 'ingest/logbook'}
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
    FieldDefinition.createIdField('identifier', 'Identifiant', 12, 4)
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
    ColumnDefinition.makeSpecialValueColumn('Identifiant',
        (item) => item.events[0].obIdIn === null ? item.events[1].obIdIn : item.events[0].obIdIn, undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Intitulé',
        (item) => !!item.evDetData ? JSON.parse(item.evDetData).EvDetailReq : '', undefined,
        () => ({'width': '200px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Statut',
        (item) => item.events[1].outcome.toUpperCase(), LogbookComponent.handleStatus,
        () => ({'width': '125px'})),
    ColumnDefinition.makeSpecialValueColumn('Service versant',
        (item) => !!item.evDetData ? JSON.parse(item.evDetData).AgIfTrans : '', undefined,
        () => ({'width': '125px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Contrat',
        (item) => !!item.evDetData ? JSON.parse(item.evDetData).ArchivalAgreement : '', undefined,
        () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Début opération', this.archiveUnitHelper.handleDateWithTime,
        () => ({'width': '100px'})),
    ColumnDefinition.makeSpecialValueColumn('Fin opération',
        (item) => item.events[1].evDateTime, this.archiveUnitHelper.handleDateWithTime,
        () => ({'width': '100px'})),
    ColumnDefinition.makeIconColumn('Bordereau', ['fa-download'],
        LogbookComponent.downloadManifest, LogbookComponent.displayManifestDownload,
        () => ({'width': '100px'}), this.ingestUtilsService),
    ColumnDefinition.makeIconColumn('AR', ['fa-download'],
        LogbookComponent.downloadReports, LogbookComponent.displayReportDownload,
        () => ({'width': '50px'}), this.ingestUtilsService)
  ];
  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evIdProc', 'Identifiant de l\'entrée', undefined,
        () => ({'width': '325px'})),
    ColumnDefinition.makeStaticColumn('ArchivalProfile', 'Profil', undefined,
        () => ({'width': '125px'})),
    ColumnDefinition.makeStaticColumn('EvDateTimeReq', 'Date', this.archiveUnitHelper.handleDate,
        () => ({'width': '100px'})),
    ColumnDefinition.makeStaticColumn('ServiceLevel', 'Niveau de service', undefined,
        () => ({'width': '125px'})),
    ColumnDefinition.makeStaticColumn('Signature', 'Signature', undefined,
        () => ({'width': '125px'}))
  ];

  constructor(public logbookService: LogbookService, private ingestUtilsService: IngestUtilsService,
              public titleService: Title, public breadcrumbService: BreadcrumbService, public archiveUnitHelper: ArchiveUnitHelper) {
    super('Journaux d\'opérations d\'entrée', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.searchForm = this.preSearchFunction({}).request;
    this.logbookService.getResults(this.searchForm, 0).subscribe(
        data => {this.response = data;},
        error => console.log('Error - ', this.response));
  }

  // TODO Move me in some utils class ?
  static handleStatus(status): string {
    switch (status) {
      case 'OK': return 'Succès';
      case 'STARTED': return 'En cours';
      case 'KO': case 'FATAL': return 'Erreur';
      default: return 'Avertissement';
    }
  }

  static displayManifestDownload(item): boolean {
    return (item.events[1].outcome.toUpperCase() === 'OK' || item.events[1].outcome.toUpperCase() === 'WARNING')
        && item.events[0].outDetail === 'STP_INGEST_FINALISATION.OK';
  }

  static displayReportDownload(item): boolean {
    return item.events[1].outcome.toUpperCase() !== 'STARTED' && item.events[0].outDetail === 'STP_INGEST_FINALISATION.OK';
  }

  // TODO Move me in some utils class ?
  static downloadManifest(item, ingestUtilsService) {
    ingestUtilsService.downloadObject(item.evIdProc, 'manifests');
  }

  // TODO Move me in some utils class ?
  static downloadReports(item, ingestUtilsService) {
    ingestUtilsService.downloadObject(item.evIdProc, 'reports');
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    request.INGEST = 'all';
    if (request.obIdIn === '') {
      delete request.obIdIn;
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
