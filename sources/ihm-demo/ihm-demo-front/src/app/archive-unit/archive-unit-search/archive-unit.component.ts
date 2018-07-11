import { Component, EventEmitter } from '@angular/core';
import { PageComponent } from '../../common/page/page-component';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../../common/breadcrumb.service';
import { ArchiveUnitService } from '../archive-unit.service';
import { FieldDefinition } from '../../common/search/field-definition';
import { Preresult } from '../../common/preresult';
import { ColumnDefinition } from '../../common/generic-table/column-definition';
import { VitamResponse } from '../../common/utils/response';
import { ArchiveUnitHelper } from '../archive-unit.helper';
import { Router } from '@angular/router';
import { DateService } from '../../common/utils/date.service';
import {MySelectionService} from "../../my-selection/my-selection.service";
import {ResourcesService} from "../../common/resources.service";

const breadcrumb: BreadcrumbElement[] = [
    { label: 'Recherche', routerLink: '' },
    { label: 'Recherche d\'archives', routerLink: 'search/archiveUnit' }
];

@Component({
    selector: 'vitam-archive-unit',
    templateUrl: './archive-unit.component.html',
    styleUrls: ['./archive-unit.component.css']
})
export class ArchiveUnitComponent extends PageComponent {
    public response: VitamResponse;
    public searchRequest: any = {};
    public searchForm: any = {};
    advancedMode = false;
    disabledFacet = false;
    public archiveUnitFields = [
        new FieldDefinition('titleCriteria', 'Intitulé ou description', 12, 4)
    ];
    public advancedSearchFields = [
        new FieldDefinition('title', 'Intitulé', 4, 12),
        new FieldDefinition('description', 'Description', 4, 12),
        FieldDefinition.createIdField('id', 'Identifiant', 4, 12),
        FieldDefinition.createDateField('startDate', 'Date de début', 4, 12),
        FieldDefinition.createDateField('endDate', 'Date de fin', 4, 12),
        new FieldDefinition('originatingagencies', 'Service producteur de l\'entrée', 4, 12),
        FieldDefinition.createDateField('appDateSup', 'Date d\'échéance de la règle de communicabilité', 4, 12),
        FieldDefinition.createSelectField('appFinalAction', 'Sort final de la règle de communicabilité',
            'Sort Final', this.archiveUnitHelper.finalActionSelector.AppraisalRule, 4, 12)
    ];

