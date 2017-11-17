import { Component, EventEmitter } from '@angular/core';
import { PageComponent } from "../../common/page/page-component";
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ArchiveUnitService } from "../archive-unit.service";
import { FieldDefinition } from "../../common/search/field-definition";
import { Preresult } from "../../common/search/preresult";
import { ColumnDefinition } from "../../common/generic-table/column-definition";
import { VitamResponse } from "../../common/utils/response";
import { ArchiveUnitHelper } from "../archive-unit.helper";
import { Router } from "@angular/router";

const breadcrumb: BreadcrumbElement[] = [
  {label: 'Recherche', routerLink: ''},
  {label: 'Recherche d\'archives', routerLink: 'search/archiveUnit'}
];

@Component({
  selector: 'vitam-archive-unit',
  templateUrl: './archive-unit.component.html',
  styleUrls: ['./archive-unit.component.css']
})
export class ArchiveUnitComponent extends PageComponent {

  public response: VitamResponse;
  public searchForm: any = {};
  advancedMode = false;
  public archiveUnitFields = [
    new FieldDefinition('titleCriteria', 'Intitulé ou description', 12, 4)
  ];
  public advancedSearchFields = [
    new FieldDefinition('title', 'Intitulé', 4, 12),
    new FieldDefinition('description', 'Description', 4, 12),
    FieldDefinition.createIdField('id', 'Identifiant', 4, 12),
    FieldDefinition.createDateField('startDate', 'Date de début', 4, 12),
    FieldDefinition.createDateField('endDate', 'Date de fin', 4, 12),
    new FieldDefinition('originatingagencies', 'Service producteur de l\'entrée', 4, 12)
  ];

  public columns = [
    ColumnDefinition.makeStaticColumn('#id', 'Identifiant', undefined, () => ({'width': '325px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('Title', 'Intitulé', undefined, () => ({'width': '200px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeStaticColumn('#unitType', 'Type', this.archiveUnitHelper.transformType, () => ({'width': '100px'})),
    ColumnDefinition.makeStaticColumn('#originating_agency', 'Service producteur', undefined, () => ({'width': '200px', 'overflow-wrap': 'break-word'})),
    ColumnDefinition.makeSpecialValueColumn('Date de début', this.archiveUnitHelper.getStartDate, this.archiveUnitHelper.handleDate, () => ({'width': '100px'})),
    ColumnDefinition.makeSpecialValueColumn('Date de fin', this.archiveUnitHelper.getEndDate, this.archiveUnitHelper.handleDate, () => ({'width': '100px'})),
    ColumnDefinition.makeSpecialIconColumn('Objet(s) disponible(s)',
      (data) => data['#object']? ['fa-check']: ['fa-close greyColor'], () => ({'width': '100px'})),
    ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], (item) => this.routeToLFC(item), () => true, () => ({'width': '50px'}))
  ];
  public extraColumns = [
  ];

  constructor(public titleService:Title, public breadcrumbService:BreadcrumbService, public service: ArchiveUnitService,
              public archiveUnitHelper: ArchiveUnitHelper, private router : Router) {
    super('Recherche d\'archive', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {
    if (ArchiveUnitService.getInputRequest() && ArchiveUnitService.getInputRequest().originatingagencies) {
      let form = ArchiveUnitComponent.addCriteriaProjection({});
      let agencies = ArchiveUnitService.getInputRequest().originatingagencies;
      form.originatingagencies = agencies;
      form.isAdvancedSearchFlag = 'Yes';
      this.service.getResults(form, 0).subscribe(
        (response) => {
          this.response = response;
          this.advancedMode = true;
          //FIXME add 'originatingagencies' value in search form
        },
        (error) => console.log('Error: ', error)
      );
      delete ArchiveUnitService.getInputRequest().originatingagencies;
    }
  }

  routeToLFC(item) {
    this.router.navigate(['search/archiveUnit/' + item['#id'] + '/unitlifecycle']);
  }

  public preSearchFunction(request, advancedMode):Preresult {
    var criteriaSearch:any = {}; // TODO Type me !
    let preResult = new Preresult();
    preResult.searchProcessSkip = false;

    if (advancedMode) {
      if (request.id) {
        criteriaSearch.id = request.id;
      } else {
        if (request.title) criteriaSearch.Title = request.title;
        if (request.description) criteriaSearch.Description = request.description;
        if (request.originatingagencies) criteriaSearch.originatingagencies = request.originatingagencies;

        const isStartDate = request.startDate;
        const isEndDate = request.endDate;
        if (isStartDate && isEndDate) {
          if (request.startDate > request.endDate) {
            preResult.searchProcessError = 'La date de début doit être antérieure à la date de fin.';
            return preResult;
          }
          criteriaSearch.StartDate = request.startDate;
          criteriaSearch.EndDate = request.endDate;
        } else if (isStartDate || isEndDate) {
          preResult.searchProcessError = 'Une date de début et une date de fin doivent être indiquées.';
          return preResult;
        }
      }

      if (criteriaSearch.id || criteriaSearch.Title || criteriaSearch.Description || criteriaSearch.StartDate
        || criteriaSearch.EndDate || criteriaSearch.originatingagencies) {
        ArchiveUnitComponent.addCriteriaProjection(criteriaSearch);
        criteriaSearch.isAdvancedSearchFlag = "Yes";
        preResult.request = criteriaSearch;
        preResult.success = true;
        return preResult;
      } else {
        preResult.searchProcessError = 'Aucun résultat. Veuillez entrer au moins un critère de recherche';
        return preResult;
      }

    } else {
      if (!!request.titleCriteria) {
        criteriaSearch.titleAndDescription = request.titleCriteria;
        ArchiveUnitComponent.addCriteriaProjection(criteriaSearch);
        criteriaSearch.isAdvancedSearchFlag = 'No';

        preResult.request = criteriaSearch;
        preResult.success = true;
        return preResult;
      } else {
        preResult.searchProcessError = 'Aucun résultat. Veuillez entrer au moins un critère de recherche';
        return preResult;
      }
    }
  }

  static addCriteriaProjection(criteriaSearch) {
    criteriaSearch.projection_startdate = 'StartDate';
    criteriaSearch.projection_enddate = 'EndDate';
    criteriaSearch.projection_createddate = 'CreatedDate';
    criteriaSearch.projection_acquireddate = 'AcquiredDate';
    criteriaSearch.projection_sentdate = 'SentDate';
    criteriaSearch.projection_receiveddate = 'ReceivedDate';
    criteriaSearch.projection_registereddate = 'RegisteredDate';
    criteriaSearch.projection_transactdate = 'TransactedDate';
    criteriaSearch.projection_descriptionlevel = 'DescriptionLevel';
    criteriaSearch.projection_originatingagencies = '#originating_agency';
    criteriaSearch.projection_id = '#id';
    criteriaSearch.projection_unitType = '#unittype';
    criteriaSearch.projection_title = 'Title';
    criteriaSearch.projection_object = '#object';
    criteriaSearch.orderby = { field: 'TransactedDate', sortType: 'ASC' };
    return criteriaSearch;
  }

  public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
    service.getResults(form, offset).subscribe(
      (response) => {
        responseEvent.emit({response: response, form: form});
      },
      (error) => console.log('Error: ', error)
    );
  }

  onNotify(event) {
    this.response = event.response;
    this.searchForm = event.form;
  }

  /**
   * clear results.
   */
  onClear(event) {
    console.log('response deletion !');
    delete this.response;
  }

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }
  
  public onClearPressed() {
    delete this.response;
  }

}
