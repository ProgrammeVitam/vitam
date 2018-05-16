import { Component, OnInit, Input, Output, ChangeDetectorRef, SimpleChanges, EventEmitter } from '@angular/core';
import { forEach } from '@angular/router/src/utils/collection';
import { Preresult } from '../../common/preresult';
import { FieldDefinition } from '../../common/search/field-definition';
declare var require: NodeRequire;

const LANGUAGES = require('../../data/languages.json');
const TITLE_QUERY_FIELD = 'Title_.';

@Component({
  selector: 'vitam-archive-unit-facet',
  templateUrl: './archive-unit-facet.component.html',
  styleUrls: ['./archive-unit-facet.component.css']
})

export class ArchiveUnitFacetComponent implements OnInit {
  @Input() data: any;
  @Input() label: string;
  @Input() advancedMode = false;
  @Input() public searchRequest: any = {};
  @Input() disabledFacet = false;
  @Input() public submitFunction: (service: any, emitter: EventEmitter<any>, request: any) => void;
  @Output() responseEvent: EventEmitter<any> = new EventEmitter<any>();
  facets: any[] = [];
  facetResults: any[] = [];
  facetDLResults: any[] = [];
  facetOAResults: any[] = [];
  selectedFacets: string[] = [];
  checked: boolean;
  preSearchReturn = new Preresult();
  size: number = 100;
  currentFullYear: number = new Date().getFullYear();
  rangeStartDate: number[] = [800, this.currentFullYear];
  rangeEndDate: number[] = [800, this.currentFullYear];
  facetLangTitleResults: string[] = [];
  facetLangResults: string[] = [];
  rangeMax = this.currentFullYear;
  facetSDResults: string[] = [];
  facetEDResults: string[] = [];
  rangeMin = 800;
  isCollapsed = true;
  mapFacetField: Map<String, String>;
  titleLangFacets: string[] = [];
  descriptionLangFacets: string[] = [];
  objectFacets: string[] = [];

  @Input() service: any;
  @Input() preSearch: (request: any, advancedMode?: boolean) => Preresult = (x) => x;

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit() {
    if (!this.mapFacetField || this.mapFacetField.size === 0) {
      this.mapFacetField = new Map()
        .set('DescriptionLevelFacet', 'DescriptionLevel')
        .set('OriginatingAgencyFacet', '#originating_agency')
        .set('LanguageFacet', 'Language')
        .set('StartDateFacet', 'StartDate')
        .set('endDateFacet', 'EndDate')
        .set('ObjectFacet', '#object');

      for (const item of LANGUAGES) {
        this.mapFacetField.set(item.id, TITLE_QUERY_FIELD + item.id);
        this.titleLangFacets.push(item.id);
      }
    }
  }

  ngOnChanges(changes: SimpleChanges) {

    if (!!this.data && this.data.$facetResults
      && this.data.$facetResults.length > 0) {
      this.facetResults = this.data.$facetResults;
      for (const facetResult of this.facetResults) {
        switch (facetResult.name) {
          case 'DescriptionLevelFacet': {
            this.facetDLResults = facetResult.buckets;
            break;
          }
          case 'OriginatingAgencyFacet': {
            this.facetOAResults = facetResult.buckets;
            break;
          }
          case 'LanguageTitleFacet': {
            this.facetLangTitleResults = facetResult.buckets;
            break;
          }
          case 'LanguageFacet': {
            this.facetLangResults = facetResult.buckets;
            break;
          }
          case 'StartDateFacet': {
            this.facetSDResults = facetResult.buckets;
            break;
          }
          case 'endDateFacet': {
            this.facetEDResults = facetResult.buckets;
            break;
          }
          case 'ObjectFacet': {
            this.objectFacets = facetResult.buckets;
            break;
          }
          default: {
            console.log('No match');
            break;
          }
        }
      }
    } else {
      delete this.facetResults;
      this.isCollapsed = true;
    }
    if (!this.advancedMode) {
      this.disabledFacet = false;
    }

    if (this.disabledFacet) {
      this.clearFacetResults();
      if (this.data && this.data.$facetResults) {
        delete this.data.$facetResults;
      }
    }
  }

