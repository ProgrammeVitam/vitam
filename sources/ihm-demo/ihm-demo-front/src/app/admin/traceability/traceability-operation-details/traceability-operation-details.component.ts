import { Component } from '@angular/core';
import { ActivatedRoute, ParamMap } from "@angular/router";
import { Title } from "@angular/platform-browser";
import { BreadcrumbService } from "../../../common/breadcrumb.service";
import { PageComponent } from "../../../common/page/page-component";
import { TraceabilityOperationService } from "../traceability-operation.service";
import { ObjectsService } from "../../../common/utils/objects.service";
import { ColumnDefinition } from "../../../common/generic-table/column-definition";
import { LogbookService } from "../../../ingest/logbook.service";
import {DateService} from "../../../common/utils/date.service";

@Component({
  selector: 'vitam-traceability-operation-details',
  templateUrl: './traceability-operation-details.component.html',
  styleUrls: ['./traceability-operation-details.component.css']
})
export class TraceabilityOperationDetailsComponent extends PageComponent {
  reportGenerated = false;
  id: string;
  item: any;
  reportItems: any[];
  selectedCols: ColumnDefinition[] = [];
  firstItem = 0;
  nbRows = 25;
  displayedItems: any[];

  cols = [
    ColumnDefinition.makeStaticColumn('outDetail', 'Intitulé de l\'évènement', undefined,
      () => ({'width': '500px', 'overflow-wrap': 'break-word', 'text-align': 'left'}), false),
    ColumnDefinition.makeStaticColumn('evDateTime', 'Date',
      DateService.handleDateWithTime, () => ({'width': '125px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('outcome', 'Statut', undefined,
      () => ({'width': '100px', 'overflow-wrap': 'break-word'}), false),
    ColumnDefinition.makeStaticColumn('outMessg', 'Message', undefined,
      () => ({'width': '525px', 'overflow-wrap': 'break-word'}), false)
  ];

  columns = {
    'operation': [
      ColumnDefinition.makeStaticColumn('startDate', 'Date début', DateService.handleDateWithTime,
        () => ({'width': '250px'}), false),
      ColumnDefinition.makeStaticColumn('endDate', 'Date fin', DateService.handleDateWithTime,
        () => ({'width': '250px'}), false),
      ColumnDefinition.makeStaticColumn('numberOfElement', 'Nombre d\'opération', undefined,
        () => ({'width': '700px'}), false),
    ], 'file': [
      ColumnDefinition.makeStaticColumn('fileName', 'Nom fichier', undefined,
        () => ({'width': '750px'}), false),
      ColumnDefinition.makeStaticColumn('fileSize', 'Taille fichier', ObjectsService.computeSize,
        () => ({'width': '250px'}), false),
      ColumnDefinition.makeSpecialIconColumn('Télécharger', (item) =>  ['fa-download'],
        () => ({'width': '200px'}), this.downloadReport, this.logbookService,false)
    ], 'traceability': [
      ColumnDefinition.makeStaticColumn('digestAlgorithm', 'Algorithme de hashage', undefined,
        () => ({'width': '250px'}), false),
      ColumnDefinition.makeStaticColumn('genTime', 'Date tampon', DateService.handleDateWithTime,
        () => ({'width': '250px'}), false),
      ColumnDefinition.makeStaticColumn('signerCertIssuer', 'CA signature', undefined,
        () => ({'width': '700px', 'overflow-wrap': 'break-word'}), false)
    ]
  };

  constructor(public traceabilityService: TraceabilityOperationService, private route: ActivatedRoute,
              public titleService: Title, public breadcrumbService: BreadcrumbService,
              public logbookService: LogbookService) {
    super('Détail de l\'opération de sécurisation', [], titleService, breadcrumbService);
  }

  pageOnInit() {
    this.route.params
      .subscribe((params) => {
        this.id =  params['id'];
        let newBreadcrumb = [
          {label: 'Administration', routerLink: ''},
          {label: 'Opérations de sécurisation', routerLink: 'admin/traceabilityOperation'},
          {label: 'Détails de l\'opération ' + this.id, routerLink: ''}
        ];
        this.setBreadcrumb(newBreadcrumb);
        return [];
      });

    // call server
    this.traceabilityService.getDetails(this.id)
      .subscribe((data) => {
        // init this.items with [result]
        if (data) {
          this.extractData(data.$results[0]);
        }
      });
  }

  extractData(data) {
    let item = this.getEvDetData(data);
    this.traceabilityService.extractTimeStampInformation(item.timeStampToken)
      .subscribe(
        (response) => {
          item.genTime = response.genTime;
          item.signerCertIssuer = response.signerCertIssuer;
          this.item = item;
        });
  }

  getEvDetData(data) {
    let evDetData: any = {};
    let details;
    // Display operation details
    if (data.evDetData !=null) {
      details = JSON.parse(data.evDetData);
    } else {
      details = JSON.parse(data.events[data.events.length - 1].evDetData);
    }
    evDetData.logType = details.LogType;
    evDetData.startDate = details.StartDate;
    evDetData.endDate = details.EndDate;
    evDetData.numberOfElement = details.NumberOfElement;
    evDetData.digestAlgorithm = details.DigestAlgorithm;
    evDetData.fileName = details.FileName;
    evDetData.fileSize = details.Size;
    evDetData.hash = details.Hash;
    evDetData.timeStampToken = details.TimeStampToken;
    evDetData.evId=this.id;
    return evDetData;
  }

  splitCA(ca) {
    return ca.split(',');
  }

  runVerification() {
    this.reportGenerated = true;
    this.traceabilityService.checkTraceabilityOperation(this.id)
      .subscribe(
        (resp) => {
          let report = resp.$results[0];
          this.selectedCols = this.cols;

          if (!!report) {
            this.reportItems = [report].concat(report.events);
            this.displayedItems = this.reportItems.slice(this.firstItem, this.firstItem + this.nbRows);
          }
        }
      );
  }

  paginate(event) {
    this.firstItem = event.first;
    this.nbRows = event.rows;
    this.displayedItems = this.reportItems.slice(this.firstItem, this.firstItem + this.nbRows);
  }

  downloadReport(item,logbookService) {
    logbookService.downloadTraceabilityReport(item.evId);
  }


}
