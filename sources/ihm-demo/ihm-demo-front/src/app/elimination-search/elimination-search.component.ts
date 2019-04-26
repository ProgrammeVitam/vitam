import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { PageComponent } from '../common/page/page-component';
import { Title } from '@angular/platform-browser';
import { BreadcrumbElement, BreadcrumbService } from '../common/breadcrumb.service';
import { FieldDefinition } from '../common/search/field-definition';
import { ColumnDefinition } from '../common/generic-table/column-definition';
import { ArchiveUnitHelper } from '../archive-unit/archive-unit.helper';
import { DateService } from '../common/utils/date.service';
import { Preresult } from '../common/preresult';
import { VitamResponse } from '../common/utils/response';
import { ArchiveUnitService } from '../archive-unit/archive-unit.service';
import { ReferentialsService } from '../referentials/referentials.service';
import { FacetDefinition } from '../common/facets/facet';
import { MySelectionService } from '../my-selection/my-selection.service';
import { Router } from '@angular/router';

const breadcrumb: BreadcrumbElement[] = [
  { label: 'Gestion des archives', routerLink: '' },
  { label: 'Résultats d\'analyse d\'élimination ', routerLink: 'search/archiveUnit' }
];

@Component({
  selector: 'vitam-elimination-search',
  templateUrl: './elimination-search.component.html',
  styleUrls: ['./elimination-search.component.css']
})
export class EliminationSearchComponent extends PageComponent {
  searchRequest: any = {};
  response: VitamResponse;
  searchForm: any = {};
  disabledFacet = false;

  searchFields: FieldDefinition[] = [
    new FieldDefinition('EliminationOperationId', 'Opération d\'élimination', 4, 12),
    new FieldDefinition('title', 'Intitulé', 4, 12),
    new FieldDefinition('description', 'Description', 4, 12),
    new FieldDefinition('documentType', 'Profil d\'unités archivistiques', 4, 12)
  ];

  facetDefinition: FacetDefinition[] = [
    FacetDefinition.makeTermFacetDefinition('Services producteurs éliminables', 'DestroyableOriginatingAgenciesFacet', '#elimination.DestroyableOriginatingAgencies'),
    FacetDefinition.makeTermFacetDefinition('Services producteurs non éliminables', 'NonDestroyableOriginatingAgenciesFacet', '#elimination.NonDestroyableOriginatingAgencies'),
    FacetDefinition.makeTermFacetDefinition('Statut global d\'élimination', 'EliminationGlobalStatusFacet', '#elimination.GlobalStatus'),
    FacetDefinition.makeTermFacetDefinition('Informations étendues d\'élimination', 'EliminationExtendedInfoTypeFacet', '#elimination.ExtendedInfo.ExtendedInfoType'),
    FacetDefinition.makeTermFacetDefinition('Niveau de description', 'DescriptionLevelFacet', 'DescriptionLevel'),
    FacetDefinition.makeDateFacetDefinition('Date de début', 'StartDateFacet', 'StartDate', 800, new Date().getFullYear()),
    FacetDefinition.makeDateFacetDefinition('Date de fin', 'EndDateFacet', 'EndDate', 800, new Date().getFullYear()),
  ];