  searchFacets() {
    const facets = this.facets = [];
    for (const facet of this.selectedFacets) {
      const facetCriteria: any = {};
      const dateRangeCriteria: any = {};
      switch (facet) {
        case 'DescriptionLevelFacet': {
          facetCriteria.name = 'DescriptionLevelFacet';
          facetCriteria.field = 'DescriptionLevel';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'OriginatingAgencyFacet': {
          facetCriteria.name = 'OriginatingAgencyFacet';
          facetCriteria.field = '#originating_agency';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'StartDateFacet': {
          facetCriteria.name = 'StartDateFacet';
          facetCriteria.field = 'StartDate';
          facetCriteria.format = 'yyyy';
          dateRangeCriteria.dateMin = this.rangeStartDate[0];
          dateRangeCriteria.dateMax = this.rangeStartDate[1];
          facetCriteria.ranges = new Array(dateRangeCriteria);
          facetCriteria.facetType = 'DATE_RANGE';
          facets.push(facetCriteria);
          break;
        }
        case 'endDateFacet': {
          facetCriteria.name = 'endDateFacet';
          facetCriteria.field = 'EndDate';
          facetCriteria.format = 'yyyy',
          dateRangeCriteria.dateMin = this.rangeEndDate[0];
          dateRangeCriteria.dateMax = this.rangeEndDate[1];
          facetCriteria.ranges = new Array(dateRangeCriteria);
          facetCriteria.facetType = 'DATE_RANGE';
          facets.push(facetCriteria);
          break;
        }
        case 'LanguageTitleFacet': {
          facetCriteria.name = 'LanguageTitleFacet';
          facetCriteria.facetType = 'FILTERS';
          facetCriteria.filters = this.getFiltersQuery(this.titleLangFacets);
          facets.push(facetCriteria);
          break;
        }
        case 'LanguageFacet': {
          facetCriteria.name = 'LanguageFacet';
          facetCriteria.field = 'Language';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'ObjectFacet': {
          facetCriteria.name = 'ObjectFacet';
          facetCriteria.facetType = 'FILTERS';
          const filtersQuery: any = [];
          filtersQuery.push({
            '$name': 'FacetWithObject',
            '$query':
              {
                '$exists': '#object'
              }
          });
          filtersQuery.push({
            '$name': 'FacetWithoutObject',
            '$query':
              {
                '$missing': '#object'
              }
          });
          facetCriteria.filters = filtersQuery;
          facets.push(facetCriteria);
          break;
        }
        default: {
          console.log('No matchs');
          break;
        }
      }
    }
    this.searchRequest.facets = this.facets;
    delete this.searchRequest.requestFacet;

    this.processPreSearch(this.searchRequest);
  }

  research(selectedFacetValue, facetField) {
    const facetSearchCriteria: any = {};
    if (facetField === 'LanguageTitleFacet') {
      const title = this.mapFacetField.get(selectedFacetValue);
      facetSearchCriteria.field = title;
      facetSearchCriteria.value = title;
      // "title" contains all we need. We don't need any different information for "value" on the back-end side.
    } else {
      facetSearchCriteria.field = this.mapFacetField.get(facetField);
      facetSearchCriteria.value = selectedFacetValue;
    }

    this.searchRequest.requestFacet = facetSearchCriteria;
    this.searchRequest.facets = [];
    this.isCollapsed = !this.isCollapsed;

    this.processPreSearch(this.searchRequest);
  }

  processPreSearch(request: any) {
    this.preSearchReturn = this.preSearch(this.searchRequest, this.advancedMode);
    if (this.preSearchReturn.success) {
      this.submitFunction(this.service, this.responseEvent, this.preSearchReturn.request);
    } else {
      this.preSearchReturn.searchProcessError = this.preSearchReturn.searchProcessError;
    }
  }

  getFiltersQuery(langFacets) {
    const filtersQuery: any = [];
    for (const item of langFacets) {
      filtersQuery.push({
        '$name': item,
        '$query':
          {
            '$exists': this.mapFacetField.get(item)
          }
      });
    }
    return filtersQuery;
  }

  clearFacets() {
    this.clearFacetResults();
    this.processPreSearch(this.searchRequest);
  }

  clearFacetResults() {
    this.selectedFacets = [];
    delete this.facets;
    delete this.facetDLResults;
    delete this.facetOAResults;
    delete this.facetSDResults;
    delete this.facetEDResults;
    delete this.facetLangTitleResults;
    delete this.facetLangResults;
    this.rangeStartDate = [800, this.currentFullYear];
    this.rangeEndDate = [800, this.currentFullYear];
    delete this.searchRequest.requestFacet;
  }

}
