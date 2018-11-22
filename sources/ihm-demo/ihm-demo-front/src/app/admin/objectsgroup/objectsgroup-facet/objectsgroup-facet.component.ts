import { Component, OnInit, Input, Output, ChangeDetectorRef, SimpleChanges, EventEmitter } from '@angular/core';
import { Preresult } from '../../../common/preresult';

@Component({
  selector: 'vitam-objectsgroup-facet',
  templateUrl: './objectsgroup-facet.component.html',
  styleUrls: ['./objectsgroup-facet.component.css']
})

export class ObjectsGroupFacetComponent implements OnInit {
  @Input() data: any;
  @Input() label: string;
  @Input() advancedMode = false;
  @Input() public searchRequest: any = {};
  @Input() disabledFacet = false;
  @Input() public submitFunction: (service: any, emitter: EventEmitter<any>, request: any) => void;
  @Output() responseEvent: EventEmitter<any> = new EventEmitter<any>();
  facets: any[] = [];
  facetResults: any[] = [];
  facetOAResults: any[] = [];
  facetFormatIdResults: string[] = [];
  facetUsageResults: string[] = [];
  selectedFacets: string[] = [];
  checked: boolean;
  preSearchReturn = new Preresult();
  size: number = 100;
  isCollapsed = true;
  mapFacetField: Map<String, String>;

  @Input() service: any;
  @Input() preSearch: (request: any, advancedMode?: boolean) => Preresult = (x) => x;

  constructor(private changeDetectorRef: ChangeDetectorRef) {
  }

  ngOnInit() {
    if (!this.mapFacetField || this.mapFacetField.size === 0) {
      this.mapFacetField = new Map()
        .set('OriginatingAgencyFacet', '#originating_agency')
        .set('FormatIdFacet', '#qualifiers.versions.FormatIdentification.FormatLitteral')
        .set('UsageFacet', '#qualifiers.versions.DataObjectVersion');
    }
  }

  ngOnChanges(changes: SimpleChanges) {

    if (!!this.data && this.data.$facetResults
      && this.data.$facetResults.length > 0) {
      this.facetResults = this.data.$facetResults;
      for (const facetResult of this.facetResults) {
        switch (facetResult.name) {
          case 'OriginatingAgencyFacet': {
            this.facetOAResults = facetResult.buckets;
            break;
          }
          case 'FormatIdFacet': {
            this.facetFormatIdResults = facetResult.buckets;
            break;
          }
          case 'UsageFacet': {
            this.facetUsageResults = facetResult.buckets;
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
        case 'OriginatingAgencyFacet': {
          facetCriteria.name = 'OriginatingAgencyFacet';
          facetCriteria.field = '#originating_agency';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'FormatIdFacet': {
          facetCriteria.name = 'FormatIdFacet';
          facetCriteria.field = '#qualifiers.versions.FormatIdentification.FormatLitteral';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
          facets.push(facetCriteria);
          break;
        }
        case 'UsageFacet': {
          facetCriteria.name = 'UsageFacet';
          facetCriteria.field = '#qualifiers.versions.DataObjectVersion';
          facetCriteria.size = this.size;
          facetCriteria.order = 'ASC';
          facetCriteria.facetType = 'TERMS';
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
    facetSearchCriteria.field = this.mapFacetField.get(facetField);
    facetSearchCriteria.value = selectedFacetValue;

    this.searchRequest.requestFacet = facetSearchCriteria;
    this.searchRequest.facets = [];
    this.isCollapsed = !this.isCollapsed;

    this.processPreSearch(this.searchRequest);
  }

  processPreSearch(request: any) {
    this.preSearchReturn = this.preSearch(this.searchRequest, this.advancedMode);
    if (this.preSearchReturn.success) {
      this.submitFunction(this.service, this.responseEvent, this.preSearchReturn.request);
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
    delete this.facetOAResults;;
    delete this.facetFormatIdResults;
    delete this.facetUsageResults;
    delete this.searchRequest.requestFacet;
  }
}