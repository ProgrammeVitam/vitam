import {Component} from '@angular/core';
import {PageComponent} from "../../../common/page/page-component";
import {BreadcrumbElement, BreadcrumbService} from "../../../common/breadcrumb.service";
import {ActivatedRoute} from "@angular/router";
import {Title} from "@angular/platform-browser";
import {ColumnDefinition} from "../../../common/generic-table/column-definition";
import {LogbookOperationComponent} from "../../../admin/logbook-operation/logbook-operation.component";
import {LogbookService} from "../../logbook.service";
import {ArchiveUnitHelper} from "../../../archive-unit/archive-unit.helper";
import {VitamResponse} from "../../../common/utils/response";
import {DateService} from "../../../common/utils/date.service";
import {ErrorService} from "../../../common/error.service";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Entrée', routerLink: ''},
  {label: 'Suivi des opérations d\'entrée', routerLink: 'ingest/logbook'},
  {label: 'Détail d\'une opération d\'entrée', routerLink: ''}
];

@Component({
  selector: 'vitam-logbook-details',
  templateUrl: './logbook-details.component.html',
  styleUrls: ['./logbook-details.component.css']
})
export class LogbookDetailsComponent extends PageComponent {
  id: string;
  public response: VitamResponse;

  public columns = [
    ColumnDefinition.makeStaticColumn('evTypeProc', 'Catégorie d\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evType', 'Opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('rightsStatementIdentifier', 'Contrat associé', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date de début',
      DateService.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date de fin', (item) => item.events[item.events.length - 1].evDateTime,
      DateService.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Statut', LogbookOperationComponent.getOperationStatus,
      undefined, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialValueColumn('Message', (item) => item.events[item.events.length - 1].outMessg,
      undefined, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agIdExt', 'Identifiants des agents externes', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false)
  ];

  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evId', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evDetData', 'Informations sur l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agId', 'Acteur(s) internes', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('agIdApp', 'Identifiant de l\'application demandée', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('evIdReq', 'Numéro de transaction', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('obId', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeSpecialIconColumn('Rapport', (item) => item.evTypeProc.toUpperCase() === 'INGEST' ? ['fa-download'] : [],
      () => ({
        'width': '75px',
        'overflow-wrap': 'break-word'
      }), LogbookOperationComponent.downloadReports, this.logbookService, false)
  ];

  constructor(private route: ActivatedRoute, public logbookService: LogbookService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              public archiveUnitHelper: ArchiveUnitHelper, private errorService: ErrorService) {
    super('Détail d\'une opération d\'entrée', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.params.subscribe(params => {
      this.id = params['id'];
      this.breadcrumb[this.breadcrumb.length - 1] = {
        label: 'Détail d\'une opération d\'entrée ' + this.id,
        routerLink: 'ingest/logbookOperation/' + this.id
      };
      this.breadcrumbService.changeState(this.breadcrumb);
      this.logbookService.getDetails(this.id).subscribe(
        (data) => {
          this.response = data;
        }, (error) => {
          this.errorService.handle404Error(error);
        }
      )
    });
  }

}
