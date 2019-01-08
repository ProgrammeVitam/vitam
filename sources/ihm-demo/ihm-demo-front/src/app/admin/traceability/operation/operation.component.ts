import {Component, EventEmitter} from '@angular/core';
import {Title} from '@angular/platform-browser';


import {ColumnDefinition} from '../../../common/generic-table/column-definition';
import {LogbookService} from '../../../ingest/logbook.service';
import {FieldDefinition} from '../../../common/search/field-definition';
import {Preresult} from '../../../common/preresult';
import {VitamResponse} from '../../../common/utils/response';
import {BreadcrumbService, BreadcrumbElement} from '../../../common/breadcrumb.service';
import {PageComponent} from '../../../common/page/page-component';
import {DateService} from '../../../common/utils/date.service';
import {ArchiveUnitHelper} from '../../../archive-unit/archive-unit.helper';

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Administration', routerLink: ''},
  {label: 'Opérations de sécurisation', routerLink: ''}
];
const searchAttribut = ['TraceabilityEndDate', 'TraceabilityId', 'TraceabilityStartDate', 'evType'];

@Component({
  selector: 'vitam-operation',
  templateUrl: './operation.component.html',
  styleUrls: ['./operation.component.css']
})
export class OperationComponent extends PageComponent {
  public getClass = (item) => 'clickableDiv';

  public response: VitamResponse;
  public searchForm: any = {};
  public options = [
    {label : "--", value : ""},
    {label : "Journal des écritures", value : "STP_STORAGE_SECURISATION"},
    {label : "Journal des opérations", value : "STP_OP_SECURISATION"},
    {label : "Journaux de cycle de vie des unités archivistiques", value : "LOGBOOK_UNIT_LFC_TRACEABILITY"},
    {label : "Journaux de cycle de vie des groupes d'objets", value : "LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY"},
  ];
    
  public logbookData = [
    FieldDefinition.createIdField('TraceabilityId', 'Identifiant de l\'opération', 3, 12),
    FieldDefinition.createDateField('TraceabilityStartDate', 'Date de début', 3, 12),
    FieldDefinition.createDateField('TraceabilityEndDate', 'Date de fin', 3, 12),
    FieldDefinition.createSelectField('evType', 'Type de journal sécurisé', '', this.options, 3, 12)
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

    ColumnDefinition.makeSpecialValueColumn('Type de journal sécurisé',
      (item) => !!item.evDetData ? JSON.parse(item.evDetData).LogType : '',
        undefined, () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Statut',
      (item) => (item.events.length > 1 && item.events[1].outcome) ? item.events[1].outcome : '',
      undefined, () => ({'width': '75px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de l\'opération',
      (item) => item.evDateTime,
      DateService.handleDateWithTime, () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de début de sécurisation',
      (item) => !!item.evDetData ? JSON.parse(item.evDetData).StartDate : '',
        DateService.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de fin de sécurisation',
      (item) => !!item.evDetData ? JSON.parse(item.evDetData).EndDate : '',
        DateService.handleDateWithTime, () => ({'width': '125px'}), false),
    ColumnDefinition.makeSpecialIconColumn('Télécharger',
      (item) => ['fa-download'],
      () => ({'width': '125px', 'overflow-wrap': 'break-word'}), OperationComponent.downloadReports,
      this.logbookService, false)
  ];
  public extraColumns = [];

  constructor(public logbookService: LogbookService, public titleService: Title,
              public breadcrumbService: BreadcrumbService, public archiveUnitHelper: ArchiveUnitHelper) {
    super('Opérations de sécurisation', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.searchForm = this.preSearchFunction({}).request;
    this.logbookService.getResults(this.searchForm, 0).subscribe(
        data => {this.response = data;},
        error => console.log('Error - ', this.response));
  }

  static downloadReports(item, logbookService) {
    logbookService.downloadTraceabilityReport(item.evIdProc);
  }

  public preSearchFunction(request): Preresult {
    let preResult = new Preresult();
    request.EventType = 'traceability';
    request.TraceabilityOk = 'true';
    request.orderby = {"field":"evDateTime","sortType":"DESC"};
    for (let i of searchAttribut) {
      if (!request[i] || request[i] === '') {
        delete request[i];
      }
    }
    if (request.TraceabilityEndDate) {
      request.TraceabilityEndDate.setDate(request.TraceabilityEndDate.getDate() + 1);
    }
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
