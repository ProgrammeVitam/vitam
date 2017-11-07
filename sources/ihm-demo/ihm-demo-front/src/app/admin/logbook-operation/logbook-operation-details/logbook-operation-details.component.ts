import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {PageComponent} from "../../../common/page/page-component";
import {BreadcrumbElement, BreadcrumbService} from "../../../common/breadcrumb.service";
import {Title} from "@angular/platform-browser";
import {VitamResponse} from "../../../common/utils/response";
import {ColumnDefinition} from "../../../common/generic-table/column-definition";
import {LogbookOperationComponent} from "../logbook-operation.component";
import {LogbookService} from "../../../ingest/logbook.service";
import {ArchiveUnitHelper} from "../../../archive-unit/archive-unit.helper";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Admin', routerLink: ''},
  {label: 'Journal des opérations', routerLink: 'admin/logbookOperation'},
  {label: 'Détail d\'une opération', routerLink: 'admin/logbookOperation/:id'}
];

@Component({
  selector: 'vitam-logbook-operation-details',
  templateUrl: './logbook-operation-details.component.html',
  styleUrls: ['./logbook-operation-details.component.css']
})
export class LogbookOperationDetailsComponent extends PageComponent {
  id: string;
  public response: VitamResponse;

  public columns = [
    ColumnDefinition.makeStaticColumn('evTypeProc', 'Catégorie d\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evType', 'Opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agIdExt', 'Acteur de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('rightsStatementIdentifier', 'Contrat associé', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date de début',
      this.archiveUnitHelper.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Date de fin', (item) => item.events[item.events.length - 1].evDateTime,
      this.archiveUnitHelper.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Statut', (item) => item.events[item.events.length - 1].outcome,
      undefined, () => ({'width': '125px', 'overflow-wrap': 'break-word'})),

    ColumnDefinition.makeStaticColumn('evDetData', 'Informations complémentaires sur l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'}))
  ];

  public extraColumns = [
    ColumnDefinition.makeStaticColumn('evId', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agId', 'Acteur(s) internes', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('agIdApp', 'Identifiant de l\'application demandée', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('evIdReq', 'Numéro de transaction', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('obId', 'Identifiant de l\'opération', undefined,
      () => ({'width': '175px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Message', (item) => item.events[item.events.length - 1].outMessg,
      undefined, () => ({'width': '125px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialIconColumn('Rapport', LogbookOperationComponent.handleReports,
      () => ({'width': '75px', 'overflow-wrap': 'break-word'}),
      LogbookOperationComponent.downloadReports, this.logbookService)
  ];

  constructor(private route: ActivatedRoute, public logbookService: LogbookService,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              public archiveUnitHelper: ArchiveUnitHelper) {
    super('Détail d\'une opération', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.params.subscribe(params => {
      this.id = params['id'];
      this.breadcrumb[this.breadcrumb.length - 1] = {
        label: 'Détail d\'une opération ' + this.id,
        routerLink: 'admin/logbookOperation/' + this.id
      };
      this.breadcrumbService.changeState(this.breadcrumb);
      this.logbookService.getDetails(this.id).subscribe(
        (data) => {
          this.response = data;
        }
      )
    });
  }

}