  columns: ColumnDefinition[] = [
    ColumnDefinition.makeStaticColumn('#id', 'Identifiant', undefined,
      () => ({ 'width': '325px', 'overflow-wrap': 'break-word' }), false),
    ColumnDefinition.makeSpecialValueColumn('Intitulé', this.archiveUnitHelper.getTitle, undefined,
      () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
    ColumnDefinition.makeStaticColumn('#originating_agency', 'Service producteur', undefined,
      () => ({ 'width': '200px', 'overflow-wrap': 'break-word' }), false),
    ColumnDefinition.makeStaticColumn('DescriptionLevel', 'Niveau de description',
      descLevel => this.archiveUnitHelper.selectionOptions.DescriptionLevel.find(x => x.value === descLevel).label,
      () => ({ 'width': '150px', 'overflow-wrap': 'break-word' }), false),
    ColumnDefinition.makeSpecialValueColumn('Règle d\'utilité administrative',
      (item, column: ColumnDefinition) => {
        if (item.RuleNameFromRuleReferentielForFront) return item.RuleNameFromRuleReferentielForFront;
        if (!item['#management'] || !item['#management'].AppraisalRule || !item['#management'].AppraisalRule.Rules) return '';

        let rules = item['#management'].AppraisalRule.Rules;
        if (rules.length === 0) return '';
        if (rules.length > 1) return 'Plus d\'une règle définie';
        return rules[0].Rule;
      },
      undefined, () => ({ 'width': '150px'}), false),
    ColumnDefinition.makeSpecialValueColumn('Date la plus ancienne', this.archiveUnitHelper.getStartDate, DateService.handleDate,
      () => ({ 'width': '100px' }), false),
    ColumnDefinition.makeSpecialValueColumn('Date la plus récente', this.archiveUnitHelper.getEndDate, DateService.handleDate,
      () => ({ 'width': '100px' }), false),
  ];

  constructor(public titleService: Title, public breadcrumbService: BreadcrumbService, public archiveUnitHelper: ArchiveUnitHelper,
              public service: ArchiveUnitService, public referentialService: ReferentialsService,
              public selectionService: MySelectionService, private router: Router) {
    super('Résultats d\'analyse d\'élimination', breadcrumb, titleService, breadcrumbService);
  }

  pageOnInit() {

  }

  public preSearchFunction(request): Preresult {
    const criteriaSearch: any = {};
    const preResult = new Preresult();
    preResult.searchProcessSkip = false;

    if (!request.EliminationOperationId) {
      preResult.searchProcessError = 'Identifiant d\'élimination obligatoire. Veuillez entrer au moins un critère de recherche';
      return preResult;
    }

    criteriaSearch.EliminationOperationId = request.EliminationOperationId;

    if (request.documentType) { criteriaSearch.DocumentType = request.documentType; }
    if (request.description) { criteriaSearch.Description = request.description; }
    if (request.title) { criteriaSearch.Title = request.title; }

    // Diff between facets and requestFacets ?
    if (!!request.requestFacet) {
      criteriaSearch.requestFacet = request.requestFacet;
    }
    if (!!request.facets) {
      criteriaSearch.facets = request.facets;
    }

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
    criteriaSearch.projection_appraisalrules = '#management.AppraisalRule';
    criteriaSearch.projection_id = '#id';
    criteriaSearch.projection_opi = '#opi';
    criteriaSearch.projection_title = 'Title';
    criteriaSearch.projection_titlefr = 'Title_.fr';
    criteriaSearch.orderby = { field: 'TransactedDate', sortType: 'ASC' };

    criteriaSearch.isAdvancedSearchFlag = 'Yes';

    preResult.request = criteriaSearch;
    preResult.success = true;
    return preResult;
  }

  initialSearch = (service: any, responseEvent: EventEmitter<any>, form: any, offset) => {

    service.getResults(form, offset).subscribe(
      async (auResponse) => {
        for (let item of auResponse.$results) {
          if (!item['#elimination']) {
            item.RuleNameFromRuleReferentielForFront = '';
            continue;
          }

          let eliminations: any[] = item['#elimination'];
          let elimination = eliminations.find((x) => x.processId === this.searchForm.EliminationOperationId);
          if (!elimination) {
            item.RuleNameFromRuleReferentielForFront = '';
            continue;
          }

          let rule = elimination.RuleId;
          let response = await this.referentialService.getRuleById(rule).toPromise();

          if (response && response.$results && response.$results.length === 1) {
            item.RuleNameFromRuleReferentielForFront = response.$results[0].RuleValue;
          } else {
            item.RuleNameFromRuleReferentielForFront = rule;
          }
        }

        responseEvent.emit({ response: auResponse, form: form });
      }, (error) => console.log('Error: ', error)
    );

  };

  public paginationSearch(service: any, offset) {
    return service.getResults(this.searchForm, offset);
  }

  onNotify(event) {
    this.response = event.response;
    this.searchForm = event.form;
  }

  onChangedSearchRequest(searchRequest) {
    this.searchRequest = searchRequest;
  }

  addInFreshBasket() {
    this.selectionService.deleteAllFromBasket(this.searchForm.EliminationOperationId);
    this.selectionService.addToSelectionWithoutTenant(true, this.response.$results.map(x => x['#id']), this.searchForm.EliminationOperationId);
    // Go to basket
    this.router.navigate(['basket', this.searchForm.EliminationOperationId]);
  }


}