    public columns = [
        ColumnDefinition.makeStaticColumn('#id', 'Identifiant', undefined,
            () => ({ 'width': '325px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Intitulé', this.archiveUnitHelper.getTitle, undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeStaticColumn('#unitType', 'Type', this.archiveUnitHelper.transformType,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeStaticColumn('#originating_agency', 'Service producteur', undefined,
            () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus ancienne', this.archiveUnitHelper.getStartDate, DateService.handleDate,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeSpecialValueColumn('Date la plus récente', this.archiveUnitHelper.getEndDate, DateService.handleDate,
            () => ({ 'width': '100px' }), false),
        ColumnDefinition.makeSpecialIconColumn('Objet(s) disponible(s)',
            (data) => data['#object'] ? ['fa-check'] : ['fa-close greyColor'], () => ({ 'width': '100px' }), null, null, false),
        ColumnDefinition.makeIconColumn('Cycle de vie', ['fa-pie-chart'], (item) => this.routeToLFC(item),
            () => true, () => ({ 'width': '50px' }), null, false),
        ColumnDefinition.makeSpecialIconColumn('Ajout au panier', ArchiveUnitComponent.getBasketIcons, () => {},
          (item, service, icon) => {
            switch(icon) {
            case 'fa-file':
              // TODO (add services)
              this.selectionService.addToSelection(false, [item['#id']], this.resourceService.getTenant());
              break;
            case 'fa-sitemap':
              this.selectionService.addToSelection(true, [item['#id']], this.resourceService.getTenant());
              break;
            case 'fa-archive':
              // FIXME: Think about change that in order to set opi and not all ids
              this.selectionService.getIdsToSelect(true, item['#opi']).subscribe(
                (response) => {
                  const ids: string[] = response.$results.reduce(
                    (x, y) => {
                      x.push(y['#id']);
                      return x;
                    }, []);
                  this.selectionService.addToSelection(false, ids, this.resourceService.getTenant());
                }, () => {
                  console.log('Error while get archive from opi')
                }
              );

              this.selectionService.addToSelection(false, [item['#id']], this.resourceService.getTenant());
              break;
            default:
              console.log('Error ? ');
            }
      }, null, false, null, ArchiveUnitComponent.getBasketIconsLabel)
    ];

    static getBasketIconsLabel(icon): string {
        switch(icon) {
          case 'fa-file': return 'Unité archivistique seule';
          case 'fa-sitemap': return 'Unitié archivistique et sa déscendance';
          case 'fa-archive': return 'Unité archivistique et son entrée';
          default: return '';
        }
    }

    static getBasketIcons(): string[] {
        return ['fa-file', 'fa-sitemap', 'fa-archive'];
    }

    public extraColumns = [];

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
        criteriaSearch.projection_opi = '#opi';
        criteriaSearch.projection_unitType = '#unittype';
        criteriaSearch.projection_title = 'Title';
        criteriaSearch.projection_titlefr = 'Title_.fr';
        criteriaSearch.projection_object = '#object';
        criteriaSearch.orderby = { field: 'TransactedDate', sortType: 'ASC' };

        return criteriaSearch;
    }

    constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, public service: ArchiveUnitService,
                public archiveUnitHelper: ArchiveUnitHelper, private router: Router, private selectionService: MySelectionService,
                private resourceService: ResourcesService) {
        super('Recherche d\'archives', breadcrumb, titleService, breadcrumbService);
    }

    pageOnInit() {
        if (ArchiveUnitService.getInputRequest() && ArchiveUnitService.getInputRequest().originatingagencies) {
            const form = ArchiveUnitComponent.addCriteriaProjection({});
            form.originatingagencies = ArchiveUnitService.getInputRequest().originatingagencies;
            form.isAdvancedSearchFlag = 'Yes';
            this.service.getResults(form, 0).subscribe(
                (response) => {
                    this.response = response;
                    this.advancedMode = true;
                    // FIXME add 'originatingagencies' value in search form
                },
                (error) => console.log('Error: ', error)
            );
            delete ArchiveUnitService.getInputRequest().originatingagencies;
        }
    }

    routeToLFC(item) {
        this.router.navigate(['search/archiveUnit/' + item['#id'] + '/unitlifecycle']);
    }

    public preSearchFunction(request, advancedMode): Preresult {
        const criteriaSearch: any = {}; // TODO Type me !
        const preResult = new Preresult();
        preResult.searchProcessSkip = false;

        if (!!request.requestFacet) {
            criteriaSearch.requestFacet = request.requestFacet;
        }

        if (advancedMode) {
            if (request.id) {
                criteriaSearch.id = request.id;
            } else {
                if (request.title) { criteriaSearch.Title = request.title; }
                if (request.description) { criteriaSearch.Description = request.description; }
                if (request.originatingagencies) { criteriaSearch.originatingagencies = request.originatingagencies; }

                let isStartDate = request.startDate;
                let isEndDate = request.endDate;
                if (isStartDate && isEndDate) {
                    if (request.startDate > request.endDate) {
                        preResult.searchProcessError = 'La date de début doit être antérieure à la date de fin.';
                        return preResult;
                    }
                    criteriaSearch.StartDate = request.startDate;
                    criteriaSearch.EndDate = request.endDate;
                    criteriaSearch.EndDate.setDate(criteriaSearch.EndDate.getDate() + 1)
                } else if (isStartDate || isEndDate) {
                    preResult.searchProcessError = 'Une date de début et une date de fin doivent être indiquées.';
                    return preResult;
                }

                if (request.appDateSup) {
                    criteriaSearch.AppDateSupp = request.appDateSup;
                    criteriaSearch.AppDateSupp.setDate(criteriaSearch.AppDateSupp.getDate() + 1)
                }

                if (request.appFinalAction) {
                    criteriaSearch.AppFinalAction = request.appFinalAction;
                }
            }

            if (criteriaSearch.id || criteriaSearch.Title || criteriaSearch.Description || criteriaSearch.StartDate
                || criteriaSearch.EndDate || criteriaSearch.originatingagencies
                || criteriaSearch.AppDateSupp || criteriaSearch.AppFinalAction) {
                if (!!request.facets) {
                    criteriaSearch.facets = request.facets;
                }
                ArchiveUnitComponent.addCriteriaProjection(criteriaSearch);
                criteriaSearch.isAdvancedSearchFlag = 'Yes';
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
                if (!!request.facets) {
                    criteriaSearch.facets = request.facets;
                }
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

    public initialSearch(service: any, responseEvent: EventEmitter<any>, form: any, offset) {
        service.getResults(form, offset).subscribe(
            (response) => {
                responseEvent.emit({ response: response, form: form });
            },
            (error) => console.log('Error: ', error)
        );
    }

    onNotify(event) {
        this.response = event.response;
        this.searchForm = event.form;
    }

    onChangedSearchRequest(searchRequest) {
        this.searchRequest = searchRequest;
    }

    /**
     * clear results.
     */
    onClear() {
        delete this.response;
    }

    public paginationSearch(service: any, offset) {
        return service.getResults(this.searchForm, offset);
    }

    // FIXME: Unused method ?
    public onClearPressed() {
        delete this.response;
    }

    onChangedSearchMode(searchMode) {
        this.advancedMode = searchMode;
    }

    onDisabledFiled(isToDisable) {
        this.disabledFacet = isToDisable;
    }

}
